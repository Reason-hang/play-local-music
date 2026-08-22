package org.akanework.gramophone.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import org.akanework.gramophone.logic.enableEdgeToEdgeProperly
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.BuildConfig

private val localLightColorScheme = lightColorScheme(
    primary = Color(0xFFFDB833),
    onPrimary = Color(0xFF2A1A00),
    primaryContainer = Color(0xFFFFE2A0),
    onPrimaryContainer = Color(0xFF241900),
    secondary = Color(0xFF7A5A1D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE9B8),
    onSecondaryContainer = Color(0xFF241900),
    tertiary = Color(0xFF6A5E37),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5E3B6),
    onTertiaryContainer = Color(0xFF211A06),
    background = Color(0xFFFFF9ED),
    onBackground = Color(0xFF1D1B16),
    surface = Color(0xFFFFF9ED),
    onSurface = Color(0xFF1D1B16),
    surfaceVariant = Color(0xFFF1E5CC),
    onSurfaceVariant = Color(0xFF51483A),
    outline = Color(0xFF817563),
    outlineVariant = Color(0xFFD4C6AE),
    inverseSurface = Color(0xFF342F27),
    inverseOnSurface = Color(0xFFF9F0E0),
    inversePrimary = Color(0xFFFFD77A),
)

private val localDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD77A),
    onPrimary = Color(0xFF3A2800),
    primaryContainer = Color(0xFF8A5F00),
    onPrimaryContainer = Color(0xFFFFE2A0),
    secondary = Color(0xFFE8C985),
    onSecondary = Color(0xFF3A2F13),
    secondaryContainer = Color(0xFF594819),
    onSecondaryContainer = Color(0xFFFFE9B8),
    tertiary = Color(0xFFD8C58D),
    onTertiary = Color(0xFF372F14),
    tertiaryContainer = Color(0xFF50451F),
    onTertiaryContainer = Color(0xFFF5E3B6),
    background = Color(0xFF1D1B16),
    onBackground = Color(0xFFF3E8D0),
    surface = Color(0xFF1D1B16),
    onSurface = Color(0xFFF3E8D0),
    surfaceVariant = Color(0xFF51483A),
    onSurfaceVariant = Color(0xFFD4C6AE),
    outline = Color(0xFF9B8D75),
    outlineVariant = Color(0xFF51483A),
    inverseSurface = Color(0xFFF3E8D0),
    inverseOnSurface = Color(0xFF342F27),
    inversePrimary = Color(0xFF8A5F00),
)

private val courseLightColorScheme = lightColorScheme(
    primary = Color(0xFFFF4E00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCD),
    onPrimaryContainer = Color(0xFF351000),
    secondary = Color(0xFF77574B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCD),
    onSecondaryContainer = Color(0xFF2C1510),
    tertiary = Color(0xFF6C5E2E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E4A4),
    onTertiaryContainer = Color(0xFF211B00),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF231A17),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF231A17),
    surfaceVariant = Color(0xFFF5DED6),
    onSurfaceVariant = Color(0xFF54433D),
    outline = Color(0xFF85736D),
    outlineVariant = Color(0xFFD8C2BA),
    inverseSurface = Color(0xFF392E2A),
    inverseOnSurface = Color(0xFFFFEDE8),
    inversePrimary = Color(0xFFFFB695),
)

private val courseDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB695),
    onPrimary = Color(0xFF5E1800),
    primaryContainer = Color(0xFF8D2B00),
    onPrimaryContainer = Color(0xFFFFDBCD),
    secondary = Color(0xFFE7BDB0),
    onSecondary = Color(0xFF442A22),
    secondaryContainer = Color(0xFF5D4036),
    onSecondaryContainer = Color(0xFFFFDBCD),
    tertiary = Color(0xFFD6C887),
    onTertiary = Color(0xFF38310A),
    tertiaryContainer = Color(0xFF504719),
    onTertiaryContainer = Color(0xFFF3E4A4),
    background = Color(0xFF201310),
    onBackground = Color(0xFFFFEDE8),
    surface = Color(0xFF201310),
    onSurface = Color(0xFFFFEDE8),
    surfaceVariant = Color(0xFF53433D),
    onSurfaceVariant = Color(0xFFD8C2BA),
    outline = Color(0xFFA18C83),
    outlineVariant = Color(0xFF53433D),
    inverseSurface = Color(0xFFFFEDE8),
    inverseOnSurface = Color(0xFF392E2A),
    inversePrimary = Color(0xFFB33E00),
)

abstract class BaseComposeActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    val pureDarkFlow by lazy {
        callbackFlow {
            val cb = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == "pureDark") {
                    trySendBlocking(prefs.getBooleanStrict("pureDark", false))
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(cb)
            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(cb)
            }
        }.stateIn(
            lifecycleScope, WhileSubscribed(),
            prefs.getBooleanStrict("pureDark", false)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeProperly()
    }
}

@Composable
fun BaseComposeActivity.GramophoneTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val pureDark by pureDarkFlow.collectAsState()
    GramophoneTheme(useDarkTheme, pureDark, content)
}

@Composable
fun GramophoneTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    pureDark: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = (if (useDarkTheme) {
            (if (BuildConfig.IS_COURSE) courseDarkColorScheme else localDarkColorScheme).let {
                if (pureDark) {
                    it.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceVariant = Color.Black,
                        surfaceContainerLowest = Color.Black,
                        surfaceContainerLow = Color.Black,
                        surfaceContainer = Color.Black,
                        surfaceContainerHigh = Color.Black,
                        surfaceContainerHighest = Color.Black,
                    )
                } else it
            }
        } else {
            if (BuildConfig.IS_COURSE) courseLightColorScheme else localLightColorScheme
        }), content = {
            CompositionLocalProvider(
                LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
            ) {
                content()
            }
        }
    )
}
