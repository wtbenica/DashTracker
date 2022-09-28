package com.wtb.dashTracker.ui.welcome

import android.annotation.SuppressLint
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.wtb.dashTracker.R
import com.wtb.dashTracker.ui.activity_welcome.theme.DashTrackerTheme

@ExperimentalMaterial3Api
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Boraku() {
    DashTrackerTheme {
        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Start Dash") },
                    icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = "Start Dash") },
                    onClick = { /*TODO*/ }
                )
            },
            bottomBar = {
                BottomNavigation() {
                    BottomNavigationItem(
                        selected = true,
                        onClick = { /*TODO*/ },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_income),
                                contentDescription = "Income"
                            )
                        },
                        label = {
                            Text("Income")
                        }
                    )
                    BottomNavigationItem(
                        selected = true,
                        onClick = { /*TODO*/ },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_expense),
                                contentDescription = "Income"
                            )
                        },
                        label = {
                            Text("Expense")
                        }
                    )
                    BottomNavigationItem(
                        selected = true,
                        onClick = { /*TODO*/ },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_insights),
                                contentDescription = "Income"
                            )
                        },
                        label = {
                            Text("Trends")
                        }
                    )
                }
            },
            topBar = {
                TopAppBar(
                    title = { Text("Banana") },
                    colors = TopAppBarDefaults.largeTopAppBarColors()
                )
            }
        ) {

        }
    }
}


@ExperimentalMaterial3Api
@Preview
@Composable
fun PreviewBoraku() {
    Boraku()
}