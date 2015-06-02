package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message;
import com.sasaug.shadowchat.message.Message.Type;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.object.SCUser;
import com.shadowchat.socket.TCPClient;

import java.util.ArrayList;

public class GetAvatarModule extends Module{

    public Type getType() {return Type.GETAVATAR_RESPONSE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            Message.GetAvatarResponse msg = Message.GetAvatarResponse.parseFrom(data);
            byte[] bin = null;
            if(msg.hasAvatar())
                msg.getAvatar().toByteArray();
            for(IShadowChat cb: core.getCallbacks())
                cb.onGetAvatar(msg.getErrorCode(), bin);
        }catch(Exception e){}
    }
}
