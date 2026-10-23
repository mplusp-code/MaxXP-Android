package com.relay.app.ui.navigation

import android.app.Activity
import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

sealed interface DevicePosture {
    data object Normal : DevicePosture
    data class TableTop(val hingeBounds: Rect) : DevicePosture
    data class BookPosture(val hingeBounds: Rect) : DevicePosture
    data class Separating(val hingeBounds: Rect, val orientation: FoldingFeature.Orientation) : DevicePosture
}

@Composable
fun rememberDevicePosture(activity: Activity): State<DevicePosture> {
    return produceState<DevicePosture>(initialValue = DevicePosture.Normal, key1 = activity) {
        WindowInfoTracker.getOrCreate(activity)
            .windowLayoutInfo(activity)
            .collect { layoutInfo ->
                val foldingFeature = layoutInfo.displayFeatures
                    .filterIsInstance<FoldingFeature>()
                    .firstOrNull()

                value = when {
                    foldingFeature == null -> DevicePosture.Normal
                    foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                        foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL ->
                        DevicePosture.TableTop(foldingFeature.bounds)
                    foldingFeature.state == FoldingFeature.State.HALF_OPENED &&
                        foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL ->
                        DevicePosture.BookPosture(foldingFeature.bounds)
                    foldingFeature.isSeparating ->
                        DevicePosture.Separating(foldingFeature.bounds, foldingFeature.orientation)
                    else -> DevicePosture.Normal
                }
            }
    }
}
