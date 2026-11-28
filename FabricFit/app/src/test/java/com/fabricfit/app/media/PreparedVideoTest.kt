package com.fabricfit.app.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * `prepareForUpload` is hard to exercise end to end off-device: it needs a real MediaCodec
 * and a real video. These tests pin the parts that are pure logic and have already caused
 * silent damage once - the "did we actually re-encode this?" contract.
 *
 * The bug being guarded: the old implementation copied source bytes for months while its
 * callers assumed the file had been compressed, and nothing anywhere reported the
 * difference.
 */
class PreparedVideoTest {

    @Test
    fun `a transcoded result reports itself as transcoded`() {
        val file = tempFileWith("transcoded", 128)
        val result = PreparedVideo(file, wasTranscoded = true)

        assertTrue(result.wasTranscoded)
        assertEquals(128L, result.file.length())
    }

    @Test
    fun `a fallback copy reports itself as not transcoded`() {
        val file = tempFileWith("copy", 4096)
        val result = PreparedVideo(file, wasTranscoded = false)

        // The composer keys its "Original file - could not be re-encoded" label off this.
        // If it ever flips to true, the UI starts claiming compression that did not happen.
        assertFalse(result.wasTranscoded)
    }

    @Test
    fun `wasTranscoded is not derived from file size`() {
        // A tiny transcoded file and a huge copied file must still report honestly: size is
        // not evidence of re-encoding, which is exactly the assumption that hid the old bug.
        val small = tempFileWith("small", 16)
        val large = tempFileWith("large", 64 * 1024)

        assertTrue(PreparedVideo(small, wasTranscoded = true).wasTranscoded)
        assertFalse(PreparedVideo(large, wasTranscoded = false).wasTranscoded)
    }

    private fun tempFileWith(name: String, bytes: Int): File {
        val dir = Files.createTempDirectory("prepared-video-test").toFile()
        return File(dir, "$name.mp4").apply {
            writeBytes(ByteArray(bytes) { 0x1F })
            deleteOnExit()
            dir.deleteOnExit()
        }
    }
}
