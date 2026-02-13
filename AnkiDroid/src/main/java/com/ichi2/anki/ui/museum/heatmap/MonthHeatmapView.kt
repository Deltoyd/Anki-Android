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
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class MonthHeatmapView
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
                strokeWidth = 3f
                color = HeatmapColors.getCurrentDayRingColor(context)
            }

        private val headerPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = 24f
                color = ContextCompat.getColor(context, R.color.museolingo_text_secondary)
            }

        private val adjacentMonthPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                alpha = 80
            }

        private val cellSize = 32f
        private val cellSpacing = 8f
        private val headerHeight = 36f

        private var dailyData: Map<LocalDate, DailyStudyData> = emptyMap()
        private var dailyAverage: Double = 0.0
        private var today: LocalDate = LocalDate.now()
        private var currentMonth: YearMonth = YearMonth.now()

        fun setData(
            data: Map<LocalDate, DailyStudyData>,
            average: Double,
        ) {
            dailyData = data
            dailyAverage = average
            today = LocalDate.now()
            currentMonth = YearMonth.now()
            invalidate()
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val totalWidth = (cellSize * 7) + (cellSpacing * 6) + paddingLeft + paddingRight
            val rows = 6
            val totalHeight = headerHeight + (cellSize * rows) + (cellSpacing * (rows - 1)) + paddingTop + paddingBottom
            setMeasuredDimension(totalWidth.toInt(), totalHeight.toInt())
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val availableWidth = width - paddingLeft - paddingRight
            val totalCellsWidth = (cellSize * 7) + (cellSpacing * 6)
            val startX = paddingLeft + (availableWidth - totalCellsWidth) / 2

            for (i in 0 until 7) {
                val dayOfWeek =
                    DayOfWeek.of(
                        if (i == 0) {
                            1
                        } else if (i == 6) {
                            7
                        } else {
                            i + 1
                        },
                    )
                val label = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                val cx = startX + (cellSize / 2) + (i * (cellSize + cellSpacing))
                canvas.drawText(label, cx, paddingTop + headerPaint.textSize, headerPaint)
            }

            val firstOfMonth = currentMonth.atDay(1)
            val firstDayOffset = (firstOfMonth.dayOfWeek.value - 1)
            val daysInMonth = currentMonth.lengthOfMonth()

            val gridStartY = paddingTop + headerHeight

            for (row in 0 until 6) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - firstDayOffset + 1

                    val cx = startX + (cellSize / 2) + (col * (cellSize + cellSpacing))
                    val cy = gridStartY + (cellSize / 2) + (row * (cellSize + cellSpacing))

                    val date: LocalDate?
                    val isCurrentMonth: Boolean

                    when {
                        dayNumber < 1 -> {
                            val prevMonth = currentMonth.minusMonths(1)
                            val prevMonthDay = prevMonth.lengthOfMonth() + dayNumber
                            date = prevMonth.atDay(prevMonthDay)
                            isCurrentMonth = false
                        }
                        dayNumber > daysInMonth -> {
                            val nextMonthDay = dayNumber - daysInMonth
                            date = currentMonth.plusMonths(1).atDay(nextMonthDay)
                            isCurrentMonth = false
                        }
                        else -> {
                            date = currentMonth.atDay(dayNumber)
                            isCurrentMonth = true
                        }
                    }

                    val dayData = dailyData[date]
                    val cardsReviewed = dayData?.cardsReviewed ?: 0
                    val intensity = StudyTrackingRepository.calculateIntensity(cardsReviewed, dailyAverage)

                    if (isCurrentMonth) {
                        circlePaint.color = HeatmapColors.getColorForIntensity(context, intensity)
                        circlePaint.alpha = 255
                    } else {
                        adjacentMonthPaint.color = HeatmapColors.getColorForIntensity(context, intensity)
                        canvas.drawCircle(cx, cy, cellSize / 2 - 2, adjacentMonthPaint)
                        continue
                    }

                    canvas.drawCircle(cx, cy, cellSize / 2 - 2, circlePaint)

                    if (date == today) {
                        canvas.drawCircle(cx, cy, cellSize / 2 - 4, ringPaint)
                    }
                }
            }
        }

        fun getMonthStudyTime(): Long {
            val firstOfMonth = currentMonth.atDay(1)
            val lastOfMonth = currentMonth.atEndOfMonth()
            var totalTime = 0L
            var date = firstOfMonth
            while (!date.isAfter(lastOfMonth)) {
                totalTime += dailyData[date]?.studyTimeMs ?: 0L
                date = date.plusDays(1)
            }
            return totalTime
        }
    }
