package com.example.echo;

import com.google.firebase.Timestamp;

public class Follow {
    private String followerId;  // Takip eden kişi
    private String followingId; // Takip edilen kişi
    private Timestamp timestamp;

    // Firestore için boş constructor (ZORUNLU)
    public Follow() {
    }

    public Follow(String followerId, String followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
        this.timestamp = Timestamp.now();
    }

    // Getters
    public String getFollowerId() { return followerId; }
    public String getFollowingId() { return followingId; }
    public Timestamp getTimestamp() { return timestamp; }

    // Setters
    public void setFollowerId(String followerId) { this.followerId = followerId; }
    public void setFollowingId(String followingId) { this.followingId = followingId; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}