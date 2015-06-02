package com.shadowchat.callbacks;

import com.shadowchat.object.SCUser;

import java.util.ArrayList;

public interface ISCSearch extends ISCBase{
    public void onSearch(int errorCode, ArrayList<SCUser> users);
}
