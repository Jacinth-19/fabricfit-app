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

        val density = composeRule.activity.resources.displayMetrics.density
        val rootWindowInsets = ViewCompat.getRootWindowInsets(
            composeRule.activity.window.decorView,
        )
        assertNotNull("Root window insets must be attached", rootWindowInsets)
        val visibleInsetPx = rootWindowInsets!!
            .getInsets(WindowInsetsCompat.Type.navigationBars())
            .bottom
        val persistentInsetPx = rootWindowInsets
            .getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())
            .bottom
        val navigationInsetPx = maxOf(visibleInsetPx, persistentInsetPx)
        assertTrue(
            "3-button navigation must report a bottom inset " +
                "(visible=$visibleInsetPx, persistent=$persistentInsetPx)",
            navigationInsetPx > 0,
        )

        val rootBottomPx = composeRule.onRoot()
            .getUnclippedBoundsInRoot().bottom.value * density
        val homeTabBottomPx = composeRule.onNodeWithContentDescription("Home")
            .getUnclippedBoundsInRoot().bottom.value * density
        val safeContentBottomPx = rootBottomPx - navigationInsetPx

        assertTrue(
            "FabricFit Home tab bottom $homeTabBottomPx must be above system " +
                "navigation at $safeContentBottomPx (inset $navigationInsetPx)",
            homeTabBottomPx <= safeContentBottomPx + 1f,
        )
    }
}
