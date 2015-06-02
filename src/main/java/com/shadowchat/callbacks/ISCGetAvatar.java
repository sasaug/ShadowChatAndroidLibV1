package com.shadowchat.callbacks;

import com.shadowchat.object.SCUser;

import java.util.ArrayList;

public interface ISCGetAvatar extends ISCBase{
    public void onGetAvatar(int errorCode, byte[] image);
}
