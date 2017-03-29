package com.davenotdavid.dndheadlines;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Custom splash screen that's displayed only during the initial runtime thread.
 */
public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent mainActIntent = new Intent(this, MainActivity.class);
        startActivity(mainActIntent);
        finish();
    }
}
