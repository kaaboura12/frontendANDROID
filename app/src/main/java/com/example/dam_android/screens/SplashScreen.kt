package com.example.dam_android.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.R
import com.example.dam_android.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToWelcome: () -> Unit
) {
    // Animation states
    var animationStep by remember { mutableStateOf(0) }

    // Auto-navigation après toute l'animation (environ 3 secondes)
    LaunchedEffect(Unit) {
        // Étape 1: Logo apparaît (0-800ms)
        delay(800)
        animationStep = 1

        // Étape 2: Logo se déplace vers la droite (800-1600ms)
        delay(800)
        animationStep = 2

        // Étape 3: Texte apparaît (1600-2400ms)
        delay(800)
        animationStep = 3

        // Navigation (2400-3000ms)
        delay(600)
        onNavigateToWelcome()
    }

    // Animation du logo (scale et position)
    val logoScale by animateFloatAsState(
        targetValue = if (animationStep >= 1) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = EaseOutBack)
    )

    val logoOffsetX by animateFloatAsState(
        targetValue = if (animationStep >= 2) -60f else 0f,
        animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic)
    )

    // Animation du texte
    val textAlpha by animateFloatAsState(
        targetValue = if (animationStep >= 3) 1f else 0f,
        animationSpec = tween(durationMillis = 400)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
    ) {
        // Fond avec vagues
        WavyBackground()

        // Contenu centré
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Logo avec animation
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = logoScale
                            scaleY = logoScale
                            translationX = logoOffsetX
                        }
                )

                // Espacement entre logo et texte
                if (animationStep >= 2) {
                    Spacer(modifier = Modifier.width(16.dp))
                }

                // Texte "Weldi Win ?" avec animation
                if (animationStep >= 3) {
                    Text(
                        text = "Weldi Win ?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange700,
                        modifier = Modifier.graphicsLayer {
                            alpha = textAlpha
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WavyBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Vague orange principale en haut
        val orangePath = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, height * 0.3f)
            cubicTo(
                width * 0.3f, height * 0.25f,
                width * 0.7f, height * 0.35f,
                width, height * 0.3f
            )
            lineTo(width, 0f)
            close()
        }
        drawPath(orangePath, color = Color(0xFFF2A765))

        // Vague beige rosé claire qui se superpose
        val beigeWave = Path().apply {
            moveTo(0f, height * 0.25f)
            cubicTo(
                width * 0.25f, height * 0.2f,
                width * 0.75f, height * 0.3f,
                width, height * 0.25f
            )
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(beigeWave, color = Color(0xFFF5E6D3))
    }
}