package org.akanework.gramophone.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.components.PlaybackSpeedDialog

/** Full-screen surface only; playback stays owned by GramophonePlaybackService. */
class VideoPlayerActivity : AppCompatActivity() {
    private val controllerViewModel: MediaControllerViewModel by viewModels()
    private lateinit var playerView: PlayerView
    private lateinit var preferences: SharedPreferences
    private lateinit var speedButton: android.view.View
    private val playerListener = object : Player.Listener {
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            updateSpeedLabel(playbackParameters)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_video_player)
        lifecycle.addObserver(controllerViewModel)
        playerView = findViewById(R.id.video_player)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        speedButton = findViewById(R.id.video_speed)
        updateSpeedLabel(PlaybackParameters.DEFAULT)
        findViewById<android.view.View>(R.id.video_back).setOnClickListener { finish() }
        speedButton.setOnClickListener {
            controllerViewModel.get()?.let { player ->
                PlaybackSpeedDialog.show(this, player, preferences)
            }
        }
        controllerViewModel.addRecreationalPlayerListener(lifecycle, playerListener) { controller ->
            playerView.player = controller
            updateSpeedLabel(controller.playbackParameters)
        }
        WindowInsetsControllerCompat(window, playerView).hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onStop() {
        // A video decoder can only render to one Surface. Releasing this output before the
        // activity is backgrounded keeps the service-owned audio renderer alive for lock screen.
        playerView.player = null
        super.onStop()
    }

    private fun updateSpeedLabel(playbackParameters: PlaybackParameters) {
        (speedButton as? android.widget.TextView)?.text = getString(
            R.string.video_playback_speed_short, playbackParameters.speed
        )
    }
}
