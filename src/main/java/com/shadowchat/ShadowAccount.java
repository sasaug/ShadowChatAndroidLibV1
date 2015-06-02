package com.shadowchat;

import android.content.Context;
import android.os.Environment;

import com.shadowchat.security.KeyChain;

import java.io.File;

public class ShadowAccount {
    private Context context;
    private ShadowChatConfig cfg;

    private String username;
    private KeyChain keys;
    private ShadowContactList contacts;
    private ShadowMessageQueue queue;

    public ShadowAccount(Context context, ShadowChatConfig cfg){
        this.cfg = cfg;
        this.context = context;
        this.username = cfg.getUsername();

        String pubPath = getPublicPath() + username + "/";
        new File(pubPath).mkdirs();
        String priPath = getPrivatePath() + username + "/";
        new File(priPath).mkdirs();


        keys = new KeyChain(getKeyChainPath() + "keychain.dat", getSafeKeyPath() + "safe.key");
        queue = new ShadowMessageQueue(keys, getQueuePath() + "queue.dat");
        contacts = new ShadowContactList(keys, getContactsPath() + "contacts.dat", getMessagesPath());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public KeyChain getKeys() {
        return keys;
    }

    public void setKeys(KeyChain keys) {
        this.keys = keys;
    }

    public ShadowContactList getContacts() {
        return contacts;
    }

    public void setContacts(ShadowContactList contacts) {
        this.contacts = contacts;
    }

    public ShadowMessageQueue getQueue() {
        return queue;
    }

    public void setQueue(ShadowMessageQueue queue) {
        this.queue = queue;
    }

    public String getPublicPath(){
        if(cfg.getPublicPath() == null || cfg.getPublicPath().isEmpty()) {
            String path = Environment.getExternalStorageDirectory() + "/ShadowChat/" + cfg.getId() + "/";
            File dir = new File(path);
            if (!dir.exists())
                dir.mkdirs();
            return path;
        }else{
            File dir = new File(cfg.getPublicPath());
            if (!dir.exists())
                dir.mkdirs();
            return cfg.getPublicPath();
        }
    }

    public String getPrivatePath(){
        if(cfg.getPrivatePath() == null || cfg.getPrivatePath().isEmpty()) {
            return context.getApplicationInfo().dataDir + "/" + cfg.getId() + "/";
        }else{
            File dir = new File(cfg.getPrivatePath() + "/" + username + "/");
            if (!dir.exists())
                dir.mkdir();
            return cfg.getPrivatePath();
        }
    }

    public String getImagePath() {
        String path = getPublicPath() + "/" + username + "/media/image/";
        File dir = new File(path);
        if(!dir.exists())
            dir.mkdirs();
        return path;
    }

    public String getMyEmojiPath() {
        String path = getPublicPath() + "/" + username + "/media/emoji/";
        File dir = new File(path);
        if(!dir.exists())
            dir.mkdirs();
        return path;
    }

    public String getReceivedEmojiPath() {
        String path = getPublicPath() + "/" + username + "/media/emoji/received";
        File dir = new File(path);
        if(!dir.exists())
            dir.mkdirs();
        return path;
    }

    public String getMessagesPath() {
        String path =  getPublicPath() + "messages/";
        File dir = new File(path);
        if(!dir.exists())
            dir.mkdirs();
        return path;
    }

    public String getKeyChainPath() {
        String path =  getPublicPath();
        File dir = new File(path);
        if(!dir.exists())
            dir.mkdirs();
        return path;
    }

    public String getSafeKeyPath() {
        String path =  getPrivatePath();
        File dir = new File(path);
        if(!dir.exists())
            dir.mkdirs();
        return path;
    }

    public String getContactsPath() {
        String path =  getPublicPath();
        File dir = new File(path);
        if(!dir.exists())
            dir.mkdirs();
        return path;
    }

    public String getQueuePath() {
        String path =  getPublicPath();
        File dir = new File(path);
        if(!dir.exists())
            dir.mkdirs();
        return path;
    }
}
