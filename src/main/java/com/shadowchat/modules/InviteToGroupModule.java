package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.ShadowChatBase;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.socket.TCPClient;

public class InviteToGroupModule extends Module{

    public Type getType() {return Type.INVITETOGROUP_RESPONSE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            InviteToGroupResponse msg = InviteToGroupResponse.parseFrom(data);
            for(IShadowChat cb: core.getCallbacks())
                cb.onInviteToGroup(msg.getErrorCode() == ShadowChatBase.ERROR_NONE, msg.getErrorCode());
        }catch(Exception e){}
    }
}
