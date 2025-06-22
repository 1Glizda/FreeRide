package com.cristeabogdan.freeride.account;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cristeabogdan.freeride.auth.LoginActivity;
import com.cristeabogdan.freeride.auth.UserProfileManager;
import com.cristeabogdan.freeride.databinding.ActivityAccountBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class AccountActivity extends AppCompatActivity {

    private static final String TAG = "AccountActivity";
    private ActivityAccountBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        setupUI();
        setUpClickListeners();
        loadUserData();
    }

    private void setupUI() {
        // Set up the toolbar or any initial UI elements
    }

    private void setUpClickListeners() {
        binding.backButton.setOnClickListener(v -> finish());

        binding.logoutButton.setOnClickListener(v -> logout());

        binding.editProfileButton.setOnClickListener(v -> {
            // TODO: Implement edit profile functionality
            Toast.makeText(this, "Edit profile feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            binding.progressBar.setVisibility(View.VISIBLE);

            // Set basic user info
            binding.emailTextView.setText(currentUser.getEmail());
            binding.nameTextView.setText(currentUser.getDisplayName() != null ? 
                currentUser.getDisplayName() : "User");

            // Load additional user data from Firestore
            UserProfileManager.getUserData(currentUser.getUid(), new UserProfileManager.UserDataCallback() {
                @Override
                public void onSuccess(Map<String, Object> userData) {
                    binding.progressBar.setVisibility(View.GONE);
                    
                    if (userData != null) {
                        String displayName = (String) userData.get("displayName");
                        Long createdAt = (Long) userData.get("createdAt");
                        
                        if (displayName != null) {
                            binding.nameTextView.setText(displayName);
                        }
                        
                        if (createdAt != null) {
                            String memberSince = "Member since " + 
                                new java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
                                    .format(new java.util.Date(createdAt));
                            binding.memberSinceTextView.setText(memberSince);
                        }
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to load user data", e);
                    Toast.makeText(AccountActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}