package com.example.safesafar.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.safesafar.R
import com.google.android.material.button.MaterialButton
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var youTubePlayerView: YouTubePlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        supportActionBar?.hide()

        val videoId = intent.getStringExtra("VIDEO_ID") ?: return
        val videoTitle = intent.getStringExtra("VIDEO_TITLE") ?: ""

        val tvTitle = findViewById<TextView>(R.id.tvPlayerTitle)
        val btnOpenYouTube = findViewById<MaterialButton>(R.id.btnOpenYouTube)
        youTubePlayerView = findViewById(R.id.youtubePlayerView)

        tvTitle.text = videoTitle

        // Tie the player lifecycle to the Activity lifecycle
        lifecycle.addObserver(youTubePlayerView)

        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                // Load video at position 0 seconds — works for both full videos and shorts IDs
                youTubePlayer.loadVideo(videoId, 0f)
            }

            override fun onError(
                youTubePlayer: YouTubePlayer,
                error: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError
            ) {
                // Fallback: show button to open in YouTube app
                btnOpenYouTube.visibility = View.VISIBLE
                btnOpenYouTube.setOnClickListener {
                    openInYouTubeApp(videoId)
                }
            }
        })

        // Also expose fallback button always (user preference)
        btnOpenYouTube.visibility = View.VISIBLE
        btnOpenYouTube.setOnClickListener { openInYouTubeApp(videoId) }
    }

    private fun openInYouTubeApp(videoId: String) {
        val youtubeIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/watch?v=$videoId")
        )
        startActivity(youtubeIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        youTubePlayerView.release()
    }
}
