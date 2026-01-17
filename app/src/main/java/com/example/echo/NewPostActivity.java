package com.example.echo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class NewPostActivity extends AppCompatActivity {

    private ImageView closeButton, profileImage, addImageButton, addGifButton;
    private ImageView addPollButton, addLocationButton, privacyButton;
    private ImageView imagePreview, removeImageButton;
    private Button postButton;
    private EditText postContentEditText;
    private TextView characterCounter;
    private RelativeLayout imagePreviewContainer;

    private Uri selectedImageUri;
    private static final int MAX_CHARACTERS = 280;

    private ActivityResultLauncher<String> imagePickerLauncher;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LocalImageManager localImageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);
        localImageManager = new LocalImageManager(this);

        // Firebase başlat
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // View'ları bağla
        closeButton = findViewById(R.id.closeButton);
        profileImage = findViewById(R.id.profileImage);
        postButton = findViewById(R.id.postButton);
        postContentEditText = findViewById(R.id.postContentEditText);
        characterCounter = findViewById(R.id.characterCounter);
        addImageButton = findViewById(R.id.addImageButton);
        privacyButton = findViewById(R.id.privacyButton);
        imagePreview = findViewById(R.id.imagePreview);
        removeImageButton = findViewById(R.id.removeImageButton);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);

        // Görsel seçici başlat
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imagePreview.setImageURI(uri);
                        imagePreviewContainer.setVisibility(View.VISIBLE);
                        checkPostButtonState();
                    }
                }
        );

        // EditText'e odaklan ve klavyeyi göster
        postContentEditText.requestFocus();

        setupListeners();
    }

    private void setupListeners() {
        // Kapat butonu
        closeButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Karakter sayacı ve buton durumu
        postContentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                characterCounter.setText(length + "/" + MAX_CHARACTERS);

                // Karakter limiti kontrolü
                if (length > MAX_CHARACTERS - 20) {
                    characterCounter.setTextColor(length >= MAX_CHARACTERS ?
                            Color.parseColor("#E0245E") : Color.parseColor("#FFD700"));
                } else {
                    characterCounter.setTextColor(getColor(R.color.text_secondary));
                }

                checkPostButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Gönder butonu
        postButton.setOnClickListener(v -> publishPost());

        // Görsel ekle butonu
        addImageButton.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        // Görseli kaldır butonu
        removeImageButton.setOnClickListener(v -> {
            selectedImageUri = null;
            imagePreviewContainer.setVisibility(View.GONE);
            checkPostButtonState();
        });

        // Gizlilik ayarları
        privacyButton.setOnClickListener(v -> {
            Toast.makeText(this, "Gizlilik ayarları yakında...", Toast.LENGTH_SHORT).show();
        });
    }

    private void checkPostButtonState() {
        String content = postContentEditText.getText().toString().trim();
        boolean hasContent = !content.isEmpty();
        boolean hasImage = selectedImageUri != null;

        // En az metin veya görsel varsa gönder butonu aktif
        postButton.setEnabled(hasContent || hasImage);
        postButton.setAlpha(postButton.isEnabled() ? 1.0f : 0.5f);
    }

    private void publishPost() {
        String content = postContentEditText.getText().toString().trim();

        if (content.isEmpty() && selectedImageUri == null) {
            Toast.makeText(this, "Gönderi içeriği boş olamaz", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Lütfen giriş yapın", Toast.LENGTH_SHORT).show();
            return;
        }

        // Butonu devre dışı bırak
        postButton.setEnabled(false);
        postButton.setText("Gönderiliyor...");

        // Kullanıcı bilgilerini Firestore'dan al
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userName;
                    String usertag;

                    if (documentSnapshot.exists()) {
                        userName = documentSnapshot.getString("fullName");
                        usertag = documentSnapshot.getString("usertag");
                    } else {
                        // Kullanıcı bilgisi yoksa varsayılan değerler
                        userName = currentUser.getEmail() != null ?
                                currentUser.getEmail().split("@")[0] : "Kullanıcı";
                        usertag = userName.toLowerCase().replace(" ", "");
                    }

                    // Post verisini Map olarak oluştur
                    Map<String, Object> postData = new HashMap<>();
                    postData.put("userId", currentUser.getUid());
                    postData.put("userName", userName);
                    postData.put("usertag", usertag);
                    postData.put("content", content);
                    postData.put("timestamp", FieldValue.serverTimestamp());
                    postData.put("commentCount", 0);
                    postData.put("retweetCount", 0);
                    postData.put("likeCount", 0);
                    postData.put("isLiked", false);

                    // Firestore'a kaydet (önce post ID'yi al)
                    db.collection("posts")
                            .add(postData)
                            .addOnSuccessListener(documentReference -> {
                                String postId = documentReference.getId();

                                // ✨ GÖRSEL VARsa LOCAL'e KAYDET
                                if (selectedImageUri != null) {
                                    String imagePath = localImageManager.savePostImage(postId, selectedImageUri);

                                    if (imagePath != null) {
                                        // Firestore'da imageUrl field'ını local path ile güncelle
                                        documentReference.update("imageUrl", imagePath)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(this, "Gönderin yayınlandı! 🎉", Toast.LENGTH_SHORT).show();
                                                    finishActivity();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("NewPostActivity", "Image path güncellenemedi", e);
                                                    Toast.makeText(this, "Gönderin yayınlandı! 🎉", Toast.LENGTH_SHORT).show();
                                                    finishActivity();
                                                });
                                    } else {
                                        Toast.makeText(this, "Görsel kaydedilemedi ama post paylaşıldı", Toast.LENGTH_SHORT).show();
                                        finishActivity();
                                    }
                                } else {
                                    // Görsel yoksa direkt bitir
                                    Toast.makeText(this, "Gönderin yayınlandı! 🎉", Toast.LENGTH_SHORT).show();
                                    finishActivity();
                                }
                            })
                            .addOnFailureListener(e -> {
                                postButton.setEnabled(true);
                                postButton.setText("Gönder");
                                Toast.makeText(this, "Gönderi yüklenemedi: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    postButton.setEnabled(true);
                    postButton.setText("Gönder");
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void finishActivity() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("post_created", true);
        setResult(RESULT_OK, resultIntent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}