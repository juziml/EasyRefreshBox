package com.rg.eb

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/7/21 18:20
 */
enum class PullUpState(val v: Int) {
    STATE_PREPARE(0),
    STATE_PRE_LOADING(1),
    STATE_LOADING(2),
    STATE_ENDING(3),
}

enum class PullDownState(val v: Int) {
    STATE_PREPARE(0),
    STATE_PULLING(1),
    STATE_EFFECTIVE(2),
    STATE_PRE_REFRESHING(2),
    STATE_REFRESHING(3),
    STATE_ENDING(4),
}