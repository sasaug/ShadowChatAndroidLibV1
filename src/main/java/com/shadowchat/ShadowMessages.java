package com.shadowchat;

import com.google.protobuf.ByteString;
import com.shadowchat.object.message.SCBase;
import com.shadowchat.object.message.SCGallery;
import com.shadowchat.object.message.SCJson;
import com.shadowchat.object.message.SCNudge;
import com.shadowchat.object.message.SCText;
import com.shadowchat.security.KeyChain;
import com.shadowchat.storage.ProtectedFile;
import com.shadowchat.storage.Store.*;
import com.shadowchat.storage.Store.MessageData;
import com.shadowchat.storage.Store.MessageType;
import com.shadowchat.storage.Store.UserStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ShadowMessages extends ProtectedFile {
    public interface MessageCoder{
        public void decodeMessageData(MessageData data);
        public MessageData encodeMessageData();
    }



   class MessageFile implements Comparable<MessageFile>{
        public int id;
        private int size = 0;
        public ArrayList<MessageData> data = new ArrayList<>();

        public int getSize(){
            if(data.size() == 0)
                return size;
            else
                return data.size();
        }

        public void setSize(int size){
            this.size = size;
        }

        public int compareTo(MessageFile file) {
            return file.id - id;    //descending
        }
    }

   public static int FILE_SIZE = 1000;

   private int pageSize = 100;

   private String path;
   private boolean isGroup = false;

   private boolean load = false;
   private ArrayList<MessageFile> files = new ArrayList<>();

   public ShadowMessages(KeyChain keyChain, String path, boolean isGroup){
       super(keyChain, path, 0);
       this.path = path;
       this.isGroup = isGroup;
   }

    //all the counts etc
   public int getPageCount(){
        int len = getMessageCount();
        int pages = (int)Math.floor(len/pageSize);
        if(len%pageSize != 0)
            pages += 1;
        return pages;
    }

   public void setPageSize(int pageSize){
        this.pageSize = pageSize;
    }

   public int getPageSize(){
        return pageSize;
   }

    public int getMessageCount(){
        load();
        int count = 0;
        for(MessageFile file: files)
            count += file.getSize();
        return count;
    }

    //load and save
    public void load(){
        Collections.sort(files);
        if(load)
            return;
        load = true;

        try {
            File folder= new File(path);
            folder.mkdirs();
            int i = 0;
            while(true){
                try {
                    File file = new File(path + i +".msg");
                    if(!file.exists())
                        break;
                    MessagesDataStore store = MessagesDataStore.parseFrom(loadFile(file.getAbsolutePath()));
                    MessageFile mf = new MessageFile();
                    mf.id = i;
                    mf.setSize(store.getMessagesCount());
                    files.add(mf);
                    i++;
                }catch(Exception e){
                    break;
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        Collections.sort(files);
    }

    public void loadContent(int id){
        try {
            MessageFile file = null;
            for(MessageFile f: files){
                if(f.id == id){
                    file = f;
                    break;
                }
            }

            MessagesDataStore store = MessagesDataStore.parseFrom(loadFile(path + id +".msg"));
            file.data.clear();

            for(ByteString bs: store.getMessagesList()){
                MessageData data = MessageData.parseFrom(bs.toByteArray());
                file.data.add(data);
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void save(int id){
        try {
            MessageFile file = null;
            for(MessageFile f: files){
                if(f.id == id){
                    file = f;
                    break;
                }
            }

            if(file == null){
                throw new Exception("File is null.");
            }

            MessagesDataStore.Builder builder = MessagesDataStore.newBuilder();
            for(MessageData data: file.data){
                builder.addMessages(ByteString.copyFrom(data.toByteArray()));
            }
            saveFile(path + id + ".msg", builder.build().toByteArray());
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    //update status
    public void updateStatus(int id, int status, long timestamp){
        updateStatus(null, id, status, timestamp);
    }

    public void updateStatus(String username, int id, int status, long timestamp){
        load();
        Collections.sort(files);
        for(int i = 0 ; i < files.size(); i++){
            try {
                boolean found = false;
                if(files.get(i).data.size() == 0){
                    loadContent(files.get(i).id);
                }

                for(int j = files.get(i).data.size()-1; j >=0; j++){
                    MessageData data = files.get(i).data.get(j);
                    if(data.getId() == id){
                        MessageData.Builder b = MessageData.newBuilder(data);
                        if(username == null) {
                            b.setStatus(status);
                            if(timestamp != 0)
                                b.setTimestamp(timestamp);
                        }else {
                            for (int k = 0; k < b.getUserStatusCount(); k++) {
                                UserStatus s = b.getUserStatus(k);
                                if (s.getUsername().equals(username)) {
                                    UserStatus.Builder bl = UserStatus.newBuilder(s);
                                    bl.setStatus(status);
                                    b.setUserStatus(k, bl);
                                    if(timestamp != 0)
                                        b.setTimestamp(timestamp);
                                }
                            }
                        }
                        files.get(i).data.set(j, b.build());
                        found = true;
                        break;
                    }
                }
                if(found) {
                    save(files.get(i).id);
                    break;
                }
            }catch(Exception e){}
        }
    }

    //add message
    public void addMessage(SCBase base){
        load();
        MessageData data = encodeMessageData(base);
        if(files.size() != 0) {
            MessageFile file = files.get(files.size() - 1);
            if (file.getSize() > FILE_SIZE) {
                MessageFile temp = file;
                file = new MessageFile();
                file.id = temp.id + 1;
                files.add(0, file);
            }
            file.data.add(data);
            save(file.id);
        }else{
            MessageFile file = new MessageFile();
            file.id = 0;
            files.add(file);
            save(file.id);
        }
    }

    public ArrayList<SCBase> getMessages(int page){
        load();

        ArrayList<SCBase> list = new ArrayList<>();
        if(page < getPageCount()){
            int start  = page * pageSize;
            int end = start + pageSize;

            int index = 0;

            for(int i = 0; i < files.size(); i++){
                int startId = index;
                int endId = index + files.get(i).getSize();
                try {
                    for (int j = startId; j < endId; j++) {
                        if (j >= start && j < end) {
                            if(files.get(i).data.size() == 0){
                                loadContent(files.get(i).id);
                            }
                            MessageData data = files.get(i).data.get(j);
                            list.add(decodeMessageData(data));
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
                index += files.get(i).getSize();
            }
        }
        return list;
    }

    //decoder and encoder
    private SCBase decodeMessageData(MessageData data){
        if(data.getType() == MessageType.SIMPLE){
            SCText msg = new SCText();
            msg.decodeMessageData(data);
            return msg;
        }else if(data.getType() == MessageType.GALLERY){
            SCGallery msg = new SCGallery();
            msg.decodeMessageData(data);
            return msg;
        }else if(data.getType() == MessageType.JSON){
            SCJson msg = new SCJson();
            msg.decodeMessageData(data);
            return msg;
        }else if(data.getType() == MessageType.NUDGE){
            SCNudge msg = new SCNudge();
            msg.decodeMessageData(data);
            return msg;
        }
        return null;
    }

    private MessageData encodeMessageData(SCBase base){
        if (base.getType() == SCBase.TEXT) {
            SCText msg = (SCText) base;
            return msg.encodeMessageData();
        } else if (base.getType() == SCBase.GALLERY) {
            SCGallery msg = (SCGallery) base;
            return msg.encodeMessageData();
        } else if (base.getType() == SCBase.JSON) {
            SCJson msg = (SCJson) base;
            return msg.encodeMessageData();
        } else if (base.getType() == SCBase.NUDGE) {
            SCNudge msg = (SCNudge) base;
            return msg.encodeMessageData();
        }
        return null;
    }

}
