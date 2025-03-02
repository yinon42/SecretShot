package com.example.secretshot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.util.Log;


public class SplashActivity extends Activity {

    // Tag used for logging
    private static final String TAG = "SplashActivity";

    // Duration to display the splash screen (2.5 seconds)
    private static final int SPLASH_DURATION = 2500; // זמן תצוגת מסך הפתיחה (2.5 שניות)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Initializing SplashActivity");

        // Enable full-screen mode, no title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.splash_screen);
        Log.d(TAG, "onCreate: Layout set for splash screen");

        // Play fade-in animation on splash image
        ImageView splashImage = findViewById(R.id.splash_image);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        splashImage.startAnimation(fadeIn);
        Log.d(TAG, "onCreate: Fade-in animation started");

        // Transition to MainActivity after the animation ends
        new Handler().postDelayed(() -> {
            Log.d(TAG, "Handler: Splash duration ended, moving to MainActivity");
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);

            // Apply fade in/out transitions
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out); // מעברים רכים
            finish();
        }, SPLASH_DURATION);
    }
}
