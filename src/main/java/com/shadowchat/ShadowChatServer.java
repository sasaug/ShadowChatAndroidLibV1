package com.shadowchat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.sasaug.shadowchat.message.Message;
import com.sasaug.shadowchat.message.Message.*;
import com.shadowchat.callbacks.*;
import com.shadowchat.modules.*;
import com.shadowchat.object.*;
import com.shadowchat.object.message.SCEmoji;
import com.shadowchat.object.message.SCGallery;
import com.shadowchat.object.message.SCImage;
import com.shadowchat.object.message.SCJson;
import com.shadowchat.object.message.SCECDHKeyExchange;
import com.shadowchat.object.message.SCNudge;
import com.shadowchat.object.message.SCText;
import com.shadowchat.security.Key;
import com.shadowchat.security.SHA256;
import com.shadowchat.security.Security;
import com.shadowchat.storage.ShadowChatMedia;
import com.shadowchat.socket.TCPClient;
import com.shadowchat.socket.TCPListenAdapter;
import com.shadowchat.socket.TCPSocket;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.ArrayList;

import com.shadowchat.utils.LogUtil;

public class ShadowChatServer extends ShadowChatBase{
    public static String TAG = "ShadowChatServer";
    public final static int INACTIVE = 0;
    public final static int ACTIVE = 1;

    public final static int DISCONNECTED = 0;
    public final static int CONNECTING = 1;
    public final static int CONNECTED = 2;
    public final static int HANDSHAKE = 3;
    public final static int AUTHENTICATED = 4;

    private int id = 0;

    //settings
    private ShadowChatConfig cfg;

    //holder
    private TCPSocket socket;

    //variables
    private Context context;
    private int status = INACTIVE;
    private int connectionStatus = DISCONNECTED;

    private long lastConnectRequest = 0;
    private int connectionId = 0;

    ArrayList<Module> modules = new ArrayList<Module>();
    protected ArrayList<IShadowChat> callbacks = new ArrayList<IShadowChat>();

    ThreadPoolExecutor pool;
    private Intent serviceIntent;
    private Sender sender;

    private ShadowAccount account;
    private SCServerInfo serverInfo;

    //gcm
    GoogleCloudMessaging gcm;

    public Context getContext(){
        return context;
    }

    public ShadowChatConfig getCfg() {
        return cfg;
    }

    public void setCfg(ShadowChatConfig cfg) {
        this.cfg = cfg;
    }

