package com.shadowchat.socket;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class TCPChildSocket extends Thread{
	
	public Socket socket = null;
	OutputStream out;
	InputStream in;
	ArrayList<TCPChildInterface> adapters = new ArrayList<TCPChildInterface>();
	static Queue<byte[]> sendingQueue = new LinkedList<byte[]>();
	public static ExecutorService pool = Executors.newFixedThreadPool(2);

	private TCPClient client;
	
	private boolean outgoing = false;


    boolean useSSL = false;
    boolean trustAll = false;
    String sslCertFile = null;
    String sslCertPass = null;
	
	public TCPChildSocket(int uid, Socket socket){
    	this.socket = socket;
    	this.client = new TCPClient(uid, socket.getInetAddress().getHostName(), socket.getPort());
    	this.outgoing = false;
    }
	
	public TCPChildSocket(int uid, String ip, int port){
		this.client = new TCPClient(uid, ip, port);
		this.outgoing = true;
	}

    public void enableSSL(boolean trustAll, String certFile, String certPass){
        this.useSSL = true;
        this.trustAll = trustAll;
        this.sslCertFile = certFile;
        this.sslCertPass = certPass;
    }
	
	public boolean isOutgoingConnection()
	{
		return outgoing;
	}
	
	public TCPClient getClient()
	{
		return client;
	}
	
	public void attach(TCPChildInterface adapter)
	{
		this.adapters.add(adapter);
	}
	
	public void detach(TCPChildInterface adapter)
	{
		this.adapters.remove(adapter);
	}

	public synchronized boolean send(byte[] msg){
		try{
            if(msg.length != 0) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                stream.write(intToBytes(msg.length));
                stream.write(msg);
                out.write(stream.toByteArray());
                stream.close();
            }
		}
		catch(Exception ioException){
			return false;
		}
		return true;
	}
	
	public boolean ping(){
		try{
			socket.sendUrgentData(0);
		}
		catch(IOException ioException){
			return false;
		}
		return true;
	}

    public void close() throws IOException{
        socket.close();
    }

	public void run() {
		try{
			if(socket == null){
                if(useSSL){
                    SSLSocket sslSocket;
                    if(sslCertFile != null && sslCertPass != null) {
                        char[] passphrase = sslCertPass.toCharArray();
                        KeyStore ksTrust = KeyStore.getInstance("BKS");
                        ksTrust.load(new FileInputStream(new File(sslCertFile)), passphrase);
                        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                        tmf.init(ksTrust);
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
                        sslSocket = (SSLSocket)sslContext.getSocketFactory().createSocket(InetAddress.getByName(client.ip), client.port);
                    }else{
                        if(trustAll){
                            TrustManager tm = new X509TrustManager() {
                                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                                }

                                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                                }

                                public X509Certificate[] getAcceptedIssuers() {
                                    return null;
                                }
                            };
                            SSLContext sslContext = SSLContext.getInstance("TLS");
                            sslContext.init(null, new TrustManager[] { tm }, null);
                            sslSocket = (SSLSocket)sslContext.getSocketFactory().createSocket(InetAddress.getByName(client.ip), client.port);
                        }else {
                            SocketFactory sf = SSLSocketFactory.getDefault();
                            sslSocket = (SSLSocket) sf.createSocket(InetAddress.getByName(client.ip), client.port);
                        }
                    }
                    socket = sslSocket;
                    sslSocket.startHandshake();
                }else{
                    socket = new Socket(InetAddress.getByName(client.ip), client.port);
                }

                if(socket.isClosed())
                    return;
				socket.setTcpNoDelay(true);
				socket.setKeepAlive(true);
				socket.setSoTimeout(60000);
			}
			
	    	out = socket.getOutputStream();
	    	in = socket.getInputStream();
	    	
	    	for(int i = 0; i < adapters.size(); i++)
				adapters.get(i).onConnected(client, this);

	    	int count = 0;
	    	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			do
			{
				if(count >= 3)
					break;
				int nRead;
				byte[] data = new byte[4096];
				
				int size = 0;
				
				while ((nRead = in.read(data, 0, data.length)) != -1) {	
					count = 0;	
					for(int i = 0; i < nRead; i++)
					{
						if(size == 0)
						{
							byte[] head = new byte[4];
							head[0] = data[i];
							head[1] = data[i+1];
							head[2] = data[i+2];
							head[3] = data[i+3];
							i += 3;
							size = bytesToInt(head);
							
						}else{
							buffer.write(data[i]);
							size--;
							
							if(size == 0){
								buffer.flush();								
								final byte[] bb = buffer.toByteArray();
								class NonBlockingThread implements Runnable{
									public void run(){
										for(int j =0; j < adapters.size();j++)
											adapters.get(j).onReceive(client, bb);
									}
								} 
								pool.execute(new NonBlockingThread());
								buffer.reset();
							}
						}
					}
				}

				count++;	
			}
			while(true);
			buffer.close();
		
    	}
		catch(Exception ex){
            ex.printStackTrace();
			for(int i =0; i < adapters.size();i++)
				adapters.get(i).onError(client, ex);
		}
    	finally
    	{
			try
			{
				in.close();
				out.close();			
				for(int i =0; i < adapters.size();i++)
					adapters.get(i).onDisconnected(client);
				socket.close();
			}
			catch(Exception exception){
				for(int i =0; i < adapters.size();i++)
					adapters.get(i).onError(client, exception);
			}
		}
    }
	
	static byte[] intToBytes(int i ) {
	    ByteBuffer bb = ByteBuffer.allocate(4); 
	    bb.putInt(i); 
	    return bb.array();
	}

    static int bytesToInt(byte[] b){
		ByteBuffer bb = ByteBuffer.wrap(b);
		return bb.getInt();
	}
}
