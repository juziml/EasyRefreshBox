package com.rg.eb.nested

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.rg.eb.dp
import com.rg.eb.log
import java.lang.Math.abs

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/8/21 15:24
 */
open class EasyRefreshLayoutV2 : ConstraintLayout, NestedScrollingParent2 {
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        "constructor".log(TAG)
    }

    private val TAG = "EasyRefreshLayout"
    var openPullDownRefresh = false
    var openPullUpLoadMore = false
    private val nestedScrollingParentHelper by lazy { NestedScrollingParentHelper(this) }
    var pullDownLoadListener: PullLoadListener? = null
    var pullUpLoadListener: PullLoadListener? = null

    private var targetViewPullDownLimit = true
    private var targetViewPullUpnLimit = true
    private lateinit var targetView: View

    private var pullDownState: PullState = PullState.STATE_UN_START
        private set(value) {
            field = value
            handlerPullDownState(value)
        }
    private var pullUpState: PullState = PullState.STATE_UN_START
        private set(value) {
            field = value
            handlerPullUpState(value)
        }
    private val RECOVERY_REBOUND_TO_REFRESH_POSITION = 100L
    private val RECOVERY_REBOUND_TO_UNSTART = 400L

    private val DAMP_FACTOR_L1 = 0.7F
    private val DAMP_FACTOR_L2 = 0.4F
    private val DAMP_FACTOR_L3 = 0.2F

     var thresholdReleaseToLoadingDowY = 150.dp
        protected set
     var maxPullDownY = 200.dp
        protected set

     var thresholdReleaseToLoadingUpY = 100.dp
        protected set
     var maxPullUpY = 150.dp
        protected set

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        "onAttachedToWindow".log(TAG)
    }

    /**
     * 在构造执行完立即调用
     */
    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount > 0) {
            for (i in 0 until childCount) {
                val view = getChildAt(i)
                if (view is RecyclerView) {
                    targetView = view
                }
            }
        }
        "onFinishInflate".log(TAG)
        if (!this::targetView.isInitialized) throw NullPointerException("EasyRefreshLayout has not targetView!")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        "onMeasure".log(TAG)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        "onLayout".log(TAG)
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        // helper 状态初始化
        nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        // 允许竖直方向上的滑动
        return isEnabled && (axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0)
    }

    /**
     *  初始化一些状态,比如对consumed[1] 做一些更改，在onNestedScroll 获取到的 dyUnconsumed = dy-consumed[1]
     *  如果 consumed[1] = dy 表示完全消耗，不会再进入onNestedScroll中
     */
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        targetViewPullDownLimit = targetView.canScrollVertically(-1)
        targetViewPullUpnLimit = targetView.canScrollVertically(1)
        //屏蔽抛投
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            return
        }
        //下拉条件，进入下拉后进行上拉
        val canPullDown = (openPullDownRefresh
                && !targetViewPullDownLimit
                && dy < 0)
                || (dy > 0 && (pullUpState == PullState.STATE_UN_START
                && pullDownState.value > PullState.STATE_UN_START.value))

        val canPullUp = (openPullUpLoadMore
                && !targetViewPullUpnLimit
                && dy > 0)
                || (dy < 0 && (pullDownState == PullState.STATE_UN_START
                && pullUpState.value > PullState.STATE_UN_START.value))

        if (canPullDown) {
            val currY = handlerPullDownPosition(-dy.toFloat())
            when {
                currY < thresholdReleaseToLoadingDowY -> {
                    pullDownState = PullState.STATE_PULLING
                }
                currY >= thresholdReleaseToLoadingDowY && pullDownState == PullState.STATE_PULLING -> {
                    pullDownState = PullState.STATE_WAIT_TO_RELEASE
                }
            }
            consumed[1] = dy
        } else if (canPullUp) {
            val currY = handlerPullUpPosition(-dy.toFloat())
            when {
                abs(currY) < thresholdReleaseToLoadingUpY -> {
                    pullUpState = PullState.STATE_PULLING
                }
                abs(currY) >= thresholdReleaseToLoadingUpY && pullUpState == PullState.STATE_PULLING -> {
                    pullUpState = PullState.STATE_WAIT_TO_RELEASE
                }
            }
            consumed[1] = dy
        }
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        nestedScrollingParentHelper.onStopNestedScroll(target, type)
        when (pullDownState) {
            PullState.STATE_WAIT_TO_RELEASE -> {
                //处理回弹出
                reboundToTopHoldingPosition()
            }
            PullState.STATE_PULLING -> {
                quickCancelState()
            }
        }
        when (pullUpState) {
            PullState.STATE_WAIT_TO_RELEASE -> {
                //处理回弹出
                reboundToBottomHoldingPosition()
            }
            PullState.STATE_PULLING -> {
                quickCancelState()
            }
        }
    }

    /**
     * 下拉回弹
     */
    private fun reboundToTopHoldingPosition() {
        targetView.animation?.cancel()
        val currY = targetView.translationY
        val gap = abs(currY - thresholdReleaseToLoadingDowY)
        val factor = gap / (maxPullDownY - thresholdReleaseToLoadingDowY)
        val duration: Long = if (factor > 1) {
            RECOVERY_REBOUND_TO_REFRESH_POSITION
        } else {
            factor.toLong() * RECOVERY_REBOUND_TO_REFRESH_POSITION
        }
        targetView.animate()
                .translationY(thresholdReleaseToLoadingDowY)
                .setDuration(duration)
                .setInterpolator(AccelerateInterpolator())
                .setUpdateListener {
                    if (it.animatedFraction == 1F) {
                        pullDownState = PullState.STATE_LOADING
                    }
                }
                .start()
    }

    /**
     * 上拉回弹
     */
    private fun reboundToBottomHoldingPosition() {
        targetView.animation?.cancel()
        val currY = abs(targetView.translationY)
        val gap = abs(currY - thresholdReleaseToLoadingUpY)
        val factor = gap / (maxPullUpY - thresholdReleaseToLoadingUpY)
        val duration: Long = if (factor > 1) {
            RECOVERY_REBOUND_TO_REFRESH_POSITION
        } else {
            factor.toLong() * RECOVERY_REBOUND_TO_REFRESH_POSITION
        }
        targetView.animate()
                .translationY(-thresholdReleaseToLoadingUpY)
                .setDuration(duration)
                .setInterpolator(AccelerateInterpolator())
                .setUpdateListener {
                    if (it.animatedFraction == 1F) {
                        pullUpState = PullState.STATE_LOADING
                    }
                }
                .start()
    }

    /**
     * 一些未触发刷新or 加载的情况强制回到起点
     */
    private fun quickCancelState() {
        pullDownState = PullState.STATE_CANCELING
        pullUpState = PullState.STATE_CANCELING
        targetView.animate()
                .translationY(0F)
                .setDuration(100)
                .setInterpolator(AccelerateInterpolator())
                .setUpdateListener {
                    if (it.animatedFraction == 1F) {
                        pullDownState = PullState.STATE_UN_START
                        pullUpState = PullState.STATE_UN_START
                    }
                }
                .start()
    }

    /**
     * 下移 targetView
     */
    private fun handlerPullDownPosition(dy: Float): Float {
        val oldTranslateY = targetView.translationY
        val newTranslateY = when {
            oldTranslateY >= maxPullDownY -> {
                dy * DAMP_FACTOR_L3 + oldTranslateY
            }
            oldTranslateY >= thresholdReleaseToLoadingDowY -> {
                dy * DAMP_FACTOR_L2 + oldTranslateY
            }
            else -> {
                dy * DAMP_FACTOR_L1 + oldTranslateY
            }
        }
        if (newTranslateY >= 0 && oldTranslateY != newTranslateY) {
            targetView.translationY = newTranslateY
        }
        return targetView.translationY
    }

    /**
     * 上移 targetView
     */
    private fun handlerPullUpPosition(dy: Float): Float {
        val oldTranslateY = targetView.translationY
        //转正便于计算区间
        val newTranslateY = when {
            abs(oldTranslateY) >= maxPullUpY -> {
                dy * DAMP_FACTOR_L3 + oldTranslateY
            }
            abs(oldTranslateY) >= thresholdReleaseToLoadingUpY -> {
                dy * DAMP_FACTOR_L2 + oldTranslateY
            }
            else -> {
                dy * DAMP_FACTOR_L1 + oldTranslateY
            }
        }
        if (newTranslateY <= 0 && oldTranslateY != newTranslateY) {
            targetView.translationY = newTranslateY
        }
        return targetView.translationY
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        //处于loading 状态以上时 屏蔽所有事件
        if (pullDownState.value >= PullState.STATE_LOADING.value) {
            return true
        }
        if (pullUpState.value >= PullState.STATE_LOADING.value) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun handlerPullUpState(value: PullState) {
        when (value) {
            PullState.STATE_UN_START -> {
                pullUpLoadListener?.onReset()
            }
            PullState.STATE_PULLING -> {
                pullUpLoadListener?.onPulling(abs(targetView.translationY))
            }
            PullState.STATE_WAIT_TO_RELEASE -> {
                //pulling本身具备跳跃性，所以等达到阈值后进行固定回调
                pullUpLoadListener?.onPulling(thresholdReleaseToLoadingUpY)
                pullUpLoadListener?.onWaitToRelease()
            }
            PullState.STATE_LOADING -> {
                pullUpLoadListener?.onLoading()
            }
            PullState.STATE_ENDING -> {
                pullUpLoadListener?.onEnding()
            }
            PullState.STATE_CANCELING -> {
                pullUpLoadListener?.onCanceling()
            }
        }
    }

    private fun handlerPullDownState(value: PullState) {
        when (value) {
            PullState.STATE_UN_START -> {
                pullDownLoadListener?.onReset()
            }
            PullState.STATE_PULLING -> {
                pullDownLoadListener?.onPulling(abs(targetView.translationY))
            }
            PullState.STATE_WAIT_TO_RELEASE -> {
                //pulling本身具备跳跃性，所以等达到阈值后进行固定回调
                pullDownLoadListener?.onPulling(thresholdReleaseToLoadingDowY)
                pullDownLoadListener?.onWaitToRelease()
            }
            PullState.STATE_LOADING -> {
                pullDownLoadListener?.onLoading()
            }
            PullState.STATE_ENDING -> {
                pullDownLoadListener?.onEnding()
            }
            PullState.STATE_CANCELING -> {
                pullDownLoadListener?.onCanceling()
            }
        }
    }

    /**
     * 刷新完成，进行复位 仅在刷新时生效
     */
    fun pullDownLoadComplete() {
        if (pullDownState != PullState.STATE_LOADING) {
            return
        }
        pullDownState = PullState.STATE_ENDING
        targetView.animation?.cancel()
        //关键性状态设置，不依赖于动画 当前场景下动画将被随意取消
        targetView.animate()
                .translationY(0F)
                .setDuration(RECOVERY_REBOUND_TO_UNSTART)
                .setUpdateListener {
                    if (it.animatedFraction == 1F) {
                        pullDownState = PullState.STATE_UN_START
                    }
                }
                .setInterpolator(AccelerateInterpolator())
                .start()
    }

    fun pullUpLoadComplete() {
        if (pullUpState != PullState.STATE_LOADING) {
            return
        }
        pullUpState = PullState.STATE_ENDING
        targetView.animation?.cancel()
        //关键性状态设置，不依赖于动画 当前场景下动画将被随意取消
        targetView.animate()
                .translationY(0F)
                .setDuration(RECOVERY_REBOUND_TO_UNSTART)
                .setUpdateListener {
                    if (it.animatedFraction == 1F) {
                        pullUpState = PullState.STATE_UN_START
                    }
                }
                .setInterpolator(AccelerateInterpolator())
                .start()
    }
}