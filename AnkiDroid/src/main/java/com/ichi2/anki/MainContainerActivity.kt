package com.ichi2.anki

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ichi2.anki.databinding.ActivityMainContainerBinding
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.ui.home.HomeFragment
import com.ichi2.anki.ui.library.LibraryFragment
import com.ichi2.anki.ui.museum.MuseumPersistence
import com.ichi2.anki.ui.museum.MuseumViewModel
import com.ichi2.anki.ui.onboarding.TopicSelectionActivity

class MainContainerActivity : AnkiActivity() {
    private lateinit var binding: ActivityMainContainerBinding

    private val museumViewModel: MuseumViewModel by viewModels()

    private var currentTabId: Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!MuseumPersistence.isOnboardingComplete(this)) {
            startActivity(Intent(this, TopicSelectionActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupBackNavigation()

        if (savedInstanceState == null) {
            showFragment(HomeFragment(), R.id.nav_home)
        } else {
            currentTabId = savedInstanceState.getInt("currentTabId", R.id.nav_home)
            binding.bottomNavigation.selectedItemId = currentTabId
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentTabId", currentTabId)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == currentTabId) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_home -> showFragment(HomeFragment(), item.itemId)
                R.id.nav_library -> showFragment(LibraryFragment(), item.itemId)
                R.id.nav_settings -> {
                    startActivity(PreferencesActivity.getIntent(this))
                    // Don't update currentTabId — stay on previous tab when returning
                    return@setOnItemSelectedListener false
                }
                else -> return@setOnItemSelectedListener false
            }
            true
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (currentTabId != R.id.nav_home) {
                        binding.bottomNavigation.selectedItemId = R.id.nav_home
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )
    }

    private fun showFragment(
        fragment: Fragment,
        tabId: Int,
    ) {
        currentTabId = tabId
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.fragmentContainer, fragment)
        }
    }
}
