package com.ichi2.anki.ui.museum.heatmap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.ichi2.anki.R
import com.ichi2.anki.ui.museum.DailyStudyData
import com.ichi2.anki.ui.museum.StudyIntensity
import com.ichi2.anki.ui.museum.StudyTrackingRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class WeekHeatmapView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : View(context, attrs, defStyleAttr) {
        private val circlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

        private val ringPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = HeatmapColors.getCurrentDayRingColor(context)
            }

        private val textPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = 28f
                color = ContextCompat.getColor(context, R.color.museolingo_text_secondary)
            }

        private val circleRadius = 40f
        private val circleSpacing = 16f
        private val textPadding = 12f

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
            val totalWidth = (circleRadius * 2 * 7) + (circleSpacing * 6) + paddingLeft + paddingRight
            val totalHeight = (circleRadius * 2) + textPadding + textPaint.textSize + paddingTop + paddingBottom
            setMeasuredDimension(totalWidth.toInt(), totalHeight.toInt())
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val startOfWeek = today.with(DayOfWeek.MONDAY)
            val availableWidth = width - paddingLeft - paddingRight
            val circleSize = circleRadius * 2
            val totalCirclesWidth = (circleSize * 7) + (circleSpacing * 6)
            val startX = paddingLeft + (availableWidth - totalCirclesWidth) / 2 + circleRadius

            for (i in 0 until 7) {
                val date = startOfWeek.plusDays(i.toLong())
                val dayOfWeek = date.dayOfWeek
                val dayLabel = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())

                val cx = startX + (i * (circleSize + circleSpacing))
                val cy = paddingTop + circleRadius

                val dayData = dailyData[date]
                val cardsReviewed = dayData?.cardsReviewed ?: 0
                val intensity = StudyTrackingRepository.calculateIntensity(cardsReviewed, dailyAverage)

                circlePaint.color = HeatmapColors.getColorForIntensity(context, intensity)
                canvas.drawCircle(cx, cy, circleRadius, circlePaint)

                if (date == today) {
                    canvas.drawCircle(cx, cy, circleRadius - 2f, ringPaint)
                }

                val textY = cy + circleRadius + textPadding + textPaint.textSize
                canvas.drawText(dayLabel, cx, textY, textPaint)
            }
        }

        fun getWeekStudyTime(): Long {
            val startOfWeek = today.with(DayOfWeek.MONDAY)
            var totalTime = 0L
            for (i in 0 until 7) {
                val date = startOfWeek.plusDays(i.toLong())
                totalTime += dailyData[date]?.studyTimeMs ?: 0L
            }
            return totalTime
        }
    }
