package com.example.echo;

import android.util.Log;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Merkezi Bildirim Yönetim Sistemi
 * Tüm bildirim oluşturma işlemlerini yönetir
 */
public class NotificationManager {

    private static final String TAG = "NotificationManager";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private final FirebaseFirestore db;
    private final String currentUserId;

    public NotificationManager(FirebaseFirestore db, String currentUserId) {
        this.db = db;
        this.currentUserId = currentUserId;
    }

    /**
     * Post beğeni bildirimi oluştur
     */
    public void createLikePostNotification(String postId, String postOwnerId,
                                           String senderName, String senderUsertag) {
        // Kendi postunu beğendiyse bildirim gönderme
        if (currentUserId.equals(postOwnerId)) {
            return;
        }

        createNotification(
                postOwnerId,
                Notification.TYPE_LIKE_POST,
                postId,
                senderName,
                senderUsertag,
                null
        );
    }

    /**
     * Comment beğeni bildirimi oluştur
     */
    public void createLikeCommentNotification(String commentId, String commentOwnerId,
                                              String postId, String senderName,
                                              String senderUsertag) {
        // Kendi yorumunu beğendiyse bildirim gönderme
        if (currentUserId.equals(commentOwnerId)) {
            return;
        }

        createNotification(
                commentOwnerId,
                Notification.TYPE_LIKE_COMMENT,
                postId,
                senderName,
                senderUsertag,
                null
        );
    }

    /**
     * Yorum bildirimi oluştur
     */
    public void createCommentNotification(String postId, String postOwnerId,
                                          String senderName, String senderUsertag,
                                          String commentContent) {
        // Kendi postuna yorum yaptıysa bildirim gönderme
        if (currentUserId.equals(postOwnerId)) {
            return;
        }

        createNotification(
                postOwnerId,
                Notification.TYPE_COMMENT,
                postId,
                senderName,
                senderUsertag,
                commentContent
        );
    }

    /**
     * Takip bildirimi oluştur
     */
    public void createFollowNotification(String followedUserId, String senderName,
                                         String senderUsertag) {
        createNotification(
                followedUserId,
                Notification.TYPE_FOLLOW,
                currentUserId, // targetId olarak follower'ın ID'si
                senderName,
                senderUsertag,
                null
        );
    }

    /**
     * Genel bildirim oluşturma metodu
     */
    private void createNotification(String recipientId, String type, String targetId,
                                    String senderName, String senderUsertag,
                                    String content) {
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("recipientId", recipientId);
        notificationData.put("senderId", currentUserId);
        notificationData.put("senderName", senderName);
        notificationData.put("senderUsertag", senderUsertag);
        notificationData.put("type", type);
        notificationData.put("targetId", targetId);
        notificationData.put("timestamp", FieldValue.serverTimestamp());
        notificationData.put("read", false);

        if (content != null) {
            notificationData.put("content", content);
        }

        db.collection(COLLECTION_NOTIFICATIONS)
                .add(notificationData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Bildirim oluşturuldu: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Bildirim oluşturulamadı", e);
                });
    }

    /**
     * Post beğeni bildirimini sil (beğeni geri alındığında)
     */
    public void deleteLikePostNotification(String postId, String postOwnerId) {
        deleteNotification(postOwnerId, Notification.TYPE_LIKE_POST, postId);
    }

    /**
     * Comment beğeni bildirimini sil (beğeni geri alındığında)
     */
    public void deleteLikeCommentNotification(String commentId, String commentOwnerId, String postId) {
        deleteNotification(commentOwnerId, Notification.TYPE_LIKE_COMMENT, postId);
    }

    /**
     * Takip bildirimini sil (takip geri alındığında)
     */
    public void deleteFollowNotification(String followedUserId) {
        deleteNotification(followedUserId, Notification.TYPE_FOLLOW, currentUserId);
    }

    /**
     * Bildirim silme metodu
     */
    private void deleteNotification(String recipientId, String type, String targetId) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientId", recipientId)
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("type", type)
                .whereEqualTo("targetId", targetId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    queryDocumentSnapshots.forEach(document -> {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Bildirim silindi: " + document.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Bildirim silinemedi", e);
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Bildirim sorgusu başarısız", e);
                });
    }

    /**
     * Kullanıcının okunmamış bildirim sayısını al
     */
    public void getUnreadNotificationCount(OnCountCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
                .whereEqualTo("recipientId", currentUserId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    callback.onResult(count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Okunmamış bildirim sayısı alınamadı", e);
                    callback.onResult(0);
                });
    }

    // Callback Interface
    public interface OnCountCallback {
        void onResult(int count);
    }
}