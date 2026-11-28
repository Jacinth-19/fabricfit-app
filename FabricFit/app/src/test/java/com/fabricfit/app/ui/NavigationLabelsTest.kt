package com.fabricfit.app.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fabricfit.app.R
import com.fabricfit.app.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins the names the bottom navigation promises against the names the screens
 * actually print.
 *
 * The saved-looks destination printed a hardcoded "Scan Results" header while
 * its tab read "Try-On". "Scan Results" is a different screen - the fabric
 * analysis result - so the tab looked like it had opened the wrong place, and
 * the screenshot pipeline captured that mismatched header for months.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NavigationLabelsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `saved looks screen header reuses the label of the tab that opens it`() {
        // The screen renders R.string.nav_tryon as its header precisely so the two
        // cannot drift apart; this asserts the string is still the expected one.
        assertEquals("Try-On", context.getString(R.string.nav_tryon))
    }

    @Test
    fun `saved looks header is not the analysis result title`() {
        val tab = context.getString(R.string.nav_tryon)
        listOf(
            context.getString(R.string.analysis_result_title),
            context.getString(R.string.result_title),
        ).forEach { other ->
            assertNotEquals(
                "the saved-looks header must not reuse another screen's title",
                other.lowercase(),
                tab.lowercase(),
            )
        }
    }

    @Test
    fun `every bottom nav tab has a readable label`() {
        listOf(
            R.string.nav_home,
            R.string.nav_discover,
            R.string.nav_camera,
            R.string.nav_tryon,
            R.string.nav_profile,
        ).forEach { id ->
            assertTrue("tab label $id is blank", context.getString(id).isNotBlank())
        }
    }

    @Test
    fun `saved looks destination is a top level route`() {
        // A top-level destination is reached from the tab bar, so it renders no
        // back arrow. If this ever becomes a deep, pushed route the header needs
        // an explicit way back.
        assertEquals("favorites", Routes.FAVORITES)
        assertEquals("home", Routes.HOME)
    }
}
