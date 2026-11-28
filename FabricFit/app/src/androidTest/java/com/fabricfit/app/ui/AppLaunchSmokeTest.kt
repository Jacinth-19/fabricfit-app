package com.fabricfit.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fabricfit.app.MainActivity
import com.fabricfit.app.R
import com.fabricfit.app.data.local.prefs.SettingsRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app actually launches and its navigation works, on a device.
 *
 * Nothing in the repository proved this before: the 189 JVM tests cover repositories and view
 * models under Robolectric, and the other instrumented tests only exercise ML. A crash in
 * MainActivity, a broken navigation graph or a Compose composition failure would have reached
 * Play undetected, because no build ever started the app.
 *
 * The assertions are deliberately shallow - they must hold on a cold emulator with no network
 * and no user data, so they only touch chrome (the bottom bar), never content that needs a
 * model, a photo or a downloaded asset.
 *
 * First run shows onboarding, not the bottom bar. The first version of this test assumed the
 * bar was there at launch and failed with "component is not displayed"; the app was behaving
 * correctly. Onboarding is completed up front, and the test that walks through it does so
 * separately so the flow itself is still covered.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun label(labelRes: Int): String = rule.activity.getString(labelRes)

    /** Tabs carry their label as a content description, so find them by that, not by text. */
    private fun tab(labelRes: Int) = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(label(labelRes))),
    )

    @Before
    fun completeOnboarding() {
        // A fresh install lands in onboarding. This runs after the activity has launched (JUnit
        // applies @Before after the rule starts it), which is fine: the shell collects the
        // preferences flow and recomposes, so writing the flag here moves it to MainScaffold and
        // the waitForIdle below waits for that. The store is keyed off the application context,
        // so this instance shares the DataStore the app is already reading - it is not a second
        // writer to the same file.
        val settings = SettingsRepositoryImpl(ApplicationProvider.getApplicationContext())
        runBlocking { settings.update { it.copy(onboardingCompleted = true) } }
    }

    @Test
    fun appStartsAndShowsItsNavigation() {
        rule.waitForIdle()

        // Three tabs from one Row prove the bar composed; the centre camera tab is a different
        // composable, so Home/Profile plus Camera covers both implementations.
        tab(R.string.nav_home).assertIsDisplayed()
        tab(R.string.nav_camera).assertIsDisplayed()
        tab(R.string.nav_profile).assertIsDisplayed()

        // Home is the start destination. Reading the tab's own selected flag rather than screen
        // text means this fails if the graph starts elsewhere, not just if it renders oddly.
        tab(R.string.nav_home).assertIsSelected()
    }

    @Test
    fun tappingATabNavigates() {
        rule.waitForIdle()

        tab(R.string.nav_profile).performClick()
        rule.waitForIdle()

        tab(R.string.nav_profile).assertIsSelected()
        tab(R.string.nav_home).assertIsNotSelected()

        // A hop in the other direction proves the graph is not stuck on the destination it
        // happened to reach.
        tab(R.string.nav_home).performClick()
        rule.waitForIdle()

        tab(R.string.nav_home).assertIsSelected()
        tab(R.string.nav_profile).assertIsNotSelected()
    }

    @Test
    fun onboardingCanBeSkippedToReachTheApp() {
        // Undo the @Before: this test needs the first-run state.
        val settings = SettingsRepositoryImpl(ApplicationProvider.getApplicationContext())
        runBlocking { settings.update { it.copy(onboardingCompleted = false) } }

        // The shell already composed with onboarding completed, so restart the activity to get
        // the real first-run path - that path is the thing worth testing.
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        rule.onNodeWithText(label(R.string.onboarding_cta_skip)).performClick()
        rule.waitForIdle()

        // The CTA is only on onboarding, so its disappearance is what proves the skip worked.
        tab(R.string.nav_home).assertIsDisplayed()
    }
}
