package com.example.echo;

import com.google.firebase.Timestamp;

public class Post {
    private String userId;
    private String postId;
    private String userName;
    private String usertag;
    private String content;
    private String timeAgo;
    private int commentCount;
    private int retweetCount;
    private int likeCount;
    private boolean isLiked;
    private String imageUrl;
    private Timestamp timestamp;

    // Firestore için boş constructor (ZORUNLU)
    public Post() {
    }

    // Yeni post oluşturmak için constructor
    public Post(String uid, String userName, String usertag, String content) {
        this.userId = uid;
        this.userName = userName;
        this.usertag = usertag;
        this.content = content;
        this.timeAgo = getTimeAgo();
        this.commentCount = 0;
        this.retweetCount = 0;
        this.likeCount = 0;
        this.isLiked = false;
        this.imageUrl = null;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getPostId() { return postId; }
    public String getUserName() { return userName; }
    public String getUserTag() { return usertag; }
    public String getContent() { return content; }
    public String getTimeAgo() { return timeAgo; }
    public int getCommentCount() { return commentCount; }
    public int getRetweetCount() { return retweetCount; }
    public int getLikeCount() { return likeCount; }
    public boolean isLiked() { return isLiked; }
    public String getImageUrl() { return imageUrl; }
    public Timestamp getTimestamp() { return timestamp; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setPostId(String postId) { this.postId = postId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserTag(String usertag) { this.usertag = usertag; }
    public void setContent(String content) { this.content = content; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setRetweetCount(int retweetCount) { this.retweetCount = retweetCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setLiked(boolean liked) { isLiked = liked; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}