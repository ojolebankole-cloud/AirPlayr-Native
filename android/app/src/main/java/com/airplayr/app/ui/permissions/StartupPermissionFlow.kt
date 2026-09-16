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
 * Ordered Android permission flow used after the app has rendered its Home screen:
 * Nearby -> Notifications -> Music -> Photo & Video.
 *
 * A granted permission is never requested again. A denied permission is not treated as
 * complete; the flow moves on for the current session, and the denied permission is
 * discovered and requested again the next time the app enters this flow.
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

    // Request exactly one Android permission at a time. Some Android versions can keep a
    // multi-permission request alive long enough that launching the next request immediately
    // from its callback is ignored. A single-permission launcher plus a short hand-off delay
    // keeps the four-step startup sequence deterministic.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
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
            Step.NEARBY -> {
                if (Build.VERSION.SDK_INT >= 33) {
                    kotlinx.coroutines.delay(150L)
                    launcher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                } else {
                    step = Step.NOTIFICATIONS
                }
            }

            Step.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= 33) {
                    kotlinx.coroutines.delay(150L)
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    step = Step.AUDIO
                }
            }

            Step.AUDIO -> {
                kotlinx.coroutines.delay(150L)
                launcher.launch(
                    if (Build.VERSION.SDK_INT >= 33)
                        Manifest.permission.READ_MEDIA_AUDIO
                    else
                        Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }

            Step.VIDEO -> {
                kotlinx.coroutines.delay(150L)
                if (Build.VERSION.SDK_INT >= 34) {
                    // Android 14+ presents the system's Photos and videos access choice
                    // when the normal video permission is requested.
                    launcher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                } else if (Build.VERSION.SDK_INT >= 33) {
                    launcher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                } else {
                    launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }

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
