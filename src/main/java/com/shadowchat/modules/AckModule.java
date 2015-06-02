package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.ShadowMessageQueue;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.object.message.SCBase;
import com.shadowchat.object.SCUser;
import com.shadowchat.socket.TCPClient;

public class AckModule extends Module{

    public Type getType() {return Type.ACK;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            Ack msg = Ack.parseFrom(data);

            //complete is not in it because client not meant to receive that
            if(msg.getAckType() == ACKType.SENT){
                // 1 tick
                //Sent, usually we process the hash because these messages usually in queue.
                //This hash is actually the hash of the message sent.
                ShadowMessageQueue.Data bin = core.getAccount().getQueue().deleteUnsent(msg.getHash());
                try{
                    //we try to convert the binary to a base message to identify if this is a message type
                    BaseMessage base = BaseMessage.parseFrom(bin.data);

                    //updating message
                    if(base.hasGroup()){
                        SCUser user = core.getAccount().getContacts().getUser(base.getGroup());
                        user.updateStatus(base.getId(), SCBase.STATUS_SENT, base.getTimestamp());
                    }else {
                        SCUser user = core.getAccount().getContacts().getUser(base.getTarget());
                        user.updateStatus(base.getId(), SCBase.STATUS_SENT, base.getTimestamp());
                    }

                    for(IShadowChat cb: core.getCallbacks()){
                        cb.onMessageStatusSent(base);
                    }

                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }else  if(msg.getAckType() == ACKType.RECEIVED){
                //2 tick
                //Received, means receiver had received the message
                //This should have a message attached to it to help identification
                try{
                    BaseMessage base = msg.getMessage();

                    //updating message
                    if(base.hasGroup()){
                        //it can be different in the group received, so we need to know who
                        SCUser user = core.getAccount().getContacts().getGroup(base.getGroup());
                        user.updateStatus(base.getTarget(), base.getId(), SCBase.STATUS_RECEIVED, 0);
                    }else {
                        SCUser user = core.getAccount().getContacts().getUser(base.getTarget());
                        user.updateStatus(base.getId(), SCBase.STATUS_RECEIVED, 0);
                    }

                    Ack.Builder ackBuilder = Ack.newBuilder();
                    ackBuilder.setType(Type.ACK);
                    ackBuilder.setAckType(ACKType.COMPLETE);
                    ackBuilder.setMessage(base);
                    core.getAccount().getQueue().add(ackBuilder.build().toByteArray(), false, false);
                    core.triggerSender();

                    for(IShadowChat cb: core.getCallbacks()){
                        cb.onMessageStatusSent(base);
                    }

                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }else  if(msg.getAckType() == ACKType.SEEN){
                //blue tick
                //This means the receiver had view the message
                //This should have a message attached to it to help identification
                try{
                    BaseMessage base = msg.getMessage();
                    //updating message
                    if(base.hasGroup()){
                        //it can be different in the group received, so we need to know who
                        SCUser user = core.getAccount().getContacts().getUser(base.getGroup());
                        user.updateStatus(base.getTarget(), base.getId(), SCBase.STATUS_SEEN, 0);
                    }else {
                        SCUser user = core.getAccount().getContacts().getUser(base.getTarget());
                        user.updateStatus(base.getId(), SCBase.STATUS_SEEN, 0);
                    }

                    for(IShadowChat cb: core.getCallbacks()){
                        cb.onMessageStatusSent(base);
                    }

                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
