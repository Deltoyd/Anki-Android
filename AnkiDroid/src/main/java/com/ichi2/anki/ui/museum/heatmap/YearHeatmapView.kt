package com.ichi2.anki.ui.museum.heatmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.ichi2.anki.ui.museum.DailyStudyData
import com.ichi2.anki.ui.museum.StudyTrackingRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class YearHeatmapView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        private val dotPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

        private val ringPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = HeatmapColors.getCurrentDayRingColor(context)
            }

        private val dotSize = 10f
        private val dotSpacing = 3f
        private val rows = 7

        private var dailyData: Map<LocalDate, DailyStudyData> = emptyMap()
        private var dailyAverage: Double = 0.0
        private var today: LocalDate = LocalDate.now()

        fun setData(
            data: Map<LocalDate, DailyStudyData>,
            average: Double,
        ) {
            dailyData = data
            dailyAverage = average
            today = LocalDate.now()
            invalidate()
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val columns = 53
            val totalWidth = (dotSize * columns) + (dotSpacing * (columns - 1)) + paddingLeft + paddingRight
            val totalHeight = (dotSize * rows) + (dotSpacing * (rows - 1)) + paddingTop + paddingBottom
            setMeasuredDimension(totalWidth.toInt(), totalHeight.toInt())
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val yearAgo = today.minusDays(364)
            val startOfGrid = yearAgo.with(DayOfWeek.MONDAY)

            val availableWidth = width - paddingLeft - paddingRight
            val columns = 53
            val totalDotsWidth = (dotSize * columns) + (dotSpacing * (columns - 1))
            val startX = paddingLeft + (availableWidth - totalDotsWidth) / 2

            for (col in 0 until columns) {
                for (row in 0 until rows) {
                    val daysFromStart = col * 7 + row
                    val date = startOfGrid.plusDays(daysFromStart.toLong())

                    if (date.isAfter(today)) continue

                    val cx = startX + (dotSize / 2) + (col * (dotSize + dotSpacing))
                    val cy = paddingTop + (dotSize / 2) + (row * (dotSize + dotSpacing))

                    val dayData = dailyData[date]
                    val cardsReviewed = dayData?.cardsReviewed ?: 0
                    val intensity = StudyTrackingRepository.calculateIntensity(cardsReviewed, dailyAverage)

                    dotPaint.color = HeatmapColors.getColorForIntensity(context, intensity)
                    canvas.drawRoundRect(
                        cx - dotSize / 2,
                        cy - dotSize / 2,
                        cx + dotSize / 2,
                        cy + dotSize / 2,
                        2f,
                        2f,
                        dotPaint,
                    )

                    if (date == today) {
                        canvas.drawRoundRect(
                            cx - dotSize / 2 + 1,
                            cy - dotSize / 2 + 1,
                            cx + dotSize / 2 - 1,
                            cy + dotSize / 2 - 1,
                            2f,
                            2f,
                            ringPaint,
                        )
                    }
                }
            }
        }

        fun getYearStudyTime(): Long {
            val yearAgo = today.minusDays(364)
            var totalTime = 0L
            var date = yearAgo
            while (!date.isAfter(today)) {
                totalTime += dailyData[date]?.studyTimeMs ?: 0L
                date = date.plusDays(1)
            }
            return totalTime
        }
    }
