package com.shadowchat.object;

public class SCUserStatus {
    private String username;
    private int status;

    public SCUserStatus(String username, int status){
        this.username = username;
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

}
