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

package com.wtb.dashTracker.ui.welcome

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.twotone.AttachMoney
import androidx.compose.material.icons.twotone.DirectionsCar
import androidx.compose.material.icons.twotone.MoneyOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_welcome.theme.DashTrackerTheme

@ExperimentalMaterial3Api
@Composable
fun Welcome() {
    DashTrackerTheme {
        Surface(
            color = primaryFaded,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Card(
                        modifier = Modifier
                            .height(96.dp)
                            .align(CenterVertically)
                            .weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = primaryDark,
                    ) {
                        Text(
                            text = "Welcome to DashTracker",
                            modifier = Modifier
                                .padding(16.dp)
                                .wrapContentHeight(),
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(R.font.lalezar)),
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(96.dp)
                            .align(CenterVertically)
                    ) {
                        Logo()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HeaderText(
                    text = "Track your income",
                    icon = Icons.TwoTone.AttachMoney,
                    iconTint = up,
                ) {
                    Text(
                        "Look at me now",
                        fontFamily = FontFamily(Font(R.font.lalezar)),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                HeaderText(
                    text = "Track your expenses",
                    icon = Icons.TwoTone.MoneyOff,
                    iconTint = down,
                )

                Spacer(modifier = Modifier.height(4.dp))

                HeaderText(
                    text = "Track your mileage",
                    icon = Icons.TwoTone.DirectionsCar,
                    iconTint = c,
                )
            }
        }
    }
}

@Composable
fun HeaderText(
    text: String, icon: ImageVector, iconTint: Color, content: @Composable (() ->
    Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(true) { isExpanded = !isExpanded },
        shape = RoundedCornerShape(24.dp),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = text,
                    modifier = Modifier.align(CenterVertically),
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.lalezar)),
                    textAlign = TextAlign.Start,
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    icon,
                    contentDescription = "Drawing of a car",
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp),
                    tint = iconTint
                )
            }

            if (content != null) {
                AnimatedVisibility(visible = isExpanded) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        content()
                    }
                }

                Crossfade(
                    targetState = isExpanded,
                ) {
                    when (it) {
                        true -> Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Show less",
                            modifier = Modifier.fillMaxWidth()
                        )
                        false -> Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Show more",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Logo() {
    ResourcesCompat.getDrawable(
        /* res = */ LocalContext.current.resources,
        /* id = */ R.mipmap.icon_c,
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
            modifier = Modifier.requiredSize(96.dp),
            colorFilter = ColorFilter.tint(primary, BlendMode.DstIn)
        )
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun PreviewWelcome() {
    Welcome()
}
