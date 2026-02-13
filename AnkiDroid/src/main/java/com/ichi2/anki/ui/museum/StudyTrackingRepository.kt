package com.ichi2.anki.ui.museum

import com.ichi2.anki.libanki.DB
import java.time.LocalDate
import java.time.ZoneId

/**
 * Repository for computing study tracking data from Anki's revlog.
 * All methods are pure functions that compute on-the-fly (no caching).
 */
class StudyTrackingRepository {
    companion object {
        private const val CARD_TIME_CAP_MS = 30_000L // 30 seconds

        /**
         * Queries revlog for all reviews in date range.
         * revlog.id is timestamp in ms, revlog.time is review duration in ms.
         * Called within withCol {} block (which provides coroutine context).
         * Not marked suspend as it performs synchronous DB operations.
         */
        fun queryRevlog(
            db: DB,
            fromDate: LocalDate,
            toDate: LocalDate,
        ): List<RevlogEntry> {
            val fromMs = fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val toMs =
                toDate
                    .plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            val entries = mutableListOf<RevlogEntry>()
            db.query("SELECT id, time FROM revlog WHERE id >= ? AND id < ?", fromMs, toMs).use { cursor ->
                while (cursor.moveToNext()) {
                    entries.add(RevlogEntry(id = cursor.getLong(0), timeMs = cursor.getInt(1)))
                }
            }
            return entries
        }

        /**
         * Groups revlog entries by date and computes daily study stats.
         * Caps each card's time at 30 seconds.
         * Returns a map with an entry for every date in range.
         */
        fun getDailyStudyData(
            entries: List<RevlogEntry>,
            fromDate: LocalDate,
            toDate: LocalDate,
        ): Map<LocalDate, DailyStudyData> {
            // Group entries by date
            val entriesByDate =
                entries.groupBy { entry ->
                    val instant = java.time.Instant.ofEpochMilli(entry.id)
                    LocalDate.ofInstant(instant, ZoneId.systemDefault())
                }

            // Build map with all dates in range
            val result = mutableMapOf<LocalDate, DailyStudyData>()
            var currentDate = fromDate
            while (!currentDate.isAfter(toDate)) {
                val dayEntries = entriesByDate[currentDate] ?: emptyList()
                val cardsReviewed = dayEntries.size
                val studyTimeMs = dayEntries.sumOf { minOf(it.timeMs.toLong(), CARD_TIME_CAP_MS) }
                val wasStudied = cardsReviewed >= 1

                result[currentDate] =
                    DailyStudyData(
                        date = currentDate,
                        cardsReviewed = cardsReviewed,
                        studyTimeMs = studyTimeMs,
                        wasStudied = wasStudied,
                    )

                currentDate = currentDate.plusDays(1)
            }

            return result
        }

        /**
         * Calculates current streak with grace day system.
         *
         * Grace accumulation: Every 3 consecutive actual study days earns 1 grace day, max 5.
         * Grace consumption: Missing a day with grace > 0 consumes 1 grace, streak continues.
         * Streak breaks: Missing a day with grace == 0 resets streak to 0.
         * Today not studied: todayStatus = NOT_STUDIED, streak counts up to yesterday.
         *
         * Algorithm: Walk backward from today to find all days in current streak, then
         * replay forward to calculate grace days correctly.
         */
        fun calculateStreak(
            dailyData: Map<LocalDate, DailyStudyData>,
            today: LocalDate,
        ): StreakInfo {
            val todayData = dailyData[today]
            val todayStatus =
                when {
                    todayData == null || !todayData.wasStudied -> DayStatus.NOT_STUDIED
                    else -> DayStatus.STUDIED
                }

            // Start from today or yesterday if today not studied
            val endDate = if (todayStatus == DayStatus.NOT_STUDIED) today.minusDays(1) else today

            // Find the earliest date by simulating backward walk with grace accumulation
            // We need to know about future grace to determine if past gaps can be filled
            // So we collect all potential streak days first
            val potentialDays = mutableListOf<Pair<LocalDate, Boolean>>() // (date, wasStudied)
            var checkDate = endDate

            // Collect days going backward - we'll check for data availability
            // Stop when we hit multiple consecutive unstudied days that would break any streak
            var consecutiveUnstudied = 0
            val maxLookback = 365 // Don't look back more than a year

            for (i in 0 until maxLookback) {
                val dayData = dailyData[checkDate]
                val wasStudied = dayData?.wasStudied ?: false
                potentialDays.add(Pair(checkDate, wasStudied))

                if (!wasStudied) {
                    consecutiveUnstudied++
                    // If we have 6+ consecutive unstudied days, streak definitely can't continue
                    // (max 5 grace days)
                    if (consecutiveUnstudied > 5) {
                        break
                    }
                } else {
                    consecutiveUnstudied = 0
                }

                checkDate = checkDate.minusDays(1)
            }

            // Now walk forward through potentialDays (reverse the list) to find actual streak
            val daysOldestFirst = potentialDays.reversed()
            var graceDays = 0
            var consecutiveStudyDays = 0
            var streakStartIndex = -1

            // Find where the valid streak starts
            for (i in daysOldestFirst.indices) {
                val (_, wasStudied) = daysOldestFirst[i]

                if (!wasStudied) {
                    // Need grace to continue
                    if (graceDays > 0) {
                        graceDays--
                        consecutiveStudyDays = 0
                        if (streakStartIndex == -1) streakStartIndex = i
                    } else {
                        // Can't continue, reset
                        graceDays = 0
                        consecutiveStudyDays = 0
                        streakStartIndex = -1
                    }
                } else {
                    // Day studied
                    if (streakStartIndex == -1) streakStartIndex = i
                    consecutiveStudyDays++

                    // Earn grace every 3 consecutive study days
                    if (consecutiveStudyDays >= 3 && consecutiveStudyDays % 3 == 0) {
                        if (graceDays < 5) {
                            graceDays++
                        }
                    }
                }
            }

            val currentStreak =
                if (streakStartIndex == -1) {
                    0
                } else {
                    daysOldestFirst.size - streakStartIndex
                }

            return StreakInfo(
                currentStreak = currentStreak,
                graceDaysAvailable = graceDays,
                todayStatus = todayStatus,
            )
        }

        /**
         * Calculates study intensity relative to user's daily average.
         *
         * NONE: 0 cards
         * LEVEL_1: <= 0.5x average
         * LEVEL_2: <= 1.0x average
         * LEVEL_3: <= 1.5x average
         * LEVEL_4: <= 2.0x average
         * LEVEL_5: > 2.0x average
         *
         * If dailyAverage is 0 and cardsReviewed > 0, returns LEVEL_5.
         */
        fun calculateIntensity(
            cardsReviewed: Int,
            dailyAverage: Double,
        ): StudyIntensity {
            if (cardsReviewed == 0) {
                return StudyIntensity.NONE
            }

            if (dailyAverage == 0.0) {
                return StudyIntensity.LEVEL_5
            }

            val ratio = cardsReviewed / dailyAverage

            return when {
                ratio <= 0.5 -> StudyIntensity.LEVEL_1
                ratio <= 1.0 -> StudyIntensity.LEVEL_2
                ratio <= 1.5 -> StudyIntensity.LEVEL_3
                ratio <= 2.0 -> StudyIntensity.LEVEL_4
                else -> StudyIntensity.LEVEL_5
            }
        }

        /**
         * Computes average cards reviewed per day across studied days only.
         * Days with 0 cards are excluded from the average.
         * Returns 0.0 if no studied days exist.
         */
        fun computeDailyAverage(dailyData: Map<LocalDate, DailyStudyData>): Double {
            val studiedDays = dailyData.values.filter { it.cardsReviewed > 0 }
            if (studiedDays.isEmpty()) {
                return 0.0
            }
            return studiedDays.map { it.cardsReviewed }.average()
        }

        /**
         * Sums study time across all days in the map.
         */
        fun getStudyTimeForPeriod(dailyData: Map<LocalDate, DailyStudyData>): Long = dailyData.values.sumOf { it.studyTimeMs }
    }
}

