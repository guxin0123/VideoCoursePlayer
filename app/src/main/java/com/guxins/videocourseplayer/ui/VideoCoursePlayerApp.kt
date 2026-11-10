package com.guxins.videocourseplayer.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guxins.videocourseplayer.AppContainer
import com.guxins.videocourseplayer.data.media.MediaPermission
import com.guxins.videocourseplayer.data.model.VideoPermissionState
import com.guxins.videocourseplayer.ui.album.AlbumPickerScreen
import com.guxins.videocourseplayer.ui.main.MainScreen
import com.guxins.videocourseplayer.ui.permission.PermissionScreen
import kotlinx.coroutines.launch

@Composable
fun VideoCoursePlayerApp(
    container: AppContainer,
) {
    val viewModel: AppViewModel = viewModel(
        factory = remember(container) { AppViewModel.Factory(container) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val activity = remember(context) { context.findActivity() }
    var lastBackPressAt by remember { mutableLongStateOf(0L) }

    BackHandler {
        val now = SystemClock.elapsedRealtime()
        if (lastBackPressAt != 0L && now - lastBackPressAt <= 2_000L) {
            lastBackPressAt = 0L
            activity?.finish()
        } else {
            lastBackPressAt = now
            snackbarScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar("再按一次返回退出应用")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.onPermissionState(MediaPermission.current(context))
    }

    val openSettings = {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ),
        )
    }

    LaunchedEffect(Unit) {
        viewModel.onPermissionState(MediaPermission.current(context))
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onPermissionState(MediaPermission.current(context))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.pauseForBackground()
            }
        }
        processLifecycle.addObserver(observer)
        onDispose { processLifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.permission == VideoPermissionState.UNKNOWN || state.isBootstrapping -> {
                LoadingScreen()
            }

            !state.permission.isGranted -> {
                PermissionScreen(
                    onRequestPermission = {
                        permissionLauncher.launch(MediaPermission.requiredPermissions())
                    },
                    onOpenSettings = openSettings,
                )
            }

            state.selectedAlbum == null -> {
                AlbumPickerScreen(
                    state = state,
                    thumbnailLoader = container.thumbnailLoader,
                    onSelectAlbum = viewModel::selectAlbum,
                    onRefresh = viewModel::refresh,
                    onOpenSettings = openSettings,
                )
            }

            else -> {
                MainScreen(
                    state = state,
                    thumbnailLoader = container.thumbnailLoader,
                    player = viewModel.player,
                    onPlay = viewModel::play,
                    onSetFilter = viewModel::setFilter,
                    onSetPlayMode = viewModel::setPlayMode,
                    onSetTags = viewModel::setVideoTags,
                    onRefresh = viewModel::refresh,
                    onChangeAlbum = viewModel::changeAlbum,
                    onOpenSettings = openSettings,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (state.selectedAlbum != null) 84.dp else 16.dp,
                ),
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
