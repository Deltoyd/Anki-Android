package com.ichi2.anki.ui.museum

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for StudyTrackingRepository.
 * Tests all streak logic including grace day accumulation/consumption,
 * daily aggregation with 30s cap, and intensity levels.
 */
class StudyTrackingRepositoryTest {
    // ===== getDailyStudyData tests =====

    @Test
    fun `getDailyStudyData with empty revlog returns all zero days`() {
        val entries = emptyList<RevlogEntry>()
        val fromDate = LocalDate.of(2026, 2, 10)
        val toDate = LocalDate.of(2026, 2, 12)

        val result = StudyTrackingRepository.getDailyStudyData(entries, fromDate, toDate)

        assertEquals(3, result.size)
        assertEquals(0, result[LocalDate.of(2026, 2, 10)]?.cardsReviewed)
        assertEquals(0L, result[LocalDate.of(2026, 2, 10)]?.studyTimeMs)
        assertEquals(false, result[LocalDate.of(2026, 2, 10)]?.wasStudied)
    }

    @Test
    fun `getDailyStudyData with single review`() {
        val entries =
            listOf(
                RevlogEntry(
                    id = LocalDate.of(2026, 2, 10).atStartOfDay().toEpochSecond(java.time.ZoneOffset.ofHours(-8)) * 1000 + 3600000,
                    timeMs = 5000,
                ),
            )
        val fromDate = LocalDate.of(2026, 2, 10)
        val toDate = LocalDate.of(2026, 2, 10)

        val result = StudyTrackingRepository.getDailyStudyData(entries, fromDate, toDate)

        assertEquals(1, result.size)
        val dayData = result[LocalDate.of(2026, 2, 10)]!!
        assertEquals(1, dayData.cardsReviewed)
        assertEquals(5000L, dayData.studyTimeMs)
        assertEquals(true, dayData.wasStudied)
    }

    @Test
    fun `getDailyStudyData caps individual card time at 30 seconds`() {
        val entries =
            listOf(
                RevlogEntry(
                    id = LocalDate.of(2026, 2, 10).atStartOfDay().toEpochSecond(java.time.ZoneOffset.ofHours(-8)) * 1000,
                    timeMs = 45000, // 45 seconds, should be capped to 30000
                ),
                RevlogEntry(
                    id = LocalDate.of(2026, 2, 10).atStartOfDay().toEpochSecond(java.time.ZoneOffset.ofHours(-8)) * 1000 + 60000,
                    timeMs = 15000, // 15 seconds, under cap
                ),
            )
        val fromDate = LocalDate.of(2026, 2, 10)
        val toDate = LocalDate.of(2026, 2, 10)

        val result = StudyTrackingRepository.getDailyStudyData(entries, fromDate, toDate)

        val dayData = result[LocalDate.of(2026, 2, 10)]!!
        assertEquals(2, dayData.cardsReviewed)
        assertEquals(30000L + 15000L, dayData.studyTimeMs) // 45s capped to 30s + 15s
    }

    @Test
    fun `getDailyStudyData groups by date across multiple days`() {
        val entries =
            listOf(
                RevlogEntry(
                    id = LocalDate.of(2026, 2, 10).atStartOfDay().toEpochSecond(java.time.ZoneOffset.ofHours(-8)) * 1000,
                    timeMs = 5000,
                ),
                RevlogEntry(
                    id = LocalDate.of(2026, 2, 11).atStartOfDay().toEpochSecond(java.time.ZoneOffset.ofHours(-8)) * 1000,
                    timeMs = 10000,
                ),
                RevlogEntry(
                    id = LocalDate.of(2026, 2, 11).atStartOfDay().toEpochSecond(java.time.ZoneOffset.ofHours(-8)) * 1000 + 3600000,
                    timeMs = 8000,
                ),
            )
        val fromDate = LocalDate.of(2026, 2, 10)
        val toDate = LocalDate.of(2026, 2, 12)

        val result = StudyTrackingRepository.getDailyStudyData(entries, fromDate, toDate)

        assertEquals(3, result.size)
        assertEquals(1, result[LocalDate.of(2026, 2, 10)]?.cardsReviewed)
        assertEquals(5000L, result[LocalDate.of(2026, 2, 10)]?.studyTimeMs)
        assertEquals(2, result[LocalDate.of(2026, 2, 11)]?.cardsReviewed)
        assertEquals(18000L, result[LocalDate.of(2026, 2, 11)]?.studyTimeMs)
        assertEquals(0, result[LocalDate.of(2026, 2, 12)]?.cardsReviewed)
        assertEquals(false, result[LocalDate.of(2026, 2, 12)]?.wasStudied)
    }

    // ===== calculateStreak tests =====

