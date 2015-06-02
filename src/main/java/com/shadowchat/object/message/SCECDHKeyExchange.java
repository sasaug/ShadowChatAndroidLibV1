package com.shadowchat.object.message;

import com.google.protobuf.ByteString;
import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.modules.ModuleInterface;
import com.shadowchat.object.SCUser;
import com.shadowchat.security.Base64;
import com.shadowchat.security.Key;
import com.shadowchat.security.SHA256;
import com.shadowchat.security.Security;
import com.shadowchat.utils.LogUtil;

import java.util.ArrayList;

public class SCECDHKeyExchange extends SCBase{
    private String keyHash = "";
    private ArrayList<byte[]> keys = new ArrayList<>();
    private ArrayList<String> referenceKeyHashes = new ArrayList<>();

    public SCECDHKeyExchange(){
        super(KEYEXCHANGE);
    }

    public SCECDHKeyExchange(String origin, SCUser target){
        super(KEYEXCHANGE);
        this.setId(target.getNextChatId());
        this.setOrigin(origin);
        this.setTarget(target.getUsername());
        this.setTimestamp(System.currentTimeMillis());
        if(target.isGroup())
            this.setGroup(target.getUsername());
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public ArrayList<byte[]> getKeys() {
        return keys;
    }

    public void setKeys(ArrayList<byte[]> keys) {
        this.keys = keys;
    }

    public void addKey(byte[] key) {
        this.keys.add(key);
    }

    public ArrayList<String> getReferenceKeyHashes() {
        return referenceKeyHashes;
    }

    public void setReferenceKeyHashes( ArrayList<String> referenceKeyHashes) {
        this.referenceKeyHashes = referenceKeyHashes;
    }

    public ArrayList<ECDHKeyExchangeMessage> build(ModuleInterface core){
        return build(core, null);
    }

    public ArrayList<ECDHKeyExchangeMessage> build(ModuleInterface core, String groupTargetUser){
        ArrayList<SCUser> users = new ArrayList<SCUser>();
        if(isGroupMessage()){
            if(groupTargetUser != null){
                SCUser user = core.getAccount().getContacts().getGroup(getGroup()).getUser(groupTargetUser);
                users.add(user);
            }else {
                SCUser group = core.getAccount().getContacts().getGroup(getGroup());
                users.addAll(group.getUsers());
            }
        }else{
            users.add(core.getAccount().getContacts().getUser(getTarget()));
        }

        ArrayList<ECDHKeyExchangeMessage> list = new ArrayList<>();
        for(int i = 0; i < users.size(); i++){
            try {
                ArrayList<Key> keys = core.getAccount().getKeys().getKeysByHashes(users.get(i).getKeyHashes(), Key.ACTIVE);
                int randomValue = Security.getInstance().randomIntValue(keys.size());
                Key key = keys.get(randomValue);
                byte[] secret = key.getSecret();
                String password = null;
                if(secret != null)
                    password = Base64.encodeToString(secret, Base64.DEFAULT);

                ECDHKeyExchangeMessage.Builder builder = ECDHKeyExchangeMessage.newBuilder();
                builder.setType(Type.ECDH_KEYX);
                builder.setId(getId());
                for(Key k: keys) {
                    byte[] d = k.getMyPublicKey().getEncoded();
                    if(password != null)
                        d = Security.getInstance().encrypt(d, password, SHA256.hash(secret));
                    builder.addKeys(ByteString.copyFrom(d));
                }
                builder.setOrigin(getOrigin());
                builder.setTarget(users.get(i).getUsername());
                builder.setTimestamp(getTimestamp());
                if(isGroupMessage())
                    builder.setGroup(getGroup());
                ECDHKeyExchangeMessage msg = builder.build();
                list.add(msg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return list;
    }

    public static SCECDHKeyExchange buildFrom(ModuleInterface core, ECDHKeyExchangeMessage msg) throws Exception{
        SCECDHKeyExchange c = new SCECDHKeyExchange();
        c.setId(msg.getId());
        c.setTimestamp(msg.getTimestamp());
        c.setOrigin(msg.getOrigin());
        c.setTarget(msg.getTarget());
        c.setIncoming(true);
        for(int i = 0; i < msg.getKeysCount(); i++){
            c.addKey(msg.getKeys(i).toByteArray());
        }
        if(msg.hasGroup())
            c.setGroup(msg.getGroup());

        Key key = null;
        byte[] secret = null;
        String password = null;

        if(msg.hasKeyHash()) {
            c.setKeyHash(msg.getKeyHash());
            key = core.getAccount().getKeys().getKeyTheirHash(msg.getKeyHash());
            if (key != null) {
                secret = key.getSecret();
                password = Base64.encodeToString(secret, Base64.DEFAULT);
            } else {
                LogUtil.error("SCText", "Key not found with hash : " + msg.getKeyHash());
            }
        }

        for(int i = 0; i < msg.getKeysCount(); i++){
            byte[] d = msg.getKeys(i).toByteArray();
            if(msg.hasKeyHash()) {
                d = Security.getInstance().decrypt(d, password, SHA256.hash(secret));
            }
            c.addKey(d);
        }

        return c;
    }

}
