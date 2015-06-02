package com.shadowchat.object.message;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.ShadowMessages;
import com.shadowchat.modules.ModuleInterface;
import com.shadowchat.object.SCUser;
import com.shadowchat.object.SCUserStatus;
import com.shadowchat.storage.Store;

public class SCNudge extends SCBase implements ShadowMessages.MessageCoder{

    public SCNudge(){
        super(NUDGE);
    }

    public SCNudge(String origin, SCUser target){
        super(NUDGE);
        this.setId(target.getNextChatId());
        this.setOrigin(origin);
        this.setTarget(target.getUsername());
        this.setTimestamp(System.currentTimeMillis());
        if(target.isGroup())
            this.setGroup(target.getUsername());
    }

    public NudgeMessage build(ModuleInterface core){
        try {
            NudgeMessage.Builder builder = NudgeMessage.newBuilder();
            builder.setType(Type.NUDGE);
            builder.setId(getId());
            builder.setOrigin(getOrigin());
            builder.setTarget(getTarget());
            builder.setTimestamp(getTimestamp());
            if(isGroupMessage())
                builder.setGroup(getGroup());

            NudgeMessage msg = builder.build();
            return msg;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static SCNudge buildFrom(ModuleInterface core, NudgeMessage msg) throws Exception{
        SCNudge c = new SCNudge();
        c.setId(msg.getId());
        c.setTimestamp(msg.getTimestamp());
        c.setOrigin(msg.getOrigin());
        c.setTarget(msg.getTarget());
        c.setIncoming(true);
        if(msg.hasGroup())
            c.setGroup(msg.getGroup());
        return c;
    }

    public void decodeMessageData(Store.MessageData data) {
        setId(data.getId());
        setOrigin(data.getOrigin());
        setTimestamp(data.getTimestamp());
        setStatus(data.getStatus());
        setIncoming(data.getIncoming());
        if(isGroupMessage()){
            for(int j = 0; j < data.getUserStatusCount(); j++){
                Store.UserStatus status = data.getUserStatus(j);
                String u = status.getUsername();
                int s = status.getStatus();
                SCUserStatus st = new SCUserStatus(u, s);
                addUserStatus(st);
            }
        }
    }

    public Store.MessageData encodeMessageData() {
        Store.MessageData.Builder b = Store.MessageData.newBuilder();
        b.setType(Store.MessageType.SIMPLE);
        b.setId(getId());
        b.setOrigin(getOrigin());
        b.setTimestamp(getTimestamp());
        b.setStatus(getStatus());
        b.setIncoming(isIncoming());
        for(int j = 0; j < getUserStatus().size(); j++){
            SCUserStatus s = getUserStatus().get(j);
            Store.UserStatus.Builder statusBuilder = Store.UserStatus.newBuilder();
            statusBuilder.setUsername(s.getUsername());
            statusBuilder.setStatus(s.getStatus());
            b.addUserStatus(statusBuilder);
        }
        return b.build();
    }
}
