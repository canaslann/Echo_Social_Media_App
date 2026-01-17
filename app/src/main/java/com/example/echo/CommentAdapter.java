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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private LikeManager likeManager;
    private FirebaseFirestore db;
    private String currentUserId;
    private Context context;

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
        this.likeManager = new LikeManager(db, currentUserId);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);

        holder.userName.setText(comment.getUserName());
        holder.usertag.setText("@" + comment.getUsertag());
        holder.content.setText(comment.getContent());
        holder.timeAgo.setText(comment.getTimeAgo());

        // Beğeni sayısı
        if (comment.getLikeCount() > 0) {
            holder.likeCount.setText(String.valueOf(comment.getLikeCount()));
            holder.likeCount.setVisibility(View.VISIBLE);
        } else {
            holder.likeCount.setVisibility(View.GONE);
        }

        // Like durumunu Firebase'den kontrol et
        likeManager.checkCommentLikeStatus(comment.getCommentId(), isLiked -> {
            comment.setLiked(isLiked);
            updateLikeButton(holder, isLiked);
        });

        // Profil fotoğrafına tıklama
        holder.profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileActivity.class);
            intent.putExtra("userId", comment.getUserId());
            v.getContext().startActivity(intent);
        });

        // Kullanıcı adına tıklama
        holder.userName.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileActivity.class);
            intent.putExtra("userId", comment.getUserId());
            v.getContext().startActivity(intent);
        });

        // Beğeni butonu - FIREBASE İLE
        holder.likeButton.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(v.getContext(), "Lütfen giriş yapın", Toast.LENGTH_SHORT).show();
                return;
            }

            // Butonu devre dışı bırak
            holder.likeButton.setEnabled(false);

            // Mevcut durumu al
            boolean isCurrentlyLiked = comment.isLiked();
            int currentLikeCount = comment.getLikeCount();

            // Optimistic UI Update
            comment.setLiked(!isCurrentlyLiked);
            if (!isCurrentlyLiked) {
                comment.setLikeCount(currentLikeCount + 1);
            } else {
                comment.setLikeCount(currentLikeCount - 1);
            }

            if (comment.getLikeCount() > 0) {
                holder.likeCount.setText(String.valueOf(comment.getLikeCount()));
                holder.likeCount.setVisibility(View.VISIBLE);
            } else {
                holder.likeCount.setVisibility(View.GONE);
            }
            updateLikeButton(holder, comment.isLiked());

            // Firebase'e kaydet
            likeManager.toggleCommentLike(comment.getCommentId(), isCurrentlyLiked, new LikeManager.OnLikeCallback() {
                @Override
                public void onSuccess(boolean isNowLiked) {
                    // Firebase'den güncel likeCount'u çek
                    db.collection("comments").document(comment.getCommentId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Long likeCountLong = documentSnapshot.getLong("likeCount");
                                    if (likeCountLong != null) {
                                        comment.setLikeCount(likeCountLong.intValue());
                                        if (comment.getLikeCount() > 0) {
                                            holder.likeCount.setText(String.valueOf(comment.getLikeCount()));
                                            holder.likeCount.setVisibility(View.VISIBLE);
                                        } else {
                                            holder.likeCount.setVisibility(View.GONE);
                                        }
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
                    comment.setLiked(isCurrentlyLiked);
                    comment.setLikeCount(currentLikeCount);

                    if (comment.getLikeCount() > 0) {
                        holder.likeCount.setText(String.valueOf(comment.getLikeCount()));
                        holder.likeCount.setVisibility(View.VISIBLE);
                    } else {
                        holder.likeCount.setVisibility(View.GONE);
                    }
                    updateLikeButton(holder, isCurrentlyLiked);
                    holder.likeButton.setEnabled(true);

                    Toast.makeText(v.getContext(), "Hata: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Yanıtla butonu
        holder.replyButton.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Yanıt özelliği yakında...", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateLikeButton(CommentViewHolder holder, boolean isLiked) {
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
        return commentList.size();
    }

    public void updateComments(List<Comment> newComments) {
        this.commentList = newComments;
        notifyDataSetChanged();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage, likeIcon;
        TextView userName, usertag, content, timeAgo, likeCount, replyButton;
        LinearLayout likeButton;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.commentProfileImage);
            userName = itemView.findViewById(R.id.commentUserName);
            usertag = itemView.findViewById(R.id.commentUsertag);
            content = itemView.findViewById(R.id.commentContent);
            timeAgo = itemView.findViewById(R.id.commentTime);
            likeIcon = itemView.findViewById(R.id.commentLikeIcon);
            likeCount = itemView.findViewById(R.id.commentLikeCount);
            likeButton = itemView.findViewById(R.id.commentLikeButton);
            replyButton = itemView.findViewById(R.id.commentReplyButton);
        }
    }
}