    public SCUser getMyself(){
        return new SCUser(cfg.getUsername(), cfg.getProfileName(), cfg.getProfileAvatar(), new String[0]);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public SCServerInfo getServerInfo(){
        return serverInfo;
    }

    public int getConnectionStatus(){
        return connectionStatus;
    }

    class Sender extends Thread{
        int idleCount = 0;
        boolean isSleeping = false;
        boolean killed = false;

        public void kill(){
            killed = true;
        }

        public void trigger(){
            if(isSleeping)
                interrupt();
        }

        public void run() {
            while(!killed)
            {
                try {
                    isSleeping = true;
                    Thread.sleep(1000);
                    isSleeping = false;
                } catch (InterruptedException e) {
                    isSleeping = false;
                }

                while(account.getQueue().peek() != null)
                {
                    if(connectionStatus < HANDSHAKE){
                        connect();
                        break;
                    }else{
                        if(connectionStatus < HANDSHAKE)
                            break;

                        final byte[] data = account.getQueue().peek();
                        boolean sent = false;
                        try{
                            sent = send(data);
                        }catch(Exception ex){
                            LogUtil.error(TAG, "Fail in sending packet data. Error : " + ex.getMessage());
                        }

                        if(sent)
                            account.getQueue().deleteQueue();
                    }
                }
                idleCount++;

                if(idleCount >= 15){
                    //TODO: Check the keep alive design structure!
                    //disable keep alive for now since it's not important
                    /*if(connectionStatus >= AUTHENTICATED) {
                        KeepAliveMessage.Builder builder = KeepAliveMessage.newBuilder();
                        builder.setType(Type.KEEPALIVE);
                        try {
                            send(builder.build().toByteArray());
                        } catch (Exception e) {}
                    }*/

                    idleCount = 0;
                }
            }
        }
    }

    public static void initAllSingleton(){
        Security.getInstance();
    }

    public ShadowChatServer(Context context, int id, ShadowChatConfig cfg){
        super(context, cfg);
        this.setId(id);
        this.setCfg(cfg);
        this.context = context;

        if(cfg.getIp() == null || cfg.getPort() == -1){
            LogUtil.error(TAG,  "IP/Port is invalid in config. Please fill it up.");
        }

        pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(cfg.getThreads());
        account = new ShadowAccount(context, cfg);

        ArrayList<Module> list = new ArrayList<Module>();
        list.add(new AuthModule());
        list.add(new TextModule());
        list.add(new GalleryModule());
        list.add(new JsonModule());
        list.add(new NudgeModule());
        list.add(new AckModule());
        list.add(new RegisterModule());
        list.add(new AcceptFriendModule());
        list.add(new AddFriendModule());
        list.add(new FriendRequestModule());
        list.add(new RequestPinModule());
        list.add(new VerifyModule());
        list.add(new SearchModule());
        list.add(new ListContactsModule());
        list.add(new CreateGroupModule());
        list.add(new InviteToGroupModule());
        list.add(new JoinGroupModule());
        list.add(new GroupInviteModule());

        for(Module module: list){
            modules.add(module);
            module.init(new ModInterface());
        }
    }

    public void onResume(){
        connect();
    }

    public void onPause(){
        disconnect();
    }

    class ModInterface implements ModuleInterface {
        public ShadowChatConfig getConfig() {
            return ShadowChatServer.this.getCfg();
        }

        public ShadowAccount getAccount() {
            return account;
        }

        public void send(byte[] data) {
            try {
                ShadowChatServer.this.send(data);
            }catch(Exception ex){}
        }

        public void triggerSender() {
            if(sender != null)
                sender.trigger();
        }

        public ShadowChatServer getServer(){
            return ShadowChatServer.this;
        }

        public void setStatus(int status) {
            ShadowChatServer.this.connectionStatus = status;
        }

        public ArrayList<IShadowChat> getCallbacks() {
            return  ShadowChatServer.this.callbacks;
        }

    }

    public void addListener(IShadowChat listener){
        removeListener(listener.getId());
        callbacks.add(listener);
    }

    public void removeListener(IShadowChat listener){
        callbacks.remove(listener);
    }

    public void removeListener(int id){
        for(int i = 0; i < callbacks.size(); i++){
            if(callbacks.get(i).getId() == id){
                callbacks.remove(i);
                i--;
            }
        }
    }

    public boolean haveListener(int id){
        for(int i = 0; i < callbacks.size(); i++){
            if(callbacks.get(i).getId() == id){
                return true;
            }
        }
        return false;
    }

    public void start(){
        if(status == INACTIVE) {
            status = ACTIVE;

            serviceIntent = new Intent(context, ShadowChatService.class);
            context.startService(serviceIntent);

            if(connectionStatus == DISCONNECTED || connectionStatus == CONNECTING) {
                connect();
            }else{
                authenticate();
            }
        }
    }

    public void reload(ShadowChatConfig cfg){
        stop();
        if(cfg != null) {
            this.setCfg(cfg);

            if (cfg.getIp() == null || cfg.getPort() == -1) {
                LogUtil.error(TAG, "IP/Port is invalid in config. Please fill it up.");
            }

            account = new ShadowAccount(context, cfg);
        }
        start();
    }

    public void stop(){
        if(serviceIntent != null)
            context.stopService(serviceIntent);

        disconnect();
        connectionStatus = DISCONNECTED;
        status = INACTIVE;
    }

    public ShadowContactList getContacts(){
        return account.getContacts();
    }

    public ShadowChatMedia buildShadowChatMedia(Context context, String path, int type){
        if(context == null)
            context = this.context;
        return new ShadowChatMedia(account.getKeys(), path, context, type);
    }

    public String getImagePath(){
        return account.getImagePath();
    }

    public String getMyEmojiPath(){
        return account.getMyEmojiPath();
    }

    public String getReceivedEmojiPath(){
        return account.getReceivedEmojiPath();
    }


    public void authenticate(){
        if(status == INACTIVE || connectionStatus == AUTHENTICATED)
            return;

        if(connectionStatus != HANDSHAKE) {
            LogUtil.debug(TAG, "Not yet connected to server!");
            connect();
            return;
        }

        /*int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                //GooglePlayServicesUtil.getErrorDialog(resultCode, context, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
        }*/

        String cloudId = "";
        if(cfg.getCloudId() != null && cfg.getCloudId().isEmpty()){
            int appVersion = 1;
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                appVersion =  packageInfo.versionCode;
            } catch (Exception e) {}
            if(cfg.getAppVersion() == appVersion){
                cloudId = cfg.getCloudId();
            }
        }

        class DoAuth implements Runnable{
            public void run(){
                LogUtil.debug(TAG, "Authenticating...");
                try {
                    Auth.Builder builder = Auth.newBuilder();
                    builder.setType(Type.AUTH);
                    builder.setCloudId(cfg.getCloudId() == null ? "" : cfg.getCloudId());
                    builder.setOs(OS.ANDROID);
                    builder.setDevice(getDevice());
                    builder.setUsername(getCfg().getUsername());
                    builder.setPassword(getCfg().getPassword());
                    Auth message = builder.build();
                    account.getQueue().addFirst(message.toByteArray(), false);
                } catch (Exception ex) {ex.printStackTrace();}
            }
        }


        if(cloudId.isEmpty()) {
            //register cloud id
            new AsyncTask<Void, Void, String>() {
                protected String doInBackground(Void... params) {
                    String msg = "";
                    try {
                        if (gcm == null) {
                            gcm = GoogleCloudMessaging.getInstance(context);
                        }
                        cfg.setCloudId(gcm.register(cfg.getGcmSenderId()));
                    } catch (Exception ex) {
                        msg = "Error :" + ex.getMessage();
                    }
                    return msg;
                }

                @Override
                protected void onPostExecute(String msg) {
                    new DoAuth().run();
                }
            }.execute(null, null, null);
        }else{
            new DoAuth().run();
        }
    }

