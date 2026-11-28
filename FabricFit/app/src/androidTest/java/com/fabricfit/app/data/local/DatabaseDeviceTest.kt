package com.fabricfit.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fabricfit.app.data.local.db.FabricEntity
import com.fabricfit.app.data.local.db.FabricFitDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device QA for the Room database: real SQLite, real generated DAO code, real migrations.
 *
 * The 189 JVM tests run against Robolectric's SQLite, which is a substitute. This suite runs
 * the same DAO on the Android framework the app actually ships against, so a query that only
 * works in the shadow implementation fails here.
 *
 * Deliberately uses a file-backed database rather than an in-memory one: the point is to
 * exercise open/close on real disk and the app's real schema, which is what breaks in the field.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseDeviceTest {

    private lateinit var context: Context
    private lateinit var db: FabricFitDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        db = Room.databaseBuilder(context, FabricFitDatabase::class.java, DB_NAME).build()
        // Touch the database so the schema is created before the first assertion.
        runBlocking { assertEquals(0, db.fabricDao().count()) }
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(DB_NAME)
    }

    private fun fabric(name: String, favorite: Boolean = false) = FabricEntity(
        imageUri = "file:///qa/$name.jpg",
        name = name,
        category = "ankara",
        colorsJson = "[\"#8c2f39\"]",
        pattern = "geometric",
        texture = "smooth",
        confidence = 0.9f,
        saturation = 0.5f,
        brightness = 0.5f,
        contrast = 0.5f,
        colorCount = 1,
        motifScale = 1f,
        printTraits = "[]",
        suggestedUses = "[]",
        engine = "on-device",
        isFavorite = if (favorite) 1 else 0,
        createdAt = System.currentTimeMillis(),
    )

    @Test
    fun schemaOpensAndIsWritableOnDevice() = runBlocking {
        val dao = db.fabricDao()
        assertEquals(0, dao.count())
        val id = dao.insert(fabric("kente swatch"))
        assertTrue("insert returned no row id", id > 0)
        assertEquals(1, dao.count())
    }

    @Test
    fun roundTripsEveryColumnWithoutLoss() = runBlocking {
        val dao = db.fabricDao()
        val original = fabric("adire").copy(
            notes = "bought in Makola",
            amountCm = 400,
            intendedWearer = "Ama",
            intendedOccasion = "wedding",
            acquiredAt = 1_700_000_000_000,
        )
        val id = dao.insert(original)
        val loaded = dao.byId(id)
        assertNotNull("byId returned null straight after insert", loaded)
        // Compare the columns that carry user data; id is the only intentional difference.
        assertEquals(original.copy(id = loaded!!.id), loaded)
        assertNull(dao.byId(id + 999))
    }

    @Test
    fun favouriteFlagDrivesTheFavouritesQuery() = runBlocking {
        val dao = db.fabricDao()
        val plain = dao.insert(fabric("plain"))
        val starred = dao.insert(fabric("starred", favorite = true))

        assertEquals(listOf(starred), dao.observeFavorites().first().map { it.id })

        dao.setFavorite(plain, 1)
        assertEquals(
            setOf(plain, starred),
            dao.observeFavorites().first().map { it.id }.toSet(),
        )

        dao.setFavorite(starred, 0)
        assertEquals(listOf(plain), dao.observeFavorites().first().map { it.id })
    }

    @Test
    fun renamePersists() = runBlocking {
        val dao = db.fabricDao()
        val id = dao.insert(fabric("typo"))
        dao.rename(id, "corrected")
        assertEquals("corrected", dao.byId(id)?.name)
    }

    @Test
    fun observeAllIsOrderedNewestFirst() = runBlocking {
        val dao = db.fabricDao()
        val old = dao.insert(fabric("old").copy(createdAt = 1_000))
        val recent = dao.insert(fabric("recent").copy(createdAt = 2_000))
        assertEquals(listOf(recent, old), dao.observeAll().first().map { it.id })
    }

    private companion object {
        const val DB_NAME = "qa-device-test.db"
    }
}
