package com.example.echo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private ImageView backButton, markAllReadButton;
    private TabLayout notificationTabLayout;
    private RecyclerView notificationsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LocalImageManager localImageManager;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private int currentTab = 0; // 0: Tümü, 1: Beğeniler, 2: Yorumlar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Firebase başlat
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        localImageManager = new LocalImageManager(this);

        if (currentUser == null) {
            Toast.makeText(this, "Lütfen giriş yapın", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupListeners();
        loadNotifications();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        markAllReadButton = findViewById(R.id.markAllReadButton);
        notificationTabLayout = findViewById(R.id.notificationTabLayout);
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notificationList, localImageManager);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecyclerView.setAdapter(notificationAdapter);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        // Tümünü okundu işaretle
        markAllReadButton.setOnClickListener(v -> markAllAsRead());

        // Tab değişimi
        notificationTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadNotifications();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Swipe to refresh
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadNotifications();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadNotifications() {
        showLoading(true);

        Query query = db.collection("notifications")
                .whereEqualTo("recipientId", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING);

        // Tab'e göre filtrele
        if (currentTab == 1) {
            // Sadece beğeniler
            query = query.whereIn("type",
                    java.util.Arrays.asList(
                            Notification.TYPE_LIKE_POST,
                            Notification.TYPE_LIKE_COMMENT
                    ));
        } else if (currentTab == 2) {
            // Sadece yorumlar
            query = query.whereEqualTo("type", Notification.TYPE_COMMENT);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Notification notification = document.toObject(Notification.class);
                        if (notification != null) {
                            notification.setNotificationId(document.getId());

                            // Timestamp'den timeAgo hesapla
                            Timestamp timestamp = document.getTimestamp("timestamp");
                            if (timestamp != null) {
                                notification.setTimeAgo(calculateTimeAgo(timestamp.toDate()));
                            } else {
                                notification.setTimeAgo("Şimdi");
                            }

                            notificationList.add(notification);
                        }
                    }

                    notificationAdapter.notifyDataSetChanged();
                    showLoading(false);

                    if (notificationList.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Bildirimler yüklenemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void markAllAsRead() {
        if (notificationList.isEmpty()) {
            Toast.makeText(this, "Okunmamış bildirim yok", Toast.LENGTH_SHORT).show();
            return;
        }

        int unreadCount = 0;
        for (Notification notification : notificationList) {
            if (!notification.isRead()) {
                unreadCount++;
                db.collection("notifications")
                        .document(notification.getNotificationId())
                        .update("read", true);
            }
        }

        if (unreadCount > 0) {
            Toast.makeText(this, unreadCount + " bildirim okundu işaretlendi",
                    Toast.LENGTH_SHORT).show();
            loadNotifications();
        } else {
            Toast.makeText(this, "Tüm bildirimler zaten okunmuş",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String calculateTimeAgo(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;

        if (seconds < 5) {
            return "Şimdi";
        } else if (seconds < 60) {
            return seconds + "s";
        } else if (minutes < 60) {
            return minutes + "dk";
        } else if (hours < 24) {
            return hours + "sa";
        } else if (days < 7) {
            return days + "g";
        } else if (weeks < 4) {
            return weeks + "h";
        } else {
            return "1ay+";
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show) {
        emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        notificationsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}