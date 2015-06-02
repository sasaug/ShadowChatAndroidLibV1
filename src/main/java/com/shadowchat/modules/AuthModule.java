package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message;
import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.ShadowChatServer;
import com.shadowchat.socket.TCPClient;
import com.shadowchat.utils.LogUtil;

public class AuthModule extends Module{

    public Message.Type getType() {return Message.Type.AUTH_RESPONSE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            AuthResponse msg = AuthResponse.parseFrom(data);
            if(msg.getErrorCode() == 0){
                core.setStatus(ShadowChatServer.AUTHENTICATED);
                LogUtil.debug(this.getClass().getSimpleName(), "Authentication success.");
                for(IShadowChat cb: core.getCallbacks())
                    cb.onAuthentication(true, 0);

                //fetch contact list
                if(core.getAccount().getContacts().requiresUpdate()) {
                    LogUtil.debug(this.getClass().getSimpleName(), "Updating contact list.");
                    core.getServer().getContactList(null);
                }
            }else{
                LogUtil.debug(this.getClass().getSimpleName(), "Authentication failed. Error code: " + msg.getErrorCode());
                for(IShadowChat cb: core.getCallbacks())
                    cb.onAuthentication(false, msg.getErrorCode());
            }
        }catch(Exception e){}
    }
}