    @Test
    fun `calculateStreak with no study history returns zero streak`() {
        val dailyData =
            mapOf(
                LocalDate.of(2026, 2, 10) to DailyStudyData(LocalDate.of(2026, 2, 10), 0, 0L, false),
            )
        val today = LocalDate.of(2026, 2, 10)

        val result = StudyTrackingRepository.calculateStreak(dailyData, today)

        assertEquals(0, result.currentStreak)
        assertEquals(0, result.graceDaysAvailable)
        assertEquals(DayStatus.NOT_STUDIED, result.todayStatus)
    }

    @Test
    fun `calculateStreak with one day studied`() {
        val dailyData =
            mapOf(
                LocalDate.of(2026, 2, 10) to DailyStudyData(LocalDate.of(2026, 2, 10), 1, 5000L, true),
            )
        val today = LocalDate.of(2026, 2, 10)

        val result = StudyTrackingRepository.calculateStreak(dailyData, today)

        assertEquals(1, result.currentStreak)
        assertEquals(0, result.graceDaysAvailable)
        assertEquals(DayStatus.STUDIED, result.todayStatus)
    }

    @Test
    fun `calculateStreak with 3 consecutive days earns 1 grace day`() {
        val dailyData =
            mapOf(
                LocalDate.of(2026, 2, 8) to DailyStudyData(LocalDate.of(2026, 2, 8), 5, 10000L, true),
                LocalDate.of(2026, 2, 9) to DailyStudyData(LocalDate.of(2026, 2, 9), 3, 8000L, true),
                LocalDate.of(2026, 2, 10) to DailyStudyData(LocalDate.of(2026, 2, 10), 4, 12000L, true),
            )
        val today = LocalDate.of(2026, 2, 10)

        val result = StudyTrackingRepository.calculateStreak(dailyData, today)

        assertEquals(3, result.currentStreak)
        assertEquals(1, result.graceDaysAvailable)
        assertEquals(DayStatus.STUDIED, result.todayStatus)
    }

    @Test
    fun `calculateStreak with missed day and grace available consumes grace and continues streak`() {
        val dailyData =
            mapOf(
                LocalDate.of(2026, 2, 7) to DailyStudyData(LocalDate.of(2026, 2, 7), 5, 10000L, true),
                LocalDate.of(2026, 2, 8) to DailyStudyData(LocalDate.of(2026, 2, 8), 3, 8000L, true),
                LocalDate.of(2026, 2, 9) to DailyStudyData(LocalDate.of(2026, 2, 9), 4, 12000L, true),
                LocalDate.of(2026, 2, 10) to DailyStudyData(LocalDate.of(2026, 2, 10), 0, 0L, false), // Missed
                LocalDate.of(2026, 2, 11) to DailyStudyData(LocalDate.of(2026, 2, 11), 6, 15000L, true),
            )
        val today = LocalDate.of(2026, 2, 11)

        val result = StudyTrackingRepository.calculateStreak(dailyData, today)

        assertEquals(5, result.currentStreak)
        assertEquals(0, result.graceDaysAvailable) // 1 earned, 1 consumed
        assertEquals(DayStatus.STUDIED, result.todayStatus)
    }

    @Test
    fun `calculateStreak with missed day and no grace breaks streak`() {
        val dailyData =
            mapOf(
                LocalDate.of(2026, 2, 8) to DailyStudyData(LocalDate.of(2026, 2, 8), 5, 10000L, true),
                LocalDate.of(2026, 2, 9) to DailyStudyData(LocalDate.of(2026, 2, 9), 0, 0L, false), // Missed
                LocalDate.of(2026, 2, 10) to DailyStudyData(LocalDate.of(2026, 2, 10), 3, 8000L, true),
            )
        val today = LocalDate.of(2026, 2, 10)

        val result = StudyTrackingRepository.calculateStreak(dailyData, today)

        assertEquals(1, result.currentStreak)
        assertEquals(0, result.graceDaysAvailable)
        assertEquals(DayStatus.STUDIED, result.todayStatus)
    }

    @Test
    fun `calculateStreak caps grace days at 5`() {
        val dailyData = mutableMapOf<LocalDate, DailyStudyData>()
        val startDate = LocalDate.of(2026, 2, 1)
        // Create 18 consecutive days of study (should earn 6 grace days, but cap at 5)
        for (i in 0 until 18) {
            val date = startDate.plusDays(i.toLong())
            dailyData[date] = DailyStudyData(date, 5, 10000L, true)
        }
        val today = startDate.plusDays(17)

        val result = StudyTrackingRepository.calculateStreak(dailyData, today)

        assertEquals(18, result.currentStreak)
        assertEquals(5, result.graceDaysAvailable) // Capped at 5, not 6
        assertEquals(DayStatus.STUDIED, result.todayStatus)
    }

