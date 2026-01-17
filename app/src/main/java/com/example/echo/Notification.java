package com.example.echo;

import com.google.firebase.Timestamp;

public class Notification {
    private String notificationId;
    private String recipientId;      // Bildirimi alacak kişi
    private String senderId;         // Bildirimi gönderen kişi
    private String senderName;       // Gönderen kişinin adı
    private String senderUsertag;    // Gönderen kişinin usertag'i
    private String type;             // LIKE_POST, COMMENT, FOLLOW, LIKE_COMMENT
    private String targetId;         // Post ID veya Comment ID
    private String content;          // Yorum içeriği (sadece COMMENT türünde)
    private Timestamp timestamp;
    private boolean isRead;          // Okundu mu?
    private String timeAgo;          // "2dk", "5sa" gibi

    // Bildirim türleri
    public static final String TYPE_LIKE_POST = "LIKE_POST";
    public static final String TYPE_LIKE_COMMENT = "LIKE_COMMENT";
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_FOLLOW = "FOLLOW";

    // Firestore için boş constructor (ZORUNLU)
    public Notification() {
    }

    // Yeni bildirim oluşturmak için constructor
    public Notification(String recipientId, String senderId, String senderName,
                        String senderUsertag, String type, String targetId) {
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderUsertag = senderUsertag;
        this.type = type;
        this.targetId = targetId;
        this.isRead = false;
    }

    // Comment için özel constructor (içerik ile)
    public Notification(String recipientId, String senderId, String senderName,
                        String senderUsertag, String type, String targetId, String content) {
        this(recipientId, senderId, senderName, senderUsertag, type, targetId);
        this.content = content;
    }

    // Getters
    public String getNotificationId() { return notificationId; }
    public String getRecipientId() { return recipientId; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getSenderUsertag() { return senderUsertag; }
    public String getType() { return type; }
    public String getTargetId() { return targetId; }
    public String getContent() { return content; }
    public Timestamp getTimestamp() { return timestamp; }
    public boolean isRead() { return isRead; }
    public String getTimeAgo() { return timeAgo; }

    // Setters
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public void setSenderUsertag(String senderUsertag) { this.senderUsertag = senderUsertag; }
    public void setType(String type) { this.type = type; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { isRead = read; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }

    /**
     * Bildirim mesajını döndürür
     */
    public String getMessage() {
        switch (type) {
            case TYPE_LIKE_POST:
                return "gönderini beğendi";
            case TYPE_LIKE_COMMENT:
                return "yorumunu beğendi";
            case TYPE_COMMENT:
                return "gönderine yorum yaptı";
            case TYPE_FOLLOW:
                return "seni takip etti";
            default:
                return "bir işlem yaptı";
        }
    }

    /**
     * Bildirim ikonu döndürür
     */
    public int getIcon() {
        switch (type) {
            case TYPE_LIKE_POST:
            case TYPE_LIKE_COMMENT:
                return android.R.drawable.btn_star_big_on; // Beğeni ikonu
            case TYPE_COMMENT:
                return android.R.drawable.ic_menu_edit; // Yorum ikonu
            case TYPE_FOLLOW:
                return android.R.drawable.ic_menu_myplaces; // Takip ikonu
            default:
                return android.R.drawable.ic_dialog_info; // Varsayılan
        }
    }
}