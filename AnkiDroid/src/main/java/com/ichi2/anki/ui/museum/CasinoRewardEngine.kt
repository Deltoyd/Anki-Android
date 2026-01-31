package com.ichi2.anki.ui.museum

import kotlin.random.Random

enum class RewardTier { NONE, COMMON, UNCOMMON, RARE }

class CasinoRewardEngine {
    private var combo = 0

    fun onAnswer(): RewardTier {
        combo++
        val hitChance = (BASE_CHANCE + combo * COMBO_INCREMENT).coerceAtMost(MAX_CHANCE)
        if (Random.nextFloat() >= hitChance) {
            return RewardTier.NONE
        }
        return rollTier()
    }

    fun getCombo(): Int = combo

    private fun rollTier(): RewardTier {
        val roll = Random.nextFloat()
        return when {
            roll < COMMON_WEIGHT -> RewardTier.COMMON
            roll < COMMON_WEIGHT + UNCOMMON_WEIGHT -> RewardTier.UNCOMMON
            else -> RewardTier.RARE
        }
    }

    companion object {
        private const val BASE_CHANCE = 0.20f
        private const val COMBO_INCREMENT = 0.03f
        private const val MAX_CHANCE = 0.45f
        private const val COMMON_WEIGHT = 0.70f
        private const val UNCOMMON_WEIGHT = 0.25f
    }
}
