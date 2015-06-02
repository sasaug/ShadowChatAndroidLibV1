package com.shadowchat;


import com.shadowchat.security.SHA256;
import com.shadowchat.security.Security;

import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.util.Random;

public class ShadowChatConfig {

    private static final int DEFAULT_PORT = 8080;

    private int id;

    private String ip;
    private int port = DEFAULT_PORT;
    private int threads = 4;
    private int keySetSize = 5;

    private String name = "";
    private String username;
    private String password;

    private String publicPath;
    private String privatePath;

    private String profileName;
    private String profileAvatar;
    private String profileEmail;
    private String profilePhone;
    private String serverPassword;

    private String gcmSenderId;
    private String cloudId;
    private int appVersion;

    private boolean useSSL = true;
    private boolean sslTrustAll = true;
    private String sslCertFile = null;
    private String sslCertkey = null;

    private int securityLevel = Security.NORMAL;

    public ShadowChatConfig(){
        this.id = new Random().nextInt();
    }

    public int getId(){
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        try {
            this.password = SHA256.hash(password);
        }catch(Exception e){
            this.password = password;
        }
    }

    public String getIp() {
        return ip;
    }

    public String getAddress(){
        if(port == DEFAULT_PORT)
            return ip;
        else
            return ip + ":" + port;
    }

    public void setAddress(String address) throws Exception {
        if(address.contains(":")) {
            String host = address.substring(0, address.indexOf(":"));
            address = address.substring(address.indexOf(":")+1);
            int port = Integer.parseInt(address);
            if(InetAddressValidator.getInstance().isValid(host) ||
                    DomainValidator.getInstance().isValid(host)){
                this.ip = host;
                this.port = port;
            }else{
                throw new Exception ("Invalid address.");
            }
        }else{
            if(InetAddressValidator.getInstance().isValid(address) ||
                    DomainValidator.getInstance().isValid(address)){
                this.ip = address;
            }else{
                throw new Exception ("Invalid address.");
            }
        }
    }

    public int getPort() {
        return port;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public String getPublicPath() {
        return publicPath;
    }

    public void setPublicPath(String publicPath) {
        this.publicPath = publicPath;
    }

    public String getPrivatePath() {
        return privatePath;
    }

    public void setPrivatePath(String privatePath) {
        this.privatePath = privatePath;
    }

    public int getKeySetSize() {
        return keySetSize;
    }

    protected void setKeySetSize(int keySetSize) {
        this.keySetSize = keySetSize;
    }

    public boolean equals(ShadowChatConfig cfg){
        if(cfg == null)
            return false;

        return cfg.getIp().equals(ip) &&
                cfg.getPort() == port &&
                cfg.getName().equals(name);
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getProfileAvatar() {
        return profileAvatar;
    }

    public void setProfileAvatar(String profileAvatar) {
        this.profileAvatar = profileAvatar;
    }

    public boolean isUsingSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public String getSslCertFile() {
        return sslCertFile;
    }

    public void setSslCertFile(String sslCertFile) {
        this.sslCertFile = sslCertFile;
    }

    public String getSslCertkey() {
        return sslCertkey;
    }

    public void setSslCertkey(String sslCertkey) {
        this.sslCertkey = sslCertkey;
    }

    public boolean isSslTrustAll() {
        return sslTrustAll;
    }

    public void setSslTrustAll(boolean sslTrustAll) {
        this.sslTrustAll = sslTrustAll;
    }

    public String getProfileEmail() {
        return profileEmail;
    }

    public void setProfileEmail(String profileEmail) {
        this.profileEmail = profileEmail;
    }

    public String getProfilePhone() {
        return profilePhone;
    }

    public void setProfilePhone(String profilePhone) {
        this.profilePhone = profilePhone;
    }

    public String getServerPassword() {
        return serverPassword;
    }

    public void setServerPassword(String serverPassword) {
        this.serverPassword = serverPassword;
    }

    public String getGcmSenderId() {
        return gcmSenderId;
    }

    public void setGcmSenderId(String gcmSenderId) {
        this.gcmSenderId = gcmSenderId;
    }

    public String getCloudId() {
        return cloudId;
    }

    public void setCloudId(String cloudId) {
        this.cloudId = cloudId;
    }

    public int getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(int appVersion) {
        this.appVersion = appVersion;
    }

    public int getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(int securityLevel) {
        this.securityLevel = securityLevel;
    }
}
