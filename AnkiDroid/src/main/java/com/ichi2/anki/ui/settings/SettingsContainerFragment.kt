package com.ichi2.anki.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ichi2.anki.R
import com.ichi2.anki.databinding.FragmentSettingsContainerBinding
import com.ichi2.anki.preferences.HeaderFragment
import dev.androidbroadcast.vbpd.viewBinding

class SettingsContainerFragment : Fragment(R.layout.fragment_settings_container) {
    private val binding by viewBinding(FragmentSettingsContainerBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.commit {
                replace(R.id.settingsContainer, HeaderFragment())
            }
        }
    }
}
