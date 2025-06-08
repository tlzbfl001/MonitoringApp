package com.aitronbiz.arron.entity

sealed class SectionItem {
    object TodayActivity : SectionItem()
    object WeeklyActivity : SectionItem()
    object ResidenceTime : SectionItem()
    object SmartEnergy : SectionItem()
}