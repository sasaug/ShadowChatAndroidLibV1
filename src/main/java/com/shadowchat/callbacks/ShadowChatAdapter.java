package com.shadowchat.callbacks;

import com.sasaug.shadowchat.message.Message;
import com.shadowchat.object.message.SCGallery;
import com.shadowchat.object.message.SCJson;
import com.shadowchat.object.message.SCNudge;
import com.shadowchat.object.message.SCText;
import com.shadowchat.object.SCUser;

import java.util.ArrayList;

public class ShadowChatAdapter implements IShadowChat{
    @Override
    public int getId() {
        return -1;
    }

    @Override
    public void onConnecting() {

    }

    @Override
    public void onConnected() {

    }

    @Override
    public void onConnectionError(String error) {

    }

    @Override
    public void onHandshake(boolean success, String reason) {

    }

    @Override
    public void onDisconnect(String reason) {

    }

    @Override
    public void onRegister(boolean success, int errorCode, int status) {

    }

    @Override
    public void onAuthentication(boolean success, int errorCode) {

    }

    @Override
    public void onFriendRequest(SCUser user) {

    }

    @Override
    public void onFriendAccepted(boolean accept, SCUser user) {

    }

    @Override
    public void onGroupInvite(SCUser group) {

    }

    @Override
    public void onGroupAccepted(boolean accept, SCUser group) {

    }

    @Override
    public void onReceive(SCText message) {

    }

    @Override
    public void onReceive(SCGallery message) {

    }

    @Override
    public void onReceive(SCNudge message) {

    }

    @Override
    public void onReceive(SCJson message) {

    }

    @Override
    public void onMessageStatusSent(Message.BaseMessage message) {

    }

    @Override
    public void onMessageStatusReceived(Message.BaseMessage message) {

    }

    @Override
    public void onMessageStatusSeen(Message.BaseMessage message) {

    }

    @Override
    public void onError(int errorCode, String error) {

    }

    @Override
    public void onVerify(int errorCode, int status) {

    }

    @Override
    public void onRequestPin(int errorCode, int status) {

    }

    @Override
    public void onAcceptFriend(boolean success, int errorCode) {

    }

    @Override
    public void onAddFriend(boolean success, int errorCode, SCUser user) {

    }

    @Override
    public void onSearch(int errorCode, ArrayList<SCUser> users) {

    }

    @Override
    public void onGetContactList(int errorCode, ArrayList<SCUser> users) {

    }

    @Override
    public void onGetAvatar(int errorCode, byte[] image) {

    }

    @Override
    public void onCreateGroup(boolean success, int errorCode, SCUser group) {

    }

    @Override
    public void onInviteToGroup(boolean success, int errorCode) {

    }

    @Override
    public void onJoinGroup(boolean success, int errorCode) {

    }
}
