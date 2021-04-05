package com.rg.eb

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/5/21 14:21
 */
class EasyRefreshBox:FrameLayout {
    constructor(context: Context) : this(context, null) {
        "constructor 1 ".log()
    }

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        "constructor 2 ".log()

    }

    private lateinit var contentView: View
    private lateinit var tvState: TextView
    override fun onFinishInflate() {
        super.onFinishInflate()
        contentView = findViewById(R.id.v_content)
        tvState = findViewById(R.id.tv_refreshState)
        tvState.tag = PullState.UN_EFFECT
    }

    init {
        "init ".log()
    }

    private val MIN_EFFECT_PULL_DOWN_Y = 20.dp

    private val MAX_PULL_DOWN_Y = 100.dp

    private var downY: Float = 0F
    override fun onTouchEvent(event: MotionEvent): Boolean {

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val moveY = event.y - downY
                moveContentView(moveY)
            }
            MotionEvent.ACTION_UP -> {
                downY = 0F
                moveContentView(0F)
            }
        }
        return true
    }

    private var grandTotal: Float = 0F

    /**
     * 使用translation 直接操作内容view
     * 这样只需要操作内容view的位置，而不用关心与其他同级view的状态
     */
    private fun moveContentView(y: Float) {
        contentView.translationY = y
        grandTotal = y


    }

    private fun handlerStatus(distance:Float){
        val absDistance = Math.abs(distance)
        val lastState:PullState = tvState.tag as PullState

    }

}
enum class PullState(val v:Int){
    UN_EFFECT(0),
    HOLDING(1),
    EFFECT(2),

}