    public void register(final String username, final String password, final String phone, final String email, final String name, final String serverPassword, final ISCRegister listener){
        if(status == INACTIVE)
            return;

        final int id = new Random().nextInt(Integer.MAX_VALUE);
        class InjectedListener extends ShadowChatAdapter {
            public int getId(){
                return id;
            }

            public void onRegister(boolean success, int errorCode, int status){
                if(listener != null)
                    listener.onRegister(success, errorCode, status);
                removeListener(id);
            }
        }
        callbacks.add(new InjectedListener());

        class Work implements Runnable{
            public void run(){
                LogUtil.debug(TAG, "Registering " + username);
                try {
                    Register.Builder builder = Register.newBuilder();
                    builder.setType(Type.REGISTER);
                    builder.setUsername(username);
                    builder.setPassword(password);
                    builder.setDevice(getDevice());
                    if(phone != null)
                        builder.setPhone(phone);
                    if(email != null)
                        builder.setEmail(email);
                    builder.setName(name);
                    if(serverPassword != null)
                        builder.setServerPassword(SHA256.hash(serverPassword));
                    Register message = builder.build();
                    boolean success = send(message.toByteArray());
                    if(!success){
                        removeListener(id);
                        if(listener != null)
                            listener.onError(ERROR_SENDFAILURE, "Fail in sending out request.");
                    }
                } catch (Exception ex) {
                    removeListener(id);
                    ex.printStackTrace();
                    if(listener != null)
                        listener.onError(ERROR_EXCEPTION, "Error while processing, refer stack trace.");
                }
            }
        }
        performHookedFunction(id, new Work(), listener);
    }

    public void verify(final String code, final ISCVerify listener){
        if(status == INACTIVE)
            return;

        final int id = new Random().nextInt(Integer.MAX_VALUE);
        class InjectedListener extends ShadowChatAdapter {
            public int getId(){
                return id;
            }

            public void onVerify(int errorCode, int status){
                if(listener != null)
                    listener.onVerify(errorCode, status);
                removeListener(id);
            }
        }
        callbacks.add(new InjectedListener());

        class Work implements Runnable{
            public void run(){
                LogUtil.debug(TAG, "Verifying...");
                try {
                    Verify.Builder builder = Verify.newBuilder();
                    builder.setType(Type.VERIFY);
                    builder.setUsername(getCfg().getUsername());
                    builder.setPassword(getCfg().getPassword());
                    builder.setDevice(getDevice());
                    builder.setCode(code);
                    Verify message = builder.build();
                    boolean success =send(message.toByteArray());
                    if(!success){
                        removeListener(id);
                        if(listener != null)
                            listener.onError(ERROR_SENDFAILURE, "Fail in sending out request.");
                    }
                } catch (Exception ex) {
                    removeListener(id);
                    ex.printStackTrace();
                    if(listener != null)
                        listener.onError(ERROR_EXCEPTION, "Error while processing, refer stack trace.");
                }
            }
        }
        performHookedFunction(id, new Work(), listener);
    }

    public void requestPin(final ISCRequestPin listener) {
        if(status == INACTIVE)
            return;

        final int id = new Random().nextInt(Integer.MAX_VALUE);
        class InjectedListener extends ShadowChatAdapter {
            public int getId(){
                return id;
            }

            public void onRequestPin(int errorCode, int status){
                if(listener != null)
                    listener.onRequestPin(errorCode, status);
                removeListener(id);
            }
        }
        callbacks.add(new InjectedListener());

        class Work implements Runnable{
            public void run(){
                LogUtil.debug(TAG, "Requesting Pin...");
                try {
                    RequestPin.Builder builder = RequestPin.newBuilder();
                    builder.setType(Type.REQUESTPIN);
                    builder.setUsername(getCfg().getUsername());
                    builder.setPassword(getCfg().getPassword());
                    builder.setDevice(getDevice());
                    RequestPin message = builder.build();
                    boolean success =send(message.toByteArray());
                    if(!success){
                        removeListener(id);
                        if(listener != null)
                            listener.onError(ERROR_SENDFAILURE, "Fail in sending out request.");
                    }
                } catch (Exception ex) {
                    removeListener(id);
                    ex.printStackTrace();
                    if(listener != null)
                        listener.onError(ERROR_EXCEPTION, "Error while processing, refer stack trace.");
                }
            }
        }
        performHookedFunction(id, new Work(), listener);
    }

