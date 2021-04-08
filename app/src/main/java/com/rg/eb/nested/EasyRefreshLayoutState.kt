package com.rg.eb.nested

/**
 *@Desc:
 *- 包含内部状态与对外回调状态
 *create by zhusw on 4/8/21 17:18
 */
internal enum class PullDownState(val value: Int) {
    STATE_UN_START(0),
    STATE_PULLING(1),
    STATE_WAIT_TO_RELEASE(2),
    STATE_REFRESHING(3),
    STATE_ENDING(4)
}

interface PullDownRefreshListener {
    fun onReset()
    fun onPulling()
    fun onWaitToRelease()
    fun onRefreshing()
    fun onEnding()

}