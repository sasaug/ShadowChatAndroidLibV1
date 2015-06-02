package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.object.SCUser;
import com.shadowchat.socket.TCPClient;

import java.util.ArrayList;

public class ListContactsModule extends Module{

    public Type getType() {return Type.LISTCONTACTS_RESPONSE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            ListContactsResponse msg = ListContactsResponse.parseFrom(data);
            ArrayList<SCUser> list = new ArrayList<>();
            for(int i = 0; i < msg.getFriendsCount(); i++){
                User user = msg.getFriends(i);
                SCUser u = new SCUser(user.getUsername(), user.getName(), user.getAvatar(), user.getFlagList().toArray(new String[user.getFlagCount()]));
                for(int j = 0; j < user.getUsersCount(); j++){
                    User member = user.getUsers(j);
                    SCUser usr = new SCUser(member.getUsername(), member.getName(), member.getAvatar(), member.getFlagList().toArray(new String[member.getFlagCount()]));
                    u.addUser(usr);
                }
                list.add(u);
            }
            for(IShadowChat cb: core.getCallbacks())
                cb.onGetContactList(msg.getErrorCode(), list);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
