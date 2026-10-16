package com.fabricfit.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fabricfit.app.R
import com.fabricfit.app.di.AppContainer
import com.fabricfit.app.domain.model.UserPreferences
import com.fabricfit.app.ui.components.HeritageLuxeMark
import com.fabricfit.app.ui.navigation.MainScaffold
import com.fabricfit.app.ui.onboarding.OnboardingScreen
import com.fabricfit.app.ui.theme.FabricFitTheme
import com.fabricfit.app.ui.theme.HeritageCream
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.theme.HeritageGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Root of the UI tree: applies theme, gates onboarding, then hosts the main navigation.
 * Opens with a Heritage Luxe Deep Forest + Royal Gold Adinkra monogram splash that
 * transitions seamlessly into the Welcome screen or Home dashboard.
 */
@Composable
fun AppShell() {
    FabricFitTheme {
        val context = LocalContext.current
        val container = AppContainer.get(context)
        val prefs by container.settings.preferences
            .collectAsStateWithLifecycle(initialValue = UserPreferences())
        val scope = rememberCoroutineScope()
        var pendingScan by remember { mutableStateOf(false) }
        var splashVisible by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            delay(1100)
            splashVisible = false
        }

        Box(Modifier.fillMaxSize()) {
            if (!prefs.onboardingCompleted) {
                OnboardingScreen(
                    onFinished = { scanNow ->
                        pendingScan = scanNow
                        scope.launch {
                            container.settings.update { it.copy(onboardingCompleted = true) }
                        }
                    },
                )
            } else {
                MainScaffold(
                    startWithScan = pendingScan,
                    onScanConsumed = { pendingScan = false },
                )
            }

            AnimatedVisibility(
                visible = splashVisible,
                exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(400)),
            ) {
                SplashArt()
            }
        }
    }
}

@Composable
private fun SplashArt() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        HeritageForestDark,
                        HeritageForest,
                        Color(0xFF083023),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HeritageLuxeMark(modifier = Modifier.size(124.dp))
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.onboarding_brand_heritage_luxe),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = HeritageGold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.titleLarge,
                color = HeritageCream,
                textAlign = TextAlign.Center,
            )
        }
    }
}
