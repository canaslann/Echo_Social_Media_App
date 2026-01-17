package com.example.echo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import java.io.File;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageView backButton, moreButton, profilePhoto, coverPhoto;
    private TextView toolbarName, toolbarPostCount, fullName, username, bio, joinDate;
    private TextView followingCount, followersCount;
    private Button actionButton;
    private TabLayout tabLayout;
    private RecyclerView postsRecyclerView;
    private LinearLayout followingLayout, followersLayout;
    private LocalImageManager localImageManager;
    private ReplyAdapter replyAdapter;
    private List<Comment> userReplies;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;
    private boolean isOwnProfile;
    private boolean isFollowing = false;
    private PostAdapter postAdapter;
    private List<Post> userPosts;
    private ActivityResultLauncher<Intent> editProfileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        localImageManager = new LocalImageManager(this);

        // Firebase başlat
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Intent'ten userId al
        userId = getIntent().getStringExtra("userId");
        if (userId == null && currentUser != null) {
            userId = currentUser.getUid();
        }

        isOwnProfile = currentUser != null && userId.equals(currentUser.getUid());

        editProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Profil güncellendi, sayfayı yenile
                        loadUserProfile();
                        // Cache'i temizle
                        localImageManager.clearGlideCache(userId);
                    }
                }
        );

        initViews();
        setupListeners();
        loadUserProfile();
        loadUserPosts();

        if (!isOwnProfile) {
            checkFollowStatus();
        }
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        moreButton = findViewById(R.id.moreButton);
        profilePhoto = findViewById(R.id.profilePhoto);
        coverPhoto = findViewById(R.id.coverPhoto);
        toolbarName = findViewById(R.id.toolbarName);
        toolbarPostCount = findViewById(R.id.toolbarPostCount);
        fullName = findViewById(R.id.fullName);
        username = findViewById(R.id.username);
        bio = findViewById(R.id.bio);
        joinDate = findViewById(R.id.joinDate);
        followingCount = findViewById(R.id.followingCount);
        followersCount = findViewById(R.id.followersCount);
        actionButton = findViewById(R.id.actionButton);
        tabLayout = findViewById(R.id.tabLayout);
        postsRecyclerView = findViewById(R.id.postsRecyclerView);
        followingLayout = findViewById(R.id.followingLayout);
        followersLayout = findViewById(R.id.followersLayout);
        userReplies = new ArrayList<>();
        replyAdapter = new ReplyAdapter(userReplies, localImageManager);
        // TabLayout tabs
        tabLayout.addTab(tabLayout.newTab().setText("Gönderiler"));
        tabLayout.addTab(tabLayout.newTab().setText("Yanıtlar"));

        // RecyclerView setup
        userPosts = new ArrayList<>();
        postAdapter = new PostAdapter(userPosts, this);
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsRecyclerView.setAdapter(postAdapter);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        moreButton.setOnClickListener(v -> {

            new AlertDialog.Builder(this)
                    .setTitle("Çıkış Yap")
                    .setMessage("Hesabınızdan çıkış yapmak istiyor musunuz?")
                    .setPositiveButton("Çıkış Yap", (dialog, which) -> {

                        // Firebase logout
                        FirebaseAuth.getInstance().signOut();

                        // LoginActivity'e yönlendir
                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("İptal", (dialog, which) -> dialog.dismiss())
                    .show();
        });


        actionButton.setOnClickListener(v -> {
            if (isOwnProfile) {
                // Profil düzenleme ekranına git (ActivityResultLauncher ile)
                Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
                editProfileLauncher.launch(intent); // ✨ DEĞİŞTİ
            } else {
                // Takip et/Takibi bırak
                if (isFollowing) {
                    unfollowUser();
                } else {
                    followUser();
                }
            }
        });

        followingLayout.setOnClickListener(v -> {
            Toast.makeText(this, "Takip edilenler yakında...", Toast.LENGTH_SHORT).show();
        });

        followersLayout.setOnClickListener(v -> {
            Toast.makeText(this, "Takipçiler yakında...", Toast.LENGTH_SHORT).show();
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Gönderiler
                    postsRecyclerView.setAdapter(postAdapter);
                    loadUserPosts();
                } else if (tab.getPosition() == 1) {
                    // Yanıtlar
                    postsRecyclerView.setAdapter(replyAdapter);
                    loadUserReplies();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadUserReplies() {
        db.collection("comments")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userReplies.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Comment reply = document.toObject(Comment.class);
                        if (reply != null) {
                            reply.setCommentId(document.getId());

                            // Timestamp'den timeAgo hesapla
                            Timestamp timestamp = document.getTimestamp("timestamp");
                            if (timestamp != null) {
                                reply.setTimeAgo(calculateTimeAgo(timestamp.toDate()));
                            } else {
                                reply.setTimeAgo("Şimdi");
                            }

                            userReplies.add(reply);
                        }
                    }

                    replyAdapter.notifyDataSetChanged();

                    if (userReplies.isEmpty()) {
                        Toast.makeText(this, "Henüz yanıt yok", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Yanıtlar yüklenemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void checkFollowStatus() {
        if (currentUser == null) return;

        db.collection("follows")
                .whereEqualTo("followerId", currentUser.getUid())
                .whereEqualTo("followingId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    isFollowing = !queryDocumentSnapshots.isEmpty();
                    updateFollowButton();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Takip durumu kontrol edilemedi", Toast.LENGTH_SHORT).show();
                });
    }

    // ProfileActivity.java içindeki followUser() metodunu bu şekilde güncelle:

    private void followUser() {
        if (currentUser == null) {
            Toast.makeText(this, "Lütfen giriş yapın", Toast.LENGTH_SHORT).show();
            return;
        }

        actionButton.setEnabled(false);

        // Follow verisini oluştur
        Map<String, Object> followData = new HashMap<>();
        followData.put("followerId", currentUser.getUid());
        followData.put("followingId", userId);
        followData.put("timestamp", FieldValue.serverTimestamp());

        // Firestore'a kaydet
        db.collection("follows")
                .add(followData)
                .addOnSuccessListener(documentReference -> {
                    // Takip eden kullanıcının followingCount'unu artır
                    db.collection("users").document(currentUser.getUid())
                            .update("followingCount", FieldValue.increment(1));

                    // Takip edilen kullanıcının followerCount'unu artır
                    db.collection("users").document(userId)
                            .update("followerCount", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid -> {
                                // Firebase'den güncel sayıyı çek
                                db.collection("users").document(userId)
                                        .get()
                                        .addOnSuccessListener(doc -> {
                                            Long followerCountLong = doc.getLong("followerCount");
                                            if (followerCountLong != null) {
                                                followersCount.setText(String.valueOf(followerCountLong));
                                            }
                                        });
                            });

                    // ✨ BİLDİRİM OLUŞTUR
                    db.collection("users").document(currentUser.getUid())
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                String senderName = userDoc.getString("fullName");
                                String senderUsertag = userDoc.getString("usertag");

                                NotificationManager notificationManager = new NotificationManager(db, currentUser.getUid());
                                notificationManager.createFollowNotification(
                                        userId,
                                        senderName,
                                        senderUsertag
                                );
                            });

                    isFollowing = true;
                    updateFollowButton();
                    actionButton.setEnabled(true);

                    Toast.makeText(this, "Takip ediliyor ✓", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    actionButton.setEnabled(true);
                    Toast.makeText(this, "Takip edilemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

// ProfileActivity.java içindeki unfollowUser() metodunu bu şekilde güncelle:

    private void unfollowUser() {
        if (currentUser == null) return;

        actionButton.setEnabled(false);

        // Takip kaydını bul ve sil
        db.collection("follows")
                .whereEqualTo("followerId", currentUser.getUid())
                .whereEqualTo("followingId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Takip eden kullanıcının followingCount'unu azalt
                                    db.collection("users").document(currentUser.getUid())
                                            .update("followingCount", FieldValue.increment(-1));

                                    // Takip edilen kullanıcının followerCount'unu azalt
                                    db.collection("users").document(userId)
                                            .update("followerCount", FieldValue.increment(-1))
                                            .addOnSuccessListener(aVoid2 -> {
                                                // Firebase'den güncel sayıyı çek
                                                db.collection("users").document(userId)
                                                        .get()
                                                        .addOnSuccessListener(doc -> {
                                                            Long followerCountLong = doc.getLong("followerCount");
                                                            if (followerCountLong != null) {
                                                                followersCount.setText(String.valueOf(followerCountLong));
                                                            }
                                                        });
                                            });

                                    // ✨ BİLDİRİMİ SİL
                                    NotificationManager notificationManager = new NotificationManager(db, currentUser.getUid());
                                    notificationManager.deleteFollowNotification(userId);

                                    isFollowing = false;
                                    updateFollowButton();
                                    actionButton.setEnabled(true);

                                    Toast.makeText(this, "Takip bırakıldı", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    actionButton.setEnabled(true);
                                    Toast.makeText(this, "Takip bırakılamadı: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    actionButton.setEnabled(true);
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFollowButton() {
        if (isFollowing) {
            actionButton.setText("Takip Ediliyor");
            actionButton.setBackgroundResource(R.drawable.button_outline);
        } else {
            actionButton.setText("Takip Et");
            actionButton.setBackgroundResource(R.drawable.button_outline);
        }
    }

    private void loadUserProfile() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("fullName");
                        String userTag = documentSnapshot.getString("usertag");
                        String userBio = documentSnapshot.getString("bio");
                        Long followingCountLong = documentSnapshot.getLong("followingCount");
                        Long followersCountLong = documentSnapshot.getLong("followerCount");

                        // UI'ı güncelle
                        if (name != null) {
                            fullName.setText(name);
                            toolbarName.setText(name);
                        }

                        if (userTag != null) {
                            username.setText("@" + userTag);
                        }

                        if (userBio != null && !userBio.isEmpty()) {
                            bio.setText(userBio);
                            bio.setVisibility(View.VISIBLE);
                        } else {
                            bio.setVisibility(View.GONE);
                        }

                        // ✨ PROFIL FOTOĞRAFI YÜKLE (LOCAL)
                        String profileImagePath = localImageManager.getProfileImagePath(userId);
                        if (profileImagePath != null && localImageManager.fileExists(profileImagePath)) {
                            Glide.with(this)
                                    .load(new File(profileImagePath))
                                    .circleCrop()
                                    .placeholder(R.mipmap.ic_launcher)
                                    .error(R.mipmap.ic_launcher)
                                    .into(profilePhoto);
                        } else {
                            // Varsayılan resim
                            profilePhoto.setImageResource(R.mipmap.ic_launcher);
                        }

                        // ✨ COVER FOTOĞRAFI YÜKLE (LOCAL)
                        String coverImagePath = localImageManager.getCoverImagePath(userId);
                        if (coverImagePath != null && localImageManager.fileExists(coverImagePath)) {
                            Glide.with(this)
                                    .load(new File(coverImagePath))
                                    .centerCrop()
                                    .into(coverPhoto);
                        }

                        if (followingCountLong != null) {
                            followingCount.setText(String.valueOf(followingCountLong));
                        }

                        if (followersCountLong != null) {
                            followersCount.setText(String.valueOf(followersCountLong));
                        }

                        // Katılma tarihi
                        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", new Locale("tr"));
                        joinDate.setText(sdf.format(new Date()) + " tarihinde katıldı");

                        // Action button - DÜZELTME
                        if (isOwnProfile) {
                            actionButton.setText("Profili Düzenle");
                            actionButton.setBackgroundResource(R.drawable.button_outline);
                        } else {
                            // Başkasının profili - takip durumuna göre ayarla
                            updateFollowButton();
                        }
                    } else {
                        Toast.makeText(this, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Profil yüklenemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserPosts() {
        db.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userPosts.clear();
                    int postCount = 0;

                    List<Post> tempPosts = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Post post = document.toObject(Post.class);
                        if (post != null) {
                            post.setPostId(document.getId());

                            Timestamp timestamp = document.getTimestamp("timestamp");
                            if (timestamp != null) {
                                post.setTimeAgo(calculateTimeAgo(timestamp.toDate()));
                                post.setTimestamp(timestamp);
                            } else {
                                post.setTimeAgo("Şimdi");
                            }

                            tempPosts.add(post);
                            postCount++;
                        }
                    }

                    // Manuel sıralama
                    tempPosts.sort((p1, p2) -> {
                        if (p1.getTimestamp() == null || p2.getTimestamp() == null) return 0;
                        return Long.compare(p2.getTimestamp().getSeconds(), p1.getTimestamp().getSeconds());
                    });

                    userPosts.addAll(tempPosts);

                    toolbarPostCount.setText(postCount + " gönderi");
                    postAdapter.notifyDataSetChanged();

                    if (userPosts.isEmpty()) {
                        Toast.makeText(this, "Henüz gönderi yok", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Gönderiler yüklenemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
        if (!isOwnProfile) {
            checkFollowStatus();
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

