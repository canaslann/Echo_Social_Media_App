package com.example.echo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView postsRecyclerView;
    private PostAdapter postAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private List<Post> postList;

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Firebase başlat
        db = FirebaseFirestore.getInstance();

        postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        setupRecyclerView();
        loadPostsFromFirestore();
        setupSwipeRefresh();

        return view;
    }

    private void setupRecyclerView() {
        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList, getContext()); // ✅ Context ekle
        postsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postsRecyclerView.setAdapter(postAdapter);
    }

    private void loadPostsFromFirestore() {
        // Firestore'dan gönderileri yükle (en yeni en üstte)
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        // Hiç gönderi yoksa örnek gönderiler ekle
                    } else {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Post post = document.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(document.getId());

                                // Timestamp'den timeAgo hesapla
                                Timestamp timestamp = document.getTimestamp("timestamp");
                                if (timestamp != null) {
                                    post.setTimeAgo(calculateTimeAgo(timestamp.toDate()));
                                } else {
                                    post.setTimeAgo("Şimdi");
                                }

                                postList.add(post);
                            }
                        }
                        postAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Gönderiler yüklenemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String calculateTimeAgo(Date date) {
        long diff = System.currentTimeMillis() - date.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;

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
        } else if (months < 12) {
            return months + "ay";
        } else {
            return years + "y";
        }
    }

    // Fragment tekrar görünür olduğunda listeyi güncelle
    @Override
    public void onResume() {
        super.onResume();
        // Firestore'dan güncel veriyi çek
        loadPostsFromFirestore();
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Gönderileri yenile
            loadPostsFromFirestore();
            swipeRefreshLayout.setRefreshing(false);
        });
    }
}