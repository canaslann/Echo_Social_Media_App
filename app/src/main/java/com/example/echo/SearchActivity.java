package com.example.echo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private ImageView backButton, clearButton;
    private EditText searchEditText;
    private TabLayout searchTabLayout;
    private RecyclerView usersRecyclerView, postsRecyclerView;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateText;
    private ProgressBar progressBar;

    private UserAdapter userAdapter;
    private PostAdapter postAdapter;
    private List<User> userList;
    private List<Post> postList;

    private FirebaseFirestore db;
    private Handler searchHandler;
    private Runnable searchRunnable;

    private static final int SEARCH_DELAY = 500; // 500ms gecikme
    private int currentTab = 0; // 0: Kullanıcılar, 1: Gönderiler

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = FirebaseFirestore.getInstance();
        searchHandler = new Handler(Looper.getMainLooper());

        initViews();
        setupListeners();
        setupRecyclerViews();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        clearButton = findViewById(R.id.clearButton);
        searchEditText = findViewById(R.id.searchEditText);
        searchTabLayout = findViewById(R.id.searchTabLayout);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        postsRecyclerView = findViewById(R.id.postsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        emptyStateText = findViewById(R.id.emptyStateText);
        progressBar = findViewById(R.id.progressBar);

        // Klavyeyi otomatik aç
        searchEditText.requestFocus();
    }

    private void setupRecyclerViews() {
        // Kullanıcılar
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(userAdapter);

        // Gönderiler
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, this); // ✅ Context ekle (this)
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsRecyclerView.setAdapter(postAdapter);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        clearButton.setOnClickListener(v -> {
            searchEditText.setText("");
            clearButton.setVisibility(View.GONE);
            showEmptyState(true);
        });

        // Tab değişimi
        searchTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                switchTab(currentTab);

                // Varsa arama yap
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Arama input değişikliği
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();

                // Temizle butonunu göster/gizle
                clearButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                // Önceki arama isteğini iptal et
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                if (query.isEmpty()) {
                    showEmptyState(true);
                    return;
                }

                // Yeni arama isteği (500ms sonra)
                searchRunnable = () -> performSearch(query);
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void switchTab(int position) {
        currentTab = position;

        if (position == 0) {
            // Kullanıcılar
            usersRecyclerView.setVisibility(View.VISIBLE);
            postsRecyclerView.setVisibility(View.GONE);
        } else {
            // Gönderiler
            usersRecyclerView.setVisibility(View.GONE);
            postsRecyclerView.setVisibility(View.VISIBLE);
        }

        // Boş durum kontrolü
        String query = searchEditText.getText().toString().trim();
        if (query.isEmpty()) {
            showEmptyState(true);
        } else if (position == 0 && userList.isEmpty()) {
            showEmptyState(true);
            emptyStateText.setText("'" + query + "' için kullanıcı bulunamadı");
        } else if (position == 1 && postList.isEmpty()) {
            showEmptyState(true);
            emptyStateText.setText("'" + query + "' için gönderi bulunamadı");
        } else {
            showEmptyState(false);
        }
    }

    private void performSearch(String query) {
        showLoading(true);

        if (currentTab == 0) {
            searchUsers(query);
        } else {
            searchPosts(query);
        }
    }

    private void searchUsers(String query) {
        String lowerQuery = query.toLowerCase();

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setUserId(document.getId());

                            // İsim veya kullanıcı adı ile eşleştir
                            String fullName = user.getFullName() != null ?
                                    user.getFullName().toLowerCase() : "";
                            String usertag = user.getUsertag() != null ?
                                    user.getUsertag().toLowerCase() : "";

                            if (fullName.contains(lowerQuery) || usertag.contains(lowerQuery)) {
                                userList.add(user);
                            }
                        }
                    }

                    userAdapter.notifyDataSetChanged();
                    showLoading(false);

                    if (userList.isEmpty()) {
                        showEmptyState(true);
                        emptyStateText.setText("'" + query + "' için kullanıcı bulunamadı");
                    } else {
                        showEmptyState(false);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Arama başarısız: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void searchPosts(String query) {
        String lowerQuery = query.toLowerCase();

        db.collection("posts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();

                    // Debug: Toplam gönderi sayısı
                    int totalPosts = queryDocumentSnapshots.size();
                    android.util.Log.d("SearchActivity", "Toplam gönderi: " + totalPosts);
                    android.util.Log.d("SearchActivity", "Aranan kelime: " + lowerQuery);

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Post post = document.toObject(Post.class);
                        if (post != null) {
                            post.setPostId(document.getId());

                            // İçerikte ara
                            String content = post.getContent() != null ?
                                    post.getContent().toLowerCase() : "";
                            String userName = post.getUserName() != null ?
                                    post.getUserName().toLowerCase() : "";
                            String userTag = post.getUserTag() != null ?
                                    post.getUserTag().toLowerCase() : "";

                            // Debug
                            android.util.Log.d("SearchActivity", "Post içeriği: " + content);

                            // İçerik, kullanıcı adı veya usertag'de ara
                            if (content.contains(lowerQuery) ||
                                    userName.contains(lowerQuery) ||
                                    userTag.contains(lowerQuery)) {

                                // Timestamp'den timeAgo hesapla
                                Timestamp timestamp = document.getTimestamp("timestamp");
                                if (timestamp != null) {
                                    post.setTimeAgo(calculateTimeAgo(timestamp.toDate()));
                                    post.setTimestamp(timestamp); // Timestamp'i de set et
                                } else {
                                    post.setTimeAgo("Şimdi");
                                }

                                postList.add(post);
                                android.util.Log.d("SearchActivity", "Post eklendi: " + content);
                            }
                        }
                    }

                    android.util.Log.d("SearchActivity", "Bulunan gönderi: " + postList.size());

                    // En yeni en üstte olacak şekilde sırala
                    if (postList.size() > 1) {
                        postList.sort((p1, p2) -> {
                            if (p1.getTimestamp() == null && p2.getTimestamp() == null) return 0;
                            if (p1.getTimestamp() == null) return 1;
                            if (p2.getTimestamp() == null) return -1;
                            return Long.compare(p2.getTimestamp().getSeconds(),
                                    p1.getTimestamp().getSeconds());
                        });
                    }

                    postAdapter.notifyDataSetChanged();
                    showLoading(false);

                    if (postList.isEmpty()) {
                        showEmptyState(true);
                        emptyStateText.setText("'" + query + "' için gönderi bulunamadı");
                    } else {
                        showEmptyState(false);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Arama başarısız: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Debug için hatayı logla
                    android.util.Log.e("SearchActivity", "Post arama hatası: ", e);
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

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean show) {
        emptyStateLayout.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show) {
            usersRecyclerView.setVisibility(View.GONE);
            postsRecyclerView.setVisibility(View.GONE);

            if (searchEditText.getText().toString().trim().isEmpty()) {
                emptyStateText.setText("Kullanıcı veya gönderi ara");
            }
        } else {
            // Tab'a göre doğru RecyclerView'ı göster
            if (currentTab == 0) {
                usersRecyclerView.setVisibility(View.VISIBLE);
                postsRecyclerView.setVisibility(View.GONE);
            } else {
                usersRecyclerView.setVisibility(View.GONE);
                postsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Handler'ı temizle
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}