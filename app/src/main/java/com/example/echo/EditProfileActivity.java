package com.example.echo;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView closeButton, coverPhotoPreview, profilePhotoPreview;
    private ImageView editProfilePhotoButton;
    private TextInputEditText fullNameEditText, usernameEditText, bioEditText;
    private Button saveButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private LocalImageManager localImageManager;

    private Uri selectedProfileImageUri;
    private Uri selectedCoverImageUri;
    private String currentProfileImagePath;
    private String currentCoverImagePath;

    // Image Picker Launchers
    private ActivityResultLauncher<String> profileImagePicker;
    private ActivityResultLauncher<String> coverImagePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Firebase başlat
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        localImageManager = new LocalImageManager(this);

        initViews();
        setupImagePickers();
        loadCurrentProfile();
        setupListeners();
    }

    private void initViews() {
        closeButton = findViewById(R.id.closeButton);
        coverPhotoPreview = findViewById(R.id.coverPhotoPreview);
        profilePhotoPreview = findViewById(R.id.profilePhotoPreview);
        editProfilePhotoButton = findViewById(R.id.editProfilePhotoButton);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        bioEditText = findViewById(R.id.bioEditText);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupImagePickers() {
        // Profil fotoğrafı picker
        profileImagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedProfileImageUri = uri;
                        // Preview göster
                        Glide.with(this)
                                .load(uri)
                                .circleCrop()
                                .into(profilePhotoPreview);
                    }
                }
        );

        // Cover photo picker
        coverImagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedCoverImageUri = uri;
                        // Preview göster
                        Glide.with(this)
                                .load(uri)
                                .centerCrop()
                                .into(coverPhotoPreview);
                    }
                }
        );
    }

    private void loadCurrentProfile() {
        if (currentUser == null) {
            Toast.makeText(this, "Kullanıcı bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = currentUser.getUid();

        // Local'den profil fotoğraflarını yükle
        currentProfileImagePath = localImageManager.getProfileImagePath(userId);
        currentCoverImagePath = localImageManager.getCoverImagePath(userId);

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("fullName");
                        String userTag = documentSnapshot.getString("usertag");
                        String userBio = documentSnapshot.getString("bio");

                        if (name != null) {
                            fullNameEditText.setText(name);
                        }

                        if (userTag != null) {
                            usernameEditText.setText(userTag);
                        }

                        if (userBio != null) {
                            bioEditText.setText(userBio);
                        }

                        // Mevcut profil fotoğrafını yükle (LOCAL)
                        if (currentProfileImagePath != null && localImageManager.fileExists(currentProfileImagePath)) {
                            Glide.with(this)
                                    .load(new File(currentProfileImagePath))
                                    .circleCrop()
                                    .placeholder(R.mipmap.ic_launcher)
                                    .into(profilePhotoPreview);
                        }

                        // Mevcut cover fotoğrafını yükle (LOCAL)
                        if (currentCoverImagePath != null && localImageManager.fileExists(currentCoverImagePath)) {
                            Glide.with(this)
                                    .load(new File(currentCoverImagePath))
                                    .centerCrop()
                                    .into(coverPhotoPreview);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Profil yüklenemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupListeners() {
        closeButton.setOnClickListener(v -> finish());

        // Profil fotoğrafı değiştir
        editProfilePhotoButton.setOnClickListener(v -> {
            profileImagePicker.launch("image/*");
        });

        // Cover photo değiştir
        coverPhotoPreview.setOnClickListener(v -> {
            coverImagePicker.launch("image/*");
        });

        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String newFullName = fullNameEditText.getText().toString().trim();
        String newUsername = usernameEditText.getText().toString().trim();
        String newBio = bioEditText.getText().toString().trim();

        // Validasyon
        if (TextUtils.isEmpty(newFullName)) {
            fullNameEditText.setError("Ad Soyad gerekli");
            fullNameEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newUsername)) {
            usernameEditText.setError("Kullanıcı adı gerekli");
            usernameEditText.requestFocus();
            return;
        }

        if (newUsername.length() < 3) {
            usernameEditText.setError("Kullanıcı adı en az 3 karakter olmalı");
            usernameEditText.requestFocus();
            return;
        }

        // Butonu devre dışı bırak
        saveButton.setEnabled(false);
        saveButton.setText("Kaydediliyor...");

        String userId = currentUser.getUid();

        // Profil fotoğrafı seçildiyse kaydet (LOCAL)
        if (selectedProfileImageUri != null) {
            currentProfileImagePath = localImageManager.saveProfileImage(userId, selectedProfileImageUri);
            if (currentProfileImagePath == null) {
                Toast.makeText(this, "Profil fotoğrafı kaydedilemedi", Toast.LENGTH_SHORT).show();
            }
        }

        // Cover fotoğrafı seçildiyse kaydet (LOCAL)
        if (selectedCoverImageUri != null) {
            currentCoverImagePath = localImageManager.saveCoverImage(userId, selectedCoverImageUri);
            if (currentCoverImagePath == null) {
                Toast.makeText(this, "Kapak fotoğrafı kaydedilemedi", Toast.LENGTH_SHORT).show();
            }
        }

        // Firestore'u güncelle (sadece text bilgileri + timestamp)
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", newFullName);
        updates.put("usertag", newUsername);
        updates.put("bio", newBio);
        // ✨ YENİ: Profil güncellenme zamanını ekle (cache busting için)
        updates.put("profileUpdatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // ✨ CACHE TEMİZLE
                    if (selectedProfileImageUri != null || selectedCoverImageUri != null) {
                        localImageManager.clearGlideCache(userId);
                    }

                    Toast.makeText(this, "Profil güncellendi! ✅", Toast.LENGTH_SHORT).show();

                    // ✨ RESULT GÖNDER (ProfileActivity güncellensin diye)
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("Kaydet");
                    Toast.makeText(this, "Güncelleme başarısız: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}

