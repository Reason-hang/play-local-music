package org.akanework.gramophone.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.diagnostics.DiagnosticStore
import org.akanework.gramophone.logic.getFile
import org.akanework.gramophone.logic.gramophoneApplication
import org.akanework.gramophone.logic.library.LocalLibraryManager
import org.akanework.gramophone.logic.setMediaItemsWithTitle
import org.akanework.gramophone.ui.fragments.settings.MainSettingsActivity
import java.text.DateFormat
import java.util.Date

private enum class VisualPage { HOME, PLAYER, MANAGEMENT, DIAGNOSTICS }

private object VisualCopy {
    const val homeEyebrow = "LOCAL MEDIA LIBRARY"
    const val homeTitle = "找到想听的，马上开始。"
    const val continueTitle = "把上次没听完的内容接着听。"
    const val continueBody = "首页只保留主标题、当前播放和下一步动作。"
    const val nowPlaying = "正在播放"
    const val allMedia = "全部媒体"
    const val compactHint = "三行标题，紧凑浏览。"
    const val playAll = "播放全部"
    const val player = "播放页"
    const val management = "媒体库整理"
    const val diagnostics = "诊断与日志"
    const val remove = "从媒体库移除"
    const val category = "加入分类"
    const val restore = "恢复已移除内容"
    const val categories = "管理分类"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainActivity.NativeVisualContent() {
    val activity = this
    var page by remember { mutableStateOf(VisualPage.HOME) }
    var controller by remember { mutableStateOf<MediaController?>(activity.getPlayer()) }
    var songs by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var libraryState by remember { mutableStateOf(LocalLibraryManager.LibraryState()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DisposableEffect(activity) {
        activity.controllerViewModel.addControllerCallback(activity.lifecycle) { instance, _ ->
            controller = instance
        }
        onDispose { }
    }
    LaunchedEffect(activity) {
        activity.reader.songListFlow.collectLatest { songs = it }
    }
    LaunchedEffect(activity) {
        activity.gramophoneApplication.localLibraryManager.state.collectLatest { libraryState = it }
    }
    BackHandler(enabled = page != VisualPage.HOME) { page = VisualPage.HOME }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets.navigationBars) {
                VisualNavItem(VisualPage.HOME, "媒体库", Icons.Rounded.Home, page) { page = it }
                VisualNavItem(VisualPage.PLAYER, "播放页", Icons.Rounded.PlayArrow, page) { page = it }
                VisualNavItem(VisualPage.MANAGEMENT, "整理", Icons.Rounded.Folder, page) { page = it }
                VisualNavItem(VisualPage.DIAGNOSTICS, "诊断", Icons.Rounded.BugReport, page) { page = it }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (page) {
                VisualPage.HOME -> VisualHome(
                    activity = activity,
                    songs = songs,
                    libraryState = libraryState,
                    controller = controller,
                    onPage = { page = it },
                    snackbar = snackbar,
                )
                VisualPage.PLAYER -> VisualPlayer(
                    activity = activity,
                    controller = controller,
                    onBack = { page = VisualPage.HOME },
                )
                VisualPage.MANAGEMENT -> VisualManagement(
                    activity = activity,
                    songs = songs,
                    libraryState = libraryState,
                    snackbar = snackbar,
                )
                VisualPage.DIAGNOSTICS -> VisualDiagnostics(activity, snackbar)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.VisualNavItem(
    target: VisualPage,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    page: VisualPage,
    onSelect: (VisualPage) -> Unit,
) {
    val selected = target == page
    Column(
        modifier = Modifier.weight(1f).clickable { onSelect(target) }.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, maxLines = 1, fontSize = 11.sp, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisualTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
            }
        },
        actions = actions,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisualHome(
    activity: MainActivity,
    songs: List<MediaItem>,
    libraryState: LocalLibraryManager.LibraryState,
    controller: MediaController?,
    onPage: (VisualPage) -> Unit,
    snackbar: SnackbarHostState,
) {
    var activeFilter by remember(libraryState.activeFilter) { mutableStateOf(libraryState.activeFilter) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val visibleSongs = songs.filterNot { activity.gramophoneApplication.localLibraryManager.isHidden(it) }
    val filteredSongs = visibleSongs.filter { item ->
        val matchesQuery = query.isBlank() || item.mediaMetadata.title?.contains(query, true) == true
        val matchesFilter = when {
            activeFilter == LocalLibraryManager.FILTER_MP3 -> item.getFile()?.extension.equals("mp3", true)
            activeFilter == LocalLibraryManager.FILTER_MP4 -> item.getFile()?.extension.equals("mp4", true)
            activeFilter.startsWith("category:") -> activity.gramophoneApplication.localLibraryManager.categoryKey(item) in
                libraryState.categories[activeFilter.removePrefix("category:")].orEmpty()
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        VisualTopBar(
            title = activity.getString(R.string.app_name),
            actions = {
                IconButton(onClick = { searchOpen = !searchOpen }) { Icon(Icons.Rounded.Search, "搜索") }
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, "更多") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("快速刷新") },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                            onClick = {
                                menuOpen = false
                                activity.updateLibrary(true) { scope.launch { snackbar.showSnackbar("媒体库已刷新") } }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("完整刷新") },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                            onClick = {
                                menuOpen = false
                                activity.updateLibrary(false) { scope.launch { snackbar.showSnackbar("媒体库已完成完整刷新") } }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("打开系统设置") },
                            leadingIcon = { Icon(Icons.Rounded.Settings, null) },
                            onClick = {
                                menuOpen = false
                                activity.startActivity(Intent(activity, MainSettingsActivity::class.java))
                            },
                        )
                    }
                }
            },
        )
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(VisualCopy.homeEyebrow, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
            Spacer(Modifier.height(6.dp))
            Text(VisualCopy.homeTitle, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Card(
                    modifier = Modifier.weight(1.25f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(VisualCopy.continueTitle, color = MaterialTheme.colorScheme.onPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(VisualCopy.continueBody, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .72f), fontSize = 13.sp)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { onPage(VisualPage.PLAYER) }) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("继续播放") }
                    }
                }
                Card(
                    modifier = Modifier.weight(.9f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(VisualCopy.nowPlaying, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        val current = controller?.currentMediaItem
                        MediaCover(current, Modifier.size(56.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(current?.mediaMetadata?.title?.toString() ?: "尚未开始播放", maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(onClick = { controller?.seekToPrevious() }) { Icon(Icons.Rounded.SkipPrevious, "上一首") }
                            IconButton(onClick = { controller?.playOrPause() }) { Icon(if (controller?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "播放") }
                            IconButton(onClick = { controller?.seekToNext() }) { Icon(Icons.Rounded.SkipNext, "下一首") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (searchOpen) {
                TextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("搜索媒体标题") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, trailingIcon = { IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "清除") } })
                Spacer(Modifier.height(10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(LocalLibraryManager.FILTER_ALL to "全部", LocalLibraryManager.FILTER_MP3 to "MP3", LocalLibraryManager.FILTER_MP4 to "MP4") + libraryState.categories.keys.map { LocalLibraryManager.categoryFilter(it) to it }
                filters.forEach { (value, label) ->
                    FilterChip(selected = activeFilter == value, onClick = { activeFilter = value; activity.gramophoneApplication.localLibraryManager.selectFilter(value) }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column { Text(VisualCopy.allMedia, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(VisualCopy.compactHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                TextButton(onClick = {
                    if (filteredSongs.isNotEmpty()) {
                        controller?.setMediaItemsWithTitle(filteredSongs, title = VisualCopy.allMedia)
                        controller?.prepare(); controller?.play(); onPage(VisualPage.PLAYER)
                    }
                }) { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null); Spacer(Modifier.width(4.dp)); Text(VisualCopy.playAll) }
            }
            Spacer(Modifier.height(6.dp))
            filteredSongs.take(80).forEachIndexed { index, item ->
                VisualMediaRow(item, controller?.currentMediaItem?.mediaId == item.mediaId, index, onClick = {
                    controller?.setMediaItemsWithTitle(listOf(item), title = VisualCopy.allMedia)
                    controller?.prepare(); controller?.play(); onPage(VisualPage.PLAYER)
                })
            }
            if (filteredSongs.isEmpty()) {
                Text("没有匹配的媒体", modifier = Modifier.padding(vertical = 40.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(18.dp))
            OutlinedButton(onClick = { onPage(VisualPage.MANAGEMENT) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Folder, null); Spacer(Modifier.width(6.dp)); Text("整理媒体库") }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun MediaCover(item: MediaItem?, modifier: Modifier = Modifier) {
    val model = item?.mediaMetadata?.artworkUri ?: R.drawable.ic_default_cover
    AsyncImage(model = model, contentDescription = "媒体封面", modifier = modifier.clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
}

@Composable
private fun VisualMediaRow(item: MediaItem, isPlaying: Boolean, index: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            MediaCover(item, Modifier.size(58.dp))
            Spacer(Modifier.width(14.dp))
            Text(item.mediaMetadata.title?.toString() ?: item.getFile()?.name ?: "未命名媒体", modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 16.sp, lineHeight = 21.sp)
            if (isPlaying) Icon(Icons.Rounded.PlayArrow, "正在播放", tint = MaterialTheme.colorScheme.primary)
            else Icon(Icons.Rounded.MoreVert, "更多操作")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisualPlayer(activity: MainActivity, controller: MediaController?, onBack: () -> Unit) {
    val current = controller?.currentMediaItem
    var position by remember { mutableLongStateOf(controller?.currentPosition ?: 0L) }
    var duration by remember { mutableLongStateOf((controller?.duration ?: 0L).coerceAtLeast(1L)) }
    var isPlaying by remember { mutableStateOf(controller?.isPlaying == true) }
    var speed by remember { mutableStateOf(controller?.playbackParameters?.speed ?: 1f) }
    var isScrubbing by remember { mutableStateOf(false) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingValue: Boolean) { isPlaying = isPlayingValue }
            override fun onPlaybackStateChanged(playbackState: Int) { duration = (controller?.duration ?: 0L).coerceAtLeast(1L) }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { position = controller?.currentPosition ?: 0L; duration = (controller?.duration ?: 0L).coerceAtLeast(1L) }
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) { speed = playbackParameters.speed }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }
    LaunchedEffect(controller, isPlaying, isScrubbing) {
        while (controller != null) {
            if (isPlaying && !isScrubbing) position = controller.currentPosition
            duration = controller.duration.coerceAtLeast(1L)
            delay(500)
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        VisualTopBar(VisualCopy.player, onBack)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF17191D)), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Box(Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF272A2F)).clickable { activity.startActivity(Intent(activity, VideoPlayerActivity::class.java)) }, contentAlignment = Alignment.Center) {
                        MediaCover(current, Modifier.fillMaxSize())
                        Surface(color = Color.Black.copy(alpha = .48f), shape = CircleShape) { Icon(Icons.Rounded.Fullscreen, "进入全屏视频播放", modifier = Modifier.padding(14.dp), tint = Color.White) }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(current?.mediaMetadata?.title?.toString() ?: "尚未选择媒体", color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis, fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Slider(value = position.coerceIn(0L, duration).toFloat(), onValueChange = { isScrubbing = true; position = it.toLong() }, onValueChangeFinished = { controller?.seekTo(position); isScrubbing = false }, valueRange = 0f..duration.toFloat(), modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatTime(position), color = Color(0xFFB8BCC3), fontSize = 12.sp); Text(formatTime(duration), color = Color(0xFFB8BCC3), fontSize = 12.sp) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { controller?.seekToPrevious() }) { Icon(Icons.Rounded.SkipPrevious, "上一首", tint = Color.White, modifier = Modifier.size(34.dp)) }
                        IconButton(onClick = { controller?.playOrPause() }, modifier = Modifier.padding(horizontal = 20.dp).size(68.dp)) { Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape) { Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "播放", modifier = Modifier.padding(20.dp), tint = MaterialTheme.colorScheme.onPrimary) } }
                        IconButton(onClick = { controller?.seekToNext() }) { Icon(Icons.Rounded.SkipNext, "下一首", tint = Color.White, modifier = Modifier.size(34.dp)) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.Speed, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("播放速度", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(.75f, 1f, 1.3f, 1.5f, 1.7f, 2f).forEach { preset ->
                            FilterChip(selected = kotlin.math.abs(speed - preset) < .01f, onClick = { controller?.let { it.playbackParameters = PlaybackParameters(preset, it.playbackParameters.pitch) } }, label = { Text("${preset}x") })
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("播放状态", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("独立视频进度已保存", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text("锁屏后继续播放声音", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    Text("上一首和下一首遵循队列语义", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisualManagement(
    activity: MainActivity,
    songs: List<MediaItem>,
    libraryState: LocalLibraryManager.LibraryState,
    snackbar: SnackbarHostState,
) {
    var sheet by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var categoryDialog by remember { mutableStateOf(false) }
    var categoryName by remember { mutableStateOf("") }
    val manager = activity.gramophoneApplication.localLibraryManager
    val visible = songs.filterNot(manager::isHidden)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        VisualTopBar(VisualCopy.management)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("所有操作只影响应用媒体库，不会删除手机文件。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            ManagementCard("从媒体库移除", "隐藏不想看到的媒体；源文件保留在手机中。", Icons.Rounded.DeleteSweep) { selected = emptySet(); sheet = "remove" }
            ManagementCard("加入分类", "将媒体加入课程、跑步或自定义分类。", Icons.Rounded.Add) { selected = emptySet(); sheet = "category" }
            ManagementCard("恢复已移除内容", "逐项恢复历史隐藏记录，不修改手机文件。", Icons.Rounded.Archive) { selected = emptySet(); sheet = "restore" }
            ManagementCard("管理分类", "删除分类不会删除媒体文件。", Icons.Rounded.Folder) { categoryDialog = true }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (sheet != null) {
        val mode = sheet!!
        val restoreItems = libraryState.hiddenRecords.entries.toList()
        val selectionItems: List<Pair<String, String>> = if (mode == "restore") {
            restoreItems.map { record -> record.key to record.value.title }
        } else {
            visible.map { media -> media.mediaId to (media.mediaMetadata.title?.toString() ?: media.getFile()?.name ?: "未命名媒体") }
        }
        ModalBottomSheet(onDismissRequest = { sheet = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.padding(horizontal = 20.dp).windowInsetsPadding(WindowInsets.safeDrawing)) {
                Text(if (mode == "remove") "勾选要移出媒体库的媒体" else if (mode == "category") "勾选要加入分类的媒体" else "勾选要恢复的媒体", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.height(420.dp)) {
                    items(selectionItems, key = { it.first }) { (id, title) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selected = if (id in selected) selected - id else selected + id }.padding(vertical = 8.dp)) {
                            Checkbox(checked = id in selected, onCheckedChange = { checked -> selected = if (checked) selected + id else selected - id })
                            Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        HorizontalDivider()
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { sheet = null }) { Text("取消") }
                    Button(onClick = {
                        when (mode) {
                            "remove" -> manager.hide(visible.filter { it.mediaId in selected })
                            "restore" -> manager.restore(selected)
                            "category" -> categoryDialog = true
                        }
                        if (mode != "category") {
                            sheet = null
                            selected = emptySet()
                            snackbar.currentSnackbarData?.dismiss()
                        }
                    }) { Text(if (mode == "remove") "移出" else if (mode == "restore") "恢复" else "下一步") }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    if (categoryDialog) {
        AlertDialog(
            onDismissRequest = { categoryDialog = false },
            title = { Text("加入分类") },
            text = {
                Column {
                    if (libraryState.categories.isNotEmpty()) {
                        libraryState.categories.keys.forEach { name ->
                            TextButton(onClick = {
                                manager.addToCategory(name, visible.filter { it.mediaId in selected })
                                categoryDialog = false; sheet = null; selected = emptySet()
                            }) { Text(name) }
                        }
                    }
                    TextField(value = categoryName, onValueChange = { categoryName = it }, label = { Text("新建分类") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(onClick = {
                if (categoryName.isNotBlank()) manager.addToCategory(categoryName, visible.filter { it.mediaId in selected })
                categoryName = ""; categoryDialog = false; sheet = null; selected = emptySet()
            }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { categoryDialog = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ManagementCard(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable(onClick = onClick), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) { Icon(icon, null, modifier = Modifier.padding(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.height(3.dp)); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VisualDiagnostics(activity: MainActivity, snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var crashRecords by remember { mutableStateOf(emptyList<DiagnosticStore.CrashRecord>()) }
    LaunchedEffect(activity) {
        crashRecords = withContext(Dispatchers.IO) { DiagnosticStore.crashRecords(activity.applicationContext) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        VisualTopBar(VisualCopy.diagnostics)
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("诊断资料保存在应用私有目录，不会自动联网发送。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            DiagnosticCard("播放器状态", "MediaSession 与后台播放服务由现有内核维护。", Icons.Rounded.PlayArrow)
            DiagnosticCard("媒体扫描", "MP3、MP4 和 AAC 音轨沿用现有扫描器。", Icons.Rounded.Refresh)
            DiagnosticCard("崩溃记录", if (crashRecords.isEmpty()) "暂无崩溃记录" else "最近 ${crashRecords.size} 条记录可查看", Icons.Rounded.BugReport)
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    val text = DiagnosticStore.copySummary(activity.applicationContext)
                    withContext(Dispatchers.Main) {
                        activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("本地听歌诊断摘要", text))
                        snackbar.showSnackbar("诊断摘要已复制")
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("一键复制诊断摘要") }
            OutlinedButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    runCatching { DiagnosticStore.export(activity.applicationContext) }.onSuccess { file ->
                        withContext(Dispatchers.Main) {
                            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileProvider", file)
                            activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "application/zip"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "导出完整诊断包"))
                        }
                    }.onFailure { withContext(Dispatchers.Main) { snackbar.showSnackbar("诊断包导出失败") } }
                }
            }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Archive, null); Spacer(Modifier.width(8.dp)); Text("导出完整诊断包") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiagnosticCard(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } }
    }
}

private fun Player.playOrPause() {
    if (isPlaying) pause() else play()
}
