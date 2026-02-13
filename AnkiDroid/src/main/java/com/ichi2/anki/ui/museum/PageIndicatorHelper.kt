package com.ichi2.anki.ui.museum

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout

object PageIndicatorHelper {
    private const val DOT_SIZE_DP = 6
    private const val DOT_SPACING_DP = 4

    private const val COLOR_AMBER = 0xFFF4A21D.toInt() // Active page
    private const val COLOR_GRAY_FILLED = 0xFFBDBDBD.toInt() // Locked
    private const val COLOR_GRAY_HOLLOW_STROKE = 0xFF9E9E9E.toInt()

    fun setupIndicators(
        container: LinearLayout,
        items: List<GalleryArtItem>,
        currentPage: Int,
    ) {
        container.removeAllViews()
        val context = container.context
        val dotSize = dpToPx(context, DOT_SIZE_DP)
        val dotSpacing = dpToPx(context, DOT_SPACING_DP)

        items.forEachIndexed { index, item ->
            val dot = View(context)
            val params = LinearLayout.LayoutParams(dotSize, dotSize)
            params.setMargins(dotSpacing, 0, dotSpacing, 0)
            params.gravity = Gravity.CENTER_VERTICAL
            dot.layoutParams = params

            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL

            if (index == currentPage) {
                // Current page always amber
                drawable.setColor(COLOR_AMBER)
            } else {
                when (item.state) {
                    ArtPieceState.ACTIVE -> drawable.setColor(COLOR_AMBER)
                    ArtPieceState.LOCKED -> {
                        drawable.setColor(0x00000000) // transparent fill
                        drawable.setStroke(dpToPx(context, 1), COLOR_GRAY_HOLLOW_STROKE)
                    }
                    ArtPieceState.COMPLETED -> {
                        // Dead code - completed items are filtered out
                        drawable.setColor(COLOR_AMBER)
                    }
                }
            }

            dot.background = drawable
            container.addView(dot)
        }
    }

    fun updateCurrentPage(
        container: LinearLayout,
        items: List<GalleryArtItem>,
        currentPage: Int,
    ) {
        if (container.childCount != items.size) {
            setupIndicators(container, items, currentPage)
            return
        }

        items.forEachIndexed { index, item ->
            val dot = container.getChildAt(index) ?: return@forEachIndexed
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.OVAL

            if (index == currentPage) {
                drawable.setColor(COLOR_AMBER)
            } else {
                when (item.state) {
                    ArtPieceState.ACTIVE -> drawable.setColor(COLOR_AMBER)
                    ArtPieceState.LOCKED -> {
                        drawable.setColor(0x00000000)
                        drawable.setStroke(
                            dpToPx(container.context, 1),
                            COLOR_GRAY_HOLLOW_STROKE,
                        )
                    }
                    ArtPieceState.COMPLETED -> {
                        // Dead code - completed items are filtered out
                        drawable.setColor(COLOR_AMBER)
                    }
                }
            }

            dot.background = drawable
        }
    }

    private fun dpToPx(
        context: Context,
        dp: Int,
    ): Int = (dp * context.resources.displayMetrics.density).toInt()
}
