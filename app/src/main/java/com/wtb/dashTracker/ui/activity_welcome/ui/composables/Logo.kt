/*
 * Copyright 2022 Wesley T. Benica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wtb.dashTracker.ui.activity_welcome.ui.composables

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.wtb.dashTracker.R

@Composable
fun Logo(darkMode: Boolean = isSystemInDarkTheme()) {
    ResourcesCompat.getDrawable(
        /* res = */ LocalContext.current.resources,
        /* id = */ if (darkMode) {
            R.drawable.launch_icon_foreground_full_night
        } else {
            R.drawable.launch_icon_foreground_full
        },
        /* theme = */ LocalContext.current.theme
    )?.let { drawable ->
        val bitmap = Bitmap.createBitmap(
            /* width = */ drawable.intrinsicWidth,
            /* height = */ drawable.intrinsicHeight,
            /* config = */ Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "DashTracker logo",
            modifier = Modifier
                .requiredSize(96.dp)
                .padding(8.dp),
        )
    }
}

@ExperimentalTextApi
@ExperimentalMaterial3Api
@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PreviewLogo() {
    Surface {
        Logo()
    }
}