package cn.vce.easylook.feature_video.presentation.home.detail

import cn.vce.easylook.base.BaseEvent
import cn.vce.easylook.feature_video.models.Daily

sealed class DailyEvent: BaseEvent() {

    object Search: DailyEvent()
}