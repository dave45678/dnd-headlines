package com.davenotdavid.dndheadlines

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

/**
 * Custom splash screen that's displayed only during the initial runtime thread.
 */
class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainActIntent = Intent(this, ArticleActivity::class.java)
        startActivity(mainActIntent)
        finish()
    }
}
