package com.example.echo;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.List;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {

    private List<Comment> replyList; // Comment modelini kullanıyoruz
    private LocalImageManager localImageManager;
    private FirebaseFirestore db;

    public ReplyAdapter(List<Comment> replyList, LocalImageManager localImageManager) {
        this.replyList = replyList;
        this.localImageManager = localImageManager;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reply, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Comment reply = replyList.get(position);

        // Kullanıcı bilgileri
        holder.userName.setText(reply.getUserName());
        holder.usertag.setText("@" + reply.getUsertag());
        holder.time.setText(reply.getTimeAgo());

        // Yanıt içeriği
        holder.content.setText(reply.getContent());

        // Beğeni sayısı
        holder.likeCount.setText(String.valueOf(reply.getLikeCount()));

        // Profil fotoğrafı yükle (LOCAL)
        String profileImagePath = localImageManager.getProfileImagePath(reply.getUserId());
        if (profileImagePath != null && localImageManager.fileExists(profileImagePath)) {
            Glide.with(holder.itemView.getContext())
                    .load(new File(profileImagePath))
                    .circleCrop()
                    .placeholder(R.mipmap.ic_launcher)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.mipmap.ic_launcher);
        }

        // Orijinal post bilgilerini yükle
        loadOriginalPost(reply.getPostId(), holder);

        // Tıklama - Post detayına git
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CommentsActivity.class);
            intent.putExtra("postId", reply.getPostId());
            v.getContext().startActivity(intent);
        });

        // Profil fotoğrafına tıklama
        holder.profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileActivity.class);
            intent.putExtra("userId", reply.getUserId());
            v.getContext().startActivity(intent);
        });
    }

    private void loadOriginalPost(String postId, ReplyViewHolder holder) {
        db.collection("posts").document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String postAuthor = documentSnapshot.getString("usertag");
                        String postContent = documentSnapshot.getString("content");

                        if (postAuthor != null) {
                            holder.originalPostAuthor.setText("@" + postAuthor);
                            holder.replyingToText.setText("@" + postAuthor + " kişisine yanıt veriyor");
                            holder.replyingToText.setVisibility(View.VISIBLE);
                        }

                        if (postContent != null) {
                            holder.originalPostContent.setText(postContent);
                            holder.originalPostPreview.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Post bulunamazsa önizlemeyi gizle
                    holder.originalPostPreview.setVisibility(View.GONE);
                });
    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    public void updateReplies(List<Comment> newReplies) {
        this.replyList = newReplies;
        notifyDataSetChanged();
    }

    static class ReplyViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView userName, usertag, time, replyingToText, content;
        TextView originalPostAuthor, originalPostContent, likeCount;
        LinearLayout originalPostPreview;

        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.replyProfileImage);
            userName = itemView.findViewById(R.id.replyUserName);
            usertag = itemView.findViewById(R.id.replyUsertag);
            time = itemView.findViewById(R.id.replyTime);
            replyingToText = itemView.findViewById(R.id.replyingToText);
            content = itemView.findViewById(R.id.replyContent);
            originalPostAuthor = itemView.findViewById(R.id.originalPostAuthor);
            originalPostContent = itemView.findViewById(R.id.originalPostContent);
            originalPostPreview = itemView.findViewById(R.id.originalPostPreview);
            likeCount = itemView.findViewById(R.id.replyLikeCount);
        }
    }
}