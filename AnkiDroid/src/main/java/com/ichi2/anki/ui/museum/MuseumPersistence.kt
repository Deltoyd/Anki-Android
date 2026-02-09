package com.ichi2.anki.ui.museum

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Persistence layer for the Museum feature using SharedPreferences.
 *
 * Stores:
 * - unlocked_pieces: CSV string of piece indices (e.g., "0,1,2,5,7,10")
 * - streak_days: Int representing current consecutive study streak
 * - last_study_date: String in "yyyy-MM-dd" format
 * - extra_lives: Int (0-3) for gamification
 *
 * Uses SharedPreferences rather than Room database because:
 * - Simple key-value storage for Set<Int>, streak count, date
 * - No complex queries needed
 * - Fast read on app startup
 * - Follows existing AnkiDroid patterns (see Reviewer.kt)
 */
object MuseumPersistence {
    private const val PREFS_NAME = "museum_prefs"
    private const val KEY_UNLOCKED_PIECES = "unlocked_pieces"
    private const val KEY_STREAK_DAYS = "streak_days"
    private const val KEY_LAST_STUDY_DATE = "last_study_date"
    private const val KEY_EXTRA_LIVES = "extra_lives"

    private const val DEFAULT_EXTRA_LIVES = 3

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Returns the set of unlocked puzzle piece indices.
     */
    fun getUnlockedPieces(context: Context): Set<Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val csv = prefs.getString(KEY_UNLOCKED_PIECES, "") ?: ""

        return if (csv.isEmpty()) {
            emptySet()
        } else {
            csv
                .split(",")
                .mapNotNull { it.toIntOrNull() }
                .toSet()
        }
    }

    /**
     * Adds a single piece index to the unlocked set.
     * @return true if piece was newly unlocked, false if already unlocked
     */
    fun addUnlockedPiece(
        context: Context,
        pieceIndex: Int,
    ): Boolean {
        val currentPieces = getUnlockedPieces(context)
        if (pieceIndex in currentPieces) {
            return false // Already unlocked
        }

        val updatedPieces = currentPieces + pieceIndex
        saveUnlockedPieces(context, updatedPieces)
        return true
    }

    /**
     * Saves the entire set of unlocked pieces (overwrites existing data).
     */
    private fun saveUnlockedPieces(
        context: Context,
        pieces: Set<Int>,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val csv = pieces.sorted().joinToString(",")
        prefs.edit().putString(KEY_UNLOCKED_PIECES, csv).apply()
    }

    /**
     * Returns the current study streak in days.
     */
    fun getStreakDays(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_STREAK_DAYS, 0)
    }

    /**
     * Updates the study streak based on the current date.
     * Call this on the first card review of each day.
     *
     * @return The updated streak count
     */
    fun updateStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = dateFormat.format(Date())
        val lastStudyDate = prefs.getString(KEY_LAST_STUDY_DATE, null)
        val currentStreak = prefs.getInt(KEY_STREAK_DAYS, 0)

        val newStreak =
            when {
                lastStudyDate == null -> 1 // First ever study
                lastStudyDate == today -> currentStreak // Already studied today
                isConsecutiveDay(lastStudyDate, today) -> currentStreak + 1 // Consecutive day
                else -> 1 // Streak broken, restart
            }

        prefs
            .edit()
            .putInt(KEY_STREAK_DAYS, newStreak)
            .putString(KEY_LAST_STUDY_DATE, today)
            .apply()

        return newStreak
    }

    /**
     * Returns the last date the user studied (null if never studied).
     */
    fun getLastStudyDate(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_STUDY_DATE, null)
    }

    /**
     * Returns the number of extra lives (0-3).
     */
    fun getExtraLives(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_EXTRA_LIVES, DEFAULT_EXTRA_LIVES)
    }

    /**
     * Sets the number of extra lives (0-3).
     */
    fun setExtraLives(
        context: Context,
        lives: Int,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val clampedLives = lives.coerceIn(0, 3)
        prefs.edit().putInt(KEY_EXTRA_LIVES, clampedLives).apply()
    }

    /**
     * Checks if two date strings represent consecutive days.
     */
    private fun isConsecutiveDay(
        lastDateStr: String,
        currentDateStr: String,
    ): Boolean {
        return try {
            val lastDate = dateFormat.parse(lastDateStr) ?: return false
            val currentDate = dateFormat.parse(currentDateStr) ?: return false

            val calendar = Calendar.getInstance()
            calendar.time = lastDate
            calendar.add(Calendar.DAY_OF_MONTH, 1)

            // Compare dates (ignoring time)
            dateFormat.format(calendar.time) == currentDateStr
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clears all museum data (for testing or reset functionality).
     */
    fun clearAllData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
