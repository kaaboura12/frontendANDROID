package com.example.dam_android.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.R
import com.example.dam_android.ui.theme.Orange700
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit
) {
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logoVisible = true
        delay(400)
        textVisible = true
        delay(1200)
        onNavigateToWelcome()
    }

    val glowAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "logoGlow"
    )

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.6f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "logoAlpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "textAlpha"
    )

    val textScale by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0.95f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "textScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF7ED), Color(0xFFFDF1E0))
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .graphicsLayer {
                            alpha = glowAlpha * 0.35f
                            scaleX = glowAlpha * 1.4f
                            scaleY = glowAlpha * 1.4f
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Orange700.copy(alpha = 0.6f), Color.Transparent)
                            ),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                )
                if (logoAlpha > 0.01f) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(96.dp)
                            .graphicsLayer {
                                scaleX = logoScale
                                scaleY = logoScale
                                alpha = logoAlpha
                            }
                    )
                }
            }

            if (textAlpha > 0.01f) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .graphicsLayer {
                            alpha = textAlpha
                            scaleX = textScale
                            scaleY = textScale
                        }
                ) {
                    Text(
                        text = "Weldi Win ?",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange700
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stay connected with what matters most.",
                        color = Color(0xFF945935),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}