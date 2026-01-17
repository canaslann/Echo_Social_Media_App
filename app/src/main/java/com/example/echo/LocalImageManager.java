package com.example.echo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import com.bumptech.glide.Glide;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Local Görsel Yönetim Sistemi
 * Görselleri cihazın internal storage'ına kaydeder
 */
public class LocalImageManager {

    private static final String TAG = "LocalImageManager";
    private static final String PREFS_NAME = "ImagePrefs";
    private static final int MAX_IMAGE_SIZE = 1024; // 1024x1024 max
    private static final int COMPRESSION_QUALITY = 85; // %85 kalite

    private final Context context;
    private final SharedPreferences prefs;

    public LocalImageManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Profil fotoğrafı kaydet ve cache'i temizle
     */
    public String saveProfileImage(String userId, Uri imageUri) {
        // Eski fotoğrafı sil
        String oldPath = getProfileImagePath(userId);
        if (oldPath != null) {
            deleteImage(oldPath);
        }

        String fileName = "profile_" + userId + "_" + System.currentTimeMillis() + ".jpg";
        String filePath = saveImageToInternalStorage(imageUri, fileName);

        if (filePath != null) {
            // Path'i SharedPreferences'a kaydet
            prefs.edit().putString("profile_" + userId, filePath).apply();

            // ✨ GLIDE CACHE'İ TEMİZLE
            clearGlideCache(userId);

            Log.d(TAG, "Profil fotoğrafı kaydedildi: " + filePath);
        }

        return filePath;
    }

    /**
     * Cover fotoğrafı kaydet ve cache'i temizle
     */
    public String saveCoverImage(String userId, Uri imageUri) {
        // Eski fotoğrafı sil
        String oldPath = getCoverImagePath(userId);
        if (oldPath != null) {
            deleteImage(oldPath);
        }

        String fileName = "cover_" + userId + "_" + System.currentTimeMillis() + ".jpg";
        String filePath = saveImageToInternalStorage(imageUri, fileName);

        if (filePath != null) {
            // Path'i SharedPreferences'a kaydet
            prefs.edit().putString("cover_" + userId, filePath).apply();

            // ✨ GLIDE CACHE'İ TEMİZLE
            clearGlideCache(userId);

            Log.d(TAG, "Cover fotoğrafı kaydedildi: " + filePath);
        }

        return filePath;
    }

    /**
     * Post görseli kaydet
     */
    public String savePostImage(String postId, Uri imageUri) {
        String fileName = "post_" + postId + "_" + System.currentTimeMillis() + ".jpg";
        String filePath = saveImageToInternalStorage(imageUri, fileName);

        if (filePath != null) {
            Log.d(TAG, "Post görseli kaydedildi: " + filePath);
        }

        return filePath;
    }

    /**
     * Profil fotoğrafı path'ini al
     */
    public String getProfileImagePath(String userId) {
        return prefs.getString("profile_" + userId, null);
    }

    /**
     * Cover fotoğrafı path'ini al
     */
    public String getCoverImagePath(String userId) {
        return prefs.getString("cover_" + userId, null);
    }

    /**
     * ✨ YENİ: Glide cache'ini temizle (profil fotoğrafı güncellendiğinde)
     */
    public void clearGlideCache(String userId) {
        try {
            // Memory cache'i temizle (UI thread'de çalışır)
            Glide.get(context).clearMemory();

            // Disk cache'i temizle (arka planda çalışmalı)
            new Thread(() -> {
                try {
                    Glide.get(context).clearDiskCache();
                    Log.d(TAG, "Glide cache temizlendi: " + userId);
                } catch (Exception e) {
                    Log.e(TAG, "Disk cache temizleme hatası", e);
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Cache temizleme hatası", e);
        }
    }

    /**
     * Görseli internal storage'a kaydet
     */
    private String saveImageToInternalStorage(Uri imageUri, String fileName) {
        try {
            // 1. Uri'den InputStream al
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "InputStream null");
                return null;
            }

            // 2. Bitmap'e çevir
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (originalBitmap == null) {
                Log.e(TAG, "Bitmap decode edilemedi");
                return null;
            }

            // 3. Boyutu küçült (aspect ratio koru)
            Bitmap resizedBitmap = resizeBitmap(originalBitmap, MAX_IMAGE_SIZE);

            // 4. Internal storage'a kaydet
            File directory = context.getFilesDir(); // /data/data/com.example.echo/files/
            File imageFile = new File(directory, fileName);

            FileOutputStream fos = new FileOutputStream(imageFile);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, fos);
            fos.close();

            // 5. Memory temizle
            originalBitmap.recycle();
            resizedBitmap.recycle();

            Log.d(TAG, "Görsel kaydedildi: " + imageFile.getAbsolutePath());
            return imageFile.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Görsel kaydetme hatası", e);
            return null;
        }
    }

    /**
     * Bitmap'i yeniden boyutlandır (aspect ratio koru)
     */
    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Zaten küçükse dokunma
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        // Aspect ratio hesapla
        float aspectRatio = (float) width / height;
        int newWidth, newHeight;

        if (width > height) {
            // Landscape
            newWidth = maxSize;
            newHeight = Math.round(maxSize / aspectRatio);
        } else {
            // Portrait
            newHeight = maxSize;
            newWidth = Math.round(maxSize * aspectRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Görseli sil
     */
    public boolean deleteImage(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            Log.d(TAG, "Görsel silindi: " + deleted);
            return deleted;
        }

        return false;
    }

    /**
     * Kullanıcının tüm görsellerini sil
     */
    public void deleteAllUserImages(String userId) {
        // Profil fotoğrafını sil
        String profilePath = getProfileImagePath(userId);
        if (profilePath != null) {
            deleteImage(profilePath);
            prefs.edit().remove("profile_" + userId).apply();
        }

        // Cover fotoğrafını sil
        String coverPath = getCoverImagePath(userId);
        if (coverPath != null) {
            deleteImage(coverPath);
            prefs.edit().remove("cover_" + userId).apply();
        }

        // Cache'i temizle
        clearGlideCache(userId);

        Log.d(TAG, "Kullanıcının tüm görselleri silindi: " + userId);
    }

    /**
     * Bitmap'i dosya path'inden yükle
     */
    public Bitmap loadBitmapFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Log.w(TAG, "Dosya bulunamadı: " + filePath);
            return null;
        }

        return BitmapFactory.decodeFile(filePath);
    }

    /**
     * Dosya var mı kontrol et
     */
    public boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        return new File(filePath).exists();
    }
}