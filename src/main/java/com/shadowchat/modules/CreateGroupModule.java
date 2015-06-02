package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message;
import com.sasaug.shadowchat.message.Message.CreateGroupResponse;
import com.sasaug.shadowchat.message.Message.Type;
import com.shadowchat.ShadowChatBase;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.object.SCUser;
import com.shadowchat.socket.TCPClient;

public class CreateGroupModule extends Module{

    public Type getType() {return Type.CREATEGROUP_RESPONSE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            CreateGroupResponse msg = CreateGroupResponse.parseFrom(data);
            Message.User user = msg.getGroup();
            SCUser group = new SCUser(user.getUsername(), user.getName(), user.getAvatar(), user.getFlagList().toArray(new String[user.getFlagCount()]));
            for(IShadowChat cb: core.getCallbacks())
                cb.onCreateGroup(msg.getErrorCode() == ShadowChatBase.ERROR_NONE, msg.getErrorCode(), group);
        }catch(Exception e){}
    }
}
