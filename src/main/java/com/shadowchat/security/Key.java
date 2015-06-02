package com.shadowchat.security;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Key {

    public static final int DISABLED = 1;   //key is not suppose to be used in any case
    public static final int INACTIVE = 2;   //key is no longer active but still used to decrypt older communication
    public static final int PENDING = 3;     //key is still new and haven't agreed to be used by both parties
    public static final int ACTIVE = 4;      //current active keys to use

    public static final int TAG_TEMP = 999;

	private PublicKey myPublicKey = null;
	private PrivateKey myPrivateKey = null;
    private PublicKey theirPublicKey = null;
    private int status = INACTIVE;
    private long timestamp = 0;

    private byte[] secret = null;
    private String myPublicHash = null;
    private String theirPublicHash = null;

    /*
    *   FILES related tag should use - 1xx (100, 101...199)
    *   TEMP tag should be 999
    * */
     private int tag = 0;


    public Key(){
        timestamp = System.currentTimeMillis()/1000L;
    }

    public byte[] getSecret() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
        if(secret == null) {
            secret = Security.getInstance().getAgreementSecret(myPrivateKey, theirPublicKey);
        }
        return secret;
    }

    public String getTheirHash(){
        if(theirPublicHash == null) {
            try {
                theirPublicHash = SHA256.hashString(theirPublicKey.getEncoded());
            }catch(Exception ex){}
        }
        return theirPublicHash;
    }

    public String getHash(){
        if(myPublicHash == null) {
            try {
                myPublicHash = SHA256.hashString(myPublicKey.getEncoded());
            }catch(Exception ex){}
        }
        return myPublicHash;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public PublicKey getMyPublicKey() {
        return myPublicKey;
    }

    public void setMyPublicKey(PublicKey myPublicKey) {
        this.myPublicKey = myPublicKey;
    }

    public PrivateKey getMyPrivateKey() {
        return myPrivateKey;
    }

    public void setMyPrivateKey(PrivateKey myPrivateKey) {
        this.myPrivateKey = myPrivateKey;
    }

    public PublicKey getTheirPublicKey() {
        return theirPublicKey;
    }

    public void setTheirPublicKey(PublicKey theirPublicKey) {
        this.theirPublicKey = theirPublicKey;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }
}
