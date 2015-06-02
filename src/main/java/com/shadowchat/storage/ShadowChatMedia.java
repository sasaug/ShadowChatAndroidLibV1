package com.shadowchat.storage;

import android.content.Context;
import android.net.Uri;

import com.shadowchat.security.Key;
import com.shadowchat.security.KeyChain;
import com.shadowchat.security.Security;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ShadowChatMedia extends ProtectedFile {
    public static final int IMAGE = 101;
    public static final int EMOJI = 102;
    public static final int OTHERS = 150;

    private int type = OTHERS;
    private Context context;

    public static int KEYSET_SIZE = 10;

    public ShadowChatMedia (KeyChain keyChain, String path, Context context, int type){
        super(keyChain, path, type);
        ArrayList<Key> keys = keyChain.getKeysTag(type);
        if(keys.size() < KEYSET_SIZE){
            int count = KEYSET_SIZE - keys.size();
            for(int i = 0; i < count; i++){
                try {
                    Key key = Security.getInstance().generateKey();
                    key.setTheirPublicKey(key.getMyPublicKey());
                    key.setStatus(Key.ACTIVE);
                    key.setTag(type);
                    keyChain.add(key);
                    keys.add(key);
                }catch(Exception ex){}
            }
        }

        //TODO: key expiry etc

        //randomly choose a key
        int index = Security.getInstance().randomIntValue(keys.size());
        setKeyHash(keys.get(index).getHash());
        this.type = type;
        this.context = context;
    }

    public boolean exists(){
        return new File(path).exists();
    }

    public static RequestCreator getImagePicasso(Context context, final byte[] data){
        class ImageDownloader implements Downloader {
            @Override
            public Response load(Uri uri, int networkPolicy) throws IOException {
                InputStream in = new ByteArrayInputStream(data);
                return new Response(in, false, -1);
            }

            @Override
            public void shutdown() {}
        }

        Picasso picasso = new Picasso.Builder(context.getApplicationContext())
                .downloader(new ImageDownloader())
                .build();

        return picasso.load("");
    }

    public RequestCreator getImagePicasso() throws Exception{
        if(type != IMAGE)
            throw new Exception("Wrong type defined. This function only for IMAGE");


        class ImageDownloader implements Downloader {
            @Override
            public Response load(Uri uri, int networkPolicy) throws IOException {
                byte[] data = new byte[0];
                try{
                    data = loadFile();
                }catch(Exception e){}
                InputStream in = new ByteArrayInputStream(data);
                return new Response(in, false, -1);
            }

            @Override
            public void shutdown() {}
        }

        Picasso picasso = new Picasso.Builder(context.getApplicationContext())
                .downloader(new ImageDownloader())
                .build();

       return picasso.load(path);
    }

    public byte[] load() throws Exception{
        return loadFile();
    }

    public void saveImage(byte[] data) throws Exception{
        if(type != IMAGE)
            throw new Exception("Wrong type defined. This function only for IMAGE");

        saveFile(data);
    }

}
