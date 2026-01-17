package com.example.echo;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;

    public UserAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.fullName.setText(user.getFullName());
        holder.username.setText("@" + user.getUsertag());

        // Bio varsa göster
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            holder.bio.setText(user.getBio());
            holder.bio.setVisibility(View.VISIBLE);
        } else {
            holder.bio.setVisibility(View.GONE);
        }

        // Kullanıcıya tıklama - Profile git
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileActivity.class);
            intent.putExtra("userId", user.getUserId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void updateUsers(List<User> newUsers) {
        this.userList = newUsers;
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage, followingIndicator;
        TextView fullName, username, bio;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.userProfileImage);
            fullName = itemView.findViewById(R.id.userFullName);
            username = itemView.findViewById(R.id.userUsername);
            bio = itemView.findViewById(R.id.userBio);
            followingIndicator = itemView.findViewById(R.id.followingIndicator);
        }
    }
}