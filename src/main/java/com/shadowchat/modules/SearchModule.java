package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message;
import com.sasaug.shadowchat.message.Message.Type;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.object.SCUser;
import com.shadowchat.socket.TCPClient;

import java.util.ArrayList;

public class SearchModule extends Module{

    public Type getType() {return Type.SEARCH_RESPONSE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            Message.SearchResponse msg = Message.SearchResponse.parseFrom(data);
            ArrayList<SCUser> users = new ArrayList<>();

            for(int i = 0; i < msg.getListCount(); i++){
                Message.User usr = msg.getList(i);
                String username = usr.getUsername();
                String name = usr.getName();
                String avatar = usr.getAvatar();
                String[] flags = new String[usr.getFlagCount()];
                for(int j = 0; j < usr.getFlagCount(); j++)
                    flags[j] = usr.getFlag(j);
                SCUser user = new SCUser(username, name, avatar, flags);

                for(int j = 0; j < usr.getUsersCount(); j++){
                    Message.User usr1 = usr.getUsers(j);
                    String username1 = usr1.getUsername();
                    String name1 = usr1.getName();
                    String avatar1 = usr1.getAvatar();
                    String[] flags1 = new String[usr1.getFlagCount()];
                    for(int k = 0; k < usr1.getFlagCount(); k++)
                        flags1[k] = usr1.getFlag(k);
                    SCUser u = new SCUser(username1, name1, avatar1, flags1);
                    user.addUser(u);
                }
                users.add(user);
            }

            for(IShadowChat cb: core.getCallbacks())
                cb.onSearch(msg.getErrorCode(), users);
        }catch(Exception e){}
    }
}
