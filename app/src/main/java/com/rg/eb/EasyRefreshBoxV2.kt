package com.rg.eb

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs

/**
 *@Desc:
 *-
 *-下拉刷新，上拉加载更多 事件管理容器
 *-V2版本将支持连贯性滑动触发 刷新
 *create by zhusw on 4/5/21 14:21
 */
class EasyRefreshBoxV2 : ConstraintLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    private val pullDownRecoveryAnim = ValueAnimator.ofFloat(0F, 1F).apply {
        duration = 400
    }
    private val pullUpRecoveryAnim = ValueAnimator.ofFloat(0F, 1F).apply {
        duration = 200
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
    var pullUpLoadMoreListener: PullUpLoadMoreListener? = null

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
        targetView.animation?.cancel()
        pullDownRecoveryAnim.cancel()
        pullUpRecoveryAnim.cancel()
        pullDownRefreshListener = null
        pullUpLoadMoreListener = null
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        targetView = findViewById(R.id.rv_contentV2)
        tvRefresh = findViewById(R.id.tv_refreshStateV2)
        if (tvRefresh.visibility == View.VISIBLE) {
            tvRefresh.visibility = View.GONE
        }
        tvLoadMore = findViewById(R.id.tv_loadMoreStateV2)
        if (tvLoadMore.visibility == View.VISIBLE) {
            tvLoadMore.visibility = View.GONE
        }
    }

    private var downY: Float = 0F
    private var lastMoveY: Float = 0F
    private var snatchEvent = false
    private var needHandlePullDownEvent = false
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
//        "isPullDownRefreshing=${isPullDownRefreshing()} isLoadingMore=${isLoadingMore()}".log()
        if (isPullDownRefreshing() || isLoadingMore()) return true
