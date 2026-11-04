package com.fabricfit.app.ui.navigation

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.fabricfit.app.MainActivity
import com.fabricfit.app.di.AppContainer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        val homeTabBounds = composeRule.onNodeWithContentDescription("Home")
            .getUnclippedBoundsInRoot()
        val homeTabBottomPx = homeTabBounds.bottom.value * density

        // Compare against the visible SystemUI control itself. Instrumented
        // activity windows can report zero insets on a headless emulator even
        // while the 3-button bar is present, making an inset-only test unreliable.
        val systemHomeButton = UiDevice.getInstance(
            InstrumentationRegistry.getInstrumentation(),
        ).findObject(UiSelector().resourceId("com.android.systemui:id/home"))
        assertTrue(
            "Android SystemUI Home button must be visible in 3-button mode",
            systemHomeButton.waitForExists(5_000),
        )
        val systemHomeTopPx = systemHomeButton.bounds.top

        assertTrue(
            "FabricFit Home tab bottom $homeTabBottomPx must be above SystemUI " +
                "Home button top $systemHomeTopPx",
            homeTabBottomPx <= systemHomeTopPx + 1f,
        )
    }
}
