package com.ichi2.anki.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.MainContainerActivity
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.databinding.ActivityWelcomeBinding
import com.ichi2.anki.shouldFetchMedia
import com.ichi2.anki.syncAuth
import com.ichi2.anki.ui.museum.MuseumPersistence
import com.ichi2.anki.worker.SyncWorker
import timber.log.Timber

class WelcomeActivity : AnkiActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    private val loginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Timber.i("Login successful, syncing decks")
                triggerSyncAndProceed()
            } else {
                Timber.i("Login was not successful")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.quickStartButton.setOnClickListener {
            proceedToArtSelection()
        }

        binding.loginButton.setOnClickListener {
            val intent = AccountActivity.getIntent(this, forResult = true)
            loginLauncher.launch(intent)
        }
    }

    private fun triggerSyncAndProceed() {
        val auth = syncAuth()
        if (auth != null) {
            SyncWorker.start(this, auth, shouldFetchMedia())
        }
        proceedToArtSelection()
    }

    private fun proceedToArtSelection() {
        startActivity(Intent(this, ArtSelectionActivity::class.java))
        finish()
    }
}
