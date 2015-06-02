package com.shadowchat;

import com.google.protobuf.ByteString;
import com.shadowchat.security.SHA256;
import com.shadowchat.storage.Store;
import com.shadowchat.utils.IO;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class MessageQueue {

    public class Data{
        public byte[] data;
        public Data(byte[] data){
            this.data = data;
        }
    }

    private String path;
    private Queue<Data> queue = new LinkedList<Data>();
    private ArrayList<Data> unsent = new ArrayList<Data>();

    public MessageQueue(String path){
        this.path = path;
        load();
    }

    public void add(byte[] data, boolean repeatable, boolean haveAck){
        if(!repeatable){
            try {
                ArrayList<Data> list = new ArrayList(queue);
                String hash = SHA256.hashString(data);
                for (int i = 0; i < list.size(); i++) {
                    String h = SHA256.hashString(list.get(i).data);
                    if (h.equals(hash)) {
                        return;
                    }
                }
            }catch(Exception ex){}
        }

        queue.add(new Data(data));
        if(haveAck){
            unsent.add(new Data(data));
            save();
        }
    }

    public void addFirst(byte[] data, boolean repeatable){
        if(!repeatable){
            try {
                ArrayList<Data> list = new ArrayList(queue);
                String hash = SHA256.hashString(data);
                for (int i = 0; i < list.size(); i++) {
                    String h = SHA256.hashString(list.get(i).data);
                    if (h.equals(hash)) {
                        return;
                    }
                }
            }catch(Exception ex){}
        }
        ArrayList<Data> list = new ArrayList(queue);
        list.add(0, new Data(data));
        queue = new LinkedList(list);
    }

    public byte[] peek(){
        Data dat = queue.peek();
        if(dat != null)
            return dat.data;
        return null;
    }

    public void deleteQueue(){
        queue.remove();
    }

    public Data deleteUnsent(String hash){
        try {
            for (int i = 0; i < unsent.size(); i++) {
                String h = SHA256.hashString(unsent.get(i).data);
                if (h.equals(hash)) {
                    Data data = unsent.get(i);
                    unsent.remove(i);
                    save();
                    return data;
                }
            }
        }catch(Exception ex){ex.printStackTrace();}
        return null;
    }

    private void load(){
        try {
            byte[] data = IO.read(path);
            queue.clear();
            unsent.clear();
            Store.ChatDataStore store = Store.ChatDataStore.parseFrom(data);
            for(int i = 0; i < store.getQueueCount(); i++){
                byte[] d = store.getQueue(i).toByteArray();
                queue.add(new Data(d));
                unsent.add(new Data(d));
            }
        }catch(Exception e){e.printStackTrace();}
    }

    private void save(){
        try {
            Store.ChatDataStore.Builder builder = Store.ChatDataStore.newBuilder();
            for(int i = 0; i < unsent.size(); i++){
                builder.addQueue(ByteString.copyFrom(unsent.get(i).data));
            }
            IO.write(path, builder.build().toByteArray());
        }catch(Exception e){e.printStackTrace();}
    }
}
