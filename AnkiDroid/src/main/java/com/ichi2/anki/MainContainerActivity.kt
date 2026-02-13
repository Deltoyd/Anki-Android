package com.ichi2.anki

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.databinding.ActivityMainContainerBinding
import com.ichi2.anki.dialogs.AsyncDialogFragment
import com.ichi2.anki.dialogs.SyncErrorDialog
import com.ichi2.anki.dialogs.SyncErrorDialog.Companion.newInstance
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.ui.home.HomeFragment
import com.ichi2.anki.ui.library.LibraryFragment
import com.ichi2.anki.ui.museum.MuseumPersistence
import com.ichi2.anki.ui.museum.MuseumViewModel
import com.ichi2.anki.ui.onboarding.WelcomeActivity
import com.ichi2.anki.worker.SyncWorker
import com.ichi2.utils.NetworkUtils
import timber.log.Timber

class MainContainerActivity :
    AnkiActivity(),
    SyncErrorDialog.SyncErrorDialogListener {
    private lateinit var binding: ActivityMainContainerBinding

    private val museumViewModel: MuseumViewModel by viewModels()

    private var currentTabId: Int = R.id.nav_home
    private var syncOnResume = false

    private val loginForSyncLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                Timber.i("Login successful, will sync on resume")
                syncOnResume = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!MuseumPersistence.isOnboardingComplete(this)) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainContainerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupBackNavigation()

        if (savedInstanceState == null) {
            showFragment(HomeFragment(), R.id.nav_home)
            if (intent.getBooleanExtra(INTENT_SYNC_FROM_LOGIN, false)) {
                syncOnResume = true
            }
        } else {
            currentTabId = savedInstanceState.getInt("currentTabId", R.id.nav_home)
            binding.bottomNavigation.selectedItemId = currentTabId
        }
    }

    override fun onResume() {
        super.onResume()
        if (syncOnResume) {
            syncOnResume = false
            Timber.i("Performing sync on resume")
            sync()
        } else {
            automaticSync()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentTabId", currentTabId)
    }

    // region SyncErrorDialogListener

    override fun sync(conflict: ConflictResolution?) {
        val hkey = Prefs.hkey
        if (hkey.isNullOrEmpty()) {
            Timber.w("User not logged in")
            showSyncErrorDialog(SyncErrorDialog.Type.DIALOG_USER_NOT_LOGGED_IN_SYNC)
            return
        }

        val auth = syncAuth() ?: return
        if (!Prefs.allowSyncOnMeteredConnections && NetworkUtils.isActiveNetworkMetered()) {
            Timber.d("Sync blocked by metered connection, syncing anyway for simplicity")
        }
        SyncWorker.start(this, auth, shouldFetchMedia())
        Timber.i("Sync started via SyncWorker")
    }

    override fun loginToSyncServer() {
        val intent = AccountActivity.getIntent(this, forResult = true)
        loginForSyncLauncher.launch(intent)
    }

    override fun showSyncErrorDialog(dialogType: SyncErrorDialog.Type) {
        showSyncErrorDialog(dialogType, "")
    }

    override fun showSyncErrorDialog(
        dialogType: SyncErrorDialog.Type,
        message: String?,
    ) {
        val newFragment: AsyncDialogFragment = newInstance(dialogType, message)
        showAsyncDialogFragment(newFragment, Channel.SYNC)
    }

    override fun mediaCheck() {
        // Delegate to DeckPicker for now
        startActivity(Intent(this, DeckPicker::class.java))
    }

    override fun integrityCheck() {
        // Delegate to DeckPicker for now
        startActivity(Intent(this, DeckPicker::class.java))
    }

    // endregion

    private fun automaticSync() {
        if (!Prefs.isAutoSyncEnabled) {
            Timber.d("autoSync: not enabled")
            return
        }
        if (!isLoggedIn()) {
            Timber.d("autoSync: not logged in")
            return
        }
        if (!NetworkUtils.isOnline) {
            Timber.d("autoSync: offline")
            return
        }
        if (!Prefs.allowSyncOnMeteredConnections && NetworkUtils.isActiveNetworkMetered()) {
            Timber.d("autoSync: blocked by metered connection")
            return
        }
        val automaticSyncIntervalInMS = AUTOMATIC_SYNC_MINIMAL_INTERVAL_IN_MINUTES * 60 * 1000
        if (millisecondsSinceLastSync() < automaticSyncIntervalInMS) {
            Timber.d("autoSync: interval not passed")
            return
        }

        Timber.i("autoSync: starting background sync")
        val auth = syncAuth() ?: return
        SyncWorker.start(this, auth, shouldFetchMedia())
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

    companion object {
        const val INTENT_SYNC_FROM_LOGIN = "syncFromLogin"
        private const val AUTOMATIC_SYNC_MINIMAL_INTERVAL_IN_MINUTES: Long = 10
    }
}
