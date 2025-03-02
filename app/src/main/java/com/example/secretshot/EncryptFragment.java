package com.example.secretshot;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class EncryptFragment extends Fragment {

    // Tag for logging
    private static final String TAG = "EncryptFragment";

    private MaterialButton encrypt_BTN_encrypt;
    private Button encrypt_BTN_selectImage;
    private ImageView encrypt_img_attached;
    private EditText encrypt_TXT_text;
    // שדה חדש לסיסמה:
    private EditText encrypt_TXT_password;

    private Bitmap selectedImage;
    private Activity activity;
    private ActivityResultLauncher<Intent> resultLauncher;

    public EncryptFragment(Activity activity) {
        this.activity = activity;
        this.selectedImage = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Inflating layout for EncryptFragment");
        View root = inflater.inflate(R.layout.fragment_encrypt, container, false);
        findViews(root);
        initVars();
        return root;
    }

    private void initVars() {
        Log.d(TAG, "initVars: Registering result launcher and setting up UI events");

        registerResult();

        // Handle the image selection button
        encrypt_BTN_selectImage.setOnClickListener(v -> {
            Log.d(TAG, "Select Image button clicked");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_MEDIA_IMAGES)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission granted, opening gallery");
                    Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                    resultLauncher.launch(intent);
                } else {
                    Log.d(TAG, "No permission to read gallery");
                    Toast.makeText(activity, "No permissions to access gallery!", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Handle the encryption button
        encrypt_BTN_encrypt.setOnClickListener(v -> {
            Log.d(TAG, "Encrypt button clicked");
            String secretText = encrypt_TXT_text.getText().toString().trim();
            String password = encrypt_TXT_password.getText().toString().trim();

            // Validate input
            if (secretText.isEmpty()) {
                Log.d(TAG, "No text provided for encryption");
                Toast.makeText(activity, "Please enter text to encrypt!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                Log.d(TAG, "No password provided for encryption");
                Toast.makeText(activity, "Please enter a password!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedImage == null) {
                Log.d(TAG, "No image selected for encryption");
                Toast.makeText(activity, "Please select an image!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Encrypt the image with text and password
            Log.d(TAG, "Encrypting image...");
            Bitmap encryptedImage = ImageEncryption.encryptText(selectedImage, secretText, password);
            if (encryptedImage != null) {
                saveImageToGallery(encryptedImage);
                Toast.makeText(activity, "Encrypted image saved!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Image encrypted and saved successfully");
            } else {
                Toast.makeText(activity, "Encryption failed!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Encryption returned null");
            }
        });
    }

    private void findViews(View root) {
        // Connect UI elements
        encrypt_BTN_encrypt = root.findViewById(R.id.encrypt_BTN_encrypt);
        encrypt_BTN_selectImage = root.findViewById(R.id.encrypt_BTN_selectImage); // Image selection button
        encrypt_img_attached = root.findViewById(R.id.encrypt_img_attached);
        encrypt_TXT_text = root.findViewById(R.id.encrypt_TXT_text);
        encrypt_TXT_password = root.findViewById(R.id.encrypt_TXT_password);

        Log.d(TAG, "findViews: UI components are initialized");

    }

    private void registerResult() {
        Log.d(TAG, "registerResult: Setting up activity result callback for image selection");
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        Log.d(TAG, "onActivityResult: Received result from image picker");
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            try {
                                Uri imageUri = result.getData().getData();
                                InputStream inputStream = activity.getContentResolver().openInputStream(imageUri);
                                selectedImage = BitmapFactory.decodeStream(inputStream);
                                inputStream.close();

                                // הצגת התמונה שנבחרה
                                encrypt_img_attached.setImageBitmap(selectedImage);
                                Log.d(TAG, "Image selected successfully");
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading selected image", e);
                                showError();
                            }
                        } else {
                            Log.d(TAG, "No image selected or result canceled");
                            showError();
                        }
                    }
                }
        );
    }

    private void showError() {
        Toast.makeText(activity, "No image selected!", Toast.LENGTH_LONG).show();
        encrypt_img_attached.setImageResource(R.drawable.attach_image);
        Log.d(TAG, "showError: Reset image to default resource");
    }

    private void saveImageToGallery(Bitmap bitmap) {
        Log.d(TAG, "saveImageToGallery: Attempting to save image to gallery");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "YourAppName");

        ContentResolver contentResolver = activity.getContentResolver();
        Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try (OutputStream outputStream = contentResolver.openOutputStream(uri)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            contentResolver.update(uri, values, null, null);
            MediaScannerConnection.scanFile(activity, new String[]{uri.toString()}, null, null);
            Toast.makeText(activity, "Image saved successfully", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Image saved to gallery URI: " + uri.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save image", e);
            Toast.makeText(activity, "Failed to save image: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (uri != null) {
                contentResolver.delete(uri, null, null);
                Log.d(TAG, "Deleted the failed image entry from media store");
            }
            e.printStackTrace();
        }
    }

    public void clean() {
        // Reset all UI fields
        encrypt_img_attached.setImageResource(R.drawable.attach_image);
        encrypt_TXT_text.setText("");
        encrypt_TXT_password.setText("");
        Log.d(TAG, "clean: UI fields reset to default");
    }
}
