package com.shadowchat.storage;


import com.google.protobuf.ByteString;
import com.shadowchat.security.Base64;
import com.shadowchat.security.Key;
import com.shadowchat.security.KeyChain;
import com.shadowchat.security.SHA256;
import com.shadowchat.security.Security;
import com.shadowchat.storage.Store.*;
import com.shadowchat.utils.IO;

import java.io.File;

/*
*   This file will act as the base to all files that we want to protect
*
*   Protected by Keychain.
*   File Protection : myPrivate+theirPub (secret from keychain)
*
*   First 64 bytes of file is hash of key used to encrypt
*
* */
public class ProtectedFile {
    protected KeyChain keyChain;
    protected String path;
    protected String keyHash = null;
    private boolean isProtected = true;
    private int tag = 0;

    public ProtectedFile(KeyChain keyChain, String path, int tag){
        this.path = path;
        this.keyChain = keyChain;
        this.tag =  tag;
    }

    public void setKeyHash(String keyHash){
        this.keyHash = keyHash;
    }

    public void enableProtection(boolean enabled){
        this.isProtected = enabled;
    }

    protected byte[] loadFile() throws Exception{
        return loadFile(path);
    }

    protected byte[] loadFile(String path) throws Exception{
        //decrypt the file
        File file = new File(path);
        if(!file.exists()){
            throw new Exception("File not found.");
        }

        byte[] data = IO.read(path);
        if(isProtectedFile(data)){
            KeyChainProtectedFileStore store = KeyChainProtectedFileStore.parseFrom(data);
            String hash = store.getHash();
            byte[] content = store.getContent().toByteArray();
            Key key = keyChain.getKeyHash(hash);
            if(key == null)
                throw new Exception("No key found to decrypt");
            byte[] secret = key.getSecret();
            return Security.getInstance().decrypt(content, Base64.encodeToString(secret, Base64.DEFAULT), SHA256.hash(secret));
        }
        return data;
    }

    protected Key saveFile(byte[] data) throws Exception{
        return saveFile(path, data);
    }

    protected Key saveFile(String path, byte[] data) throws Exception{
        File file = new File(path);

        Key key = null;
        if(isProtected) {
            if (keyHash != null) {
                key = keyChain.getKeyHash(keyHash);
            } else if (file.exists()) {
                byte[] old = IO.read(path);
                KeyChainProtectedFileStore store = KeyChainProtectedFileStore.parseFrom(old);
                String hash = store.getHash();
                key = keyChain.getKeyHash(hash);
            }

            //key is null, so we need to create a new key
            if (key == null) {
                key = keyChain.generate();
                key.setTheirPublicKey(key.getMyPublicKey());
                key.setStatus(Key.ACTIVE);
                key.setTag(tag);
                keyChain.save();
            } else {
                //existing key, check if it expired, if yes then generate new one
                if (keyHash == null && key.getTimestamp() + Security.getInstance().getKeysExpiryTimeLimit() < System.currentTimeMillis() / 1000L) {
                    keyChain.delete(key);
                    key = keyChain.generate();
                    key.setTheirPublicKey(key.getMyPublicKey());
                    key.setStatus(Key.ACTIVE);
                    key.setTag(tag);
                    keyChain.save();
                }
            }

            byte[] content = Security.getInstance().encrypt(data, Base64.encodeToString(key.getSecret(), Base64.DEFAULT), SHA256.hash(key.getSecret()));
            KeyChainProtectedFileStore.Builder builder = KeyChainProtectedFileStore.newBuilder();
            builder.setHash(key.getHash());
            builder.setContent(ByteString.copyFrom(content));

            IO.write(path, builder.build().toByteArray());
        }else{
            IO.write(path, data);
        }

        return key;
    }

    private boolean isProtectedFile(){
        try{
            byte[] data = IO.read(path);
            KeyChainProtectedFileStore.parseFrom(data);
        }catch(Exception ex){
            isProtected = false;
            return false;
        }
        isProtected = true;
        return true;
    }

    private boolean isProtectedFile(byte[] data){
        try{
            KeyChainProtectedFileStore.parseFrom(data);
        }catch(Exception ex){
            isProtected = false;
            return false;
        }
        isProtected = true;
        return true;
    }
}
