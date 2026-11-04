package com.fabricfit.app.ui.navigation

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fabricfit.app.MainActivity
import com.fabricfit.app.di.AppContainer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the custom bottom bar in the real edge-to-edge activity window. */
@RunWith(AndroidJUnit4::class)
class BottomNavigationInsetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun openMainScaffold() {
        val context = composeRule.activity.applicationContext
        runBlocking {
            AppContainer.get(context).settings.update {
                it.copy(onboardingCompleted = true)
            }
        }
        composeRule.activityRule.scenario.recreate()
    }

    @Test
    fun tryOnTabOpensFitRoomRatherThanScanResults() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithContentDescription("Try-On")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Try-On").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("FitRoom").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("FitRoom").assertIsDisplayed()
        composeRule.onNodeWithText("Scan Results").assertDoesNotExist()
    }

    @Test
    fun tabsRemainAboveThreeButtonSystemNavigation() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithContentDescription("Home")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val navigationMode = Settings.Secure.getInt(
            composeRule.activity.contentResolver,
            "navigation_mode",
            2,
        )
        assertEquals("Device QA must enable Android's 3-button navigation", 0, navigationMode)

        val rootWindowInsets = ViewCompat.getRootWindowInsets(
            composeRule.activity.window.decorView,
        )
        assertNotNull("Root window insets must be attached", rootWindowInsets)
        // Compose may consume the visible inset before this assertion runs. The
        // ignoring-visibility value still describes the system bar frame; the
        // platform dimension is a fallback on headless emulator windows.
        val reportedInsetPx = rootWindowInsets!!
            .getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
            .bottom
        val navigationBarResource = composeRule.activity.resources.getIdentifier(
            "navigation_bar_height",
            "dimen",
            "android",
        )
        val platformInsetPx = if (navigationBarResource > 0) {
            composeRule.activity.resources.getDimensionPixelSize(navigationBarResource)
        } else {
            0
        }
        val navigationInsetPx = maxOf(reportedInsetPx, platformInsetPx)
        assertTrue(
            "3-button navigation must have a bottom inset " +
                "(reported=$reportedInsetPx, platform=$platformInsetPx)",
            navigationInsetPx > 0,
        )

        val density = composeRule.activity.resources.displayMetrics.density
        val navigationInset = (navigationInsetPx / density).dp
        val rootBounds = composeRule.onRoot().getUnclippedBoundsInRoot()
        val homeTabBounds = composeRule.onNodeWithContentDescription("Home")
            .getUnclippedBoundsInRoot()
        val safeContentBottom = rootBounds.bottom - navigationInset

        assertTrue(
            "Home tab bottom ${homeTabBounds.bottom} must be above system navigation " +
                "at $safeContentBottom (inset $navigationInset)",
            homeTabBounds.bottom <= safeContentBottom + 1.dp,
        )
    }
}
