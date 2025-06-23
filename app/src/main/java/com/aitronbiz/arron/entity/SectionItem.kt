package com.aitronbiz.arron.entity

sealed class SectionItem {
    object TodayActivity : SectionItem()
    object DailyActivity : SectionItem()
    object ResidenceTime : SectionItem()
}