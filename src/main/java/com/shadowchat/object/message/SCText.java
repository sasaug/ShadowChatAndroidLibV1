package com.shadowchat.object.message;

import com.google.protobuf.ByteString;
import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.ShadowMessages;
import com.shadowchat.modules.ModuleInterface;
import com.shadowchat.object.SCServerInfo;
import com.shadowchat.object.SCUser;
import com.shadowchat.object.SCUserStatus;
import com.shadowchat.security.Base64;
import com.shadowchat.security.Key;
import com.shadowchat.security.SHA256;
import com.shadowchat.security.Security;
import com.shadowchat.storage.ShadowChatMedia;
import com.shadowchat.storage.Store;
import com.shadowchat.utils.LogUtil;

import java.util.ArrayList;

public class SCText extends SCBase implements ShadowMessages.MessageCoder {
    private String message;

    private String keyHash = "";
    private ArrayList<SCEmoji> emojis = new ArrayList<SCEmoji>();

    public SCText(){
        super(TEXT);
    }

    public SCText(String origin, SCUser target, String message){
        super(TEXT);
        this.setId(target.getNextChatId());
        this.setOrigin(origin);
        this.setTarget(target.getUsername());
        this.setMessage(message);
        this.setTimestamp(System.currentTimeMillis());
        if(target.isGroup())
            this.setGroup(target.getUsername());
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public ArrayList<SCEmoji> getEmojis() {
        return emojis;
    }

    public void setEmojis(ArrayList<SCEmoji> emojis) {
        this.emojis = emojis;
    }

    public void addEmoji(SCEmoji emoji) {
        this.emojis.add(emoji);
    }

    public ArrayList<SimpleMessage> build(ModuleInterface core){
        ArrayList<SCUser> users = new ArrayList<SCUser>();
        if(isGroupMessage()){
            SCUser group = core.getAccount().getContacts().getGroup(getGroup());
            users.addAll(group.getUsers());
        }else{
            users.add(core.getAccount().getContacts().getUser(getTarget()));
        }

        ArrayList<SimpleMessage> list = new ArrayList<SimpleMessage>();
        for(int i = 0; i < users.size(); i++){
            try {
                String password = null;
                Key key = null;
                byte[] secret = null;
                if(core.getServer().getServerInfo().getSecurity() != SCServerInfo.SECURITY_NONE) {
                    if(core.getServer().getServerInfo().getSecurity() != SCServerInfo.SECURITY_E2E_NORMAL ||
                            (getGroup() == null || getGroup().isEmpty())){
                        ArrayList<Key> keys = core.getAccount().getKeys().getKeysByHashes(users.get(i).getKeyHashes(), Key.ACTIVE);
                        int randomValue = Security.getInstance().randomIntValue(keys.size());
                        key = keys.get(randomValue);
                        secret = key.getSecret();
                        if (secret != null)
                            password = Base64.encodeToString(secret, Base64.DEFAULT);
                    }
                }

                SimpleMessage.Builder builder = SimpleMessage.newBuilder();
                builder.setType(Type.SIMPLE);
                builder.setId(getId());
                if(password != null){
                    builder.setMessage(Base64.encodeToString(Security.getInstance().encrypt(message.getBytes("UTF-8"), password, SHA256.hash(secret)), Base64.DEFAULT));
                    builder.setKeyHash(key.getHash());
                }else
                    builder.setMessage(message);
                builder.setOrigin(getOrigin());
                builder.setTarget(users.get(i).getUsername());
                builder.setTimestamp(getTimestamp());

                if(isGroupMessage())
                    builder.setGroup(getGroup());
                for(int j = 0; j < getEmojis().size(); j++){
                    try {
                        SCEmoji emoji = getEmojis().get(j);
                        Emoji.Builder b = Emoji.newBuilder();
                        b.setTag(emoji.getTag());
                        String path = core.getAccount().getMyEmojiPath() + emoji.getHash();
                        ShadowChatMedia file = new ShadowChatMedia(core.getAccount().getKeys(), path, core.getServer().getContext(),ShadowChatMedia.EMOJI);
                        byte[] d = file.load();
                        if(secret != null)
                            d = Security.getInstance().encrypt(d, password, SHA256.hash(secret));
                        b.setImage(ByteString.copyFrom(d));
                        builder.addEmoji(b.build());
                    }catch(Exception ex){}
                }
                SimpleMessage msg = builder.build();
                list.add(msg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return list;
    }

    public static SCText buildFrom(ModuleInterface core, SimpleMessage msg) throws Exception{
        SCText c = new SCText();
        c.setId(msg.getId());
        c.setTimestamp(msg.getTimestamp());
        c.setOrigin(msg.getOrigin());
        c.setTarget(msg.getTarget());
        c.setIncoming(true);
        c.setMessage(msg.getMessage());
        if(msg.hasGroup())
            c.setGroup(msg.getGroup());
        Key key = null;
        byte[] secret = null;
        String password = null;
        if(msg.hasKeyHash()) {
            c.setKeyHash(msg.getKeyHash());
            if(c.getMessage() != null && !c.getMessage().isEmpty()) {
                key = core.getAccount().getKeys().getKeyTheirHash(msg.getKeyHash());
                if (key != null) {
                    secret = key.getSecret();
                    password = Base64.encodeToString(secret, Base64.DEFAULT);
                    c.setMessage(new String(Security.getInstance().decrypt(Base64.decode(msg.getMessage(), Base64.DEFAULT), password, SHA256.hash(secret)), "UTF-8"));
                } else {
                    LogUtil.error("SCText", "Key not found with hash : " + msg.getKeyHash());
                }
            }
        }

        for(int i = 0; i < msg.getEmojiCount(); i++){
            byte[] d = msg.getEmoji(i).toByteArray();
            if(msg.hasKeyHash()) {
                if (key != null) {
                    byte[] emojiData = Security.getInstance().decrypt(d, password, SHA256.hash(secret));
                    String hash = SHA256.hashString(emojiData);
                    SCEmoji emoji = new SCEmoji(msg.getEmoji(i).getTag(), hash);
                    emoji.setData(emojiData);
                    c.addEmoji(emoji);
                } else {
                    LogUtil.error("SCText", "Key not found with hash : " + msg.getKeyHash());
                }
            }else{
                String hash = SHA256.hashString(d);
                SCEmoji emoji = new SCEmoji(msg.getEmoji(i).getTag(), hash);
                emoji.setData(d);
                c.addEmoji(emoji);
            }
        }

        return c;
    }

    public void decodeMessageData(Store.MessageData data) {
        setId(data.getId());
        setOrigin(data.getOrigin());
        setTimestamp(data.getTimestamp());
        setStatus(data.getStatus());
        setMessage(data.getMessage());
        setIncoming(data.getIncoming());
        for(Store.MessageEmoji emoji: data.getEmojiList()){
            SCEmoji emo = new SCEmoji(emoji.getTag(), emoji.getHash());
            addEmoji(emo);
        }
        if(isGroupMessage()){
            for(int j = 0; j < data.getUserStatusCount(); j++){
                Store.UserStatus status = data.getUserStatus(j);
                String u = status.getUsername();
                int s = status.getStatus();
                SCUserStatus st = new SCUserStatus(u, s);
                addUserStatus(st);
            }
        }
    }

    public Store.MessageData encodeMessageData() {
        Store.MessageData.Builder b = Store.MessageData.newBuilder();
        b.setType(Store.MessageType.SIMPLE);
        b.setId(getId());
        b.setOrigin(getOrigin());
        b.setTimestamp(getTimestamp());
        b.setMessage(getMessage());
        b.setStatus(getStatus());
        b.setIncoming(isIncoming());
        for (SCEmoji emoji : getEmojis()) {
            Store.MessageEmoji.Builder emoBuilder = Store.MessageEmoji.newBuilder();
            emoBuilder.setTag(emoji.getTag());
            emoBuilder.setHash(emoji.getHash());
            b.addEmoji(emoBuilder);
        }
        for(int j = 0; j < getUserStatus().size(); j++){
            SCUserStatus s = getUserStatus().get(j);
            Store.UserStatus.Builder statusBuilder = Store.UserStatus.newBuilder();
            statusBuilder.setUsername(s.getUsername());
            statusBuilder.setStatus(s.getStatus());
            b.addUserStatus(statusBuilder);
        }
        return b.build();
    }
}
