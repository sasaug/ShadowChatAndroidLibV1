package com.shadowchat;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


import com.shadowchat.utils.LogUtil;

import java.util.ArrayList;

public class ShadowChat {
    private static final String TAG = "ShadowChat";
    private ArrayList<ShadowChatServer> servers = new ArrayList<ShadowChatServer>();

    private int indexCount = -1;

    private static ShadowChat instance;
    public static ShadowChat getInstance(){
        if(instance == null)
            instance = new ShadowChat();
        return instance;
    }

    public ShadowChatServer getServer(int id){
        for(int i = 0; i < servers.size(); i++){
            if(servers.get(i).getId() == id){
                return servers.get(i);
            }
        }
        return null;
    }

    public ShadowChatServer getServerByIndex(int id){
        if(servers.size() > id)
            return servers.get(id);
        return null;
    }

    public ShadowChatServer getServer(ShadowChatConfig cfg){
        for(int i = 0; i < servers.size(); i++){
            if(servers.get(i).getCfg().equals(cfg)){
                return servers.get(i);
            }
        }
        return null;
    }

    public void stopServer(int id){
        for(int i = 0; i < servers.size(); i++){
            if(servers.get(i).getId() == id){
                servers.get(i).stop();
                getServers().remove(i);
                break;
            }
        }
    }

    public void stopServer(ShadowChatConfig cfg){
        for(int i = 0; i < servers.size(); i++){
            if(servers.get(i).getCfg().equals(cfg)){
                servers.get(i).stop();
                getServers().remove(i);
                break;
            }
        }
    }

    public void stopServer(ShadowChatServer server){
        for(int i = 0; i < servers.size(); i++){
            if(servers.get(i).equals(server)){
                servers.get(i).stop();
                getServers().remove(i);
                break;
            }
        }
    }

    public ArrayList<ShadowChatServer> getServers() {
        return servers;
    }

    public void rearrangeServers(ArrayList<ShadowChatConfig> configs){
        ArrayList<ShadowChatServer> temp = new ArrayList<>();
        for(int i = 0; i < configs.size(); i++){
            for(int j = 0; j < servers.size(); j++){
                if(servers.get(j).getCfg().equals(configs.get(i))){
                    temp.add(servers.get(j));
                    servers.remove(j);
                    break;
                }
            }
        }

        temp.addAll(servers);
        servers.clear();
        servers.addAll(temp);
    }

    public ShadowChatServer createServer(Context c, ShadowChatConfig cfg){
         ShadowChatServer s = getServer(cfg);
        if(s == null) {
            indexCount++;
            final ShadowChatServer server = new ShadowChatServer(c, indexCount, cfg);
            server.start();
            servers.add(server);
            return server;
        }
        return s;
    }

    public void attachApplication(Application app){
        class Callback implements Application.ActivityLifecycleCallbacks{
            int runningActivityCount = 0;

            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
                runningActivityCount++;

                if(runningActivityCount == 1){
                    startService(activity);
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(Activity activity) {
                for(ShadowChatServer server: servers){
                    server.onResume();
                }
            }

            @Override
            public void onActivityPaused(Activity activity) {
                for(ShadowChatServer server: servers){
                    //server.onPause();
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                runningActivityCount--;

                if(runningActivityCount <= 0){

                }
            }
        }
        app.registerActivityLifecycleCallbacks(new Callback());


    }

    /*
    * Service related
    * */
    protected ShadowChatService service;
    protected ShadowChatServiceConnection serviceConnection;

    private void startService(Context c) {
        c.startService(new Intent(c, ShadowChatService.class));

        // Now connect to it
        /*serviceConnection = new ShadowChatServiceConnection();

        boolean result = c.bindService(
                new Intent(c, ShadowChatService.class),
                serviceConnection,
                Context.BIND_AUTO_CREATE
        );*/
    }

    private void stopService(Context c){
        c.stopService(new Intent(c, ShadowChatService.class));
    }

    protected class ShadowChatServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            LogUtil.debug(TAG, "onServiceConnected AirWavesService");
            service = ((ShadowChatService.LocalBinder) binder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            LogUtil.debug(TAG, "onServiceDisconnected AirWavesService");
            service = null;
        }
    }


}
