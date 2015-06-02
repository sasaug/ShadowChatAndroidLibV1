package com.shadowchat.callbacks;

import com.shadowchat.object.SCUser;

public interface ISCAddFriend extends ISCBase{
    public void onAddFriend(boolean success, int errorCode, SCUser user);
}
