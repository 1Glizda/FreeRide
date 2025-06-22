package com.cristeabogdan.freeride.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cristeabogdan.freeride.databinding.ActivityLoginBinding;
import com.cristeabogdan.freeride.maps.MapsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        setUpClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMaps();
        }
    }

    private void setUpClickListeners() {
        binding.loginButton.setOnClickListener(v -> loginUser());

        binding.registerButton.setOnClickListener(v -> registerUser());

        binding.registerTextView.setOnClickListener(v -> {
            binding.registerLayout.setVisibility(View.VISIBLE);
            binding.loginLayout.setVisibility(View.GONE);
        });

        binding.backToLoginTextView.setOnClickListener(v -> {
            binding.loginLayout.setVisibility(View.VISIBLE);
            binding.registerLayout.setVisibility(View.GONE);
        });
    }

    private void loginUser() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.emailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordEditText.setError("Password is required");
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.loginButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        navigateToMaps();
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUser() {
        String email = binding.registerEmailEditText.getText().toString().trim();
        String password = binding.registerPasswordEditText.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();
        String name = binding.nameEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.nameEditText.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            binding.registerEmailEditText.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.registerPasswordEditText.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            binding.registerPasswordEditText.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordEditText.setError("Passwords do not match");
            return;
        }

        binding.registerProgressBar.setVisibility(View.VISIBLE);
        binding.registerButton.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.registerProgressBar.setVisibility(View.GONE);
                    binding.registerButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Update user profile with name
                        if (user != null) {
                            UserProfileManager.updateUserProfile(user, name, null);
                        }

                        Toast.makeText(LoginActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        navigateToMaps();
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Registration failed: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToMaps() {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
        finish();
    }
}