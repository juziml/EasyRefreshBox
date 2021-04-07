package com.rg.eb

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView
import java.math.BigDecimal
import java.text.DecimalFormat
import kotlin.math.abs

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/5/21 14:21
 */
class EasyRefreshBox : FrameLayout {
    constructor(context: Context) : this(context, null) {
    }

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {

    }

    private val pullDownRecoveryAnim = ValueAnimator.ofFloat(0F, 1F).apply {
        duration = 500
    }
    private val moveSlop = ViewConfiguration.get(context).scaledTouchSlop
    private lateinit var contentView: View
    private lateinit var tvState: TextView

    override fun onFinishInflate() {
        super.onFinishInflate()
        contentView = findViewById(R.id.v_content)
        tvState = findViewById(R.id.tv_refreshState)
        tvState.tag = PullState.UN_START
    }

    init {
        pullDownRecoveryAnim.addUpdateListener(RecoveryTopAnimListener())
    }

    private val MIN_EFFECT_PULL_DOWN_Y: Float = 20.dp
    private val EFFECT_THRESHOLD_PULL_DOWN_Y = 150.dp
    private val MAX_PULL_DOWN_Y = 200.dp

    private var downY: Float = 0F
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val tag = tvState.tag as? PullState ?: PullState.UN_START
        if (tag == PullState.REFRESHING) {
            "刷新中,别乱动".log()
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val moveY = event.y - downY
                if (abs(moveY) > moveSlop) {
                    grandTotalPullDownDistance = moveY
                    moveContentView(moveY)
                }
            }
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                downY = 0F
                recoveryTop()
            }
        }
        return true
    }

    private var grandTotalPullDownDistance: Float = 0F

    /**
     * 使用translation 直接操作内容view
     * 这样只需要操作内容view的位置，而不用关心与其他同级view的状态
     */
    private fun moveContentView(y: Float) {
        val translationY = if (y >= MAX_PULL_DOWN_Y) {
            val offset = (y - MAX_PULL_DOWN_Y) * 0.15F
            offset + MAX_PULL_DOWN_Y
        } else {
            y
        }
        contentView.translationY = translationY
        tvState.tag = when {
            translationY < MIN_EFFECT_PULL_DOWN_Y -> {
                PullState.PULLING
            }
            translationY > EFFECT_THRESHOLD_PULL_DOWN_Y -> {
                PullState.EFFECTIVE
            }
            else -> {
                PullState.PULLING
            }
        }
        handlerStatus()
    }

    private fun recoveryTop() {
        if (grandTotalPullDownDistance >= EFFECT_THRESHOLD_PULL_DOWN_Y) {
            tvState.tag = PullState.REFRESHING
            pullDownRecoveryAnim.start()
        } else {
            tvState.tag = PullState.UN_START
            moveContentView(0F)
        }
        handlerStatus()
    }

    private fun handlerStatus() {
        val desc = when (tvState.tag as PullState) {
            PullState.UN_START -> {
                "下拉刷新"
            }
            PullState.PULLING -> {
                "继续下拉"
            }
            PullState.EFFECTIVE -> {
                "松开刷新"
            }
            PullState.REFRESHING -> {
                "刷新中"
            }
            else -> {
                "下拉刷新"
            }
        }
        tvState.text = desc
    }

    inner class RecoveryTopAnimListener : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(animation: ValueAnimator) {
            val faction = animation.animatedValue as Float
            "动画刷新中 $faction $grandTotalPullDownDistance".log()
//            val bigDecimal = DecimalFormat("0.0").format(faction).toFloat()
            var endY = if (faction >= 1F) {
                grandTotalPullDownDistance = 0F
                tvState.tag = PullState.UN_START
                0F
            } else {
                grandTotalPullDownDistance * faction
            }
            moveContentView(endY)
        }
    }
}

enum class PullState(val v: Int) {
    UN_START(0),
    PULLING(1),
    EFFECTIVE(2),
    REFRESHING(3),
}