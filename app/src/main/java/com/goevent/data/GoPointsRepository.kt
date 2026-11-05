package com.goevent.data

import com.goevent.core.model.Achievement
import com.goevent.core.model.GoPointsHistoryItem
import com.goevent.core.model.Reward
import com.goevent.core.model.RewardRedemption
import com.goevent.core.model.UserAchievement
import com.goevent.core.util.safeCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class GoPointsRepository {

    private val client = SupabaseClientManager.client

    suspend fun fetchPointsHistory(userId: String): Result<List<GoPointsHistoryItem>> = withContext(Dispatchers.IO) {
        safeCall {
            client.postgrest["gopoints_history"]
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<GoPointsHistoryItem>()
        }
    }

    suspend fun fetchRewards(): Result<List<Reward>> = withContext(Dispatchers.IO) {
        safeCall {
            client.postgrest["rewards"]
                .select {
                    filter { eq("is_active", true) }
                    order("points_cost", Order.ASCENDING)
                }
                .decodeList<Reward>()
        }
    }

    suspend fun fetchRedemptions(userId: String): Result<List<RewardRedemption>> = withContext(Dispatchers.IO) {
        safeCall {
            client.postgrest["reward_redemptions"]
                .select {
                    filter { eq("user_id", userId) }
                    order("redeemed_at", Order.DESCENDING)
                }
                .decodeList<RewardRedemption>()
        }
    }

    suspend fun redeemReward(rewardId: String): Result<RewardRedemptionResult> = withContext(Dispatchers.IO) {
        safeCall {
            val params = buildJsonObject { put("p_reward_id", rewardId) }
            val json = Json.parseToJsonElement(client.postgrest.rpc("redeem_reward", params).data).jsonObject
            RewardRedemptionResult(
                redemptionId = json["redemption_id"]?.jsonPrimitive?.content.orEmpty(),
                redemptionCode = json["redemption_code"]?.jsonPrimitive?.content.orEmpty(),
                title = json["title"]?.jsonPrimitive?.content.orEmpty(),
                pointsSpent = json["points_spent"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            )
        }
    }

    suspend fun fetchAchievements(): Result<List<Achievement>> = withContext(Dispatchers.IO) {
        safeCall {
            client.postgrest["achievements"].select().decodeList<Achievement>()
        }
    }

    suspend fun fetchUserAchievements(userId: String): Result<List<UserAchievement>> = withContext(Dispatchers.IO) {
        safeCall {
            client.postgrest["user_achievements"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserAchievement>()
        }
    }

    /** Recalculates achievement progress on the server (and awards points once). Returns how many were newly unlocked. */
    suspend fun refreshAchievements(): Result<Int> = withContext(Dispatchers.IO) {
        safeCall {
            val json = Json.parseToJsonElement(
                client.postgrest.rpc("refresh_achievements", buildJsonObject { }).data
            ).jsonObject
            json["new_unlocks"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        }
    }
}

data class RewardRedemptionResult(
    val redemptionId: String,
    val redemptionCode: String,
    val title: String,
    val pointsSpent: Int
)