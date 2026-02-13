package com.ichi2.anki.ui.museum

import android.os.Bundle
import android.view.View
import android.widget.ViewFlipper
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.ichi2.anki.R
import com.ichi2.anki.databinding.BottomsheetStreakBinding
import com.ichi2.anki.ui.museum.heatmap.MonthHeatmapView
import com.ichi2.anki.ui.museum.heatmap.WeekHeatmapView
import com.ichi2.anki.ui.museum.heatmap.YearHeatmapView
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

class StreakBottomSheet : BottomSheetDialogFragment(R.layout.bottomsheet_streak) {
    private val viewModel: MuseumViewModel by activityViewModels()
    private val binding by viewBinding(BottomsheetStreakBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.streakCount.text = getString(R.string.streak_days_format, state.streakDays)
                binding.totalStudyTime.text = formatStudyTime(state.totalStudyTimeMs)
                binding.graceDaysCount.text = getString(R.string.streak_grace_days_format, state.graceDaysAvailable)

                val dailyAverage = StudyTrackingRepository.computeDailyAverage(state.dailyStudyData)

                binding.weekHeatmap.setData(state.dailyStudyData, dailyAverage)
                binding.monthHeatmap.setData(state.dailyStudyData, dailyAverage)
                binding.yearHeatmap.setData(state.dailyStudyData, dailyAverage)

                updateStudyTimeLabels()
            }
        }
    }

    private fun setupTabs() {
        binding.heatmapTabs.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        binding.heatmapFlipper.displayedChild = it.position
                        updateStudyTimeLabels()
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            },
        )
    }

    private fun updateStudyTimeLabels() {
        val weekTime = binding.weekHeatmap.getWeekStudyTime()
        val monthTime = binding.monthHeatmap.getMonthStudyTime()
        val yearTime = binding.yearHeatmap.getYearStudyTime()

        binding.weekStudyTime.text = getString(R.string.heatmap_study_time_label, formatStudyTime(weekTime))
        binding.monthStudyTime.text = getString(R.string.heatmap_study_time_label, formatStudyTime(monthTime))
        binding.yearStudyTime.text = getString(R.string.heatmap_study_time_label, formatStudyTime(yearTime))
    }

    private fun formatStudyTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> getString(R.string.streak_time_hours_minutes, hours.toInt(), minutes.toInt())
            minutes > 0 -> getString(R.string.streak_time_minutes, minutes.toInt())
            totalSeconds > 0 -> getString(R.string.streak_time_seconds, seconds.toInt())
            else -> getString(R.string.streak_time_minutes, 0)
        }
    }

    companion object {
        const val TAG = "StreakBottomSheet"
    }
}
