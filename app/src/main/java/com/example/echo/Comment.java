package com.example.echo;

import com.google.firebase.Timestamp;

public class Comment {
    private String commentId;
    private String postId;
    private String userId;
    private String userName;
    private String usertag;
    private String content;
    private Timestamp timestamp;
    private String timeAgo;
    private int likeCount;
    private boolean isLiked;

    // Firestore için boş constructor (ZORUNLU)
    public Comment() {
    }

    // Yeni yorum oluşturmak için constructor
    public Comment(String postId, String userId, String userName, String usertag, String content) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.usertag = usertag;
        this.content = content;
        this.likeCount = 0;
        this.isLiked = false;
    }

    // Getters
    public String getCommentId() { return commentId; }
    public String getPostId() { return postId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUsertag() { return usertag; }
    public String getContent() { return content; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getTimeAgo() { return timeAgo; }
    public int getLikeCount() { return likeCount; }
    public boolean isLiked() { return isLiked; }

    // Setters
    public void setCommentId(String commentId) { this.commentId = commentId; }
    public void setPostId(String postId) { this.postId = postId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUsertag(String usertag) { this.usertag = usertag; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setLiked(boolean liked) { isLiked = liked; }
}