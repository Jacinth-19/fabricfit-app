package com.fabricfit.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fabricfit.app.MainActivity
import com.fabricfit.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app actually launches and its navigation works, on a device.
 *
 * Nothing in the repository proved this before: the JVM suite tests repositories and view
 * models with Robolectric, and the two existing instrumented tests only exercise ML. A
 * crash in `MainActivity`, a broken navigation graph or a Compose composition failure would
 * have reached Play undetected, because no build ever started the app.
 *
 * These assertions are deliberately shallow. They must stay green on a cold emulator with no
 * network and no user data, so they assert on navigation chrome (which is always present) and
 * never on content that needs a model, a photo or a downloaded asset.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun tab(labelRes: Int) = rule.onNode(
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(rule.activity.getString(labelRes))),
    )

    @Test
    fun appStartsAndShowsItsNavigation() {
        // If the activity, DI container or theme fails, this throws before any assertion.
        rule.waitForIdle()

        // All five tabs come from one Row, so three of them prove the bar composed; they are
        // asserted by content description because the label text alone is ambiguous once a
        // screen repeats the same word.
        tab(R.string.nav_home).assertIsDisplayed()
        tab(R.string.nav_discover).assertIsDisplayed()
        tab(R.string.nav_profile).assertIsDisplayed()

        // Home is the start destination. This reads the semantics `selected` flag rather than
        // any screen text, so it fails if the navigation graph starts somewhere else.
        tab(R.string.nav_home).assertIsSelected()
    }

    @Test
    fun tappingATabNavigates() {
        rule.waitForIdle()

        // The assertion is the tab's own selected flag, not the presence of text: every tab
        // label is on screen at all times, so looking for "Profile" would pass even if the
        // click did nothing at all.
        tab(R.string.nav_profile).performClick()
        rule.waitForIdle()

        tab(R.string.nav_profile).assertIsSelected()
        tab(R.string.nav_home).assertIsNotSelected()

        // A second hop, in the other direction, proves the graph is not simply stuck on the
        // destination it happened to reach.
        tab(R.string.nav_home).performClick()
        rule.waitForIdle()

        tab(R.string.nav_home).assertIsSelected()
        tab(R.string.nav_profile).assertIsNotSelected()
    }
}
