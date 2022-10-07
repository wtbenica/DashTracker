package com.wtb.dashTracker.ui.activity_welcome.ui.composables

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RowScope.FillSpacer() {
    Spacer(modifier = Modifier.weight(1f, true))
}

@Composable
fun ColumnScope.FillSpacer() {
    Spacer(modifier = Modifier.weight(1f, true))
}

@Composable
fun DefaultSpacer() {
    Spacer(modifier = Modifier.size(16.dp))
}

@Composable
fun HalfSpacer() {
    Spacer(modifier = Modifier.size(8.dp))
}

@Composable
fun QuarterSpacer() {
    Spacer(modifier = Modifier.size(4.dp))
}

@Composable
fun NarrowSpacer() {
    Spacer(modifier = Modifier.size(2.dp))
}

