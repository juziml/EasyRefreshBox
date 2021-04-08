package com.rg.eb.nested

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.rg.eb.PullDownState
import com.rg.eb.dp
import com.rg.eb.log

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/8/21 15:24
 */
class NestedEasyRefreshLayout(context: Context, attributeSet: AttributeSet)
    : ConstraintLayout(context, attributeSet),
        NestedScrollingParent2 {
    private val TAG = "NestedEasyRefreshLayout"
    private val nestedScrollingParentHelper by lazy { NestedScrollingParentHelper(this) }
    private var targetViewPullDownLimit = true
    private var targetViewPullUpnLimit = true
    private lateinit var targetView: View
    private var totalPullDownY = 0F
    private var pullDownState = PullDownState.STATE_PREPARE
    private val DAMP_FACTOR = 0.7F
    private val MAX_PULL_DOWN_Y = 200.dp

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
        if (!this::targetView.isInitialized) NullPointerException("NestedEasyRefreshLayout has not targetView!")
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
        "dy=$dy type=$type 能否继续下拉=$targetViewPullDownLimit 能否继续上拉=$targetViewPullUpnLimit".log(TAG)
        //屏蔽抛投
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            "丢弃 TYPE_NON_TOUCH".log(TAG)
            return
        }
        if (dy < 0 && targetViewPullDownLimit) {
            totalPullDownY+= dy
            handlerPullDownRefresh(-dy.toFloat())

        } else if (dy > 0 && targetViewPullUpnLimit) {

        }

    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        nestedScrollingParentHelper.onStopNestedScroll(target, type)
        totalPullDownY = 0F
        handlerPullDownRefresh(0F)
    }

    /**
     * 下移 targetView
     * @param translationY 必须是正值
     */
    private fun handlerPullDownRefresh(translationY: Float) {
        var y = translationY * DAMP_FACTOR
        if (y > MAX_PULL_DOWN_Y) {
            y = MAX_PULL_DOWN_Y
        }
        if(targetView.translationY != y){
            targetView.translationY = y
        }
    }
    /**
     * 上移 targetView
     * @param translationY 必须是负值
     */
    private fun handlerPullUpRefresh(dy: Int) {

    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (pullDownState == PullDownState.STATE_REFRESHING) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

}