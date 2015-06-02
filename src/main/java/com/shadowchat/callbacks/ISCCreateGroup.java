package com.shadowchat.callbacks;

import com.shadowchat.object.SCUser;

public interface ISCCreateGroup extends ISCBase{
    public void onCreateGroup(boolean success, int errorCode, SCUser group);
}
