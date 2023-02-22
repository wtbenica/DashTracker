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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.ui.theme.DashTrackerTheme
import com.wtb.dashTracker.ui.theme.FontFamilyOswald
import com.wtb.dashTracker.ui.theme.onPrimary
import com.wtb.dashTracker.ui.theme.onPrimaryVariant

@OptIn(ExperimentalTextApi::class)
@Composable
fun RowScope.HeaderText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    padding: Dp = 16.dp
) {
    Text(
        text = text,
        modifier = modifier
            .padding(padding)
            .weight(weight = 1f, fill = true)
            .align(CenterVertically),
        color = onPrimaryVariant(),
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
    textAlign: TextAlign = TextAlign.End,
    padding: Dp = 16.dp
) {
    Text(
        text = text,
        modifier = modifier
            .padding(all = padding)
            .weight(weight = 1f, fill = true),
        color = onPrimary(),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamilyOswald,
        textAlign = textAlign,
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun ColumnScope.HeaderText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    padding: Dp = 16.dp
) {
    Text(
        text = text,
        modifier = modifier
            .padding(padding)
            .width(IntrinsicSize.Max),
        color = onPrimaryVariant(),
        fontSize = 14.sp,
        fontFamily = FontFamilyOswald,
        textAlign = textAlign
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun ColumnScope.ValueText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.End,
    padding: Dp? = null,
    paddingTop: Dp = 16.dp,
    paddingBottom: Dp = 16.dp,
    paddingStart: Dp = 16.dp,
    paddingEnd: Dp = 16.dp
) {
    Text(
        text = text,
        modifier = modifier.apply {
            width(IntrinsicSize.Max)
            if (padding != null)
                padding(all = padding)
            else
                padding(start = paddingStart, top = paddingTop, end = paddingEnd, bottom = paddingBottom)
        },
        color = onPrimary(),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamilyOswald,
        textAlign = textAlign
    )
}

@Preview
@Composable
fun TextViews() {
    Card {
        Row {
            HeaderText(text = "Header")
            ValueText(text = "Value")
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun TextViewsNight() {
    DashTrackerTheme {
        Card {
            Row {
                HeaderText(text = "Header")
                ValueText(text = "Value")
            }
        }
    }
}

