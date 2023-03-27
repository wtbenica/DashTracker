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

package com.wtb.dashTracker.ui.activity_get_permissions.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.twotone.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.theme.DashTrackerTheme

@Composable
internal fun PageIndicator(
    modifier: Modifier = Modifier,
    numPages: Int,
    selectedPage: Int = 0
) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onTertiary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .then(modifier)
        ) {
            for (i in 0 until numPages) {
                Icon(
                    if (i == selectedPage) Icons.Filled.Circle else Icons.TwoTone.Circle,
                    contentDescription = stringResource(
                        R.string.content_desc_page_indicator,
                        selectedPage + 1,
                        numPages
                    ),
                    modifier = Modifier.size(8.dp),
                    tint = MaterialTheme.colorScheme.inversePrimary
                )
            }
        }
    }
}

@ExperimentalTextApi
@Preview(showBackground = true)
@Composable
fun PageIndicatorPreview() {
    DashTrackerTheme {
        PageIndicator(numPages = 4, selectedPage = 1)
    }
}