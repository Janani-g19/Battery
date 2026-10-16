package com.example.chargelove

object Thresholds {
    /** Highest to lowest. */
    val ALL = listOf(30 ,20, 10, 5, 3)

    /** The lowest milestone the battery has dropped to, or null if above all of them. */
    fun lowestReached(level: Int): Int? = ALL.filter { level <= it }.minOrNull()
}

object Messages {
    const val DEFAULT_CUSTOM =
        "Hey you 💕 your battery's at 20%. Please plug in before it gets dramatic!"

    fun title(threshold: Int, level: Int): String = when (threshold) {
        20 -> "🔋 $level% – a gentle reminder"
        10 -> "😬 $level% – danger zone"
        5 -> "🚨 $level% – code red"
        else -> "💀 $level% – this is goodbye"
    }

    /** 20% uses the custom message; the rest escalate on their own. */
    fun body(threshold: Int, custom: String): String = when (threshold) {
        90 -> "Test: the 90% milestone works! 💕"
        20 -> custom
        10 -> "10%?! Your phone is wheezing. Find a charger, babe. Please. 🔌"
        5 -> "5%!! Your phone is writing its will. I am begging you to plug it in. 😭"
        else -> "3%... It's been an honor. If you go dark, I'll assume you were eaten by a charger-less void. CHARGE. IT. NOW. 🪦"
    }
}