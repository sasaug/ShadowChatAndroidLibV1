package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.object.SCUser;
import com.shadowchat.security.Key;
import com.shadowchat.security.SHA256;
import com.shadowchat.security.Security;
import com.shadowchat.socket.TCPClient;

import java.security.PublicKey;
import java.util.ArrayList;

public class FriendRequestModule extends Module{

    public Type getType() {return Type.FRIEND_REQUEST;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            FriendRequest msg = FriendRequest.parseFrom(data);
            Ack.Builder ackBuilder = Ack.newBuilder();
            ackBuilder.setType(Type.ACK);
            ackBuilder.setAckType(ACKType.COMPLETE);
            ackBuilder.setHash(SHA256.hashString(data));
            core.getAccount().getQueue().add(ackBuilder.build().toByteArray(), false, false);
            core.triggerSender();

            ArrayList<PublicKey> pubKeys = new ArrayList<PublicKey>();
            for(int i = 0; i < msg.getKeysCount(); i++)
                pubKeys.add(Security.getInstance().getECDHPublicKey(msg.getKeys(i).toByteArray()));

            if(msg.getStatus() == SCUser.FRSTATUS_REQUESTING){
                ArrayList<Key> keys = new ArrayList<>();
                for(PublicKey pubKey: pubKeys) {
                    keys.add(core.getAccount().getKeys().add(pubKey));
                }

                User usr = msg.getUser();
                String username = usr.getUsername();
                String name = usr.getName();
                String avatar = usr.getAvatar();
                String[] flags = new String[usr.getFlagCount()];
                for(int i = 0; i < usr.getFlagCount(); i++)
                    flags[i] = usr.getFlag(i);
                SCUser user = SCUser.initAsContact(core.getAccount().getKeys(), core.getAccount().getMessagesPath(), username, name, avatar, flags, false);
                user.setFriendStatus(SCUser.FRSTATUS_REQUESTED);

                if(core.getAccount().getContacts() != null){
                    core.getAccount().getContacts().add(user);
                }

                for(Key key: keys){
                    user.addKeyHash(key.getHash());
                }

                for(IShadowChat cb: core.getCallbacks()){
                    cb.onFriendRequest(user);
                }
            }else if(msg.getStatus() == SCUser.FRSTATUS_ACCEPTED){
                User usr = msg.getUser();
                String username = usr.getUsername();
                String name = usr.getName();
                String avatar = usr.getAvatar();
                String[] flags = new String[usr.getFlagCount()];
                for(int i = 0; i < usr.getFlagCount(); i++)
                    flags[i] = usr.getFlag(i);
                SCUser user = new SCUser(username, name, avatar, flags);
                user.setFriendStatus(SCUser.FRSTATUS_ACCEPTED);
                if(core.getAccount().getContacts() != null){
                    SCUser u = core.getAccount().getContacts().getUser(username);
                    u.setName(usr.getName());
                    u.setAvatar(usr.getAvatar());
                    u.setFlags(flags);
                    u.setFriendStatus(SCUser.FRSTATUS_ACCEPTED);
                    for(int i = 0; i < pubKeys.size(); i++) {
                        PublicKey pubKey = pubKeys.get(i);
                        String hash = msg.getReferenceKeyHash(i);
                        core.getAccount().getKeys().setTheirPublicKeyWithMyPublicHash(pubKey, hash);
                    }
                    core.getAccount().getContacts().save();
                    user = u;
                }

                for(IShadowChat cb: core.getCallbacks()){
                    cb.onFriendAccepted(true, user);
                }
            }else if(msg.getStatus() == SCUser.FRSTATUS_REJECTED){
                User usr = msg.getUser();
                String username = usr.getUsername();
                String name = usr.getName();
                String avatar = usr.getAvatar();
                String[] flags = new String[usr.getFlagCount()];
                for(int i = 0; i < usr.getFlagCount(); i++)
                    flags[i] = usr.getFlag(i);
                SCUser user = new SCUser(username, name, avatar, flags);
                user.setFriendStatus(SCUser.FRSTATUS_REJECTED);
                if(core.getAccount().getContacts() != null){
                    SCUser u = core.getAccount().getContacts().getUser(username);
                    u.setName(usr.getName());
                    u.setAvatar(usr.getAvatar());
                    u.setFlags(flags);
                    u.setFriendStatus(SCUser.FRSTATUS_REJECTED);
                    for(int i = 0; i < pubKeys.size(); i++) {
                        PublicKey pubKey = pubKeys.get(i);
                        String hash = msg.getReferenceKeyHash(i);
                        core.getAccount().getKeys().setTheirPublicKeyWithMyPublicHash(pubKey, hash);
                    }
                    core.getAccount().getContacts().save();
                    user = u;
                }

                for(IShadowChat cb: core.getCallbacks()){
                    cb.onFriendAccepted(false, user);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
