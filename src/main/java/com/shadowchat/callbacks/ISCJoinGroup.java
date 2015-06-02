package com.shadowchat.callbacks;

public interface ISCJoinGroup extends ISCBase{
    void onJoinGroup(boolean success, int errorCode);
}
