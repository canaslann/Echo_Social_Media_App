package com.example.echo;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;
    private FirebaseFirestore db;
    private LocalImageManager localImageManager;

    public NotificationAdapter(List<Notification> notificationList, LocalImageManager localImageManager) {
        this.notificationList = notificationList;
        this.localImageManager = localImageManager;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        // Gönderen kişinin adı
        holder.senderName.setText(notification.getSenderName());

        // Bildirim mesajı
        holder.message.setText(notification.getMessage());

        // Zaman
        holder.time.setText(notification.getTimeAgo());

        // Bildirim türü ikonu
        holder.typeIcon.setImageResource(notification.getIcon());
        holder.typeIcon.setColorFilter(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.primary)
        );

        // Yorum içeriği (sadece COMMENT türünde)
        if (Notification.TYPE_COMMENT.equals(notification.getType()) &&
                notification.getContent() != null && !notification.getContent().isEmpty()) {
            holder.content.setText(notification.getContent());
            holder.content.setVisibility(View.VISIBLE);
        } else {
            holder.content.setVisibility(View.GONE);
        }

        // Okunmamış göstergesi
        if (!notification.isRead()) {
            holder.unreadIndicator.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.surface)
            );
        } else {
            holder.unreadIndicator.setVisibility(View.GONE);
            holder.itemView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.background)
            );
        }

        // Profil fotoğrafı yükle (LOCAL)
        String profileImagePath = localImageManager.getProfileImagePath(notification.getSenderId());
        if (profileImagePath != null && localImageManager.fileExists(profileImagePath)) {
            Glide.with(holder.itemView.getContext())
                    .load(new File(profileImagePath))
                    .circleCrop()
                    .placeholder(R.mipmap.ic_launcher)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.mipmap.ic_launcher);
        }

        // Tıklama olayları
        holder.itemView.setOnClickListener(v -> {
            // Bildirimi okundu olarak işaretle
            markAsRead(notification);

            // Bildirim türüne göre yönlendirme
            switch (notification.getType()) {
                case Notification.TYPE_LIKE_POST:
                case Notification.TYPE_COMMENT:
                    // Post detay sayfasına git (CommentsActivity)
                    Intent postIntent = new Intent(v.getContext(), CommentsActivity.class);
                    postIntent.putExtra("postId", notification.getTargetId());
                    v.getContext().startActivity(postIntent);
                    break;

                case Notification.TYPE_FOLLOW:
                    // Profil sayfasına git
                    Intent profileIntent = new Intent(v.getContext(), ProfileActivity.class);
                    profileIntent.putExtra("userId", notification.getSenderId());
                    v.getContext().startActivity(profileIntent);
                    break;

                case Notification.TYPE_LIKE_COMMENT:
                    // Yorumun bulunduğu post'a git
                    Intent commentIntent = new Intent(v.getContext(), CommentsActivity.class);
                    commentIntent.putExtra("postId", notification.getTargetId());
                    v.getContext().startActivity(commentIntent);
                    break;
            }
        });

        // Profil fotoğrafına tıklama
        holder.profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileActivity.class);
            intent.putExtra("userId", notification.getSenderId());
            v.getContext().startActivity(intent);
        });
    }

    private void markAsRead(Notification notification) {
        if (!notification.isRead()) {
            notification.setRead(true);
            db.collection("notifications")
                    .document(notification.getNotificationId())
                    .update("read", true)
                    .addOnSuccessListener(aVoid -> {
                        notifyDataSetChanged();
                    });
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public void updateNotifications(List<Notification> newNotifications) {
        this.notificationList = newNotifications;
        notifyDataSetChanged();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage, typeIcon;
        TextView senderName, message, content, time;
        View unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.notificationProfileImage);
            typeIcon = itemView.findViewById(R.id.notificationTypeIcon);
            senderName = itemView.findViewById(R.id.notificationSenderName);
            message = itemView.findViewById(R.id.notificationMessage);
            content = itemView.findViewById(R.id.notificationContent);
            time = itemView.findViewById(R.id.notificationTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
    }
}