package com.shadowchat.security;


import com.google.protobuf.ByteString;
import com.shadowchat.storage.Store.*;
import com.shadowchat.utils.IO;
import com.shadowchat.utils.LogUtil;

import java.io.File;
import java.security.PublicKey;
import java.util.ArrayList;

public class KeyChain{
    private ArrayList<Key> keys = new ArrayList<>();

    private String path;
    private String keyPath;

    public KeyChain(String path, String keyPath){
        this.path = path;
        this.keyPath = keyPath;
        load();
    }

    //generate new set of key and add to keychain
    public Key generate(){
        Key key = null;
        try{
            key = Security.getInstance().generateKey();
            keys.add(key);
            save();
        }catch(Exception e){}
        return key;
    }

    //add an incoming public key, generate another set as well to pair it up
    public Key add(PublicKey pub){
        Key key = null;
        try{
            key = Security.getInstance().generateKey();
            key.setTheirPublicKey(pub);
            key.setStatus(Key.PENDING);
            keys.add(key);
            save();
        }catch(Exception e){}
        return key;
    }

    //add a normal key to this keychain
    public void add(Key key){
        try{
            key.setStatus(Key.PENDING);
            keys.add(key);
            save();
        }catch(Exception e){}
    }

    //delete a key
    public void delete(Key key){
        try{
            for(int i = 0; i < keys.size(); i++){
                Key k = keys.get(i);
                if(k.getHash().equals(key.getHash())) {
                    keys.remove(i);
                    break;
                }
            }
            save();
        }catch(Exception e){}
    }

    public Key setTheirPublicKeyWithMyPublicHash(PublicKey pub, String hash){
        for(Key key: keys){
            if(key.getHash().equals(hash)){
                key.setTheirPublicKey(pub);
                key.setStatus(Key.ACTIVE);
                save();
                return key;
            }
        }
        return null;
    }

