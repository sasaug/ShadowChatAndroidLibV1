package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.socket.TCPClient;

public class VerifyModule extends Module{

    public Type getType() {return Type.VERIFY_RESPONSE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            VerifyResponse msg =VerifyResponse.parseFrom(data);

            for(IShadowChat cb: core.getCallbacks())
                cb.onVerify(msg.getErrorCode(), msg.getStatus().getNumber());
        }catch(Exception e){}
    }
}