//        "isCanDoRefresh= ${!isCanDoRefresh()} isCanDoLoadMore=${!isCanDoLoadMore()}".log()
        if (!isCanDoRefresh() || !isCanDoLoadMore()) return false
        //能否继续向上滚动
        val targetViewCanScrollUp = targetView.canScrollVertically(-1)
        //能否继续向下滚动
        val targetViewCanScrollDown = targetView.canScrollVertically(1)
        //下拉到底后，canDown true canUp false
        //上拉到底后，canDown false canUp true
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                "Intercept ACTION_DOWN".log()
                downY = event.y
                lastMoveY = event.y
                //每次一开始 都先不抢 给子view留着
                snatchEvent = false
                needHandlePullDownEvent = false
            }
            MotionEvent.ACTION_MOVE -> {
                "Intercept ACTION_MOVE".log()
                //只要滑起来发现能抢了 就抢，这里只是让子View不再接手move事件而已
                val curY = event.y
                if (curY - downY > 0) {
                    snatchEvent = !targetViewCanScrollUp
                    needHandlePullDownEvent = snatchEvent
                } else {
                    //上拉加载不再需要跟随手势，此处直接主动移动targetView，显示加载loading，并进入阻塞即可
                    if (!targetViewCanScrollDown
                        && pullUpLoadMoreState == PullUpState.STATE_PREPARE
                    ) {
                        snatchEvent = true
                        needShowLoadMoreView()
                    }
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                "Intercept ACTION_UP || ACTION_CANCEL".log()
                //此处抢和不抢并不影响EasyRefreshBox
                snatchEvent = false
            }
        }
        return snatchEvent
    }


    /**
     * 因为是抢的所以 不一定有down，down在onInterceptTouchEvent中也做初始化
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        //event将包含pullUp的拦截事件，需要过滤
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
                "onTouchEvent.ACTION_UP || ACTION_CANCEL".log()
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
            val offset = (y - MAX_PULL_DOWN_Y) * 0.35F
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
            //回弹一下先
            val diff = grandTotalPullDownDistance - EFFECT_THRESHOLD_PULL_DOWN_Y
            if (diff > 0) {
                grandTotalPullDownDistance = EFFECT_THRESHOLD_PULL_DOWN_Y
            }
            pullDownRefreshState = PullDownState.STATE_PRE_REFRESHING
            targetView.animate().translationY(EFFECT_THRESHOLD_PULL_DOWN_Y)
                .setDuration(100)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .setUpdateListener {
                    if (it.animatedFraction == 1.0F) {
                        pullDownRefreshState = PullDownState.STATE_REFRESHING
                    }
                }
        } else {
            pullDownRefreshState = PullDownState.STATE_PREPARE
            pullDownRecoveryAnim.start()
        }
    }

    private fun handlerPullDownStatus() {
        val desc = when (pullDownRefreshState) {
            PullDownState.STATE_PREPARE -> {
                if (tvRefresh.visibility != View.GONE) {
                    tvRefresh.visibility = View.GONE
                }
                pullDownRefreshListener?.onPrepare()
                "准备下拉刷新"
            }
            PullDownState.STATE_PULLING -> {
                if (tvRefresh.visibility != View.VISIBLE) {
                    tvRefresh.visibility = View.VISIBLE
                }
                pullDownRefreshListener?.onPulling(grandTotalPullDownDistance / EFFECT_THRESHOLD_PULL_DOWN_Y)
                "继续下拉"
            }
            PullDownState.STATE_EFFECTIVE -> {
                pullDownRefreshListener?.onEffective()
                "松开刷新"
            }
            PullDownState.STATE_PRE_REFRESHING -> {
                "即将进入刷新..."
            }
            PullDownState.STATE_REFRESHING -> {
                pullDownRefreshListener?.onRefreshing()
                "刷新中..."
            }
            PullDownState.STATE_ENDING -> {
                pullDownRefreshListener?.onEnding()
                "结束中..."
            }
        }
        tvRefresh.text = desc
    }

    private fun needShowLoadMoreView() {
        pullUpLoadMoreState = PullUpState.STATE_PRE_LOADING
        targetView.animate().translationY(-LOAD_MORE_CONTENT_HEIGHT)
            .setDuration(100)
            .setInterpolator(AccelerateInterpolator())
            .setUpdateListener {
                if (it.animatedFraction == 1F) {
                    pullUpLoadMoreState = PullUpState.STATE_LOADING
                }
            }
            .start()
    }

    private fun handlerPullUpStatus() {
        val desc = when (pullUpLoadMoreState) {
            PullUpState.STATE_PREPARE -> {
                if (tvLoadMore.visibility != View.GONE) {
                    tvLoadMore.visibility = View.GONE
                }
                pullUpLoadMoreListener?.onPrepare()
                "准备加载"
            }
            PullUpState.STATE_PRE_LOADING -> {
                if (tvLoadMore.visibility != View.VISIBLE) {
                    tvLoadMore.visibility = View.VISIBLE
                }
                "加载中..."
            }
            PullUpState.STATE_LOADING -> {
                pullUpLoadMoreListener?.onLoading()
                "加载中..."
            }
            PullUpState.STATE_ENDING -> {
                pullUpLoadMoreListener?.onEnding()
                "结束加载中..."
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
            var endY = if (faction == 1F) {
                pullUpLoadMoreState = PullUpState.STATE_PREPARE
                0F
            } else {
                (1F - faction) * -LOAD_MORE_CONTENT_HEIGHT
            }
            targetView.translationY = endY
        }
    }

    /**
     * 刷新完成后需要主动取消刷新状态
     */
    fun refreshComplete() {
        pullDownRefreshState = PullDownState.STATE_ENDING
        targetView.animation?.cancel()
        pullDownRecoveryAnim.start()
    }

    fun loadMoreComplete() {
        pullUpLoadMoreState = PullUpState.STATE_ENDING
        targetView.animation?.cancel()
        pullUpRecoveryAnim.start()
    }

    fun setRefreshable(able: Boolean) {
        pullDownRefreshable = able
    }

    fun setLoadMoreAble(able: Boolean) {
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
                && pullUpLoadMoreState == PullUpState.STATE_PREPARE
    }

    private fun isCanDoRefresh(): Boolean {
        return pullDownRefreshable
                && pullDownRefreshState != PullDownState.STATE_ENDING
                && pullDownRefreshState != PullDownState.STATE_REFRESHING
                && pullDownRefreshState != PullDownState.STATE_PRE_REFRESHING
    }

    interface PullUpLoadMoreListener {
        fun onPrepare()
        fun onLoading()
        fun onEnding()
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
