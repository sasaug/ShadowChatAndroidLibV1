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
import com.shadowchat.utils.IO;
import com.shadowchat.utils.LogUtil;

import java.util.ArrayList;

public class SCGallery extends SCBase implements ShadowMessages.MessageCoder{
    private String message;
    private ArrayList<SCImage> images = new ArrayList<SCImage>();

    private String keyHash = "";
    private ArrayList<SCEmoji> emojis = new ArrayList<SCEmoji>();

    public SCGallery(){
        super(GALLERY);
    }

    public SCGallery(String origin, SCUser target, String message){
        super(GALLERY);
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

    public ArrayList<SCImage> getImages() {
        return images;
    }

    public void setImages(ArrayList<SCImage> images) {
        this.images = images;
    }

    public void addImage(SCImage image) {
        this.images.add(image);
    }

    public ArrayList<GalleryMessage> build(ModuleInterface core){
        ArrayList<SCUser> users = new ArrayList<SCUser>();
        if(isGroupMessage()){
            SCUser group = core.getAccount().getContacts().getGroup(getGroup());
            users.addAll(group.getUsers());
        }else{
            users.add(core.getAccount().getContacts().getUser(getTarget()));
        }

        ArrayList<GalleryMessage> list = new ArrayList<GalleryMessage>();
        for(int i = 0; i < images.size(); i++){
            try{
                SCImage image = images.get(i);
                String path = core.getAccount().getImagePath() + image.getHash();
                byte[] imgData = IO.read(path);
                for(int j = 0; j < users.size(); j++){
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

                        GalleryMessage.Builder builder = GalleryMessage.newBuilder();
                        builder.setType(Type.SIMPLE);
                        builder.setId(getId());
                        if(password != null){
                            builder.setMessage(new String(Security.getInstance().encrypt(message.getBytes("UTF-8"), password, SHA256.hash(secret)), "UTF-8"));
                            builder.setKeyHash(key.getHash());
                        }else
                            builder.setMessage(message);
                        builder.setOrigin(getOrigin());
                        builder.setTarget(users.get(j).getUsername());
                        builder.setTimestamp(getTimestamp());

                        if(isGroupMessage())
                            builder.setGroup(getGroup());
                        for(int k = 0; i < getEmojis().size(); k++){
                            try {
                                SCEmoji emoji = emojis.get(k);
                                Emoji.Builder b = Emoji.newBuilder();
                                b.setTag(emoji.getTag());
                                String pth = core.getAccount().getMyEmojiPath() + emoji.getHash();
                                ShadowChatMedia file = new ShadowChatMedia(core.getAccount().getKeys(), pth, core.getServer().getContext(),ShadowChatMedia.EMOJI);
                                byte[] d = file.load();
                                if(secret != null)
                                    d = Security.getInstance().encrypt(d, password, SHA256.hash(secret));
                                b.setImage(ByteString.copyFrom(d));
                                builder.addEmoji(b.build());
                            }catch(Exception ex){}
                        }

                        if(secret != null){
                            builder.setImageData(ByteString.copyFrom(Security.getInstance().encrypt(imgData, password, SHA256.hash(secret))));
                        }else{
                            builder.setImageData(ByteString.copyFrom(imgData));
                        }

                        GalleryMessage msg = builder.build();
                        list.add(msg);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }catch(Exception e){}
        }
        return list;
    }

    public static SCGallery buildFrom(ModuleInterface core, GalleryMessage msg) throws Exception{
        SCGallery c = new SCGallery();
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
                    LogUtil.error("SCGallery", "Key not found with hash : " + msg.getKeyHash());
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
                    LogUtil.error("SCGallery", "Key not found with hash : " + msg.getKeyHash());
                }
            }else{
                String hash = SHA256.hashString(d);
                SCEmoji emoji = new SCEmoji(msg.getEmoji(i).getTag(), hash);
                emoji.setData(d);
                c.addEmoji(emoji);
            }
        }

        byte[] d = msg.getImageData().toByteArray();
        if(msg.hasKeyHash()){
            if (key != null) {
                byte[] imgData = Security.getInstance().decrypt(d, password, SHA256.hash(secret));
                String hash = SHA256.hashString(imgData);
                SCImage img = new SCImage(msg.getId(), hash);
                img.setData(imgData);
                c.addImage(img);
            } else {
                LogUtil.error("SCGallery", "Key not found with hash : " + msg.getKeyHash());
            }

        }else {
            String hash = SHA256.hashString(d);
            SCImage img = new SCImage(msg.getId(), hash);
            img.setData(d);
            c.addImage(img);
        }
        return c;
    }

    public void decodeMessageData(Store.MessageData data) {
        setId(data.getId());
        setOrigin(data.getOrigin());
        setTimestamp(data.getTimestamp());
        setStatus(data.getStatus());
        setIncoming(data.getIncoming());
        if(data.hasMessage())
            setMessage(data.getMessage());
        for(Store.MessageEmoji emoji: data.getEmojiList()){
            SCEmoji emo = new SCEmoji(emoji.getTag(), emoji.getHash());
            addEmoji(emo);
        }
        for(int j = 0; j < data.getDataCount(); j++){
            SCImage image = new SCImage(j, data.getData(j));
            addImage(image);
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

    @Override
    public Store.MessageData encodeMessageData() {
        Store.MessageData.Builder b = Store.MessageData.newBuilder();
        b.setType(Store.MessageType.SIMPLE);
        b.setId(getId());
        b.setOrigin(getOrigin());
        b.setTimestamp(getTimestamp());
        b.setStatus(getStatus());
        b.setIncoming(isIncoming());
        if (getMessage() != null)
            b.setMessage(getMessage());
        for (SCEmoji emoji : getEmojis()) {
            Store.MessageEmoji.Builder emoBuilder = Store.MessageEmoji.newBuilder();
            emoBuilder.setTag(emoji.getTag());
            emoBuilder.setHash(emoji.getHash());
            b.addEmoji(emoBuilder);
        }
        for (SCImage img : getImages()) {
            b.addData(img.getHash());
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
