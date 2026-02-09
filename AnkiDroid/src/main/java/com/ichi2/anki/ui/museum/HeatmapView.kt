package com.ichi2.anki.ui.museum

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Calendar

/**
 * Custom View that renders a study-activity heatmap (year / month / week).
 * Tap drills down: Year → Month → Week → (back to Year).
 * Activity data is keyed by "YYYY-MM-DD" strings mapped to card-review counts.
 */
class HeatmapView(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    enum class Granularity { YEAR, MONTH, WEEK }

    private var granularity = Granularity.YEAR
    private var activityData: Map<String, Int> = emptyMap()
    private var focusYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var focusMonth: Int = Calendar.getInstance().get(Calendar.MONTH)

    // Warm museum palette
    private val colorEmpty = Color.parseColor("#E8E0D8")
    private val colorLight = Color.parseColor("#F5C842") // 1-5 cards
    private val colorMedium = Color.parseColor("#D4A017") // 6-15 cards
    private val colorHeavy = Color.parseColor("#8B6914") // 16+ cards

    private val squarePaint =
        Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    private val labelPaint =
        Paint().apply {
            color = Color.parseColor("#6B5B4E")
            textSize = 10f * context.resources.displayMetrics.density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

    private var squareSize = 0f
    private var gap = 2f

    // Stores (x, y, dateKey) for each drawn square — used for touch hit-testing
    private val datePositions = mutableListOf<Triple<Float, Float, String>>()

    /** Callback invoked when user taps a specific day in week view. */
    var onDayTapped: ((String) -> Unit)? = null

    fun setActivityData(data: Map<String, Int>) {
        activityData = data
        invalidate()
    }

    fun setGranularity(g: Granularity) {
        granularity = g
        invalidate()
        requestLayout()
    }

    fun getGranularity(): Granularity = granularity

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height =
            when (granularity) {
                Granularity.YEAR -> (width * 0.48).toInt()
                Granularity.MONTH -> (width * 0.62).toInt()
                Granularity.WEEK -> (width * 0.22).toInt()
            }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        datePositions.clear()

        when (granularity) {
            Granularity.YEAR -> drawYearView(canvas)
            Granularity.MONTH -> drawMonthView(canvas)
            Granularity.WEEK -> drawWeekView(canvas)
        }
    }

    // ─── YEAR VIEW ────────────────────────────────────────────────────────────

    private fun drawYearView(canvas: Canvas) {
        val padding = 16f
        val labelHeight = 14f
        val availableWidth = width - padding * 2

        val jan1 = calendarForDay(focusYear, Calendar.JANUARY, 1)
        val dec31 = calendarForDay(focusYear, Calendar.DECEMBER, 31)

        // Monday-based day-of-week: 0=Mon … 6=Sun
        val startDow = mondayBasedDow(jan1)
        val totalDays = dec31.get(Calendar.DAY_OF_YEAR)
        val totalCols = (startDow + totalDays + 6) / 7

        // Size squares to fit width, cap at 12dp
        squareSize = minOf(12f, (availableWidth - gap * (totalCols - 1)) / totalCols)
        gap = if (totalCols > 1) (availableWidth - squareSize * totalCols) / (totalCols - 1) else 0f

        // Year label (centred, above grid)
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.isFakeBoldText = true
        canvas.drawText(focusYear.toString(), width / 2f, padding + 10f, labelPaint)
        labelPaint.isFakeBoldText = false

        val current = jan1.clone() as Calendar
        var col = 0
        var row = startDow
        var lastMonth = -1

        while (!current.after(dec31)) {
            val x = padding + col * (squareSize + gap)
            val y = padding + labelHeight + row * (squareSize + gap)

            // Short month label on first day ≤ 3
            val month = current.get(Calendar.MONTH)
            if (month != lastMonth && current.get(Calendar.DAY_OF_MONTH) <= 3) {
                lastMonth = month
                val labels = arrayOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                labelPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(labels[month], x, padding + labelHeight - 2f, labelPaint)
            }

            drawSquare(canvas, x, y, current.toDateKey())
            datePositions.add(Triple(x, y, current.toDateKey()))

            row++
            if (row >= 7) {
                row = 0
                col++
            }
            current.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    // ─── MONTH VIEW ───────────────────────────────────────────────────────────

    private fun drawMonthView(canvas: Canvas) {
        val padding = 16f
        val titleHeight = 24f
        val dayHeaderHeight = 18f
        val availableWidth = width - padding * 2

        squareSize = (availableWidth - gap * 6) / 7

        // Month + year title
        val monthNames =
            arrayOf(
                "January",
                "February",
                "March",
                "April",
                "May",
                "June",
                "July",
                "August",
                "September",
                "October",
                "November",
                "December",
            )
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.isFakeBoldText = true
        canvas.drawText("${monthNames[focusMonth]} $focusYear", width / 2f, padding + 14f, labelPaint)
        labelPaint.isFakeBoldText = false

        // Day-of-week headers
        val dayHeaders = arrayOf("M", "T", "W", "T", "F", "S", "S")
        for (i in 0..6) {
            val x = padding + i * (squareSize + gap) + squareSize / 2
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(dayHeaders[i], x, padding + titleHeight + 12f, labelPaint)
        }

        val firstDay = calendarForDay(focusYear, focusMonth, 1)
        val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startDow = mondayBasedDow(firstDay)

        val current = firstDay.clone() as Calendar
        var col = startDow
        var row = 0

        for (day in 1..daysInMonth) {
            val x = padding + col * (squareSize + gap)
            val y = padding + titleHeight + dayHeaderHeight + row * (squareSize + gap)

            drawSquare(canvas, x, y, current.toDateKey())
            datePositions.add(Triple(x, y, current.toDateKey()))

            col++
            if (col >= 7) {
                col = 0
                row++
            }
            current.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    // ─── WEEK VIEW ────────────────────────────────────────────────────────────

    private fun drawWeekView(canvas: Canvas) {
        val padding = 16f
        val titleHeight = 24f
        val dayHeaderHeight = 18f
        val availableWidth = width - padding * 2

        squareSize = (availableWidth - gap * 6) / 7

        // Title
        labelPaint.textAlign = Paint.Align.CENTER
        labelPaint.isFakeBoldText = true
        canvas.drawText("This Week", width / 2f, padding + 14f, labelPaint)
        labelPaint.isFakeBoldText = false

        // Day headers
        val dayHeaders = arrayOf("M", "T", "W", "T", "F", "S", "S")
        for (i in 0..6) {
            val x = padding + i * (squareSize + gap) + squareSize / 2
            labelPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(dayHeaders[i], x, padding + titleHeight + 12f, labelPaint)
        }

        // Find Monday of current week
        val today = Calendar.getInstance()
        val monday =
            (today.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -mondayBasedDow(this))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        val current = monday.clone() as Calendar
        for (i in 0..6) {
            val x = padding + i * (squareSize + gap)
            val y = padding + titleHeight + dayHeaderHeight

            drawSquare(canvas, x, y, current.toDateKey())
            datePositions.add(Triple(x, y, current.toDateKey()))
            current.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    // ─── SHARED HELPERS ───────────────────────────────────────────────────────

    private fun drawSquare(
        canvas: Canvas,
        x: Float,
        y: Float,
        dateKey: String,
    ) {
        val cards = activityData[dateKey] ?: 0
        squarePaint.color = colorForCards(cards)
        val r = squareSize * 0.2f
        canvas.drawRoundRect(RectF(x, y, x + squareSize, y + squareSize), r, r, squarePaint)
    }

    private fun colorForCards(cards: Int): Int =
        when {
            cards == 0 -> colorEmpty
            cards in 1..5 -> colorLight
            cards in 6..15 -> colorMedium
            else -> colorHeavy
        }

    /** Monday = 0, … Sunday = 6 */
    private fun mondayBasedDow(cal: Calendar): Int = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7

    private fun calendarForDay(
        year: Int,
        month: Int,
        day: Int,
    ): Calendar =
        Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

    // ─── TOUCH ────────────────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x
            val y = event.y

            for ((px, py, dateKey) in datePositions) {
                if (x in px..(px + squareSize) && y in py..(py + squareSize)) {
                    when (granularity) {
                        Granularity.YEAR -> {
                            focusMonth = dateKey.split("-")[1].toInt() - 1
                            setGranularity(Granularity.MONTH)
                        }
                        Granularity.MONTH -> setGranularity(Granularity.WEEK)
                        Granularity.WEEK -> onDayTapped?.invoke(dateKey)
                    }
                    return true
                }
            }

            // Tapped outside any square — drill back up
            when (granularity) {
                Granularity.MONTH -> setGranularity(Granularity.YEAR)
                Granularity.WEEK -> setGranularity(Granularity.MONTH)
                else -> {}
            }
            return true
        }
        return event.action == MotionEvent.ACTION_DOWN
    }
}

/** "YYYY-MM-DD" key for use as heatmap data key. */
fun Calendar.toDateKey(): String = String.format("%04d-%02d-%02d", get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH))
