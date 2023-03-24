package com.wtb.dashTracker.ui.activity_welcome.ui.composables

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.wtb.dashTracker.R


@Composable
fun marginDefault() = dimensionResource(id = R.dimen.margin_default)

@Composable
fun marginHalf() = dimensionResource(id = R.dimen.margin_half)
@Composable
fun marginNarrow() = dimensionResource(id = R.dimen.margin_narrow)
@Composable
fun marginSkinny() = dimensionResource(id = R.dimen.margin_skinny)

@Composable
fun RowScope.FillSpacer() {
    Spacer(modifier = Modifier.weight(1f, true))
}

@Composable
fun ColumnScope.FillSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.weight(1f, true))
}

@Composable
fun DefaultSpacer() {
    Spacer(modifier = Modifier.size(marginDefault()))
}

@Composable
fun WideSpacer() {
    Spacer(modifier = Modifier.size(24.dp))
}

@Composable
fun HalfSpacer() {
    Spacer(modifier = Modifier.size(marginHalf()))
}

@Composable
fun QuarterSpacer() {
    Spacer(modifier = Modifier.size(marginNarrow()))
}

@Composable
fun NarrowSpacer() {
    Spacer(modifier = Modifier.size(marginSkinny()))
}

