package com.wtb.dashTracker.ui.welcome

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wtb.dashTracker.R

@ExperimentalMaterial3Api
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun Potato() {
    val configuration = LocalConfiguration.current

    val density = configuration.densityDpi / 160f
    val screenHeight = configuration.screenHeightDp * density
    val screenWidth = configuration.screenWidthDp * density

    var isStarted by remember { mutableStateOf(true) }


    var fabPosition by remember { mutableStateOf<Offset?>(Offset(900f, 1800f)) }
    var fabSize by remember { mutableStateOf(IntSize(300, 200)) }

//  These are useful if cutout is a circle, otherwise for oval need to use top/left offset
//    val xPos = fabPosition?.x?.let { xP ->
//        xP + fabSize.width / 2
//    } ?: screenWidth
//    val yPos = fabPosition?.y?.let { yP ->
//        yP + fabSize.height / 2
//    } ?: screenHeight

    val startX by animateFloatAsState(
        targetValue = if (!isStarted) fabPosition?.x ?: 0f else 200f,
        animationSpec = TweenSpec(durationMillis = 3000, easing = LinearOutSlowInEasing),
    )

    val startY by animateFloatAsState(
        targetValue = if (!isStarted) fabPosition?.y ?: 0f else 200f,
        animationSpec = TweenSpec(durationMillis = 3000, easing = FastOutSlowInEasing),
    )

//    AndroidView(
//        factory = { context ->
//            val view = LayoutInflater.from(context).inflate(R.layout.activity_main, null, false)
//
//            val binding = ActivityMainBinding.bind(view)
//
//            fabPosition = Offset(binding.fab.top.toFloat(), binding.fab.left.toFloat())
//            fabSize = IntSize(binding.fab.width, binding.fab.height)
//
//            view // return the view
//        },
//        update = { view ->
//            // Update the view
//        }
//    )

    Image(
        painter = painterResource(R.drawable.justice_league),
        contentDescription = "Batman, Superman, and Flash urinating in the bathrrom",
        contentScale = ContentScale.FillBounds,
        modifier = Modifier
            .fillMaxSize()
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clickable { isStarted = !isStarted },
        onDraw = {
            val roundPat = Path().apply {
                addRoundRect(
                    RoundRect(
                        Rect(
                            left = startX - (fabSize.width * .1f),
                            top = startY - (fabSize.height * .2f),
                            right = startX + (fabSize.width * 1.1f),
                            bottom = startY + (fabSize.height * 1.2f)
                        ),
                        topLeft = CornerRadius(16f * density),
                        topRight = CornerRadius(16f * density),
                        bottomLeft = CornerRadius(16f * density),
                        bottomRight = CornerRadius(16f * density),
                    )
                )
            }
            clipPath(roundPat, clipOp = ClipOp.Difference) {
                drawRect(Color(R.color.regular))
            }
        }
    )

    Column {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(16.dp)
        ) {
            Crossfade(
                targetState = isStarted,
                animationSpec = TweenSpec(durationMillis = 3000)
            ) {
                if (it) {
                    Text(
                        text = "Welcome!",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = Color(R.color.regular),
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "Honky!",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = Color(R.color.regular),
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

        }
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun PreviewPotato() {
    Potato()
}