package com.shadowchat.callbacks;

import com.shadowchat.object.SCUser;

import java.util.ArrayList;

public interface ISCGetContactList extends ISCBase{
    public void onGetContactList(int errorCode, ArrayList<SCUser> users);
}
