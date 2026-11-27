package com.fabricfit.app.data.remote

import com.fabricfit.app.domain.model.DressStyle
import com.fabricfit.app.domain.model.GenderCategory
import com.fabricfit.app.domain.model.GarmentSilhouette
import com.fabricfit.app.domain.model.LengthOption
import com.fabricfit.app.domain.model.Occasion
import com.fabricfit.app.domain.model.StyleCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The remote ranker is optional, but when it is configured its replies are parsed without
 * crashing the app. These tests cover the hostile and malformed cases, because that is the
 * code most likely to break silently in production.
 */
class CloudRecommendationClientTest {

    private val client = CloudRecommendationClient()

    private fun style(id: String) = DressStyle(
        id = id,
        titleKey = "style_${id}_title",
        descriptionKey = "style_${id}_desc",
        gender = GenderCategory.WOMEN,
        category = StyleCategory.TRADITIONAL,
        occasions = setOf(Occasion.WEDDING),
        silhouette = GarmentSilhouette.KABA,
        affinity = emptySet(),
        structure = 0.5f,
    )

    private val candidates = listOf(style("kaba"), style("maxi"), style("peplum"))

    @Test
    fun `parses a well formed response`() {
        val reply = client.parseResponse(
            """
            {"recommendations":[
              {"styleId":"kaba","score":0.9,"reasons":["holds a structured cut"]},
              {"styleId":"maxi","score":0.5}
            ],"modelVersion":"test-1"}
            """.trimIndent(),
            candidates,
        )
        assertNotNull(reply)
        assertEquals("test-1", reply!!.modelVersion)
        assertEquals(2, reply.scored.size)
        assertEquals("kaba", reply.scored[0].styleId)
        assertEquals(0.9f, reply.scored[0].score, 0.001f)
        assertEquals(listOf("holds a structured cut"), reply.scored[0].reasons)
    }

    @Test
    fun `drops style ids that are not in the candidate set`() {
        // A backend must not be able to invent catalogue entries the app has no artwork for.
        val reply = client.parseResponse(
            """{"recommendations":[
                 {"styleId":"not_in_catalogue","score":0.99},
                 {"styleId":"kaba","score":0.4}
               ]}""",
            candidates,
        )
        assertEquals(1, reply!!.scored.size)
        assertEquals("kaba", reply.scored[0].styleId)
    }

    @Test
    fun `clamps scores so a backend cannot claim certainty`() {
        val reply = client.parseResponse(
            """{"recommendations":[
                 {"styleId":"kaba","score":42.0},
                 {"styleId":"maxi","score":-7.0}
               ]}""",
            candidates,
        )!!
        assertEquals(1.0f, reply.scored[0].score, 0.001f)
        assertEquals(0.0f, reply.scored[1].score, 0.001f)
    }

    @Test
    fun `returns null rather than throwing on malformed payloads`() {
        for (bad in listOf("", "not json", "{", "[]", "null", """{"recommendations":"nope"}""")) {
            assertNull("should tolerate: $bad", client.parseResponse(bad, candidates))
        }
    }

    @Test
    fun `returns null when the reply contains no usable styles`() {
        assertNull(client.parseResponse("""{"recommendations":[]}""", candidates))
        assertNull(client.parseResponse("""{"other":"field"}""", candidates))
        assertNull(client.parseResponse("""{"recommendations":[{"score":0.5}]}""", candidates))
    }

    @Test
    fun `customization falls back to defaults for unknown enum names`() {
        val reply = client.parseResponse(
            """{"recommendations":[{"styleId":"kaba","score":0.5,
                 "customization":{"sleeve":"MADE_UP","neckline":"OFF_SHOULDER","length":"MAXI"}}]}""",
            candidates,
        )!!
        val customization = reply.scored[0].customization
        assertNotNull(customization)
        // Unknown name keeps the default; known names are applied.
        assertEquals(LengthOption.MAXI, customization!!.length)
        assertTrue(customization.sleeve.name != "MADE_UP")
    }

    @Test
    fun `endpoint normalises base urls`() {
        // Operators type these by hand, so trailing slashes and an existing /v1 must both work.
        assertEquals("http://h:8008/v1/recommend", client.endpointForTest("http://h:8008"))
        assertEquals("http://h:8008/v1/recommend", client.endpointForTest("http://h:8008/"))
        assertEquals("http://h:8008/v1/recommend", client.endpointForTest("http://h:8008/v1/"))
        assertEquals("http://h:8008/v1/recommend", client.endpointForTest("  http://h:8008  "))
    }
}
