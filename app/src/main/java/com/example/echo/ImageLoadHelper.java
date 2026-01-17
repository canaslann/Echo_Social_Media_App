package com.example.echo;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;

public class ImageLoadHelper {

    public static void loadProfileImage(Context context, String userId,
                                        ImageView imageView,
                                        LocalImageManager localImageManager) {
        String profileImagePath = localImageManager.getProfileImagePath(userId);

        if (profileImagePath != null && localImageManager.fileExists(profileImagePath)) {
            File imageFile = new File(profileImagePath);

            Glide.with(context)
                    .load(imageFile)
                    .circleCrop()
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    // ✨ ÖNEMLİ: Dosya değişiklik zamanını signature olarak kullan
                    .signature(new ObjectKey(imageFile.lastModified()))
                    // ✨ Cache stratejisini ayarla
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(false)
                    .into(imageView);
        } else {
            // Varsayılan resim
            imageView.setImageResource(R.mipmap.ic_launcher);
        }
    }

    /**
     * Cover fotoğrafını yükle (Cache-aware)
     */
    public static void loadCoverImage(Context context, String userId,
                                      ImageView imageView,
                                      LocalImageManager localImageManager) {
        String coverImagePath = localImageManager.getCoverImagePath(userId);

        if (coverImagePath != null && localImageManager.fileExists(coverImagePath)) {
            File imageFile = new File(coverImagePath);

            Glide.with(context)
                    .load(imageFile)
                    .centerCrop()
                    .signature(new ObjectKey(imageFile.lastModified()))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(imageView);
        }
    }

    public static void loadPostImage(Context context, String imagePath, ImageView imageView) {
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);

            if (imageFile.exists()) {
                Glide.with(context)
                        .load(imageFile)
                        .centerCrop()
                        .placeholder(R.color.background)
                        .signature(new ObjectKey(imageFile.lastModified()))
                        .into(imageView);
            }
        }
    }
}