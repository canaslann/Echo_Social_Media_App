package com.example.echo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {

    private ImageView backButton, postProfileImage, sendCommentButton;
    private TextView postUserName, postUsertag, postContent, postTime;
    private EditText commentInput;
    private RecyclerView commentsRecyclerView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String postId;
    private Post currentPost;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        // Firebase başlat
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Intent'ten postId al
        postId = getIntent().getStringExtra("postId");
        if (postId == null) {
            Toast.makeText(this, "Gönderi bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadPost();
        loadComments();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        postProfileImage = findViewById(R.id.postProfileImage);
        postUserName = findViewById(R.id.postUserName);
        postUsertag = findViewById(R.id.postUsertag);
        postContent = findViewById(R.id.postContent);
        postTime = findViewById(R.id.postTime);
        commentInput = findViewById(R.id.commentInput);
        sendCommentButton = findViewById(R.id.sendCommentButton);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);

        // RecyclerView setup
        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentList);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentAdapter);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        // Yorum input değişikliği
        commentInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendCommentButton.setAlpha(s.toString().trim().isEmpty() ? 0.5f : 1.0f);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Yorum gönder butonu
        sendCommentButton.setOnClickListener(v -> postComment());

        // Profil fotoğrafına tıklama
        postProfileImage.setOnClickListener(v -> {
            if (currentPost != null) {
                android.content.Intent intent = new android.content.Intent(this, ProfileActivity.class);
                intent.putExtra("userId", currentPost.getUserId());
                startActivity(intent);
            }
        });
    }

    private void loadPost() {
        db.collection("posts").document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentPost = documentSnapshot.toObject(Post.class);
                        if (currentPost != null) {
                            currentPost.setPostId(documentSnapshot.getId());

                            // UI'ı güncelle
                            postUserName.setText(currentPost.getUserName());
                            postUsertag.setText("@" + currentPost.getUserTag());
                            postContent.setText(currentPost.getContent());

                            // Zaman formatı
                            Timestamp timestamp = documentSnapshot.getTimestamp("timestamp");
                            if (timestamp != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a · d MMM yyyy", new Locale("tr"));
                                postTime.setText(sdf.format(timestamp.toDate()));
                            }
                        }
                    } else {
                        Toast.makeText(this, "Gönderi bulunamadı", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gönderi yüklenemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadComments() {
        db.collection("comments")
                .whereEqualTo("postId", postId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Yorumlar yüklenemedi: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
                        commentList.clear();

                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Comment comment = document.toObject(Comment.class);
                            if (comment != null) {
                                comment.setCommentId(document.getId());

                                // Timestamp'den timeAgo hesapla
                                Timestamp timestamp = document.getTimestamp("timestamp");
                                if (timestamp != null) {
                                    comment.setTimeAgo(calculateTimeAgo(timestamp.toDate()));
                                } else {
                                    comment.setTimeAgo("Şimdi");
                                }

                                commentList.add(comment);
                            }
                        }

                        commentAdapter.notifyDataSetChanged();
                    }
                });
    }

    // CommentsActivity.java içindeki postComment() metodunu bu şekilde güncelle:

    private void postComment() {
        String content = commentInput.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(this, "Yorum boş olamaz", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Lütfen giriş yapın", Toast.LENGTH_SHORT).show();
            return;
        }

        // Butonu devre dışı bırak
        sendCommentButton.setEnabled(false);
        sendCommentButton.setAlpha(0.5f);

        // Kullanıcı bilgilerini al
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userName = documentSnapshot.getString("fullName");
                    String usertag = documentSnapshot.getString("usertag");

                    if (userName == null) {
                        userName = currentUser.getEmail() != null ?
                                currentUser.getEmail().split("@")[0] : "Kullanıcı";
                    }
                    if (usertag == null) {
                        usertag = userName.toLowerCase().replace(" ", "");
                    }

                    // Yorum verisini oluştur
                    Map<String, Object> commentData = new HashMap<>();
                    commentData.put("postId", postId);
                    commentData.put("userId", currentUser.getUid());
                    commentData.put("userName", userName);
                    commentData.put("usertag", usertag);
                    commentData.put("content", content);
                    commentData.put("timestamp", FieldValue.serverTimestamp());
                    commentData.put("likeCount", 0);
                    commentData.put("isLiked", false);

                    // Final değişkenler
                    final String finalUserName = userName;
                    final String finalUsertag = usertag;

                    // Firestore'a kaydet
                    db.collection("comments")
                            .add(commentData)
                            .addOnSuccessListener(documentReference -> {
                                // Post'un yorum sayısını artır
                                db.collection("posts").document(postId)
                                        .update("commentCount", FieldValue.increment(1));

                                // ✨ BİLDİRİM OLUŞTUR
                                if (currentPost != null && !currentUser.getUid().equals(currentPost.getUserId())) {
                                    NotificationManager notificationManager = new NotificationManager(db, currentUser.getUid());
                                    notificationManager.createCommentNotification(
                                            postId,
                                            currentPost.getUserId(),
                                            finalUserName,
                                            finalUsertag,
                                            content
                                    );
                                }

                                // Input'u temizle
                                commentInput.setText("");
                                sendCommentButton.setEnabled(true);

                                Toast.makeText(this, "Yorum eklendi! 💬", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                sendCommentButton.setEnabled(true);
                                sendCommentButton.setAlpha(1.0f);
                                Toast.makeText(this, "Yorum eklenemedi: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    sendCommentButton.setEnabled(true);
                    sendCommentButton.setAlpha(1.0f);
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String calculateTimeAgo(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 5) {
            return "Şimdi";
        } else if (seconds < 60) {
            return seconds + "s";
        } else if (minutes < 60) {
            return minutes + "dk";
        } else if (hours < 24) {
            return hours + "sa";
        } else {
            return days + "g";
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}