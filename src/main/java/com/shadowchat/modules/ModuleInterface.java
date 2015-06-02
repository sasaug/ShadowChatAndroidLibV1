package com.shadowchat.modules;

import com.shadowchat.ShadowAccount;
import com.shadowchat.ShadowChatServer;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.ShadowChatConfig;

import java.util.ArrayList;

public interface ModuleInterface {
    public void setStatus(int status);
    public ArrayList<IShadowChat> getCallbacks();

    public ShadowChatServer getServer();
    public ShadowChatConfig getConfig();
    public ShadowAccount getAccount();

    public void send(byte[] data);
    public void triggerSender();
}
