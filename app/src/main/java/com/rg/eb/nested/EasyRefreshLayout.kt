package com.rg.eb.nested

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.rg.eb.R
import com.rg.eb.dp
import com.rg.eb.log
import java.lang.Math.abs

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/8/21 15:24
 */
class EasyRefreshLayout(context: Context, attributeSet: AttributeSet)
    : ConstraintLayout(context, attributeSet),
        NestedScrollingParent2 {
    private val TAG = "EasyRefreshLayout"
    var openPullDownRefresh = false
    var openPullUpLoadMore = false
    private val nestedScrollingParentHelper by lazy { NestedScrollingParentHelper(this) }
    var pullDownRefreshListener: PullDownRefreshListener? = null
    var pullUpRefreshListener: PullDownRefreshListener? = null

    private var targetViewPullDownLimit = true
    private var targetViewPullUpnLimit = true
    private lateinit var targetView: View
    private lateinit var tvTopState: TextView
    private lateinit var tvBottomState: TextView

    private var totalPullDownY = 0F
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

    private val RELEASE_TO_REFRESH_DOWN_Y = 150.dp
    private val MAX_PULL_DOWN_Y = 200.dp

    private val RELEASE_TO_LOAD_UP_Y = 100.dp
    private val MAX_PULL_UP_Y = 150.dp
    override fun onFinishInflate() {
        super.onFinishInflate()
        tvTopState = findViewById(R.id.aer_fl_top)
        tvBottomState = findViewById(R.id.aer_fl_bottom)
        if (childCount > 0) {
            for (i in 0 until childCount) {
                val view = getChildAt(i)
                if (view is RecyclerView) {
                    targetView = view
                }
            }
        }
        if (!this::targetView.isInitialized) NullPointerException("EasyRefreshLayout has not targetView!")
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
        "onNestedPreScroll dy=$dy ".log(TAG)
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
            val currY = handlerPullDownRefresh(-dy.toFloat())
            when {
                currY < RELEASE_TO_REFRESH_DOWN_Y -> {
                    pullDownState = PullState.STATE_PULLING
                }
                currY >= RELEASE_TO_REFRESH_DOWN_Y && pullDownState == PullState.STATE_PULLING -> {
                    pullDownState = PullState.STATE_WAIT_TO_RELEASE
                }
            }
            consumed[1] = dy
        } else if (canPullUp) {
            val currY = handlerPullUpRefresh(-dy.toFloat())
            when {
                abs(currY) < RELEASE_TO_LOAD_UP_Y -> {
                    pullUpState = PullState.STATE_PULLING
                }
                abs(currY) >= RELEASE_TO_LOAD_UP_Y && pullUpState == PullState.STATE_PULLING -> {
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
        totalPullDownY = 0F
        "onStopNestedScroll pullState=$pullDownState".log(TAG)
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
        val gap =  abs(currY - RELEASE_TO_REFRESH_DOWN_Y)
        val factor = gap / (MAX_PULL_DOWN_Y - RELEASE_TO_REFRESH_DOWN_Y)
        val duration: Long = if (factor > 1) {
            RECOVERY_REBOUND_TO_REFRESH_POSITION
        } else {
            factor.toLong() * RECOVERY_REBOUND_TO_REFRESH_POSITION
        }
        targetView.animate()
                .translationY(RELEASE_TO_REFRESH_DOWN_Y)
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
        val gap = abs(currY - RELEASE_TO_LOAD_UP_Y)
        val factor = gap / (MAX_PULL_UP_Y - RELEASE_TO_LOAD_UP_Y)
        val duration: Long = if (factor > 1) {
            RECOVERY_REBOUND_TO_REFRESH_POSITION
        } else {
            factor.toLong() * RECOVERY_REBOUND_TO_REFRESH_POSITION
        }
        targetView.animate()
                .translationY(-RELEASE_TO_LOAD_UP_Y)
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
    private fun handlerPullDownRefresh(dy: Float): Float {
        val oldTranslateY = targetView.translationY
        val newTranslateY = when {
            oldTranslateY >= MAX_PULL_DOWN_Y -> {
                dy * DAMP_FACTOR_L3 + oldTranslateY
            }
            oldTranslateY >= RELEASE_TO_REFRESH_DOWN_Y -> {
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
    private fun handlerPullUpRefresh(dy: Float): Float {
        val oldTranslateY = targetView.translationY
        "handlerPullUpRefresh oldTranslateY=$oldTranslateY".log(TAG)
        //转正便于计算区间
        val newTranslateY = when {
            abs(oldTranslateY) >= MAX_PULL_UP_Y -> {
                dy * DAMP_FACTOR_L3 + oldTranslateY
            }
            abs(oldTranslateY) >= RELEASE_TO_LOAD_UP_Y -> {
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
                tvBottomState.text = "STATE_UN_START"
                pullUpRefreshListener?.onReset()
            }
            PullState.STATE_PULLING -> {
                tvBottomState.text = "STATE_PULLING"
                pullUpRefreshListener?.onPulling(targetView.translationY)
            }
            PullState.STATE_WAIT_TO_RELEASE -> {
                tvBottomState.text = "STATE_WAIT_TO_RELEASE"
                pullUpRefreshListener?.onWaitToRelease()
            }
            PullState.STATE_LOADING -> {
                tvBottomState.text = "STATE_LOADING"
                pullUpRefreshListener?.onLoading()
            }
            PullState.STATE_ENDING -> {
                tvBottomState.text = "STATE_ENDING"
                pullUpRefreshListener?.onEnding()
            }
            PullState.STATE_CANCELING -> {
                tvTopState.text = "STATE_CANCELING"
                pullUpRefreshListener?.onCanceling()
            }
        }
    }

    private fun handlerPullDownState(value: PullState) {
        when (value) {
            PullState.STATE_UN_START -> {
                tvTopState.text = "STATE_UN_START"
                pullDownRefreshListener?.onReset()
            }
            PullState.STATE_PULLING -> {
                tvTopState.text = "STATE_PULLING"
                pullDownRefreshListener?.onPulling(targetView.translationY)
            }
            PullState.STATE_WAIT_TO_RELEASE -> {
                tvTopState.text = "STATE_WAIT_TO_RELEASE"
                pullDownRefreshListener?.onWaitToRelease()
            }
            PullState.STATE_LOADING -> {
                tvTopState.text = "STATE_LOADING"
                pullDownRefreshListener?.onLoading()
            }
            PullState.STATE_ENDING -> {
                tvTopState.text = "STATE_ENDING"
                pullDownRefreshListener?.onEnding()
            }
            PullState.STATE_CANCELING -> {
                tvTopState.text = "STATE_CANCELING"
                pullDownRefreshListener?.onCanceling()
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