    public void load(){
        try {
            if(!new File(path).exists())
                return;

            byte[] data = loadFile();
            KeyChainStore store = KeyChainStore.parseFrom(data);
            if(store.getKeysCount() != 0)
                keys.clear();
            for(int i = 0; i < store.getKeysCount(); i++){
                KeyStore s = store.getKeys(i);
                Key key = new Key();
                key.setMyPublicKey(Security.getInstance().getECDHPublicKey(s.getMyPublic().toByteArray()));
                key.setMyPrivateKey(Security.getInstance().getECDHPrivateKey(s.getMyPrivate().toByteArray()));
                if(s.hasTheirPublic())
                    key.setTheirPublicKey(Security.getInstance().getECDHPublicKey(s.getTheirPublic().toByteArray()));
                if(s.hasTag())
                    key.setTag(s.getTag());
                key.setTimestamp(s.getTimestamp());
                key.setStatus(s.getStatus().getNumber());
                keys.add(key);
            }
            LogUtil.debug("KeyChain" , keys.size() + " keys loaded.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(){
        try {
            KeyChainStore.Builder builder = KeyChainStore.newBuilder();
            for(Key key: keys) {
                KeyStore.Builder s = KeyStore.newBuilder();
                s.setMyPrivate(ByteString.copyFrom(key.getMyPrivateKey().getEncoded()));
                s.setMyPublic(ByteString.copyFrom(key.getMyPublicKey().getEncoded()));
                if(key.getTheirPublicKey() != null)
                    s.setTheirPublic(ByteString.copyFrom(key.getTheirPublicKey().getEncoded()));
                s.setStatus(KeyStatus.valueOf(key.getStatus()));
                if(key.getTag() != 0)
                    s.setTag(key.getTag());
                s.setTimestamp(key.getTimestamp());
                builder.addKeys(s.build());
            }
            saveFile(builder.build().toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] loadFile() throws Exception{
        //decrypt the file
        File file = new File(path);
        if(!file.exists()){
            throw new Exception("File not found.");
        }

        File keyFile = new File(keyPath);
        if(!keyFile.exists()){
            throw new Exception("Key file not found.");
        }

        byte[] k = IO.read(keyPath);
        byte[] data = IO.read(path);
        KeyStore s = KeyStore.parseFrom(k);
        Key key = new Key();
        key.setMyPublicKey(Security.getInstance().getECDHPublicKey(s.getMyPublic().toByteArray()));
        key.setMyPrivateKey(Security.getInstance().getECDHPrivateKey(s.getMyPrivate().toByteArray()));
        if(s.hasTheirPublic())
            key.setTheirPublicKey(Security.getInstance().getECDHPublicKey(s.getTheirPublic().toByteArray()));
        key.setTimestamp(s.getTimestamp());
        key.setStatus(s.getStatus().getNumber());


        return Security.getInstance().decrypt(data, Base64.encodeToString(key.getSecret(), Base64.DEFAULT), SHA256.hash(key.getSecret()));
    }

    private void saveFile(byte[] data) throws Exception{
        //encrypt the file
        File keyFile = new File(keyPath);

        Key key;
        if(!keyFile.exists()){
            //create the key file
            key = Security.getInstance().generateKey();
            key.setTheirPublicKey(key.getMyPublicKey());
            key.setStatus(Key.ACTIVE);
            KeyStore.Builder s = KeyStore.newBuilder();
            s.setMyPrivate(ByteString.copyFrom(key.getMyPrivateKey().getEncoded()));
            s.setMyPublic(ByteString.copyFrom(key.getMyPublicKey().getEncoded()));
            if(key.getTheirPublicKey() != null)
                s.setTheirPublic(ByteString.copyFrom(key.getTheirPublicKey().getEncoded()));
            s.setStatus(KeyStatus.valueOf(key.getStatus()));
            s.setTimestamp(key.getTimestamp());
            IO.write(keyPath, s.build().toByteArray());
        }else {
            byte[] bin = IO.read(keyPath);
            KeyStore s = KeyStore.parseFrom(bin);
            key = new Key();
            key.setMyPublicKey(Security.getInstance().getECDHPublicKey(s.getMyPublic().toByteArray()));
            key.setMyPrivateKey(Security.getInstance().getECDHPrivateKey(s.getMyPrivate().toByteArray()));
            if(s.hasTheirPublic())
                key.setTheirPublicKey(Security.getInstance().getECDHPublicKey(s.getTheirPublic().toByteArray()));
            key.setTimestamp(s.getTimestamp());
            key.setStatus(s.getStatus().getNumber());
        }

        IO.write(path, Security.getInstance().encrypt(data, Base64.encodeToString(key.getSecret(), Base64.DEFAULT), SHA256.hash(key.getSecret())));
    }

    public ArrayList<Key> getKeysTag(int tag){
        ArrayList<Key> keys = new ArrayList<>();
        for(Key key: keys){
            if(key.getTag() == tag)
                keys.add(key);
        }
        return keys;
    }


    public Key getKeyHash(String hash){
        for(Key key: keys){
            if(key.getHash().equals(hash))
                return key;
        }
        return null;
    }

    public Key getKeyTheirHash(String hash){
        for(Key key: keys){
            if(key.getTheirHash().equals(hash))
                return key;
        }
        return null;
    }

    public ArrayList<Key> getKeysByHashes(ArrayList<String> hashes){
        ArrayList<Key> list = new ArrayList<>();
        for(String hash: hashes) {
            for (Key key : keys) {
                if (key.getHash().equals(hash)) {
                    list.add(key);
                    break;
                }
            }
        }
        return list;
    }

    public ArrayList<Key> getKeysByTheirHashes(ArrayList<String> hashes){
        ArrayList<Key> list = new ArrayList<>();
        for(String hash: hashes) {
            for (Key key : keys) {
                if (key.getTheirHash().equals(hash)) {
                    list.add(key);
                    break;
                }
            }
        }
        return list;
    }

    public ArrayList<Key> getKeysByHashes(ArrayList<String> hashes, int statusFilter){
        ArrayList<Key> list = new ArrayList<>();
        for(String hash: hashes) {
            for (Key key : keys) {
                if (key.getHash().equals(hash) && key.getStatus() == statusFilter) {
                    list.add(key);
                    break;
                }
            }
        }
        return list;
    }

    public ArrayList<Key> getKeysByHashes(ArrayList<String> hashes, int statusFilter, int tagFilter){
        ArrayList<Key> list = new ArrayList<>();
        for(String hash: hashes) {
            for (Key key : keys) {
                if (key.getHash().equals(hash) && key.getStatus() == statusFilter && key.getTag() == tagFilter) {
                    list.add(key);
                    break;
                }
            }
        }
        return list;
    }

    public ArrayList<Key> getKeysByTheirHashes(ArrayList<String> hashes, int statusFilter){
        ArrayList<Key> list = new ArrayList<>();
        for(String hash: hashes) {
            for (Key key : keys) {
                if (key.getTheirHash().equals(hash) && key.getStatus() == statusFilter) {
                    list.add(key);
                    break;
                }
            }
        }
        return list;
    }

    public void activateKeysByHashes(ArrayList<String> hashes){
        ArrayList<Key> keys = getKeysByHashes(hashes);
        for(Key key: keys){
            key.setStatus(Key.ACTIVE);
        }
        save();
    }
}
