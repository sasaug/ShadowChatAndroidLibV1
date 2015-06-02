package com.shadowchat.socket;


public interface TCPChildInterface 
{
	public void onReceive(TCPClient client, byte[] data);
	public void onSent(TCPClient client, int id);
	public void onConnected(TCPClient client, TCPChildSocket socket);
	public void onDisconnected(TCPClient client);
	public void onError(TCPClient client, Exception error);
}