/**
 * Single revlog entry with timestamp and review duration.
 */
data class RevlogEntry(
    val id: Long, // Review timestamp in ms (revlog.id)
    val timeMs: Int, // Time spent in ms (revlog.time)
)

/**
 * Daily study statistics for a single date.
 */
data class DailyStudyData(
    val date: LocalDate,
    val cardsReviewed: Int,
    val studyTimeMs: Long, // Capped at 30s per card
    val wasStudied: Boolean, // true if cardsReviewed >= 1
)

/**
 * Status of a day in the streak calculation.
 */
enum class DayStatus {
    STUDIED, // User studied on this day
    GRACE_USED, // User didn't study but grace day was consumed
    NOT_STUDIED, // User hasn't studied yet (typically today)
}

/**
 * Streak information including grace days.
 */
data class StreakInfo(
    val currentStreak: Int,
    val graceDaysAvailable: Int,
    val todayStatus: DayStatus,
)

/**
 * Study intensity levels relative to personal average.
 */
enum class StudyIntensity {
    NONE, // 0 cards
    LEVEL_1, // <= 0.5x average
    LEVEL_2, // <= 1.0x average
    LEVEL_3, // <= 1.5x average
    LEVEL_4, // <= 2.0x average
    LEVEL_5, // > 2.0x average
}
