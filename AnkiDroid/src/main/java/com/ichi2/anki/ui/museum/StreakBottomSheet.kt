package com.ichi2.anki.ui.museum

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ichi2.anki.R
import com.ichi2.anki.databinding.BottomsheetStreakBinding
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

/**
 * Bottom sheet displaying the user's study streak information.
 * Shows current streak count, total study time, and grace days remaining.
 */
class StreakBottomSheet : BottomSheetDialogFragment(R.layout.bottomsheet_streak) {
    private val viewModel: MuseumViewModel by activityViewModels()
    private val binding by viewBinding(BottomsheetStreakBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.streakCount.text = getString(R.string.streak_days_format, state.streakDays)
                binding.totalStudyTime.text = formatStudyTime(state.totalStudyTimeMs)
                binding.graceDaysCount.text = getString(R.string.streak_grace_days_format, state.graceDaysAvailable)
            }
        }
    }

    /**
     * Formats milliseconds into a human-readable time string.
     * - >= 60 min: "Xh Ym"
     * - >= 60 sec: "Xm"
     * - < 60 sec: "Xs"
     * - 0ms: "0m"
     */
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
