package com.shadowchat.object.message;

import com.shadowchat.object.SCUserStatus;

import java.util.ArrayList;

public class SCBase {
    public final static int TEXT = 0;
    public final static int GALLERY = 1;
    public final static int JSON = 2;
    public final static int NUDGE = 3;
    public final static int LIVEBOARD = 4;
    public final static int JOINGROUP = 5;
    public final static int LEAVEGROUP = 6;
    public final static int KEYEXCHANGE = 7;

    public final static int STATUS_UNSENT = 0;
    public final static int STATUS_SENT = 1;
    public final static int STATUS_RECEIVED = 2;
    public final static int STATUS_SEEN = 3;

    private int type;
    private int id;
    private String origin;
    private String target;
    private long timestamp;
    private String group;

    private boolean incoming = false;
    private int status = STATUS_UNSENT;
    private ArrayList<SCUserStatus> userStatus = new ArrayList<SCUserStatus>();

    private boolean enableEncryption = true;

    public SCBase(int type){
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isGroupMessage(){
        return group != null && !group.equals("");
    }


    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void addUserStatus(SCUserStatus status){
        this.userStatus.add(status);
    }

    public ArrayList<SCUserStatus> getUserStatus(){
        return userStatus;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
    }

    public boolean isEnableEncryption() {
        return enableEncryption;
    }

    public void setEnableEncryption(boolean enableEncryption) {
        this.enableEncryption = enableEncryption;
    }
}
