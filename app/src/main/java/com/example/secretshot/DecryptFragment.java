package com.example.secretshot;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.io.InputStream;

public class DecryptFragment extends Fragment {

    // Tag used for logging
    private static final String TAG = "DecryptFragment";

    private Activity activity;
    private ImageView decrypt_img_attached;
    private TextView decrypt_TXT_encryptedData;
    private Button decrypt_BTN_selectImage;
    private EditText decrypt_TXT_password;
    private MaterialButton decrypt_BTN_decrypt;

    private Bitmap selectedImage; // משתנה גלובלי לשמירת התמונה הנבחרת
    private ActivityResultLauncher<Intent> resultLauncher;

    public DecryptFragment(Activity activity) {
        this.activity = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout
        View root = inflater.inflate(R.layout.fragment_decrypt, container, false);
        findViews(root);
        initVars();
        return root;
    }

    private void initVars() {
        Log.d(TAG, "Initializing variables and setting up listeners");

        registerResult();

        // Handle the button for selecting an image
        decrypt_BTN_selectImage.setOnClickListener(v -> {
            Log.d(TAG, "Select Image button clicked");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_MEDIA_IMAGES)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted - opening gallery");
                    Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                    resultLauncher.launch(intent);
                } else {
                    Log.d(TAG, "Permission NOT granted - user must enable it first");
                    Toast.makeText(activity, "No permissions to access gallery!", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Handle the button for decryption
        decrypt_BTN_decrypt.setOnClickListener(v -> {
            Log.d(TAG, "Decrypt button clicked");

            // Check if an image has been selected
            if (selectedImage == null) {
                Log.d(TAG, "No image selected");
                Toast.makeText(getContext(), "Please select an image first!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if a password has been entered
            String password = decrypt_TXT_password.getText().toString().trim();
            if (password.isEmpty()) {
                Log.d(TAG, "No password provided");
                Toast.makeText(getContext(), "Please enter the decryption password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Attempt to decrypt the selected image
            try {
                Log.d(TAG, "Attempting to decrypt image");

                String decryptedText = ImageEncryption.decryptText(selectedImage, password);
                decrypt_TXT_encryptedData.setText(decryptedText);
                Log.d(TAG, "Decryption successful");
            } catch (Exception e) {
                Log.e(TAG, "Decryption failed", e);
                decrypt_TXT_encryptedData.setText("Incorrect password or no encrypted data found!");
            }
        });
    }

    private void findViews(View root) {
        // Connect UI components to fields
        decrypt_img_attached = root.findViewById(R.id.decrypt_img_attached);
        decrypt_TXT_encryptedData = root.findViewById(R.id.decrypt_TXT_encryptedData);
        decrypt_BTN_selectImage = root.findViewById(R.id.decrypt_BTN_selectImage);
        decrypt_TXT_password = root.findViewById(R.id.decrypt_TXT_password);
        decrypt_BTN_decrypt = root.findViewById(R.id.decrypt_BTN_decrypt);
    }

    public void registerResult() {
        // Register for selecting images from the gallery
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            try {
                                Uri imageUri = result.getData().getData();
                                InputStream inputStream = activity.getContentResolver().openInputStream(imageUri);
                                selectedImage = BitmapFactory.decodeStream(inputStream);
                                inputStream.close();

                                decrypt_img_attached.setImageBitmap(selectedImage);
                                decrypt_TXT_encryptedData.setText(""); // Clear previous text

                            } catch (Exception e) {
                                showError();
                            }
                        } else {
                            showError();
                        }
                    }
                }
        );
    }

    private void showError() {
        // Show an error message if something goes wrong
        Toast.makeText(activity, "No image selected!", Toast.LENGTH_LONG).show();
        decrypt_TXT_encryptedData.setText("");
        decrypt_img_attached.setImageResource(R.drawable.attach_image);
    }

    public void clean() {
        // Reset UI elements
        decrypt_img_attached.setImageResource(R.drawable.attach_image);
        decrypt_TXT_encryptedData.setText("");
        decrypt_TXT_password.setText(""); // Clearing the password field
        Log.d(TAG, "UI elements reset (clean)");

    }
}
