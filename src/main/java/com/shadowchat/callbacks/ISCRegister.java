package com.shadowchat.callbacks;

public interface ISCRegister extends ISCBase{
    public void onRegister(boolean success, int errorCode, int status);
}
