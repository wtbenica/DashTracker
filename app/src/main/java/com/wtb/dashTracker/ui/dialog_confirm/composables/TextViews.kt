/*
 * Copyright 2023 Wesley T. Benica
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

package com.wtb.dashTracker.ui.dialog_confirm.composables

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.FontFamilyOswald

@OptIn(ExperimentalTextApi::class)
@Composable
fun RowScope.HeaderText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = modifier
            .padding(16.dp)
            .weight(weight = 1f, fill = true),
        fontSize = 14.sp,
        fontFamily = FontFamilyOswald,
        textAlign = textAlign
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun RowScope.ValueText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.End
) {
    Text(
        text = text,
        modifier = modifier
            .padding(all = 16.dp)
            .weight(weight = 1f, fill = true),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamilyOswald,
        textAlign = textAlign
    )
}

@Preview
@Composable
fun TextViews() {
    Row {
        HeaderText(text = "Header")
        ValueText(text = "Value")
    }
}

@OptIn(ExperimentalTextApi::class)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun TextViewsNight() {
    DashTrackerTheme() {
        Card() {
            Row {
                HeaderText(text = "Header")
                ValueText(text = "Value")
            }
        }
    }
}

