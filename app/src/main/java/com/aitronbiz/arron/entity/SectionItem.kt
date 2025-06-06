package com.aitronbiz.arron.entity

sealed class SectionItem {
    data class TodayActivity(val subjectId: Int, val deviceId: Int) : SectionItem()
    object WeeklyActivity : SectionItem()
    object ResidenceTime : SectionItem()
    object SmartEnergy : SectionItem()
}