    public void search(final String term, final ISCSearch listener) {
        if(status == INACTIVE)
            return;
        try {
            final int id = new Random().nextInt(Integer.MAX_VALUE);
            class InjectedListener extends ShadowChatAdapter {
                public int getId() {
                    return id;
                }

                public void onSearch(int errorCode, ArrayList<SCUser> users) {
                    removeListener(id);
                    if(listener != null)
                        listener.onSearch(errorCode, users);
                }
            }
            callbacks.add(new InjectedListener());

            class Work implements Runnable {
                public void run() {
                    LogUtil.debug(TAG, "Searching for " + term);
                    try {
                        Search.Builder builder = Search.newBuilder();
                        builder.setType(Type.SEARCH);
                        builder.setTerm(term);
                        Search message = builder.build();
                        boolean success =send(message.toByteArray());
                        if(!success){
                            removeListener(id);
                            if(listener != null)
                                listener.onError(ERROR_SENDFAILURE, "Fail in sending out request.");
                        }
                    } catch (Exception ex) {
                        removeListener(id);
                        ex.printStackTrace();
                        if(listener != null)
                            listener.onError(ERROR_EXCEPTION, "Error while processing, refer stack trace.");
                    }
                }
            }
            performHookedFunctionAuth(id, new Work(), listener);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void addFriendRequest(final String target, final ISCAddFriend listener) {
        if(status == INACTIVE)
            return;

        if(account.getContacts().getUser(target) != null){
            listener.onAddFriend(false, -1, null);
        }

        try {
            final ArrayList<Key> keys = new ArrayList<>();
            for(int i = 0; i < getCfg().getKeySetSize(); i++)
                keys.add(Security.getInstance().generateKey());

            final int id = new Random().nextInt(Integer.MAX_VALUE);
            class InjectedListener extends ShadowChatAdapter {
                public int getId() {
                    return id;
                }

                public void onAddFriend(boolean success, int errorCode, SCUser user) {
                    if(success) {

                        if (account.getContacts() != null) {
                            SCUser usr = SCUser.initAsContact(account.getKeys(),account.getMessagesPath(), user.getUsername(), user.getName(), user.getAvatar(), user.getFlags(), false);
                            for (Key key : keys) {
                                account.getKeys().add(key);
                                usr.addKeyHash(key.getHash());
                            }
                            usr.setFriendStatus(SCUser.FRSTATUS_REQUESTING);
                            account.getContacts().add(usr);
                        }
                    }
                    if(listener != null)
                        listener.onAddFriend(success, errorCode, user);
                    removeListener(id);
                }
            }
            callbacks.add(new InjectedListener());

            class Work implements Runnable {
                public void run() {
                    if (account.getContacts() != null && account.getContacts().exist(target)) {
                        return;
                    }

                    LogUtil.debug(TAG, "Sending friend request to " + target);
                    try {
                        AddFriend.Builder builder = AddFriend.newBuilder();
                        builder.setType(Type.ADDFRIEND);
                        builder.setFriend(target);
                        for(Key key: keys)
                            builder.addKeys(ByteString.copyFrom(key.getMyPublicKey().getEncoded()));
                        AddFriend message = builder.build();
                        boolean success =send(message.toByteArray());
                        if(!success){
                            removeListener(id);
                            if(listener != null)
                                listener.onError(ERROR_SENDFAILURE, "Fail in sending out request.");
                        }
                    } catch (Exception ex) {
                        removeListener(id);
                        ex.printStackTrace();
                        if(listener != null)
                            listener.onError(ERROR_EXCEPTION, "Error while processing, refer stack trace.");
                    }
                }
            }
            performHookedFunctionAuth(id, new Work(), listener);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void acceptFriendRequest(final boolean accept, final String target, final ISCAcceptFriend listener) {
        if(status == INACTIVE)
            return;

        try {
            final ArrayList<Key> keys = account.getKeys().getKeysByHashes(account.getContacts().getUser(target).getKeyHashes(), Key.PENDING);

            final ArrayList<String> hashes = new ArrayList<>();
            for(Key key: keys)
                hashes.add(key.getTheirHash());

            final int id = new Random().nextInt(Integer.MAX_VALUE);
            class InjectedListener extends ShadowChatAdapter {
                public int getId() {
                    return id;
                }

                public void onAcceptFriend(boolean success, int errorCode) {
                    if(success){
                        SCUser user = account.getContacts().getUser(target);
                        if(accept) {
                            user.setFriendStatus(SCUser.FRSTATUS_ACCEPTED);
                            ArrayList<String> hashes = new ArrayList<>();
                            for (Key key : keys) {
                                hashes.add(key.getHash());
                            }
                            account.getKeys().activateKeysByHashes(hashes);

                            account.getContacts().save();
                        }else{
                            user.setFriendStatus(SCUser.FRSTATUS_REJECTED);
                            for (Key key : keys) {
                                account.getKeys().delete(key);
                            }
                            account.getContacts().save();
                        }
                    }
                    if(listener != null)
                        listener.onAcceptFriend(success, errorCode);
                    removeListener(id);
                }
            }
            callbacks.add(new InjectedListener());

            class Work implements Runnable {
                public void run() {
                    LogUtil.debug(TAG, "Accepting friend request from " + target);
                    try {
                        AcceptFriend.Builder builder = AcceptFriend.newBuilder();
                        builder.setType(Type.ACCEPTFRIEND);
                        builder.setAccept(accept);
                        builder.setFriend(target);
                        for(Key key: keys)
                            builder.addKeys(ByteString.copyFrom(key.getMyPublicKey().getEncoded()));
                        for(String hash: hashes)
                            builder.addReferenceKeyHash(hash);
                        AcceptFriend message = builder.build();
                        boolean success =send(message.toByteArray());
                        if(!success){
                            removeListener(id);
                            if(listener != null)
                                listener.onError(ERROR_SENDFAILURE, "Fail in sending out request.");
                        }
                    } catch (Exception ex) {
                        removeListener(id);
                        ex.printStackTrace();
                        if(listener != null)
                            listener.onError(ERROR_EXCEPTION, "Error while processing, refer stack trace.");
                    }
                }
            }
            performHookedFunctionAuth(id, new Work(), listener);
        }catch(Exception ex){ex.printStackTrace();}
    }

    public void getContactList(final ISCGetContactList listener) {
        if(status == INACTIVE)
            return;

        try {
            final int id = new Random().nextInt(Integer.MAX_VALUE);
            class InjectedListener extends ShadowChatAdapter {
                public int getId() {
                    return id;
                }

                public void onGetContactList(int errorCode, ArrayList<SCUser> users) {
                    //sync contacts
                    ArrayList<SCUser> contacts = account.getContacts().getUsers();
                    for(int i = 0; i < users.size(); i++){
                        SCUser user = users.get(i);
                        for(int j = 0; j < contacts.size(); j++){
                            SCUser contact = contacts.get(j);
                            if(contact.getUsername().equals(user.getUsername()) && contact.getName().startsWith("@")){
                                contact.setName(user.getName());
                                contact.setAvatar(user.getAvatar());
                            }
                        }
                    }
                    account.getContacts().update();
                    account.getContacts().save();

                    if(listener != null)
                        listener.onGetContactList(errorCode, users);
                    removeListener(id);
                }
            }
            callbacks.add(new InjectedListener());

            class Work implements Runnable {
                public void run() {
                    LogUtil.debug(TAG, "Fetching contact list from server");
                    try {
                        ListContacts.Builder builder = ListContacts.newBuilder();
                        builder.setType(Type.LISTCONTACTS);
                        ListContacts message = builder.build();
                        boolean success =send(message.toByteArray());
                        if(!success){
                            removeListener(id);
                            if(listener != null)
                                listener.onError(ERROR_SENDFAILURE, "Fail in sending out request.");
                        }
                    } catch (Exception ex) {
                        removeListener(id);
                        ex.printStackTrace();
                        if(listener != null)
                            listener.onError(ERROR_EXCEPTION, "Error while processing, refer stack trace.");
                    }
                }
            }
            performHookedFunctionAuth(id, new Work(), listener);
        }catch(Exception ex){ex.printStackTrace();}
    }

    public void getAvatar(final String hash, final ISCGetAvatar listener) {
        if(status == INACTIVE)
            return;

        try {
            final int id = new Random().nextInt(Integer.MAX_VALUE);
            class InjectedListener extends ShadowChatAdapter {
                public int getId() {
                    return id;
                }

                public void onGetAvatar(int errorCode, byte[] data) {
                    removeListener(id);
                    if(listener != null)
                        listener.onGetAvatar(errorCode, data);
                }
            }
            callbacks.add(new InjectedListener());

            class Work implements Runnable {
                public void run() {
                    LogUtil.debug(TAG, "Fetching avatar with hash of, " + hash);
                    try {
                        GetAvatar.Builder builder = GetAvatar.newBuilder();
                        builder.setType(Type.GETAVATAR);
                        builder.setHash(hash);
                        GetAvatar message = builder.build();
                        boolean success = send(message.toByteArray());
                        if(!success){
                            removeListener(id);
                            if(listener != null)
                                listener.onError(ERROR_SENDFAILURE, "Fail in sending out request.");
                        }
                    } catch (Exception ex) {
                        removeListener(id);
                        ex.printStackTrace();
                        if(listener != null)
                            listener.onError(ERROR_EXCEPTION, "Error while processing, refer stack trace.");
                    }
                }
            }
            performHookedFunctionAuth(id, new Work(), listener);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void createGroup(final String name, final byte[] avatar, final String owner, final ISCCreateGroup listener){
        if(status == INACTIVE)
            return;
        try {
            final int id = new Random().nextInt(Integer.MAX_VALUE);
            class InjectedListener extends ShadowChatAdapter {
                public int getId() {
                    return id;
                }

                public void onCreateGroup(boolean success, int errorCode, SCUser group) {
                    removeListener(id);
                    if(listener != null)
                        listener.onCreateGroup(success, errorCode, group);
                }
            }
            callbacks.add(new InjectedListener());

            class Work implements Runnable {
                public void run() {
                    try {
                        CreateGroup.Builder builder = CreateGroup.newBuilder();
                        builder.setType(Type.CREATEGROUP);
                        builder.setName(name);
                        if(avatar != null)
                            builder.setAvatar(ByteString.copyFrom(avatar));
                        builder.setOwner(owner);
                        CreateGroup message = builder.build();
                        boolean success = send(message.toByteArray());
                        if(!success){
                            removeListener(id);
                            if(listener != null)
                                listener.onError(ERROR_SENDFAILURE, "Fail in sending out request.");
                        }
                    } catch (Exception ex) {
                        removeListener(id);
                        ex.printStackTrace();
                        if(listener != null)
                            listener.onError(ERROR_EXCEPTION, "Error while processing, refer stack trace.");
                    }
                }
            }
            performHookedFunctionAuth(id, new Work(), listener);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void inviteToGroup(final String groupId, final String username, final ISCInviteToGroup listener){
        if(status == INACTIVE)
            return;

        try {
            final Key key = Security.getInstance().generateKey();

            final int id = new Random().nextInt(Integer.MAX_VALUE);
            class InjectedListener extends ShadowChatAdapter {
                public int getId() {
                    return id;
                }

                public void onInviteToGroup(boolean success, int errorCode) {
                    if(errorCode == ShadowChatBase.ERROR_NONE){
                        key.setStatus(key.ACTIVE);
                        key.setTag(Key.TAG_TEMP);
                        account.getKeys().add(key);
                    }
                    removeListener(id);
                    if(listener != null)
                        listener.onInviteToGroup(success, errorCode);
                }
            }
            callbacks.add(new InjectedListener());

            class Work implements Runnable {
                public void run() {
                    try {
                        InviteToGroup.Builder builder = InviteToGroup.newBuilder();
                        builder.setType(Type.INVITETOGROUP);
                        builder.setGroup(groupId);
                        builder.setUser(username);
                        builder.setKey(ByteString.copyFrom(key.getMyPublicKey().getEncoded()));

                        InviteToGroup message = builder.build();
                        boolean success = send(message.toByteArray());
                        if(!success){
                            removeListener(id);
                            if(listener != null)
                                listener.onError(ERROR_SENDFAILURE, "Fail in sending out request.");
                        }
                    } catch (Exception ex) {
                        removeListener(id);
                        ex.printStackTrace();
                        if(listener != null)
                            listener.onError(ERROR_EXCEPTION, "Error while processing, refer stack trace.");
                    }
                }
            }
            performHookedFunctionAuth(id, new Work(), listener);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void joinGroup(final String groupId, final boolean accept, final ISCJoinGroup listener){
        if(status == INACTIVE)
            return;

        try {
            final ArrayList<Key> keys = account.getKeys().getKeysByHashes(account.getContacts().getGroup(groupId).getKeyHashes(), Key.PENDING);

            for(int i = 0; i < keys.size(); i++){
                Key k = keys.get(i);
                if(k.getTag() != Key.TAG_TEMP) {
                    keys.remove(i);
                    i--;
                }
            }

            //choose the latest key
            Key key = null;
            if(keys.size() > 1){
                 key = keys.get(0);

                for(int i = 1; i < keys.size(); i++){
                    if(keys.get(i).getTimestamp() > key.getTimestamp()){
                        key = keys.get(i);
                    }
                }
            }else if(keys.size() == 0){
                if(listener != null)
                    listener.onError(ERROR_EXCEPTION, "No keys found.");
            }

            final int id = new Random().nextInt(Integer.MAX_VALUE);
            class InjectedListener extends ShadowChatAdapter {
                public int getId() {
                    return id;
                }

                public void onJoinGroup(boolean success, int errorCode) {
                    if(success){
                        SCUser user = account.getContacts().getGroup(groupId);
                        if(accept) {
                            user.setFriendStatus(SCUser.FRSTATUS_PENDING);
                            ArrayList<String> hashes = new ArrayList<>();
                            for (Key key : keys) {
                                hashes.add(key.getHash());
                            }
                            account.getKeys().activateKeysByHashes(hashes);
                            account.getContacts().save();
                        }else{
                            user.setFriendStatus(SCUser.FRSTATUS_REJECTED);
                            for (Key key : keys) {
                                account.getKeys().delete(key);
                            }
                            account.getContacts().save();
                        }
                    }
                    if(listener != null)
                        listener.onJoinGroup(success, errorCode);
                    removeListener(id);
                }
            }
            callbacks.add(new InjectedListener());

            final Key mKey = key;
            class Work implements Runnable {
                public void run() {
                    LogUtil.debug(TAG, "Joining/Rejecting to join " + groupId);
                    try {
                        JoinGroup.Builder builder = JoinGroup.newBuilder();
                        builder.setType(Type.JOINGROUP);
                        builder.setAccept(accept);
                        builder.setGroup(groupId);
                        builder.setKey(ByteString.copyFrom(mKey.getMyPublicKey().getEncoded()));
                        builder.setReferenceKeyHash(mKey.getTheirHash());
                        JoinGroup message = builder.build();
                        boolean success =send(message.toByteArray());
                        if(!success){
                            removeListener(id);
                            if(listener != null)
                                listener.onError(ERROR_SENDFAILURE, "Fail in sending out request.");
                        }
                    } catch (Exception ex) {
                        removeListener(id);
                        ex.printStackTrace();
                        if(listener != null)
                            listener.onError(ERROR_EXCEPTION, "Error while processing, refer stack trace.");
                    }
                }
            }
            performHookedFunctionAuth(id, new Work(), listener);
        }catch(Exception ex){ex.printStackTrace();}
    }


    public void sendText(SCText text){
        if (status == INACTIVE)
            return;

        if (connectionStatus < HANDSHAKE) {
            LogUtil.debug(TAG, "Not yet connected to server!");
            connect();
        }else if(connectionStatus != AUTHENTICATED){
            authenticate();
        }

        LogUtil.debug(TAG, "Sending message to " + text.getTarget());

        //store message in local
        if(text.isGroupMessage()){
            SCUser user = account.getContacts().getGroup(text.getGroup());
            user.addMessage(true, text);
        }else{
            SCUser user = account.getContacts().getUser(text.getTarget());
            user.addMessage(true, text);
        }

        //build message
        ArrayList<SimpleMessage> list = text.build(new ModInterface());
        for(SimpleMessage msg: list){
            account.getQueue().add(msg.toByteArray(), false, true);
        }
        if(sender != null)
            sender.trigger();
    }

    public void sendGallery(SCGallery gallery){
        if (status == INACTIVE)
            return;

        if (connectionStatus < HANDSHAKE) {
            LogUtil.debug(TAG, "Not yet connected to server!");
            connect();
        }else if(connectionStatus != AUTHENTICATED){
            authenticate();
        }

        LogUtil.debug(TAG, "Sending message to " + gallery.getTarget());

        //store message in local
        if(gallery.isGroupMessage()){
            SCUser user = account.getContacts().getUser(gallery.getTarget());
            user.addMessage(true, gallery);
        }else{
            SCUser user = account.getContacts().getUser(gallery.getTarget());
            user.addMessage(true, gallery);
        }

        //build message
        ArrayList<GalleryMessage> list = gallery.build(new ModInterface());
        for(GalleryMessage msg: list){
            account.getQueue().add(msg.toByteArray(), false, true);
        }

        if(sender != null)
            sender.trigger();
    }

    public void sendJson(SCJson json){
        if (status == INACTIVE)
            return;

        if (connectionStatus < HANDSHAKE) {
            LogUtil.debug(TAG, "Not yet connected to server!");
            connect();
        }else if(connectionStatus != AUTHENTICATED){
            authenticate();
        }

        LogUtil.debug(TAG, "Sending message to " + json.getTarget());

        //store message in local
        if(json.isGroupMessage()){
            SCUser user = account.getContacts().getUser(json.getTarget());
            user.addMessage(true, json);
        }else{
            SCUser user = account.getContacts().getUser(json.getTarget());
            user.addMessage(true, json);
        }

        //build message
        ArrayList<JsonMessage> list = json.build(new ModInterface());
        for(JsonMessage msg: list){
            account.getQueue().add(msg.toByteArray(), false, true);
        }

        if(sender != null)
            sender.trigger();
    }

    public void sendNudge(SCNudge nudge){
        if (status == INACTIVE)
            return;

        if (connectionStatus < HANDSHAKE) {
            LogUtil.debug(TAG, "Not yet connected to server!");
            connect();
        }else if(connectionStatus != AUTHENTICATED){
            authenticate();
        }

        LogUtil.debug(TAG, "Sending message to " + nudge.getTarget());

        //store message in local
        if(nudge.isGroupMessage()){
            SCUser user = account.getContacts().getUser(nudge.getTarget());
            user.addMessage(true, nudge);
        }else{
            SCUser user = account.getContacts().getUser(nudge.getTarget());
            user.addMessage(true, nudge);
        }

        //build message
        NudgeMessage msg = nudge.build(new ModInterface());
        account.getQueue().add(msg.toByteArray(), false, true);

        if(sender != null)
            sender.trigger();
    }


    private void performHookedFunction(final int id, final Runnable work, final ISCBase listener){
        class Work implements Runnable{
            int interval = 100;
            int timeout = 10000;
            public void run(){
                if(connectionStatus < HANDSHAKE) {
                    LogUtil.debug(TAG, "Not yet connected to server!");
                    connect();

                    for(int i = 0; i < timeout/interval; i++){
                        try {
                            Thread.sleep(100);
                        }catch(Exception e){}

                        if(connectionStatus >= HANDSHAKE)
                            break;
                    }
                }

                work.run();

                for(int i = 0; i < timeout/interval; i++){
                    try {
                        Thread.sleep(100);
                    }catch(Exception e){}

                    if(!haveListener(id))
                        break;
                }

                if(haveListener(id)){
                    removeListener(id);
                    listener.onError(ERROR_TIMEOUT, "Exceed timeout limit.");
                }
            }
        }
        pool.execute(new Work());
    }

    private void performHookedFunctionAuth(final int id, final Runnable work, final ISCBase listener){
        class Work implements Runnable{
            int interval = 100;
            int timeout = 10000;
            public void run(){
                if(connectionStatus < HANDSHAKE) {
                    LogUtil.debug(TAG, "Not yet connected to server!");
                    connect();

                    for(int i = 0; i < timeout/interval; i++){
                        try {
                            Thread.sleep(interval);
                        }catch(Exception e){}

                        if(connectionStatus >= HANDSHAKE)
                            break;
                    }
                }

                if(connectionStatus < AUTHENTICATED){
                    LogUtil.debug(TAG, "Not yet authenticated to server!");
                    authenticate();
                    for(int i = 0; i < timeout/interval; i++){
                        try {
                            Thread.sleep(interval);
                        }catch(Exception e){}

                        if(connectionStatus >= AUTHENTICATED)
                            break;
                    }
                }

                work.run();

                for(int i = 0; i < timeout/interval; i++){
                    try {
                        Thread.sleep(interval);
                    }catch(Exception e){}

                    if(!haveListener(id))
                        break;
                }

                if(haveListener(id)){
                    removeListener(id);
                    listener.onError(ERROR_TIMEOUT, "Exceed timeout limit.");
                }
            }
        }
        pool.execute(new Work());
    }

    private synchronized void connect(){
        if(status == INACTIVE)
            return;

        boolean isStillConnected = false;
        if(connectionStatus >= CONNECTED){
            if(socket != null){
                isStillConnected = true;
            }
        }

        if(!isStillConnected){
            LogUtil.debug(TAG, "Connecting to server.");
            for(IShadowChat cb: callbacks)
                cb.onConnecting();

            if(System.currentTimeMillis()-lastConnectRequest < 30000 && connectionStatus == CONNECTING)
                return;

            connectionStatus = CONNECTING;
            lastConnectRequest = System.currentTimeMillis();

            if(getCfg().isUsingSSL()){
                if(getCfg().getSslCertFile() != null && getCfg().getSslCertkey() != null){
                    socket = new TCPSocket(getCfg().getIp(), getCfg().getPort(), false, true, getCfg().isSslTrustAll(), getCfg().getSslCertFile(), getCfg().getSslCertkey());
                }else{
                    socket = new TCPSocket(getCfg().getIp(), getCfg().getPort(), false, true, getCfg().isSslTrustAll(), null, null);
                }
            }else{
                socket = new TCPSocket(getCfg().getIp(), getCfg().getPort(), false, false, false, null, null);
            }
            socket.attach(new Listener());
            socket.start();
        }
    }

    private synchronized void disconnect(){
        if(status == INACTIVE)
            return;
        if(socket != null){
            try {
                socket.close(connectionId);
            }catch(Exception ex){}
        }
    }

    private void handshake() {
        if (status == INACTIVE || connectionStatus != CONNECTED) {
            LogUtil.debug(TAG, "Unable to perform handshake due to not connected!");
            for(IShadowChat cb: callbacks)
                cb.onHandshake(false, "Unable to perform handshake due to not connected!");
            return;
        }

        try {
            LogUtil.debug(TAG, "Performing handshake.");
            Initial.Builder builder = Initial.newBuilder();
            builder.setType(Message.Type.INITIAL);
            Initial message = builder.build();
            send(message.toByteArray());
        } catch (Exception ex) {ex.printStackTrace();}
    }

    private boolean send(byte[] data) throws Exception{
        if(data == null || data.length == 0)
            return false;
        LogUtil.debug(TAG, "Sending " + data.length + " bytes");
        return socket.send(connectionId, data);
    }

    class Listener extends TCPListenAdapter {
        public void onClientConnected(TCPClient client) {
            LogUtil.debug(TAG, "Connected to server.");
            connectionStatus = CONNECTED;
            connectionId = client.uid;

            sender = new Sender();
            sender.start();

            for(int i = 0; i < callbacks.size(); i++)
                callbacks.get(i).onConnected();
            handshake();
        }

        public void onClientDisconnected(TCPClient client) {
            LogUtil.debug(TAG, "Disconnected from server.");
            connectionStatus = DISCONNECTED;
            socket = null;
            if(sender != null)
                sender.kill();
            sender = null;

            for(int i = 0; i < callbacks.size(); i++)
                callbacks.get(i).onDisconnect("Timeout");
        }

        public void onError(Exception ex){
            if(connectionStatus == CONNECTING)
                connectionStatus = DISCONNECTED;
            for(IShadowChat cb: callbacks)
                cb.onConnectionError(ex != null ? ex.getMessage() : "");
        }

        public void onReceive(TCPClient client, byte[] data){
            try {
                LogUtil.debug(TAG, "Receiving " + data.length + " bytes");
                if(connectionStatus == CONNECTED){
                    InitialResponse message = InitialResponse.parseFrom(data);
                    connectionStatus = HANDSHAKE;

                    serverInfo = new Gson().fromJson(message.getInfo(), SCServerInfo.class);
                    System.out.println(message.getInfo());

                    for (IShadowChat cb : callbacks)
                        cb.onHandshake(true, null);

                    if (getCfg().getUsername() != null && !getCfg().getUsername().isEmpty() &&
                            getCfg().getPassword() != null && !getCfg().getPassword().isEmpty()) {
                        authenticate();
                    }

                }else {
                    Base base = Base.parseFrom(data);
                    for (Module module : modules) {
                        if (module.getType() == base.getType()) {
                            module.onReceive(client, data);
                        }
                    }
                }
            }catch(Exception ex){
                LogUtil.error(TAG, "Fail in parsing packet. Error: " + ex.getMessage());
            }
        }
    }

    public Builder builder = null;

    public Builder getBuilder(){
        if(builder == null)
            builder = new Builder();
        return builder;
    }

    public class Builder{
        public SCText buildSCText(SCUser target, String message, ArrayList<SCEmoji> emojis){
            SCText msg =  new SCText(account.getUsername(), target, message);
            if(emojis != null)
                msg.setEmojis(emojis);
            if(target.isGroup()){
                msg.setGroup(target.getUsername());
            }
            return msg;
        }

        public SCGallery buildSCGallery(SCUser target, String message,  ArrayList<byte[]> images, ArrayList<SCEmoji> emojis){
            SCGallery msg =  new SCGallery(account.getUsername(), target, message);
            if(emojis != null)
                msg.setEmojis(emojis);
            for(int i = 0; i < images.size(); i++){
                try {
                    String hash = SHA256.hashString(images.get(i));
                    String path = account.getImagePath() + hash;
                    ShadowChatMedia file = new ShadowChatMedia(account.getKeys(), path, context, ShadowChatMedia.IMAGE);
                    if(!file.exists()){
                        if(cfg.getSecurityLevel() < Security.HIGH)
                            file.enableProtection(false);
                        file.saveImage(images.get(i));
                    }
                    SCImage img = new SCImage(i, hash);
                    msg.addImage(img);
                }catch(Exception e){}
            }
            if(target.isGroup()){
                msg.setGroup(target.getUsername());
            }
            return msg;
        }

        public SCJson buildSCJson( SCUser target, String message, String json, ArrayList<SCEmoji> emojis){
            SCJson msg =  new SCJson(account.getUsername(), target, message, json);
            if(emojis != null)
                msg.setEmojis(emojis);
            if(target.isGroup()){
                msg.setGroup(target.getUsername());
            }
            return msg;
        }

        public SCNudge buildSCNudge(SCUser target){
            SCNudge msg =  new SCNudge(account.getUsername(), target);
            if(target.isGroup()){
                msg.setGroup(target.getUsername());
            }
            return msg;
        }

        public SCECDHKeyExchange buildSCKeyExchange(SCUser target){
            SCECDHKeyExchange msg =  new SCECDHKeyExchange(account.getUsername(), target);
            if(target.isGroup()){
                msg.setGroup(target.getUsername());
            }
            return msg;
        }
    }

    public boolean equals(ShadowChatServer server){
        return server.getCfg().equals(cfg);
    }
}
