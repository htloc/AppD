package com.kamenrider.simulator.view.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kamenrider.simulator.view.components.GameButton

/**
 * Home Screen – entry point of the app.
 *
 * Shows the title with an entrance animation, then Start / Exit buttons.
 */
@Composable
fun HomeScreen(
    onNavigateToDrivers: () -> Unit,
    onExit: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // Entrance animation
    val titleAlpha   = remember { Animatable(0f) }
    val titleScale   = remember { Animatable(0.7f) }
    val buttonsAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        titleAlpha.animateTo(1f, tween(700))
        titleScale.animateTo(1f, tween(700))
        buttonsAlpha.animateTo(1f, tween(500))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A1A), Color(0xFF0D1B2A), Color(0xFF0A0A1A))
                )
            )
    ) {
        // Decorative background glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0x33E91E63),
                            Color(0x00000000)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- Title ---
            Text(
                text = "KAMEN RIDER",
                color = Color(0xFFE91E63),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .scale(titleScale.value)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "DRIVER\nSIMULATOR",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                lineHeight = 48.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .scale(titleScale.value)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Henshin!",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.alpha(titleAlpha.value)
            )

            Spacer(Modifier.height(64.dp))

            // --- Buttons ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(buttonsAlpha.value),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GameButton(
                    text = "START GAME",
                    onClick = { viewModel.onStartClicked(onNavigateToDrivers) },
                    modifier = Modifier.fillMaxWidth(0.75f),
                    gradientColors = listOf(Color(0xFFE91E63), Color(0xFFFF5722))
                )

                GameButton(
                    text = "EXIT",
                    onClick = { viewModel.onExitClicked(onExit) },
                    modifier = Modifier.fillMaxWidth(0.75f),
                    gradientColors = listOf(Color(0xFF37474F), Color(0xFF263238))
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = "v1.0.0",
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 11.sp
            )
        }
    }
}
