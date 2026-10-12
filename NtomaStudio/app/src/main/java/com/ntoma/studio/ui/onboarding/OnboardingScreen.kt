package com.ntoma.studio.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ntoma.studio.R
import com.ntoma.studio.ui.components.BrandMark
import com.ntoma.studio.ui.theme.KenteGold
import kotlinx.coroutines.launch

private data class Page(
    @DrawableRes val illustration: Int?,
    @StringRes val title: Int,
    @StringRes val body: Int,
)

private val pages = listOf(
    Page(null, R.string.onboarding_welcome_title, R.string.onboarding_welcome_body),
    Page(R.drawable.illust_onboarding_scan, R.string.onboarding_step1_title, R.string.onboarding_step1_body),
    Page(R.drawable.illust_onboarding_designs, R.string.onboarding_step2_title, R.string.onboarding_step2_body),
    Page(R.drawable.illust_onboarding_tryon, R.string.onboarding_step3_title, R.string.onboarding_step3_body),
    Page(R.drawable.illust_onboarding_save, R.string.onboarding_step4_title, R.string.onboarding_step4_body),
)

/** Heritage Luxe brand screen: deep forest green, gold mark and serif wordmark. */
@Composable
fun OnboardingScreen(onFinished: (scanNow: Boolean) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val ivory = MaterialTheme.colorScheme.onPrimary

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { onFinished(false) },
                    colors = ButtonDefaults.textButtonColors(contentColor = ivory),
                ) {
                    Text(stringResource(R.string.onboarding_cta_skip))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { index ->
                val page = pages[index]
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (page.illustration == null) {
                        BrandMark(modifier = Modifier.size(110.dp), transparent = true)
                        Spacer(Modifier.height(14.dp))
                        Text(
                            stringResource(R.string.brand_name),
                            style = MaterialTheme.typography.displayLarge,
                            color = ivory,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.brand_tagline).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = KenteGold,
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            stringResource(R.string.brand_scanline),
                            style = MaterialTheme.typography.headlineMedium,
                            color = ivory,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Image(
                            painter = painterResource(page.illustration),
                            contentDescription = null,
                            modifier = Modifier.height(220.dp),
                        )
                        Spacer(Modifier.height(32.dp))
                        Text(
                            stringResource(page.title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = ivory,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(page.body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = ivory.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (index == 0) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.onboarding_privacy_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = ivory.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Indicator — gold on forest
            val selectedColor = KenteGold
            val unselectedColor = ivory.copy(alpha = 0.35f)
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(if (i == pagerState.currentPage) 10.dp else 7.dp)
                            .background(
                                if (i == pagerState.currentPage) selectedColor else unselectedColor,
                                CircleShape,
                            ),
                    )
                }
            }

            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                val last = pagerState.currentPage == pages.size - 1
                if (last) {
                    Text(
                        stringResource(R.string.onboarding_started_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = ivory,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.onboarding_started_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ivory.copy(alpha = 0.75f),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onFinished(true) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KenteGold,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(stringResource(R.string.onboarding_cta_scan))
                    }
                    OutlinedButton(
                        onClick = { onFinished(false) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ivory),
                    ) {
                        Text(stringResource(R.string.onboarding_cta_skip))
                    }
                } else {
                    Button(
                        onClick = { scope.launch { pagerState.scrollToPage(pagerState.currentPage + 1) } },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KenteGold,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(stringResource(R.string.onboarding_next))
                    }
                }
            }
        }
    }
}
