package com.shadowchat.socket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

public class TCPSocket {
	
	public class TCPChildListener extends TCPChildAdapter
	{
		public void onConnected(TCPClient client, TCPChildSocket socket)
		{
			clients.put(client.uid, socket);
			for(int i = 0; i < adapters.size(); i++)
				adapters.get(i).onClientConnected(client);
		}
		
		public void onReceive(TCPClient client, byte[] data)
		{
			for(int i =0; i < adapters.size();i++)
				adapters.get(i).onReceive(client, data);
		}

		public void onDisconnected(TCPClient client)
		{
			clients.remove(client.uid);
			for(int i = 0; i < adapters.size(); i++)
				adapters.get(i).onClientDisconnected(client);
		}		
		
		public void onClientError(TCPClient client, Exception ex)
		{
			for(int i = 0; i < adapters.size(); i++)
				adapters.get(i).onClientError(client, ex);
		}

        public void onError(TCPClient client, Exception ex)
        {
            for(int i = 0; i < adapters.size(); i++)
                adapters.get(i).onError(ex);
        }
	}
	
	ArrayList<TCPListenInterface> adapters = new ArrayList<TCPListenInterface>();
	Hashtable<Integer, TCPChildSocket> clients = new Hashtable<Integer, TCPChildSocket>();
	
	private TCPListenSocket listenSocket = null;
	private TCPChildSocket childSocket = null;
	
	private int currentId = 0;
	private boolean isListenServer = false;
	
	public TCPSocket(String ip, int port, boolean listen, boolean useSSL, boolean trustAll, String certFile, String certPass)
	{
		isListenServer = listen;
		if(listen){
			listenSocket = new TCPListenSocket(this, ip, port, adapters, clients, new TCPChildListener());
		}else{
			childSocket = new TCPChildSocket(generateId(), ip, port);
            if(useSSL){
                childSocket.enableSSL(trustAll, certFile, certPass);
            }
			childSocket.attach(new TCPChildListener());
		}
	}
	
	public void start()
	{
		if(isListenServer && listenSocket != null)
			listenSocket.start();
		else if(!isListenServer && childSocket != null)
			childSocket.start();
	}
		
	public void attach(TCPListenInterface adapter)
	{
		this.adapters.add(adapter);
	}
	
	public void detach(TCPListenInterface adapter)
	{
		this.adapters.remove(adapter);
	}
	
	public boolean send(int uid, byte[] data){
		TCPChildSocket socket = clients.get(uid);
		if(socket != null)
			return socket.send(data);
		else
			return false;
	}
	
	public boolean ping(int uid){
		TCPChildSocket socket = clients.get(uid);
		if(socket != null)
		{
			return socket.ping();
		}
		return false;
	}
	
	public void close(int uid) throws IOException {
		TCPChildSocket socket = clients.get(uid);
		if(socket != null){
			socket.close();
            socket.interrupt();
		}
	}
	
	public void broadcast(byte[] data)
	{
		for(int i =0; i < clients.size();i++)
		{
			clients.get(i).send(data);
		}
	}

	public int generateId()
	{
		if(currentId == Integer.MAX_VALUE)
			currentId = 0;
		currentId += 1;
		
		return currentId;
	}

}
