package com.ichi2.anki.ui.museum.heatmap

import android.content.Context
import androidx.core.content.ContextCompat
import com.ichi2.anki.R
import com.ichi2.anki.ui.museum.StudyIntensity

object HeatmapColors {
    fun getColorForIntensity(
        context: Context,
        intensity: StudyIntensity,
    ): Int =
        when (intensity) {
            StudyIntensity.NONE -> ContextCompat.getColor(context, R.color.heatmap_intensity_none)
            StudyIntensity.LEVEL_1 -> ContextCompat.getColor(context, R.color.heatmap_intensity_1)
            StudyIntensity.LEVEL_2 -> ContextCompat.getColor(context, R.color.heatmap_intensity_2)
            StudyIntensity.LEVEL_3 -> ContextCompat.getColor(context, R.color.heatmap_intensity_3)
            StudyIntensity.LEVEL_4 -> ContextCompat.getColor(context, R.color.heatmap_intensity_4)
            StudyIntensity.LEVEL_5 -> ContextCompat.getColor(context, R.color.heatmap_intensity_5)
        }

    fun getCurrentDayRingColor(context: Context): Int = ContextCompat.getColor(context, R.color.heatmap_current_day_ring)
}
