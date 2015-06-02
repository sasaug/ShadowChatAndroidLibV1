package com.shadowchat.callbacks;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.object.*;
import com.shadowchat.object.message.SCGallery;
import com.shadowchat.object.message.SCJson;
import com.shadowchat.object.message.SCNudge;
import com.shadowchat.object.message.SCText;

import java.util.ArrayList;

public interface IShadowChat extends ISCRegister, ISCVerify, ISCRequestPin, ISCAddFriend, ISCAcceptFriend, ISCSearch, ISCGetContactList, ISCGetAvatar, ISCCreateGroup, ISCInviteToGroup, ISCJoinGroup{

    public int getId();

    //connection related
    public void onConnecting();
    public void onConnected();
    public void onConnectionError(String error);
    public void onHandshake(boolean success, String reason);
    public void onDisconnect(String reason);

    //functions
    public void onAuthentication(boolean success, int errorCode);
    public void onFriendRequest(SCUser user);
    public void onFriendAccepted(boolean accept, SCUser user);
    public void onGroupInvite(SCUser group);
    public void onGroupAccepted(boolean accept, SCUser group);


    //message
    public void onReceive(SCText message);
    public void onReceive(SCGallery message);
    public void onReceive(SCNudge message);
    public void onReceive(SCJson message);

    public void onMessageStatusSent(BaseMessage message);
    public void onMessageStatusReceived(BaseMessage message);
    public void onMessageStatusSeen(BaseMessage message);
}
