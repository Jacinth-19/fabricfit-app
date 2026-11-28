package com.fabricfit.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
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
import com.fabricfit.app.ui.screens.security.BiometricLockScreen
import com.fabricfit.app.ui.theme.FabricFitTheme
import com.fabricfit.app.ui.theme.HeritageCream
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.theme.HeritageGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource

/**
 * Root of the UI tree: applies theme, gates onboarding and biometric lock, then hosts the main navigation.
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
        var biometricAuthenticated by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(1100)
            splashVisible = false
        }

        Box(Modifier.fillMaxSize()) {
            // Show biometric lock screen if enabled and not yet authenticated
            if (prefs.security.biometricLock && !biometricAuthenticated) {
                BiometricLockScreen(
                    onAuthenticated = { biometricAuthenticated = true },
                    onFailed = { /* Stay on lock screen */ }
                )
            } else if (!prefs.onboardingCompleted) {
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
    // One continuous timeline, matching the store promo's opening beat: the mark arrives,
    // a gold ring settles around it, then the wordmark and tagline follow. Staggering the
    // four beats is what makes it read as a composed ident instead of a pop-in.
    val timeline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        timeline.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 1150,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            ),
        )
    }

    fun beat(startAt: Float, length: Float): Float =
        ((timeline.value - startAt) / length).coerceIn(0f, 1f)

    val markIn = beat(0f, 0.42f)
    val ringIn = beat(0.18f, 0.46f)
    val wordIn = beat(0.42f, 0.36f)
    val taglineIn = beat(0.60f, 0.40f)

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
        // A faint gold bloom behind the mark, breathing open with the ring.
        Box(
            Modifier
                .size(300.dp)
                .graphicsLayer {
                    alpha = ringIn * 0.5f
                    scaleX = 0.7f + ringIn * 0.5f
                    scaleY = 0.7f + ringIn * 0.5f
                }
                .background(
                    Brush.radialGradient(
                        listOf(HeritageGold.copy(alpha = 0.16f), Color.Transparent),
                    ),
                    shape = CircleShape,
                ),
        )

        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                // The settling ring is drawn, not an asset, so it can resolve from a wide
                // arc to its final weight without a second drawable.
                Box(
                    Modifier
                        .size(168.dp)
                        .graphicsLayer {
                            alpha = ringIn
                            scaleX = 1.14f - ringIn * 0.14f
                            scaleY = 1.14f - ringIn * 0.14f
                        }
                        .border(
                            width = 1.dp,
                            color = HeritageGold.copy(alpha = 0.34f),
                            shape = CircleShape,
                        ),
                )
                Image(
                    painter = painterResource(R.drawable.ic_splash_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(132.dp)
                        .graphicsLayer {
                            scaleX = 0.86f + markIn * 0.14f
                            scaleY = 0.86f + markIn * 0.14f
                            alpha = markIn
                        },
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                stringResource(R.string.onboarding_brand_heritage_luxe),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = HeritageGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = wordIn
                    translationY = (1f - wordIn) * 18.dp.toPx()
                },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.titleLarge,
                color = HeritageCream,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = taglineIn
                    translationY = (1f - taglineIn) * 12.dp.toPx()
                },
            )
        }
    }
}
