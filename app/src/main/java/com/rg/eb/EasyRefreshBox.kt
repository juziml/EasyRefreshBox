package com.rg.eb

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs

/**
 *@Desc:
 *-
 *-下拉刷新，上拉加载更多 事件管理容器
 *-
 *create by zhusw on 4/5/21 14:21
 */
class EasyRefreshBox : ConstraintLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    private val pullDownRecoveryAnim = ValueAnimator.ofFloat(0F, 1F).apply {
        duration = 400
    }
    private val pullUpRecoveryAnim = ValueAnimator.ofFloat(0F, 1F).apply {
        duration = 400
    }
    private val moveSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val LOAD_MORE_CONTENT_HEIGHT = 50.dp
    private lateinit var targetView: View
    private lateinit var tvRefresh: TextView
    private lateinit var tvLoadMore: TextView
    private var pullDownRefreshState = PullDownState.STATE_PREPARE
        set(value) {
            field = value
            handlerPullDownStatus()
        }
    private var pullUpLoadMoreState = PullUpState.STATE_PREPARE
        set(value) {
            field = value
            handlerPullUpStatus()
        }
    private var pullDownRefreshable: Boolean = true
    private var pullUpLoadMoreAble: Boolean = true

    var pullDownRefreshListener: PullDownRefreshListener? = null

    init {
        pullDownRecoveryAnim.interpolator = AccelerateInterpolator()
        pullDownRecoveryAnim.addUpdateListener(RecoveryTopAnimListener())
        pullUpRecoveryAnim.interpolator = AccelerateInterpolator()
        pullUpRecoveryAnim.addUpdateListener(RecoveryBottomAnimListener())
    }

    private val MIN_EFFECT_PULL_DOWN_Y: Float = 50.dp
    private val EFFECT_THRESHOLD_PULL_DOWN_Y = 150.dp
    private val MAX_PULL_DOWN_Y = 200.dp

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pullDownRecoveryAnim.cancel()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        targetView = findViewById(R.id.rv_content)
        tvRefresh = findViewById(R.id.tv_refreshState)
        tvLoadMore = findViewById(R.id.tv_loadMoreState)
    }

    private var downY: Float = 0F
    private var lastMoveY: Float = 0F
    private var snatchEvent = false
    private var needHandlePullDownEvent = false
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        "isPullDownRefreshing=${isPullDownRefreshing()} isLoadingMore=${isLoadingMore()}".log()
        if (isPullDownRefreshing() || isLoadingMore()) return true
        "isCanDoRefresh= ${!isCanDoRefresh()} isCanDoLoadMore=${!isCanDoLoadMore()}".log()
        if (!isCanDoRefresh() || !isCanDoLoadMore()) return false
        //能否继续向上滚动
        val targetViewCanScrollUp = targetView.canScrollVertically(-1)
        //能否继续向下滚动
        val targetViewCanScrollDown = targetView.canScrollVertically(1)
        //下拉到底后，canDown true canUp false
        //上拉到底后，canDown false canUp true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                lastMoveY = event.y
                //每次一开始 都先不抢 给子view留着
                snatchEvent = false
                needHandlePullDownEvent = false
            }
            MotionEvent.ACTION_MOVE -> {
                //只要滑起来发现能抢了 就抢，这里只是让子View不再接手move事件而已
                val curY = event.y
                if (curY - downY > 0) {
                    snatchEvent = !targetViewCanScrollUp
                    needHandlePullDownEvent = snatchEvent
                } else {
                    snatchEvent = false
                    //上拉加载不再需要跟随手势，此处直接主动移动targetView，显示加载loading，并进入阻塞中即可
                    if (!targetViewCanScrollDown
                        && pullUpLoadMoreState == PullUpState.STATE_PREPARE
                    ) {
                        needShowLoadMoreView()
                    }
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                //有始有终 头尾都不抢,
                snatchEvent = false
            }
        }
        return snatchEvent
    }


    /**
     * 因为是抢的所以 不一定有down，down在onInterceptTouchEvent中也做初始化
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        //这将包含pullUp的拦截事件，需要过滤
        if (!needHandlePullDownEvent) return false
        if (!isCanDoRefresh()) {
            "last refresh task not completed,can not touch in this time".log()
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                "onTouchEvent.ACTION_MOVE".log()
                val moveY = event.y - downY
                if (abs(moveY) > moveSlop) {
                    onPullDownContentView(moveY)
                }
                lastMoveY = event.y
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                "onTouchEvent.ACTION_UP".log()
                downY = 0F
                lastMoveY = 0F
                handlerFingerLeave()
            }
        }
        return true
    }

    private var grandTotalPullDownDistance: Float = 0F

    /**
     * 使用translation 直接操作内容view
     * 这样只需要操作内容view的位置，而不用关心与其他同级view的状态
     */
    private fun onPullDownContentView(y: Float) {
        val translationY = if (y >= MAX_PULL_DOWN_Y) {
            val offset = (y - MAX_PULL_DOWN_Y) * 0.15F
            offset + MAX_PULL_DOWN_Y
        } else {
            y
        }
        grandTotalPullDownDistance = translationY
        targetView.translationY = translationY
        pullDownRefreshState = when {
            translationY < MIN_EFFECT_PULL_DOWN_Y -> {
                PullDownState.STATE_PULLING
            }
            translationY > EFFECT_THRESHOLD_PULL_DOWN_Y -> {
                PullDownState.STATE_EFFECTIVE
            }
            else -> {
                PullDownState.STATE_PULLING
            }
        }
    }

    private fun handlerFingerLeave() {
        if (grandTotalPullDownDistance >= EFFECT_THRESHOLD_PULL_DOWN_Y) {
            pullDownRefreshState = PullDownState.STATE_REFRESHING
        } else {
            pullDownRefreshState = PullDownState.STATE_PREPARE
            pullDownRecoveryAnim.start()
        }
    }

    private fun handlerPullDownStatus() {
        val desc = when (pullDownRefreshState) {
            PullDownState.STATE_PREPARE -> {
                pullDownRefreshListener?.onPrepare()
                "下拉刷新"
            }
            PullDownState.STATE_PULLING -> {
                pullDownRefreshListener?.onPulling(grandTotalPullDownDistance / EFFECT_THRESHOLD_PULL_DOWN_Y)
                "继续下拉"
            }
            PullDownState.STATE_EFFECTIVE -> {
                pullDownRefreshListener?.onEffective()
                "松开刷新"
            }
            PullDownState.STATE_REFRESHING -> {
                pullDownRefreshListener?.onRefreshing()
                "刷新中..."
            }
            else -> {
                pullDownRefreshListener?.onPrepare()
                "下拉刷新"
            }
        }
        tvRefresh.text = desc
    }

    private fun needShowLoadMoreView() {
        targetView.translationY = -LOAD_MORE_CONTENT_HEIGHT
        pullUpLoadMoreState = PullUpState.STATE_LOADING
    }

    private fun handlerPullUpStatus() {
        val desc = when (pullUpLoadMoreState) {
            PullUpState.STATE_PREPARE -> {
                "加载更多"
            }
            PullUpState.STATE_LOADING -> {
                "加载中..."
            }
            PullUpState.STATE_ENDING -> {
                "结束加载中..."
            }
            else -> {
                "加载中"
            }
        }
        tvLoadMore.text = desc
    }

    private inner class RecoveryTopAnimListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val faction = animation.animatedFraction
            var endY = if (faction >= 1F) {
                grandTotalPullDownDistance = 0F
                pullDownRefreshState = PullDownState.STATE_PREPARE
                0F
            } else {
                (1F - faction) * grandTotalPullDownDistance
            }
            targetView.translationY = endY
        }
    }
    private inner class RecoveryBottomAnimListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val faction = animation.animatedFraction
            var endY = if (faction >= 1F) {
                pullDownRefreshState = PullDownState.STATE_PREPARE
                0F
            } else {
                (1F - faction) * grandTotalPullDownDistance
            }
            targetView.translationY = endY
        }
    }
    /**
     * 刷新完成后需要主动取消刷新状态
     */
    fun refreshComplete() {
        pullDownRefreshState = PullDownState.STATE_ENDING
        pullDownRecoveryAnim.start()
    }
    fun loadMoreComplete(){
        pullUpLoadMoreState = PullUpState.STATE_ENDING

    }

    fun setRefreshable(able: Boolean) {
        pullDownRefreshable = able
    }
    fun setLoadMoreAble(able:Boolean){
        pullUpLoadMoreAble = able
    }

    fun isPullDownRefreshing(): Boolean {
        return pullDownRefreshState == PullDownState.STATE_REFRESHING
    }

    fun isLoadingMore(): Boolean {
        return pullUpLoadMoreState == PullUpState.STATE_LOADING
    }

    private fun isCanDoLoadMore(): Boolean {
        return pullUpLoadMoreAble
                && pullUpLoadMoreState != PullUpState.STATE_LOADING
                && pullUpLoadMoreState != PullUpState.STATE_ENDING
    }

    private fun isCanDoRefresh(): Boolean {
        return pullDownRefreshable
                && pullDownRefreshState != PullDownState.STATE_ENDING
                && pullDownRefreshState != PullDownState.STATE_REFRESHING
    }

    interface PullDownRefreshListener {
        fun onPrepare()

        /**将提供一个百分比的值，达到100%时松开可触发刷新,便于处理一些下拉交互动画*/
        fun onPulling(percent: Float)
        fun onEffective()
        fun onRefreshing()
        fun onEnding()
    }

}

enum class PullUpState(val v: Int) {
    STATE_PREPARE(0),
    STATE_LOADING(1),
    STATE_ENDING(2),
}

enum class PullDownState(val v: Int) {
    STATE_PREPARE(0),
    STATE_PULLING(1),
    STATE_EFFECTIVE(2),
    STATE_REFRESHING(3),
    STATE_ENDING(4),
}