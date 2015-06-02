package com.shadowchat.modules;

import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.callbacks.IShadowChat;
import com.shadowchat.object.message.SCEmoji;
import com.shadowchat.object.message.SCText;
import com.shadowchat.object.SCUser;
import com.shadowchat.security.Security;
import com.shadowchat.storage.ShadowChatMedia;
import com.shadowchat.socket.TCPClient;

public class TextModule extends Module{

    public Type getType() {return Type.SIMPLE;}

    ModuleInterface core;

    public void init(ModuleInterface core) {
        this.core = core;
    }

    public void onReceive(TCPClient client, byte[] data) {
        try {
            SimpleMessage msg = SimpleMessage.parseFrom(data);

            BaseMessage.Builder base = BaseMessage.newBuilder(BaseMessage.parseFrom(data));
            Ack.Builder ackBuilder = Ack.newBuilder();
            ackBuilder.setType(Type.ACK);
            ackBuilder.setAckType(ACKType.RECEIVED);
            ackBuilder.setMessage(base);
            core.getAccount().getQueue().add(ackBuilder.build().toByteArray(), false, false);
            core.triggerSender();

            SCText c = SCText.buildFrom(core, msg);

            for(SCEmoji emoji : c.getEmojis()){
                if(core.getConfig().getSecurityLevel() >= Security.HIGH){
                    String path = core.getAccount().getReceivedEmojiPath() + emoji.getHash();
                    ShadowChatMedia media = core.getServer().buildShadowChatMedia(null, path, ShadowChatMedia.EMOJI);
                    media.saveImage(emoji.getData());
                }
            }

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
