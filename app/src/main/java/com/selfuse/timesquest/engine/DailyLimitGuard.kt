package com.selfuse.timesquest.engine

import com.selfuse.timesquest.data.DailyUsage
import com.selfuse.timesquest.data.ParentSettings

data class LimitStatus(
    val isLocked: Boolean,
    val minutesUsed: Int,
    val minutesAllowed: Int,
    val stagesUsed: Int,
    val stagesAllowed: Int,
    val reasonText: String
)

/**
 * Pure-Kotlin eye-care & daily anti-addiction guard.
 * Enforces daily minutes and daily completed stages (never interrupts mid-battle; checked at gate).
 */
object DailyLimitGuard {

    fun evaluate(usage: DailyUsage, settings: ParentSettings): LimitStatus {
        val minutesAllowed = if (settings.dailyMinutesLimit <= 0) Int.MAX_VALUE
        else settings.dailyMinutesLimit + usage.bonusMinutesGranted

        val stagesAllowed = if (settings.dailyStagesLimit <= 0) Int.MAX_VALUE
        else settings.dailyStagesLimit + usage.bonusStagesGranted

        val minutesUsed = usage.activeBattleSeconds / 60
        val stagesUsed = usage.stagesCompleted

        val timeExceeded = settings.dailyMinutesLimit > 0 && usage.activeBattleSeconds >= minutesAllowed * 60
        val stageExceeded = settings.dailyStagesLimit > 0 && stagesUsed >= stagesAllowed

        val reason = when {
            timeExceeded && stageExceeded -> "今天已经冒险满 $minutesAllowed 分钟并完成 $stagesUsed 关啦，让眼睛休息一下吧！"
            timeExceeded -> "今天已经专注冒险满 $minutesAllowed 分钟啦，小勇者和眼睛都需要充充电！"
            stageExceeded -> "今天已经打赢 $stagesUsed 关啦，战绩超棒！明天再来继续闯关吧！"
            else -> ""
        }

        return LimitStatus(
            isLocked = timeExceeded || stageExceeded,
            minutesUsed = minutesUsed,
            minutesAllowed = if (minutesAllowed == Int.MAX_VALUE) 0 else minutesAllowed,
            stagesUsed = stagesUsed,
            stagesAllowed = if (stagesAllowed == Int.MAX_VALUE) 0 else stagesAllowed,
            reasonText = reason
        )
    }
}
