package com.example.echo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextInputEditText fullNameEditText, usernameEditText, emailEditText,
            passwordEditText, confirmPasswordEditText;
    private CheckBox termsCheckBox;
    private Button signUpButton;
    private TextView loginLinkText;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        setContentView(R.layout.activity_signup);

        // View'ları bağlama
        fullNameEditText = findViewById(R.id.fullNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        signUpButton = findViewById(R.id.signUpButton);
        loginLinkText = findViewById(R.id.loginLinkText);
        backButton = findViewById(R.id.backButton);

        // Kayıt Ol butonu
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Giriş Yap linki
        loginLinkText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });

        // Geri butonu
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }
        });
    }

    private void registerUser() {
        String fullName = fullNameEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validasyon
        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError("Enter Name and Surname");
            fullNameEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(username)) {
            usernameEditText.setError("Enter Username");
            usernameEditText.requestFocus();
            return;
        }

        if (username.length() < 3) {
            usernameEditText.setError("Username must be 3 characters in length");
            usernameEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Enter Email");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Enter Password");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be 6 characters in length");
            passwordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            confirmPasswordEditText.requestFocus();
            return;
        }

        if (!termsCheckBox.isChecked()) {
            Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Butonu devre dışı bırak
        signUpButton.setEnabled(false);
        Toast.makeText(this, "Kaydınız yapılıyor...", Toast.LENGTH_SHORT).show();

        // Firebase ile kullanıcıyı e-posta ve şifre ile oluştur
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Kullanıcı başarıyla oluşturuldu, şimdi Firestore'a kullanıcı bilgilerini kaydet
                        String userId = mAuth.getCurrentUser().getUid();

                        Map<String, Object> user = new HashMap<>();
                        user.put("fullName", fullName);
                        user.put("usertag", username);
                        user.put("email", email);
                        user.put("bio", "");
                        user.put("profileImageUrl", "");
                        user.put("followerCount", 0);
                        user.put("followingCount", 0);

                        db.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(SignUpActivity.this, "Kayıt başarılı! 🎉", Toast.LENGTH_SHORT).show();
                                    finish();
                                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                                })
                                .addOnFailureListener(e -> {
                                    signUpButton.setEnabled(true);
                                    Toast.makeText(SignUpActivity.this, "Kullanıcı bilgileri kaydedilemedi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    } else {
                        // Kayıt başarısız oldu
                        signUpButton.setEnabled(true);
                        Toast.makeText(SignUpActivity.this, "Kayıt başarısız: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}