package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message.*;
import com.sasaug.shadowchat.message.Message.Type;
import com.shadowchat.ShadowChatBase;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.object.SCUser;
import com.shadowchat.socket.TCPClient;

public class AddFriendModule extends Module{

    public Type getType() {return Type.ADDFRIEND_RESPONSE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            AddFriendResponse msg = AddFriendResponse.parseFrom(data);
            User usr = msg.getUser();
            String username = usr.getUsername();
            String name = usr.getName();
            String avatar = usr.getAvatar();
            String[] flags = new String[usr.getFlagCount()];
            for(int i = 0; i < usr.getFlagCount(); i++)
                flags[i] = usr.getFlag(i);
            SCUser user = new SCUser(username, name, avatar, flags);
            user.setFriendStatus(SCUser.FRSTATUS_REQUESTING);
            for(IShadowChat cb: core.getCallbacks())
                cb.onAddFriend(msg.getErrorCode() == ShadowChatBase.ERROR_NONE, msg.getErrorCode(), user);
        }catch(Exception e){}
    }
}
