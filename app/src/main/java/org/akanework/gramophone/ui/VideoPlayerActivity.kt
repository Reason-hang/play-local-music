package org.akanework.gramophone.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.PlayerView
import org.akanework.gramophone.R

/** Full-screen surface only; playback stays owned by GramophonePlaybackService. */
class VideoPlayerActivity : AppCompatActivity() {
    private val controllerViewModel: MediaControllerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_video_player)
        lifecycle.addObserver(controllerViewModel)
        val playerView = findViewById<PlayerView>(R.id.video_player)
        controllerViewModel.addControllerCallback(lifecycle) { controller, _ -> playerView.player = controller }
        WindowInsetsControllerCompat(window, playerView).hide(WindowInsetsCompat.Type.systemBars())
    }
}
