package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.object.SCUser;
import com.shadowchat.object.message.SCECDHKeyExchange;
import com.shadowchat.security.Key;
import com.shadowchat.security.SHA256;
import com.shadowchat.security.Security;
import com.shadowchat.socket.TCPClient;

import java.security.PublicKey;
import java.util.ArrayList;

public class GroupInviteModule extends Module{

    public Type getType() {return Type.GROUPINVITE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            GroupInvite msg = GroupInvite.parseFrom(data);
            Ack.Builder ackBuilder = Ack.newBuilder();
            ackBuilder.setType(Type.ACK);
            ackBuilder.setAckType(ACKType.COMPLETE);
            ackBuilder.setHash(SHA256.hashString(data));
            core.getAccount().getQueue().add(ackBuilder.build().toByteArray(), false, false);
            core.triggerSender();

            PublicKey pubKey = null;
            if(msg.hasKey())
                pubKey = Security.getInstance().getECDHPublicKey(msg.getKey().toByteArray());

            if(msg.getStatus() == SCUser.FRSTATUS_REQUESTING){
                Key key = null;
                if(pubKey != null)
                    key = core.getAccount().getKeys().add(pubKey);
                User usr = msg.getGroup();
                String username = usr.getUsername();
                String name = usr.getName();
                String avatar = usr.getAvatar();
                String[] flags = new String[usr.getFlagCount()];
                for(int i = 0; i < usr.getFlagCount(); i++)
                    flags[i] = usr.getFlag(i);
                SCUser user = SCUser.initAsContact(core.getAccount().getKeys(), core.getAccount().getMessagesPath(), username, name, avatar, flags, true);
                user.setFriendStatus(SCUser.FRSTATUS_REQUESTED);

                if(core.getAccount().getContacts() != null){
                    core.getAccount().getContacts().add(user);
                }

                if(key != null)
                    user.addKeyHash(key.getHash());

                for(IShadowChat cb: core.getCallbacks()){
                    cb.onGroupInvite(user);
                }
            }else if(msg.getStatus() == SCUser.FRSTATUS_ACCEPTED){
                User usr = msg.getGroup();
                String username = usr.getUsername();
                String name = usr.getName();
                String avatar = usr.getAvatar();
                String[] flags = new String[usr.getFlagCount()];
                for(int i = 0; i < usr.getFlagCount(); i++)
                    flags[i] = usr.getFlag(i);
                SCUser user = new SCUser(username, name, avatar, flags);
                user.setGroup(true);
                user.setFriendStatus(SCUser.FRSTATUS_ACCEPTED);
                if(core.getAccount().getContacts() != null){
                    SCUser u = core.getAccount().getContacts().get(username);
                    u.setName(usr.getName());
                    u.setAvatar(usr.getAvatar());
                    u.setFlags(flags);
                    u.setFriendStatus(SCUser.FRSTATUS_ACCEPTED);
                    if(pubKey != null){
                        String hash = msg.getReferenceKeyHash();
                        core.getAccount().getKeys().setTheirPublicKeyWithMyPublicHash(pubKey, hash);
                    }
                    core.getAccount().getContacts().save();
                    user = u;

                    //TODO: send private key of the group currently using
                    /*try{
                        ArrayList<Key> keys = core.getAccount().getKeys().getKeysByHashes(user.getKeyHashes(), Key.ACTIVE);
                        for(int i = 0; i < keys.size(); i++){
                            if(keys.get(i).getTag() == Key.TAG_TEMP) {
                                keys.remove(i);
                                i--;
                            }
                        }
                    }catch(Exception e){}*/
                }

                for(IShadowChat cb: core.getCallbacks()){
                    cb.onGroupAccepted(true, user);
                }
            }else if(msg.getStatus() == SCUser.FRSTATUS_REJECTED){
                User usr = msg.getGroup();
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
                    if(pubKey != null){
                        String hash = msg.getReferenceKeyHash();
                        core.getAccount().getKeys().setTheirPublicKeyWithMyPublicHash(pubKey, hash);
                    }
                    core.getAccount().getContacts().save();
                    user = u;
                }

                for(IShadowChat cb: core.getCallbacks()){
                    cb.onGroupAccepted(true, user);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
