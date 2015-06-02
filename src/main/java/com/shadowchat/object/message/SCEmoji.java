package com.shadowchat.object.message;

public class SCEmoji {
    private String tag;
    private String hash;

    private byte[] data = null;

    public SCEmoji(String tag, String hash){
        this.setTag(tag);
        this.setHash(hash);
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
