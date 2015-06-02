package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.socket.TCPClient;

public class RequestPinModule extends Module{

    public Type getType() {return Type.REQUESTPIN_RESPONSE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            RequestPinResponse msg = RequestPinResponse.parseFrom(data);
            for(IShadowChat cb: core.getCallbacks())
                cb.onRequestPin(msg.getErrorCode(), msg.getStatus().getNumber());
        }catch(Exception e){}
    }
}
