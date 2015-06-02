package com.shadowchat.object;

import com.shadowchat.ShadowMessages;
import com.shadowchat.object.message.SCBase;
import com.shadowchat.object.message.SCText;
import com.shadowchat.security.KeyChain;
import com.shadowchat.security.Security;

import java.util.ArrayList;
import java.util.Comparator;

public class SCUser implements Comparable<SCUser>{
    public static final int FRSTATUS_REJECTED = 0;
    public static final int FRSTATUS_REQUESTING = 1;
    public static final int FRSTATUS_REQUESTED = 2;
    public static final int FRSTATUS_ACCEPTED = 3;
    public static final int FRSTATUS_PENDING = 4;   //seek final approval from admin

    //base
    private String username;
    private String name;
    private String avatar;
    private String[] flags;

    //local management
    private int chatId = 0;
    private int friendStatus = FRSTATUS_ACCEPTED;

    //group
    private boolean isGroup = false;
    private ArrayList<SCUser> users = new ArrayList<SCUser>();

    //encryption
    private ArrayList<String> keyHashes = new ArrayList<String>();

    //messages
    private ShadowMessages messages;

    //stats
    private SCStats stats = new SCStats();

    public static SCUser initAsContact(KeyChain keyChain, String folder, String username, String name, String avatar, String[] flags, boolean isGroup){
        SCUser user = new SCUser(username, name, avatar, flags);
        user.isGroup = isGroup;
        user.messages = new ShadowMessages(keyChain, folder + "/" + username + "/", isGroup);
        return user;
    }

    public SCUser(String username, String name, String avatar, String[] flags){
        this.username = username;
        this.name = name;
        this.avatar = avatar;
        this.flags = flags;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        if(name == null || name.isEmpty())
            return username;
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String[] getFlags() {
        return flags;
    }

    public void setFlags(String[] flags) {
        this.flags = flags;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean isGroup) {
        this.isGroup = isGroup;
    }

    public ArrayList<SCUser> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<SCUser> users) {
        this.users = users;
    }

    public void addUser(SCUser user) {
        this.users.add(user);
    }

    public ArrayList<String> getKeyHashes() {
        return keyHashes;
    }

    public void setKeyHashes(ArrayList<String> keyHashes) {
        this.keyHashes = keyHashes;
    }

    public void addKeyHash(String hash){
        keyHashes.add(hash);
    }

    public void removeKeyHash(String hash){
        keyHashes.remove(hash);
    }

    public boolean haveKeyHash(String hash){
        return keyHashes.contains(hash);
    }

    public int getChatId(){
        return chatId;
    }

    public void setChatId(int id){
        this.chatId = id;
    }

    public int getNextChatId(){
        chatId++;
        return chatId;
    }

    public int getFriendStatus() {
        return friendStatus;
    }

    public void setFriendStatus(int friendStatus) {
        this.friendStatus = friendStatus;
    }

    public SCStats getStats() {
        return stats;
    }

    public void setStats(SCStats stats) {
        this.stats = stats;
    }

    public SCUser getUser(String username){
        SCUser result = null;

        for(SCUser user: users){
            if(user.getUsername().equals(username)){
                result = user;
                break;
            }
        }

        return result;
    }

    //messages related
    public ArrayList<SCBase> getMessages(int page){
        return messages.getMessages(page);
    }

    public int getPageCount(){
        return messages.getPageCount();
    }

    public int getPageSize(){
        return messages.getPageSize();
    }

    public void addMessage(boolean send, SCBase msg){
        if(!send)
            getStats().increaseUnreadMessages(1);
        getStats().setLastActivity(System.currentTimeMillis());
        messages.addMessage(msg);
    }

    public void updateStatus(int id, int status, long timestamp){messages.updateStatus(id, status, timestamp);}

    public void updateStatus(String username, int id, int status, long timestamp){messages.updateStatus(username, id, status, timestamp);}

    //sorting purposes
    public int compareTo(SCUser user) {
        String name1= getName();
        String name2 = user.getName();
        return name1.compareTo(name2);
    }

    public static Comparator<SCUser> NameAscendingComparator
            = new Comparator<SCUser>() {
        public int compare(SCUser item1, SCUser item2) {
            String name1= item1.getName();
            String name2 = item2.getName();
            return name1.compareTo(name2);
        }
    };

    public static Comparator<SCUser> NameDescendingComparator
            = new Comparator<SCUser>() {
        public int compare(SCUser item1, SCUser item2) {
            String name1= item1.getName();
            String name2 = item2.getName();
            return name2.compareTo(name1);
        }
    };


}
