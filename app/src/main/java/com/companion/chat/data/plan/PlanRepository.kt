package com.companion.chat.data.plan

data class PlanState(
    val planName: String = "Local",
    val premiumVoiceEnabled: Boolean = false,
    val cloudFeaturesEnabled: Boolean = false,
    val renewalLabel: String = "No renewal"
)

class PlanRepository {

    fun getPlanState(): PlanState {
        return PlanState()
    }
}
