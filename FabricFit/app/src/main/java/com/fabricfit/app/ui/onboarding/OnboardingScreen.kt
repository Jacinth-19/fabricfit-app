package com.fabricfit.app.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabricfit.app.R
import com.fabricfit.app.ui.components.AssetImage
import com.fabricfit.app.ui.components.HeritageLuxeMark
import com.fabricfit.app.ui.theme.HeritageCream
import com.fabricfit.app.ui.theme.HeritageForest
import com.fabricfit.app.ui.theme.HeritageForestDark
import com.fabricfit.app.ui.theme.HeritageGold
import com.fabricfit.app.ui.theme.HeritageGoldSoft
import kotlinx.coroutines.launch

private data class Page(
    @DrawableRes val illustration: Int?,
    @StringRes val title: Int,
    @StringRes val body: Int,
    val dark: Boolean = false,
)

private val pages = listOf(
    Page(null, R.string.onboarding_welcome_title, R.string.onboarding_welcome_body, dark = true),
    Page(R.drawable.illust_onboarding_scan, R.string.onboarding_step1_title, R.string.onboarding_step1_body),
    Page(R.drawable.illust_onboarding_designs, R.string.onboarding_step2_title, R.string.onboarding_step2_body),
    Page(R.drawable.illust_onboarding_tryon, R.string.onboarding_step3_title, R.string.onboarding_step3_body),
    Page(R.drawable.illust_onboarding_save, R.string.onboarding_step4_title, R.string.onboarding_step4_body),
)

@Composable
fun OnboardingScreen(onFinished: (scanNow: Boolean) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val pageIndex by remember { derivedStateOf { pagerState.currentPage } }
    val page = pages[pageIndex]

    val bgColor = if (page.dark) HeritageForest else MaterialTheme.colorScheme.background

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Column(Modifier.fillMaxSize()) {
            // Top bar with "Skip for now" accessible on all pages
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { onFinished(false) }) {
                    Text(
                        stringResource(R.string.onboarding_cta_skip),
                        color = if (page.dark) {
                            HeritageGoldSoft.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { index ->
                val p = pages[index]
                if (p.dark) {
                    WelcomeDarkPage()
                } else {
                    LightPage(p)
                }
            }

            // Indicator dots (shown on step pages 1..4)
            if (!page.dark) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(pages.size) { i ->
                        val isCurrent = i == pagerState.currentPage
                        Box(
                            Modifier
                                .padding(horizontal = 5.dp)
                                .size(if (isCurrent) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                ),
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(14.dp))
            }

            // Bottom actions
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
            ) {
                val last = pagerState.currentPage == pages.size - 1
                when {
                    page.dark -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.scrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B4D3B),
                                contentColor = HeritageCream,
                            ),
                        ) {
                            Text(
                                stringResource(R.string.onboarding_cta_get_started),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onFinished(false) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HeritageGoldSoft,
                            ),
                            border = BorderStroke(
                                1.5.dp,
                                HeritageGoldSoft.copy(alpha = 0.6f),
                            ),
                        ) {
                            Text(
                                stringResource(R.string.onboarding_cta_login),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }

                    last -> {
                        Button(
                            onClick = { onFinished(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(stringResource(R.string.onboarding_cta_scan))
                        }
                    }

                    else -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.scrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(stringResource(R.string.onboarding_next))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeDarkPage() {
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
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))

            // Brand mark — gold adinkra monogram
            HeritageLuxeMark(modifier = Modifier.size(116.dp))

            Spacer(Modifier.height(16.dp))

            // Wordmark — "Heritage Luxe" in royal gold serif
            Text(
                stringResource(R.string.onboarding_brand_heritage_luxe),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                color = HeritageGold,
                textAlign = TextAlign.Center,
                lineHeight = 48.sp,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(10.dp))

            // Tagline — "Scan. Discover. Try. Wear."
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                ),
                color = HeritageCream,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(20.dp))

            // Stacked Kente textile folds hero
            AssetImage(
                asset = "catalog/heritage/hero_kente_folds.jpg",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun LightPage(page: Page) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (page.illustration != null) {
            Image(
                painter = painterResource(page.illustration),
                contentDescription = null,
                modifier = Modifier.height(220.dp),
            )
            Spacer(Modifier.height(28.dp))
        }
        Text(
            stringResource(page.title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
