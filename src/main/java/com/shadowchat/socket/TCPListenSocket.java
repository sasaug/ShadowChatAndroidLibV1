package com.shadowchat.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;

public class TCPListenSocket extends Thread{
	
	//Connection related
	private ServerSocket serverSocket = null;
	Socket socket = null;
	private String ip;
	private int port;
	
	ArrayList<TCPListenInterface> adapters;
	Hashtable<Integer, TCPChildSocket> clients;

    TCPChildInterface childListener;
	TCPSocket parent;
	
	public TCPListenSocket(TCPSocket parent, String ip, int port, ArrayList<TCPListenInterface> adapters, Hashtable<Integer, TCPChildSocket> clients, TCPChildInterface childListener)
	{
		this.ip = ip;
		this.port = port;
		this.adapters =  adapters;
		this.clients = clients;
		this.childListener = childListener;
		this.parent = parent;
	}
	
	public void run()
	{
		try{
			serverSocket = new ServerSocket(port, 0, InetAddress.getByName(ip));
			for(int i =0; i < adapters.size();i++)
				adapters.get(i).onSocketBinded();

			while(true)
			{
				socket = serverSocket.accept();
				socket.setTcpNoDelay(true);
				socket.setKeepAlive(true);
				socket.setSoTimeout(60000);
				TCPChildSocket client = new TCPChildSocket(parent.generateId(), socket);
				client.attach(childListener);
				client.start();
			}
		}
		catch(Exception ex)
		{
			for(int i =0; i < adapters.size();i++)
				adapters.get(i).onError(ex);
		}
		finally
		{
			try
			{
				serverSocket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}	
	}
}
