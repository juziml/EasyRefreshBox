package com.rg.eb.nested

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/8/21 16:32
 */
enum class PullDownState(val value: Int) {
    STATE_UN_START(0),
    STATE_PULLING(1),
    STATE_WAIT_TO_RELEASE(2),
    STATE_REFRESHING(3),
    STATE_ENDING(4)
}