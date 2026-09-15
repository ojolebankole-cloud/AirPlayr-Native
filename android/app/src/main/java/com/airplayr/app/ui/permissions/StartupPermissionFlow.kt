package com.airplayr.app.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

/**
 * Phase 1 startup permission orchestration.
 *
 * Parity with reference app:
 *  1) Nearby devices
 *  2) Notifications (Android 13+)
 *  3) Music library
 *  4) Video library
 *
 * The reference app does not render an extra in-app permission card while the
 * system dialogs are being shown, so this native implementation deliberately
 * keeps the UI transparent and lets Android own the permission presentation.
 */
private enum class Step { IDLE, NEARBY, NOTIFICATIONS, AUDIO, VIDEO, COMPLETE }

data class PermissionFlowState(
    val audio: Boolean = false,
    val video: Boolean = false,
    val nearby: Boolean = false,
    val notifications: Boolean = false,
    val attempted: Boolean = false,
)

private fun granted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun audioGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 33) granted(context, Manifest.permission.READ_MEDIA_AUDIO)
    else granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)

private fun videoGranted(context: Context): Boolean = when {
    Build.VERSION.SDK_INT >= 34 && granted(context, Manifest.permission.READ_MEDIA_VIDEO) -> true
    Build.VERSION.SDK_INT >= 34 && granted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> true
    Build.VERSION.SDK_INT >= 33 -> granted(context, Manifest.permission.READ_MEDIA_VIDEO)
    else -> granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun nearbyGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 || granted(context, Manifest.permission.NEARBY_WIFI_DEVICES)

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 || granted(context, Manifest.permission.POST_NOTIFICATIONS)

@Composable
fun StartupPermissionFlow(
    context: Context,
    onCompleted: (PermissionFlowState) -> Unit,
) {
    var step by remember { mutableStateOf(Step.IDLE) }
    var completed by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        step = when (step) {
            Step.NEARBY -> Step.NOTIFICATIONS
            Step.NOTIFICATIONS -> Step.AUDIO
            Step.AUDIO -> Step.VIDEO
            Step.VIDEO -> Step.COMPLETE
            Step.COMPLETE, Step.IDLE -> Step.COMPLETE
        }
    }

    LaunchedEffect(Unit) {
        step = when {
            Build.VERSION.SDK_INT >= 33 && !nearbyGranted(context) -> Step.NEARBY
            Build.VERSION.SDK_INT >= 33 && !notificationsGranted(context) -> Step.NOTIFICATIONS
            !audioGranted(context) -> Step.AUDIO
            !videoGranted(context) -> Step.VIDEO
            else -> Step.COMPLETE
        }
    }

    LaunchedEffect(step) {
        when (step) {
            Step.NEARBY -> if (Build.VERSION.SDK_INT >= 33) {
                launcher.launch(mapOf(Manifest.permission.NEARBY_WIFI_DEVICES to true))
            } else {
                step = Step.NOTIFICATIONS
            }

            Step.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= 33) {
                launcher.launch(mapOf(Manifest.permission.POST_NOTIFICATIONS to true))
            } else {
                step = Step.AUDIO
            }

            Step.AUDIO -> launcher.launch(
                if (Build.VERSION.SDK_INT >= 33)
                    mapOf(Manifest.permission.READ_MEDIA_AUDIO to true)
                else
                    mapOf(Manifest.permission.READ_EXTERNAL_STORAGE to true)
            )

            Step.VIDEO -> launcher.launch(
                when {
                    Build.VERSION.SDK_INT >= 34 -> mapOf(
                        Manifest.permission.READ_MEDIA_VIDEO to true,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED to true,
                    )
                    Build.VERSION.SDK_INT >= 33 -> mapOf(
                        Manifest.permission.READ_MEDIA_VIDEO to true,
                    )
                    else -> mapOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE to true,
                    )
                }
            )

            Step.COMPLETE -> if (!completed) {
                completed = true
                onCompleted(
                    PermissionFlowState(
                        audio = audioGranted(context),
                        video = videoGranted(context),
                        nearby = nearbyGranted(context),
                        notifications = notificationsGranted(context),
                        attempted = true,
                    )
                )
            }

            Step.IDLE -> Unit
        }
    }
}
