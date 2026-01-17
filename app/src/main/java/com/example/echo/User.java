package com.example.echo;

public class User {
    private String userId;
    private String fullName;
    private String usertag;
    private String email;
    private String bio;
    private String profileImageUrl;
    private String coverImageUrl;  // ✨ YENİ ALAN
    private int followerCount;
    private int followingCount;

    // Firestore için boş constructor (ZORUNLU)
    public User() {
    }

    public User(String userId, String fullName, String usertag, String email) {
        this.userId = userId;
        this.fullName = fullName;
        this.usertag = usertag;
        this.email = email;
        this.bio = "";
        this.profileImageUrl = "";
        this.coverImageUrl = "";
        this.followerCount = 0;
        this.followingCount = 0;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getUsertag() { return usertag; }
    public String getEmail() { return email; }
    public String getBio() { return bio; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public int getFollowerCount() { return followerCount; }
    public int getFollowingCount() { return followingCount; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setUsertag(String usertag) { this.usertag = usertag; }
    public void setEmail(String email) { this.email = email; }
    public void setBio(String bio) { this.bio = bio; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }
}