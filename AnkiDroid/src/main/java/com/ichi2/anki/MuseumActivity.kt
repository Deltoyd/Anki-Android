package com.ichi2.anki

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import java.util.Calendar
import java.util.Random

import com.ichi2.anki.databinding.ActivityMuseumBinding
import com.ichi2.anki.ui.museum.toDateKey


/**
 * IKASI museum home screen.
 *
 * Displays:
 *   • The current painting-in-progress as a puzzle (PaintingPuzzleView)
 *   • A streak bar with extra-life hearts
 *   • A year heatmap of study activity (HeatmapView)
 *   • A "Study" button that launches the card reviewer
 *
 * PoC state: painting, streak, and heatmap data are generated locally.
 * A future revision will wire these to the FSRS backend and persistent storage.
 */
class MuseumActivity : AnkiActivity() {

    private lateinit var binding: ActivityMuseumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMuseumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupPainting()
        setupStreak()
        setupHeatmap()
        setupStudyButton()
    }

    // ─── PAINTING ─────────────────────────────────────────────────────────────

    private fun setupPainting() {
        binding.paintingView.setPainting(generateTestPainting())
        // PoC: first 8 scattered pieces are "unlocked" to demonstrate the concept
        binding.paintingView.setUnlockedPieces(setOf(0, 1, 2, 3, 5, 6, 7, 10))
    }

    /**
     * Generates a colourful abstract test image until real public-domain art is loaded.
     * Warm palette matches the IKASI museum aesthetic.
     */
    private fun generateTestPainting(): Bitmap {
        val size = 500
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Warm gradient background
        val gradient = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            intArrayOf(
                0xFFE8A838.toInt(),
                0xFFD4840A.toInt(),
                0xFF8B5A0B.toInt(),
                0xFFCC7A22.toInt(),
            ),
            floatArrayOf(0f, 0.3f, 0.7f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), Paint().apply { shader = gradient })

        // Abstract shapes — stand-in for real art
        val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }

        paint.color = 0xFFFF6B35.toInt()
        canvas.drawCircle(120f, 180f, 90f, paint)

        paint.color = 0xFFDAA520.toInt()
        canvas.drawCircle(350f, 120f, 110f, paint)

        paint.color = 0xFF8B4513.toInt()
        canvas.drawCircle(280f, 380f, 70f, paint)

        paint.color = 0xFFCD853F.toInt()
        canvas.drawCircle(80f, 400f, 55f, paint)

        paint.color = 0xFFFF8C00.toInt()
        canvas.drawCircle(420f, 350f, 80f, paint)

        // A rectangle to add geometric contrast
        paint.color = 0xFF6B3A2A.toInt()
        canvas.drawRect(200f, 60f, 370f, 200f, paint)

        return bitmap
    }

    // ─── STREAK ───────────────────────────────────────────────────────────────

    private fun setupStreak() {
        // PoC: hard-coded mock values
        val streakDays = 12
        val extraLives = 2
        val maxLives = 3

        binding.streakText.text = "🔥 $streakDays days"
        binding.livesText.text = "❤️".repeat(extraLives) + "☐".repeat(maxLives - extraLives)
    }

    // ─── HEATMAP ──────────────────────────────────────────────────────────────

    private fun setupHeatmap() {
        binding.heatmapView.setActivityData(generateMockActivityData())

        // Tap on a day in week view shows a simple toast with details
        binding.heatmapView.onDayTapped = { dateKey ->
            val cards = generateMockActivityData()[dateKey] ?: 0
            showThemedToast(this, "$dateKey — $cards cards reviewed", false)
        }
    }

    /**
     * Generates 120 days of plausible mock study data.
     * Uses a fixed seed so the heatmap looks consistent across runs.
     */
    private fun generateMockActivityData(): Map<String, Int> {
        val data = mutableMapOf<String, Int>()
        val random = Random(42)

        for (i in 0..120) {
            val date = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -i)
            }
            // Recent 12 days always have data (simulates current streak)
            if (i <= 12 || random.nextFloat() < 0.6f) {
                val cards = if (i <= 12) random.nextInt(15) + 10 else random.nextInt(25) + 5
                data[date.toDateKey()] = cards
            }
        }
        return data
    }

    // ─── STUDY BUTTON ─────────────────────────────────────────────────────────

    private fun setupStudyButton() {
        binding.studyButton.setOnClickListener {
            // Launch the existing Anki reviewer.
            // Future: select the curated Basque deck, inject casino layer.
            startActivity(Intent(this, Reviewer::class.java))
        }
    }
}