    @Test
    fun `calculateStreak with today not studied shows NOT_STUDIED status and counts streak up to yesterday`() {
        val dailyData =
            mapOf(
                LocalDate.of(2026, 2, 8) to DailyStudyData(LocalDate.of(2026, 2, 8), 5, 10000L, true),
                LocalDate.of(2026, 2, 9) to DailyStudyData(LocalDate.of(2026, 2, 9), 3, 8000L, true),
                LocalDate.of(2026, 2, 10) to DailyStudyData(LocalDate.of(2026, 2, 10), 0, 0L, false),
            )
        val today = LocalDate.of(2026, 2, 10)

        val result = StudyTrackingRepository.calculateStreak(dailyData, today)

        assertEquals(2, result.currentStreak) // Counts yesterday's streak
        assertEquals(0, result.graceDaysAvailable)
        assertEquals(DayStatus.NOT_STUDIED, result.todayStatus)
    }

    // ===== calculateIntensity tests =====

    @Test
    fun `calculateIntensity returns NONE for zero cards`() {
        val result = StudyTrackingRepository.calculateIntensity(0, 50.0)
        assertEquals(StudyIntensity.NONE, result)
    }

    @Test
    fun `calculateIntensity returns LEVEL_1 for below half average`() {
        val result = StudyTrackingRepository.calculateIntensity(20, 50.0)
        assertEquals(StudyIntensity.LEVEL_1, result)
    }

    @Test
    fun `calculateIntensity returns LEVEL_2 for up to average`() {
        val result = StudyTrackingRepository.calculateIntensity(50, 50.0)
        assertEquals(StudyIntensity.LEVEL_2, result)
    }

    @Test
    fun `calculateIntensity returns LEVEL_3 for up to 1_5x average`() {
        val result = StudyTrackingRepository.calculateIntensity(75, 50.0)
        assertEquals(StudyIntensity.LEVEL_3, result)
    }

    @Test
    fun `calculateIntensity returns LEVEL_4 for up to 2x average`() {
        val result = StudyTrackingRepository.calculateIntensity(100, 50.0)
        assertEquals(StudyIntensity.LEVEL_4, result)
    }

    @Test
    fun `calculateIntensity returns LEVEL_5 for above 2x average`() {
        val result = StudyTrackingRepository.calculateIntensity(150, 50.0)
        assertEquals(StudyIntensity.LEVEL_5, result)
    }

    @Test
    fun `calculateIntensity returns LEVEL_5 for any study when no history`() {
        val result = StudyTrackingRepository.calculateIntensity(1, 0.0)
        assertEquals(StudyIntensity.LEVEL_5, result)
    }

    // ===== computeDailyAverage tests =====

    @Test
    fun `computeDailyAverage returns 0 for empty data`() {
        val dailyData = emptyMap<LocalDate, DailyStudyData>()
        val result = StudyTrackingRepository.computeDailyAverage(dailyData)
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `computeDailyAverage calculates average across studied days only`() {
        val dailyData =
            mapOf(
                LocalDate.of(2026, 2, 8) to DailyStudyData(LocalDate.of(2026, 2, 8), 10, 10000L, true),
                LocalDate.of(2026, 2, 9) to DailyStudyData(LocalDate.of(2026, 2, 9), 0, 0L, false),
                LocalDate.of(2026, 2, 10) to DailyStudyData(LocalDate.of(2026, 2, 10), 20, 20000L, true),
                LocalDate.of(2026, 2, 11) to DailyStudyData(LocalDate.of(2026, 2, 11), 30, 30000L, true),
            )
        val result = StudyTrackingRepository.computeDailyAverage(dailyData)
        assertEquals(20.0, result, 0.001) // (10 + 20 + 30) / 3 = 20.0
    }

    @Test
    fun `computeDailyAverage returns 0 when no studied days exist`() {
        val dailyData =
            mapOf(
                LocalDate.of(2026, 2, 8) to DailyStudyData(LocalDate.of(2026, 2, 8), 0, 0L, false),
                LocalDate.of(2026, 2, 9) to DailyStudyData(LocalDate.of(2026, 2, 9), 0, 0L, false),
            )
        val result = StudyTrackingRepository.computeDailyAverage(dailyData)
        assertEquals(0.0, result, 0.001)
    }

    // ===== getStudyTimeForPeriod tests =====

    @Test
    fun `getStudyTimeForPeriod sums all study time`() {
        val dailyData =
            mapOf(
                LocalDate.of(2026, 2, 8) to DailyStudyData(LocalDate.of(2026, 2, 8), 10, 10000L, true),
                LocalDate.of(2026, 2, 9) to DailyStudyData(LocalDate.of(2026, 2, 9), 0, 0L, false),
                LocalDate.of(2026, 2, 10) to DailyStudyData(LocalDate.of(2026, 2, 10), 20, 45000L, true),
            )
        val result = StudyTrackingRepository.getStudyTimeForPeriod(dailyData)
        assertEquals(55000L, result)
    }

    @Test
    fun `getStudyTimeForPeriod returns 0 for empty data`() {
        val dailyData = emptyMap<LocalDate, DailyStudyData>()
        val result = StudyTrackingRepository.getStudyTimeForPeriod(dailyData)
        assertEquals(0L, result)
    }
}
