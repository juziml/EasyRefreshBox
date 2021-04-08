package com.rg.eb.nested

/**
 *@Desc:
 *- 包含内部状态与对外回调状态
 *create by zhusw on 4/8/21 17:18
 */
internal enum class PullState(val value: Int) {
    STATE_UN_START(0),
    STATE_PULLING(1),
    STATE_WAIT_TO_RELEASE(2),
    STATE_LOADING(3),
    STATE_ENDING(4),
    STATE_CANCELING(5)
}

interface PullDownRefreshListener {
    fun onReset()
    /**
     * @param percent 距离松手触发刷新 or 加载位置的下拉百分比
     */
    fun onPulling(percent:Float)
    fun onWaitToRelease()
    fun onLoading()
    fun onEnding()
    fun onCanceling()

}