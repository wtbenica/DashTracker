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

package com.wtb.dashTracker.welcome.ui.composables

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ThumbUpAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.ui.activity_main.MainActivity
import com.wtb.dashTracker.welcome.ui.theme.DashTrackerTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalTextApi
@ExperimentalMaterial3Api
@Composable
fun WhatsNew(modifier: Modifier = Modifier) {
    DashTrackerTheme {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            DefaultSpacer()

            Row(
                verticalAlignment = CenterVertically,
            ) {
                Text(
                    text = "What's New",
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentHeight()
                        .weight(1f),
                    fontSize = 20.sp,
                )

                DefaultSpacer()

                OutlinedCard(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(96.dp)
                        .width(96.dp)
                        .align(CenterVertically),
                ) {
                    Logo()
                }
            }

            DefaultSpacer()

            ListRow(text = "Automatic mileage tracking")
            ListRow(text = "Simplified start/end dash dialogs")
        }
    }
}


@ExperimentalCoroutinesApi
@Composable
fun WhatsNewNav() {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        val context = LocalContext.current
        FillSpacer()

        CustomTextButton(
            onClick = {
                context.startActivity(Intent(context, MainActivity::class.java))
            },
        ) {
            Text("Opt-out")
        }

        HalfSpacer()

        CustomButton(
            onClick = {
                context.startActivity(Intent(context, MainActivity::class.java))
            },
        ) {
            Text("I'm in!")

            HalfSpacer()

            Icon(
                Icons.Rounded.ThumbUpAlt,
                contentDescription = "Next screen"
            )
        }
    }
}

@ExperimentalTextApi
@ExperimentalCoroutinesApi
@ExperimentalMaterial3Api
@Preview
@Composable
fun PreviewWhatsNew() {
    DashTrackerTheme {
        Column {
            WhatsNew(modifier = Modifier.weight(1f))
            WhatsNewNav()
        }
    }
}