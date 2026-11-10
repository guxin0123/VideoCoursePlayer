package com.guxins.videocourseplayer.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.guxins.videocourseplayer.data.model.VideoPermissionState

object MediaPermission {
    fun current(context: Context): VideoPermissionState {
        val hasFullAccess = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                context.isGranted(Manifest.permission.READ_MEDIA_VIDEO)

            else -> context.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (hasFullAccess) return VideoPermissionState.FULL

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            context.isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        ) {
            return VideoPermissionState.LIMITED
        }

        return VideoPermissionState.DENIED
    }

    fun requiredPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
        )

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun Context.isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
