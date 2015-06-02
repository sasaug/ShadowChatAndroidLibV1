package com.shadowchat.object.message;

public class SCImage {
    private int type;

    private int id;
    private String hash;

    private byte[] data = null;

    public SCImage(int id, String hash){
        this.setId(id);
        this.setHash(hash);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
