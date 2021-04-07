package com.rg.eb

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import kotlin.math.abs

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/5/21 14:21
 */
class EasyRefreshBox : FrameLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    private val pullDownRecoveryAnim = ValueAnimator.ofFloat(0F, 1F).apply {
        duration = 400
    }
    private val moveSlop = ViewConfiguration.get(context).scaledTouchSlop
    private lateinit var targetView: View
    private lateinit var tvState: TextView
    private var pullDownRefreshState = PullState.STATE_PREPARE
        set(value) {
            field = value
            handlerStatus()
        }
    private var pullDownRefreshable: Boolean = true
    var pullDownRefreshListener: PullDownRefreshListener? = null

    init {
        pullDownRecoveryAnim.interpolator = AccelerateInterpolator()
        pullDownRecoveryAnim.addUpdateListener(RecoveryTopAnimListener())
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
        tvState = findViewById(R.id.tv_refreshState)
    }

    private var downY: Float = 0F
    private var lastMoveY: Float = 0F
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isCanRefresh()) {
            "last refresh task not completed,can not touch in this time".log()
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                lastMoveY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val moveY = event.y - downY
                if (abs(moveY) > moveSlop) {
                    onPullDownContentView(moveY)
                }
                lastMoveY = event.y
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
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
                PullState.STATE_PULLING
            }
            translationY > EFFECT_THRESHOLD_PULL_DOWN_Y -> {
                PullState.STATE_EFFECTIVE
            }
            else -> {
                PullState.STATE_PULLING
            }
        }
    }

    private fun handlerFingerLeave() {
        if (grandTotalPullDownDistance >= EFFECT_THRESHOLD_PULL_DOWN_Y) {
            pullDownRefreshState = PullState.STATE_REFRESHING
        } else {
            pullDownRefreshState = PullState.STATE_PREPARE
            pullDownRecoveryAnim.start()
        }
    }

    private fun handlerStatus() {
        val desc = when (pullDownRefreshState) {
            PullState.STATE_PREPARE -> {
                pullDownRefreshListener?.onPrepare()
                "下拉刷新"
            }
            PullState.STATE_PULLING -> {
                pullDownRefreshListener?.onPulling(grandTotalPullDownDistance / EFFECT_THRESHOLD_PULL_DOWN_Y)
                "继续下拉"
            }
            PullState.STATE_EFFECTIVE -> {
                pullDownRefreshListener?.onEffective()
                "松开刷新"
            }
            PullState.STATE_REFRESHING -> {
                pullDownRefreshListener?.onRefreshing()
                "刷新中..."
            }
            else -> {
                pullDownRefreshListener?.onPrepare()
                "下拉刷新"
            }
        }
        tvState.text = desc
    }

    private inner class RecoveryTopAnimListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val faction = animation.animatedFraction
            var endY = if (faction >= 1F) {
                grandTotalPullDownDistance = 0F
                pullDownRefreshState = PullState.STATE_PREPARE
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
        pullDownRefreshState = PullState.STATE_ENDING
        pullDownRecoveryAnim.start()
    }

    fun setRefreshable(able: Boolean) {
        pullDownRefreshable = able
    }

    private fun isCanRefresh(): Boolean {
        return pullDownRefreshable
                && pullDownRefreshState != PullState.STATE_ENDING
                && pullDownRefreshState != PullState.STATE_REFRESHING
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


enum class PullState(val v: Int) {
    STATE_PREPARE(0),
    STATE_PULLING(1),
    STATE_EFFECTIVE(2),
    STATE_REFRESHING(3),
    STATE_ENDING(4),
}