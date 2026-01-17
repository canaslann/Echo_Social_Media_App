package com.example.echo;

import android.util.Log;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class LikeManager {

    private static final String TAG = "LikeManager";
    private static final String COLLECTION_LIKES = "likes";
    private static final String COLLECTION_POSTS = "posts";
    private static final String COLLECTION_COMMENTS = "comments";
    private final FirebaseFirestore db;
    private final String currentUserId;
    private final NotificationManager notificationManager; // ✨ YENİ

    public LikeManager(FirebaseFirestore db, String currentUserId) {
        this.db = db;
        this.currentUserId = currentUserId;
        this.notificationManager = new NotificationManager(db, currentUserId); // ✨ YENİ
    }

    /**
     * Post'u beğen/beğeniyi kaldır
     */
    public void togglePostLike(String postId, boolean isCurrentlyLiked, OnLikeCallback callback) {
        if (currentUserId == null) {
            callback.onFailure("Kullanıcı oturumu bulunamadı");
            return;
        }

        if (isCurrentlyLiked) {
            // Beğeniyi kaldır
            unlikeTarget(postId, "post", COLLECTION_POSTS, callback);
        } else {
            // Beğen
            likeTarget(postId, "post", COLLECTION_POSTS, callback);
        }
    }

    /**
     * Comment'i beğen/beğeniyi kaldır
     */
    public void toggleCommentLike(String commentId, boolean isCurrentlyLiked, OnLikeCallback callback) {
        if (currentUserId == null) {
            callback.onFailure("Kullanıcı oturumu bulunamadı");
            return;
        }

        if (isCurrentlyLiked) {
            // Beğeniyi kaldır
            unlikeTarget(commentId, "comment", COLLECTION_COMMENTS, callback);
        } else {
            // Beğen
            likeTarget(commentId, "comment", COLLECTION_COMMENTS, callback);
        }
    }

    /**
     * Beğeni ekle (Generic)
     * ✨ BİLDİRİM OLUŞTURMA EKLENDİ
     */
    private void likeTarget(String targetId, String targetType, String targetCollection, OnLikeCallback callback) {
        // 1. Likes collection'a ekle
        Map<String, Object> likeData = new HashMap<>();
        likeData.put("userId", currentUserId);
        likeData.put("targetId", targetId);
        likeData.put("targetType", targetType);
        likeData.put("timestamp", FieldValue.serverTimestamp());

        db.collection(COLLECTION_LIKES)
                .add(likeData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Like eklendi: " + documentReference.getId());

                    // 2. Target'in likeCount'unu artır
                    db.collection(targetCollection)
                            .document(targetId)
                            .update("likeCount", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "likeCount artırıldı");

                                // ✨ 3. BİLDİRİM OLUŞTUR
                                createLikeNotification(targetId, targetType, targetCollection);

                                callback.onSuccess(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "likeCount artırılamadı", e);
                                callback.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Like eklenemedi", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * Beğeniyi kaldır (Generic)
     * ✨ BİLDİRİM SİLME EKLENDİ
     */
    private void unlikeTarget(String targetId, String targetType, String targetCollection, OnLikeCallback callback) {
        // 1. Likes collection'dan sil
        db.collection(COLLECTION_LIKES)
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("targetId", targetId)
                .whereEqualTo("targetType", targetType)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.w(TAG, "Silinecek like bulunamadı");
                        callback.onFailure("Beğeni bulunamadı");
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Like silindi: " + document.getId());

                                    // 2. Target'in likeCount'unu azalt
                                    db.collection(targetCollection)
                                            .document(targetId)
                                            .update("likeCount", FieldValue.increment(-1))
                                            .addOnSuccessListener(aVoid2 -> {
                                                Log.d(TAG, "likeCount azaltıldı");

                                                // ✨ 3. BİLDİRİMİ SİL
                                                deleteLikeNotification(targetId, targetType, targetCollection);

                                                callback.onSuccess(false);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "likeCount azaltılamadı", e);
                                                callback.onFailure(e.getMessage());
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Like silinemedi", e);
                                    callback.onFailure(e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Like sorgusu başarısız", e);
                    callback.onFailure(e.getMessage());
                });
    }

    /**
     * ✨ YENİ: Beğeni bildirimi oluştur
     */
    private void createLikeNotification(String targetId, String targetType, String targetCollection) {
        // Önce target'ın sahibini bul
        db.collection(targetCollection)
                .document(targetId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String ownerId = documentSnapshot.getString("userId");

                        // Kendi içeriğini beğendiyse bildirim gönderme
                        if (ownerId == null || ownerId.equals(currentUserId)) {
                            return;
                        }

                        // Kullanıcı bilgilerini al
                        db.collection("users").document(currentUserId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    String senderName = userDoc.getString("fullName");
                                    String senderUsertag = userDoc.getString("usertag");

                                    // Bildirim türüne göre oluştur
                                    if ("post".equals(targetType)) {
                                        notificationManager.createLikePostNotification(
                                                targetId, ownerId, senderName, senderUsertag
                                        );
                                    } else if ("comment".equals(targetType)) {
                                        // Comment için postId'yi bul
                                        String postId = documentSnapshot.getString("postId");
                                        notificationManager.createLikeCommentNotification(
                                                targetId, ownerId, postId, senderName, senderUsertag
                                        );
                                    }
                                });
                    }
                });
    }

    /**
     * ✨ YENİ: Beğeni bildirimini sil
     */
    private void deleteLikeNotification(String targetId, String targetType, String targetCollection) {
        db.collection(targetCollection)
                .document(targetId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String ownerId = documentSnapshot.getString("userId");

                        if (ownerId == null || ownerId.equals(currentUserId)) {
                            return;
                        }

                        // Bildirim türüne göre sil
                        if ("post".equals(targetType)) {
                            notificationManager.deleteLikePostNotification(targetId, ownerId);
                        } else if ("comment".equals(targetType)) {
                            String postId = documentSnapshot.getString("postId");
                            notificationManager.deleteLikeCommentNotification(targetId, ownerId, postId);
                        }
                    }
                });
    }

    /**
     * Kullanıcının post'u beğenip beğenmediğini kontrol et
     */
    public void checkPostLikeStatus(String postId, OnLikeCheckCallback callback) {
        checkLikeStatus(postId, "post", callback);
    }

    /**
     * Kullanıcının comment'i beğenip beğenmediğini kontrol et
     */
    public void checkCommentLikeStatus(String commentId, OnLikeCheckCallback callback) {
        checkLikeStatus(commentId, "comment", callback);
    }

    /**
     * Beğeni durumunu kontrol et (Generic)
     */
    private void checkLikeStatus(String targetId, String targetType, OnLikeCheckCallback callback) {
        if (currentUserId == null) {
            callback.onResult(false);
            return;
        }

        db.collection(COLLECTION_LIKES)
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("targetId", targetId)
                .whereEqualTo("targetType", targetType)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isLiked = !queryDocumentSnapshots.isEmpty();
                    callback.onResult(isLiked);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Like durumu kontrol edilemedi", e);
                    callback.onResult(false);
                });
    }

    // Callback Interfaces
    public interface OnLikeCallback {
        void onSuccess(boolean isNowLiked);
        void onFailure(String error);
    }

    public interface OnLikeCheckCallback {
        void onResult(boolean isLiked);
    }
}