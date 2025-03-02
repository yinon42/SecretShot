package com.example.secretshot;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // Tag for logging
    private static final String TAG = "MainActivity";

    private final int GALLERY_ACCESS = 100;

    private BottomNavigationView bottomNavigationView;
    private EncryptFragment encryptFragment;
    private DecryptFragment decryptFragment;
    private FrameLayout encryptFrame, decryptFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: MainActivity started");
        findViews();
        initViews();
        requestPermissions();


        // Handle bottom navigation item selection
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Log.d(TAG, "onNavigationItemSelected: " + item.getTitle());
                if (item.getItemId() == R.id.action_encrypt) {
                    // Show Encrypt fragment
                    decryptFrame.setVisibility(View.INVISIBLE);
                    encryptFrame.setVisibility(View.VISIBLE);
                    encryptFragment.clean();
                    Log.d(TAG, "Encrypt screen displayed");
                    return true;
                } else if (item.getItemId() == R.id.action_decrypt) {
                    // Show Decrypt fragment
                    decryptFrame.setVisibility(View.VISIBLE);
                    encryptFrame.setVisibility(View.INVISIBLE);
                    decryptFragment.clean();
                    Log.d(TAG, "Decrypt screen displayed");
                    return true;
                }
                return false;
            }
        });

    }

    /**
     * Checks and requests permissions for reading media images and external storage
     */
    private void requestPermissions() {
        Log.d(TAG, "requestPermissions: Checking gallery permissions");
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "requestPermissions: Permissions not granted, requesting now");
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_MEDIA_IMAGES},
                    GALLERY_ACCESS);
        }
    }

    /**
     * Callback for handling the user's response to the permission requests
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GALLERY_ACCESS) {
            // Check if the permissions were granted
            if (!(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {

                Log.d(TAG, "onRequestPermissionsResult: Permissions denied, requesting again");
                requestPermissions();
            }else{
                Log.d(TAG, "onRequestPermissionsResult: Permissions granted");
            }
        }

    }

    /**
     * Initializes fragments and sets the default screen to EncryptFragment
     */
    private void initViews() {
        Log.d(TAG, "initViews: Initializing fragments");

        encryptFragment = new EncryptFragment(this);
        decryptFragment = new DecryptFragment(this);

        // Add fragments to their respective containers
        getSupportFragmentManager().beginTransaction().add(R.id.frame_encrypt, encryptFragment).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.frame_decrypt, decryptFragment).commit();

        // Show encrypt frame by default
        decryptFrame.setVisibility(View.INVISIBLE);
        encryptFrame.setVisibility(View.VISIBLE);

        Log.d(TAG, "initViews: Encrypt fragment is set as default");
    }

    /**
     * Finds all views from the layout
     */
    private void findViews() {
        Log.d(TAG, "findViews: Finding views by ID");
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        encryptFrame = findViewById(R.id.frame_encrypt);
        decryptFrame = findViewById(R.id.frame_decrypt);

    }
}