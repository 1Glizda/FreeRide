package com.cristeabogdan.freeride.auth;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserProfileManager {
    private static final String TAG = "UserProfileManager";
    private static final String USERS_COLLECTION = "users";

    public static void updateUserProfile(FirebaseUser user, String displayName, Uri photoUri) {
        if (user == null) return;

        UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder();

        if (displayName != null) {
            profileUpdates.setDisplayName(displayName);
        }

        if (photoUri != null) {
            profileUpdates.setPhotoUri(photoUri);
        }

        user.updateProfile(profileUpdates.build())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User profile updated.");
                        saveUserToFirestore(user, displayName);
                    } else {
                        Log.e(TAG, "Failed to update user profile", task.getException());
                    }
                });
    }

    private static void saveUserToFirestore(FirebaseUser user, String displayName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("displayName", displayName != null ? displayName : user.getDisplayName());
        userData.put("createdAt", System.currentTimeMillis());

        db.collection(USERS_COLLECTION)
                .document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User data saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving user data", e));
    }

    public interface UserDataCallback {
        void onSuccess(Map<String, Object> userData);
        void onFailure(Exception e);
    }

    public static void getUserData(String uid, UserDataCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        callback.onSuccess(documentSnapshot.getData());
                    } else {
                        callback.onFailure(new Exception("User data not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
}