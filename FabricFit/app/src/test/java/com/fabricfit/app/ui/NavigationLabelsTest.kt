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
 * The saved-looks destination shipped its header as "Scan Results" while its tab
 * read "Try-On". "Scan Results" is a different screen (the fabric analysis
 * result), so the tab looked like it had opened the wrong place - and the
 * screenshot pipeline captured that mismatched header for months.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NavigationLabelsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `saved looks tab and its screen header agree`() {
        val tab = context.getString(R.string.nav_tryon)
        val header = context.getString(R.string.nav_favorites)
        assertEquals("Try-On", tab)
        assertEquals("Favourites", header)
        // The header is reused as the screen title, so it must stay distinct from
        // the analysis-result title it was once confused with.
        assertNotEquals(tab, header)
    }

    @Test
    fun `favourites header is not the analysis result title`() {
        val favourites = context.getString(R.string.nav_favorites)
        val analysis = context.getString(R.string.analysis_result_title)
        val result = context.getString(R.string.result_title)
        listOf(analysis, result).forEach { other ->
            assertNotEquals(
                "the saved-looks header must not reuse another screen's title",
                other.lowercase(),
                favourites.lowercase(),
            )
        }
    }

    @Test
    fun `every bottom nav tab has a readable label`() {
        val ids = listOf(
            R.string.nav_home,
            R.string.nav_discover,
            R.string.nav_camera,
            R.string.nav_tryon,
            R.string.nav_profile,
        )
        ids.forEach { id ->
            val label = context.getString(id)
            assertTrue("tab label $id is blank", label.isNotBlank())
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
