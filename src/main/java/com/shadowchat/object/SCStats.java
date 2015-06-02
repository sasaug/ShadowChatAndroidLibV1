package com.shadowchat.object;

import java.util.Comparator;

public class SCStats {
    private long lastOnline = 0;
    private long lastActivity = 0;
    private int unreadMessages = 0;

    public static Comparator<SCUser> LastActivityComparator
            = new Comparator<SCUser>() {
        public int compare(SCUser item1, SCUser item2) {
            long time1 = item1.getStats().getLastActivity();
            long time2 = item2.getStats().getLastActivity();
            long diff = time2 - time1;   //descending
            return (int)diff;
        }

    };


    public long getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }

    public int getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(int unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public void increaseUnreadMessages(int unreadMessages) {
        this.unreadMessages += unreadMessages;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }
}
