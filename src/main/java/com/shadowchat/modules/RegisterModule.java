package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.socket.TCPClient;

public class RegisterModule extends Module{

    public Type getType() {return Type.REGISTER_RESPONSE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            RegisterResponse msg = RegisterResponse.parseFrom(data);
            for(IShadowChat cb: core.getCallbacks())
                cb.onRegister(msg.getErrorCode() == 0, msg.getErrorCode(), msg.getStatus().getNumber());
        }catch(Exception e){}
    }
}
