package com.shadowchat;

import com.shadowchat.object.SCUser;
import com.shadowchat.security.KeyChain;
import com.shadowchat.storage.ProtectedFile;
import com.shadowchat.storage.Store.*;

import java.io.File;
import java.util.ArrayList;

public class ShadowContactList extends ProtectedFile {
    public final long CACHE_REFRESH_TIME = 120;

    private String path;
    private String messagePath;

    private long lastUpdated = 0;
    private ArrayList<SCUser> users = new ArrayList<SCUser>();

    public ShadowContactList(KeyChain keyChain, String path, String messagePath){
        super(keyChain, path, 0);
        this.path = path;
        this.messagePath = messagePath;
        load();
    }

    public SCUser get(String username){
        for(SCUser user: users){
            if(user.getUsername().equals(username))
                return user;
        }
        return null;
    }

    public SCUser getUser(String username){
        for(SCUser user: users){
            if(user.getUsername().equals(username) && !user.isGroup())
                return user;
        }
        return null;
    }

    public SCUser getGroup(String username){
        for(SCUser user: users){
            if(user.getUsername().equals(username) && user.isGroup())
                return user;
        }
        return null;
    }

    //return full contact list even not verified ones
    public ArrayList<SCUser> getAllUsers() {
        return users;
    }

    //return contact list with only verified ones
    public ArrayList<SCUser> getUsers() {
        ArrayList<SCUser> list = new ArrayList<>();
        for(SCUser user: users){
            if(user.getFriendStatus() == SCUser.FRSTATUS_ACCEPTED){
                list.add(user);
            }
        }
        return list;
    }

    //return contact list with only groups
    public ArrayList<SCUser> getGroups() {
        ArrayList<SCUser> list = new ArrayList<>();
        for(SCUser user: users){
            if(user.isGroup()){
                list.add(user);
            }
        }
        return list;
    }

    //return contact list unverified ones
    public ArrayList<SCUser> getPendingIncomingRequest() {
        ArrayList<SCUser> list = new ArrayList<>();
        for(SCUser user: users){
            if(user.getFriendStatus() == SCUser.FRSTATUS_REQUESTED){
                list.add(user);
            }
        }
        return list;
    }

    public ArrayList<SCUser> getPendingOutgoingRequest() {
        ArrayList<SCUser> list = new ArrayList<>();
        for(SCUser user: users){
            if(user.getFriendStatus() == SCUser.FRSTATUS_REQUESTING){
                list.add(user);
            }
        }
        return list;
    }

    public void setUsers(ArrayList<SCUser> users) {
        this.users = users;
    }

    public boolean exist(String username){
        for(SCUser user: getUsers()){
            if(user.getUsername().equals(username))
                return true;
        }
        return false;
    }

    public void add(SCUser user){
        users.add(user);
        save();
    }

    public void remove(String username){
        for(int i = 0; i < getUsers().size(); i++){
            if(getUsers().get(i).getUsername().equals(username)){
                getUsers().remove(i);
                break;
            }
        }
        save();
    }

    public boolean requiresUpdate() {
        if (lastUpdated >= System.currentTimeMillis() / 1000L + CACHE_REFRESH_TIME)
            return true;
        return false;
    }

    public void update(){
        lastUpdated = System.currentTimeMillis()/1000L;
    }

    public void load(){
        try {
            File file = new File(path);
            if(!file.exists()){
                return;
            }
            byte[] data = loadFile();
            ContactsDataStore store = ContactsDataStore.parseFrom(data);
            lastUpdated = store.getLastUpdate();
            for(int i = 0; i < store.getContactsCount(); i++){
                Contact ctc = store.getContacts(i);
                String username = ctc.getUsername();
                String name = ctc.getUsername();
                String avatar = ctc.getAvatar();
                String[] flags = new String[ctc.getFlagCount()];
                for(int j = 0; j < flags.length; j++){
                    flags[j] = ctc.getFlag(j);
                }
                long lastOnline = ctc.getStats().getLastOnline();
                long lastActivity = ctc.getStats().getLastActivity();
                int unreadMessage = ctc.getStats().getUnreadMessage();


                SCUser user = SCUser.initAsContact(keyChain, messagePath, username, name, avatar, flags, ctc.getIsGroup());
                user.setChatId(ctc.getChatId());
                user.setFriendStatus(ctc.getFriendStatus());
                user.getStats().setLastOnline(lastOnline);
                user.getStats().setLastActivity(lastActivity);
                user.getStats().setUnreadMessages(unreadMessage);

                for(int j = 0; j < ctc.getKeyHashesCount(); j++)
                    user.addKeyHash(ctc.getKeyHashes(j));

                for(int j = 0; j < ctc.getUsersCount(); j++){
                    Contact c = ctc.getUsers(j);
                    String username1 = c.getUsername();
                    String name1 = c.getUsername();
                    String avatar1 = c.getAvatar();
                    String[] flags1 = new String[c.getFlagCount()];
                    for(int k = 0; k < flags1.length; k++){
                        flags1[k] = ctc.getFlag(k);
                    }
                    SCUser usr = new SCUser(username1, name1, avatar1, flags1);
                    usr.setGroup(false);
                    for(int k = 0; k < c.getKeyHashesCount(); k++)
                        usr.addKeyHash(c.getKeyHashes(k));
                    user.addUser(usr);
                }
                users.add(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(){
        try {
            ContactsDataStore.Builder builder = ContactsDataStore.newBuilder();
            builder.setLastUpdate(lastUpdated);
            for(SCUser user : users){
                Contact.Builder b = Contact.newBuilder();
                b.setUsername(user.getUsername());
                b.setName(user.getName());
                b.setAvatar(user.getAvatar());
                b.setChatId(user.getChatId());
                b.setFriendStatus(user.getFriendStatus());
                b.setIsGroup(user.isGroup());
                UserStats.Builder usBuilder = UserStats.newBuilder();
                usBuilder.setLastOnline(user.getStats().getLastOnline());
                usBuilder.setLastActivity(user.getStats().getLastActivity());
                usBuilder.setUnreadMessage(user.getStats().getUnreadMessages());
                String[] flag = user.getFlags();
                for(int i = 0; i < flag.length; i++)
                    b.addFlag(flag[i]);
                b.addAllKeyHashes(user.getKeyHashes());
                if(user.isGroup()){
                    for(SCUser u : user.getUsers()){
                        Contact.Builder bd = Contact.newBuilder();
                        bd.setUsername(u.getUsername());
                        bd.setName(u.getName());
                        bd.setAvatar(u.getAvatar());
                        bd.setIsGroup(u.isGroup());
                        String[] flags = u.getFlags();
                        for(int i = 0; i < flags.length; i++)
                            bd.addFlag(flag[i]);
                        bd.addAllKeyHashes(u.getKeyHashes());
                        b.addUsers(bd.build());
                    }
                }
                builder.addContacts(b);
            }
            byte[] data = builder.build().toByteArray();
            saveFile(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
