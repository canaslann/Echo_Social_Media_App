package com.example.echo;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private LikeManager likeManager;
    private FirebaseFirestore db;
    private String currentUserId;
    private Context context; // ✅ Context'i sakla

    public PostAdapter(List<Post> postList, Context context) { // ✅ Context parametre ekle
        this.postList = postList;
        this.context = context; // ✅ Context'i sakla
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
        this.likeManager = new LikeManager(db, currentUserId);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.userName.setText(post.getUserName());
        holder.usertag.setText("@" + post.getUserTag());
        holder.content.setText(post.getContent());
        holder.timeAgo.setText(post.getTimeAgo());
        holder.commentCount.setText(String.valueOf(post.getCommentCount()));
        holder.likeCount.setText(String.valueOf(post.getLikeCount()));

        // Like durumunu Firebase'den kontrol et
        likeManager.checkPostLikeStatus(post.getPostId(), isLiked -> {
            post.setLiked(isLiked);
            updateLikeButton(holder, isLiked);
        });

        // Profil fotoğrafına tıklama
        holder.itemView.findViewById(R.id.postProfileImage).setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileActivity.class);
            intent.putExtra("userId", post.getUserId());
            v.getContext().startActivity(intent);
        });

        // Kullanıcı adına tıklama
        holder.userName.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileActivity.class);
            intent.putExtra("userId", post.getUserId());
            v.getContext().startActivity(intent);
        });

        // ✨ POST GÖRSELİNİ YÜKLE (LOCAL)
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            File imageFile = new File(post.getImageUrl());

            if (imageFile.exists()) {
                holder.postImage.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(imageFile)
                        .centerCrop()
                        .placeholder(R.color.background)
                        .into(holder.postImage);
            } else {
                holder.postImage.setVisibility(View.GONE);
            }
        } else {
            holder.postImage.setVisibility(View.GONE);
        }

        // ✨ KULLANICI PROFİL FOTOĞRAFINI YÜKLE (LOCAL)
        LocalImageManager localImageManager = new LocalImageManager(context); // ✅ Context kullan
        String profileImagePath = localImageManager.getProfileImagePath(post.getUserId());

        if (profileImagePath != null && localImageManager.fileExists(profileImagePath)) {
            ImageLoadHelper.loadProfileImage(
                    holder.itemView.getContext(),
                    post.getUserId(),
                    holder.profileImage,
                    new LocalImageManager(holder.itemView.getContext())
            );
        } else {
            holder.profileImage.setImageResource(R.mipmap.ic_launcher);
        }

        // Beğeni butonu - FIREBASE İLE
        holder.likeButton.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(v.getContext(), "Lütfen giriş yapın", Toast.LENGTH_SHORT).show();
                return;
            }

            // Butonu devre dışı bırak (double-click önleme)
            holder.likeButton.setEnabled(false);

            // Mevcut durumu al
            boolean isCurrentlyLiked = post.isLiked();
            int currentLikeCount = post.getLikeCount();

            // Optimistic UI Update (hızlı görünüm için)
            post.setLiked(!isCurrentlyLiked);
            if (!isCurrentlyLiked) {
                post.setLikeCount(currentLikeCount + 1);
            } else {
                post.setLikeCount(currentLikeCount - 1);
            }
            holder.likeCount.setText(String.valueOf(post.getLikeCount()));
            updateLikeButton(holder, post.isLiked());

            // Firebase'e kaydet
            likeManager.togglePostLike(post.getPostId(), isCurrentlyLiked, new LikeManager.OnLikeCallback() {
                @Override
                public void onSuccess(boolean isNowLiked) {
                    // Firebase'den güncel likeCount'u çek
                    db.collection("posts").document(post.getPostId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Long likeCountLong = documentSnapshot.getLong("likeCount");
                                    if (likeCountLong != null) {
                                        post.setLikeCount(likeCountLong.intValue());
                                        holder.likeCount.setText(String.valueOf(post.getLikeCount()));
                                    }
                                }
                                holder.likeButton.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                holder.likeButton.setEnabled(true);
                            });
                }

                @Override
                public void onFailure(String error) {
                    // Hata durumunda eski haline döndür
                    post.setLiked(isCurrentlyLiked);
                    post.setLikeCount(currentLikeCount);
                    holder.likeCount.setText(String.valueOf(currentLikeCount));
                    updateLikeButton(holder, isCurrentlyLiked);
                    holder.likeButton.setEnabled(true);

                    Toast.makeText(v.getContext(), "Hata: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Yorum butonu
        holder.commentButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CommentsActivity.class);
            intent.putExtra("postId", post.getPostId());
            v.getContext().startActivity(intent);
        });

        // Menü butonu
        holder.menuButton.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Gönderi seçenekleri...", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateLikeButton(PostViewHolder holder, boolean isLiked) {
        if (isLiked) {
            holder.likeIcon.setImageResource(android.R.drawable.btn_star_big_on);
            holder.likeIcon.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.error)
            );
            holder.likeCount.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.error)
            );
        } else {
            holder.likeIcon.setImageResource(android.R.drawable.btn_star_big_off);
            holder.likeIcon.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary)
            );
            holder.likeCount.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary)
            );
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public void updatePosts(List<Post> newPosts) {
        this.postList = newPosts;
        notifyDataSetChanged();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView userName, usertag, content, timeAgo;
        TextView commentCount, likeCount;
        ImageView likeIcon, menuButton;
        LinearLayout commentButton, retweetButton, likeButton;
        ImageView postImage, profileImage;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.postUserName);
            usertag = itemView.findViewById(R.id.postUserTag);
            content = itemView.findViewById(R.id.postContent);
            timeAgo = itemView.findViewById(R.id.postTime);
            commentCount = itemView.findViewById(R.id.commentCount);
            likeCount = itemView.findViewById(R.id.likeCount);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            menuButton = itemView.findViewById(R.id.postMenuButton);
            commentButton = itemView.findViewById(R.id.commentButton);
            retweetButton = itemView.findViewById(R.id.retweetButton);
            likeButton = itemView.findViewById(R.id.likeButton);
            postImage = itemView.findViewById(R.id.postImage);
            profileImage = itemView.findViewById(R.id.postProfileImage);
        }
    }
}