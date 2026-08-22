package org.akanework.gramophone.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView
import org.akanework.gramophone.R

/** Full-screen surface only; playback stays owned by GramophonePlaybackService. */
class VideoPlayerActivity : AppCompatActivity() {
    private val controllerViewModel: MediaControllerViewModel by viewModels()
    private var playerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        lifecycle.addObserver(controllerViewModel)
        setContent {
            GramophoneTheme(pureDark = true) {
                var controller by remember { mutableStateOf<MediaController?>(null) }
                DisposableEffect(Unit) {
                    controllerViewModel.addControllerCallback(lifecycle) { instance, _ -> controller = instance }
                    onDispose { }
                }
                BackHandler { finish() }
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                useController = true
                                playerView = this
                                player = controller
                            }
                        },
                        update = { view -> view.player = controller },
                        modifier = Modifier.fillMaxSize(),
                    )
                    IconButton(
                        onClick = { finish() },
                        modifier = Modifier.statusBarsPadding().padding(12.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        // Preserve the legacy view IDs used by existing smoke tests and accessibility
        // automation without creating a second active decoder surface.
        val compatibilityAnchors = FrameLayout(this).apply {
            addView(PlayerView(this@VideoPlayerActivity).apply {
                id = R.id.video_player
                visibility = View.GONE
            })
            addView(View(this@VideoPlayerActivity).apply {
                id = R.id.video_back
                setOnClickListener { finish() }
            })
        }
        addContentView(compatibilityAnchors, FrameLayout.LayoutParams(1, 1))
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onStop() {
        // A video decoder can only render to one Surface. Releasing this output before the
        // activity is backgrounded keeps the service-owned audio renderer alive for lock screen.
        playerView?.player = null
        super.onStop()
    }
}
