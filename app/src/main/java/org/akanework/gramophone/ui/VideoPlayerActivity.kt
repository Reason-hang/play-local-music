package org.akanework.gramophone.ui

import android.os.Bundle
import android.view.WindowManager
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
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_video_player)
        lifecycle.addObserver(controllerViewModel)
        playerView = findViewById(R.id.video_player)
        findViewById<android.view.View>(R.id.video_back).setOnClickListener { finish() }
        controllerViewModel.addControllerCallback(lifecycle) { controller, _ -> playerView.player = controller }
        WindowInsetsControllerCompat(window, playerView).hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onStop() {
        // A video decoder can only render to one Surface. Releasing this output before the
        // activity is backgrounded keeps the service-owned audio renderer alive for lock screen.
        playerView.player = null
        super.onStop()
    }
}
