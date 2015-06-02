package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message;
import com.shadowchat.ShadowChatServer;
import com.shadowchat.socket.TCPClient;

public class Module {
    public Message.Type getType(){return Message.Type.AUTH;}

    public void init(ModuleInterface core){}
    public void onReceive(TCPClient client, byte[] data){}
    public void onSent(TCPClient client, byte[] data){}
}
