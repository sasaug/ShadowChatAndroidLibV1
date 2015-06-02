package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.object.message.SCNudge;
import com.shadowchat.object.SCUser;
import com.shadowchat.socket.TCPClient;

public class NudgeModule extends Module{

    public Type getType() {return Type.NUDGE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            NudgeMessage msg = NudgeMessage.parseFrom(data);

            BaseMessage.Builder base = BaseMessage.newBuilder(BaseMessage.parseFrom(data));
            base.setTarget(core.getConfig().getUsername());
            Ack.Builder ackBuilder = Ack.newBuilder();
            ackBuilder.setType(Type.ACK);
            ackBuilder.setAckType(ACKType.RECEIVED);
            ackBuilder.setMessage(base);
            core.getAccount().getQueue().add(ackBuilder.build().toByteArray(), false, false);
            core.triggerSender();

            SCNudge c = SCNudge.buildFrom(core, msg);
            if(c.isGroupMessage()){
                SCUser user = core.getAccount().getContacts().getGroup(c.getGroup());
                user.addMessage(false, c);
            }else{
                SCUser user = core.getAccount().getContacts().getUser(c.getOrigin());
                user.addMessage(false, c);
            }

            for(IShadowChat cb: core.getCallbacks())
                cb.onReceive(c);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
