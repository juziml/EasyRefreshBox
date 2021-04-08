package com.rg.eb

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.NestedScrollingParent2
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView

/**
 *@Desc:
 *create by zhusw on 4/8/21 11:31
 */
class NestedScrollingSimpleView(context: Context, attributeSet: AttributeSet) : LinearLayout(context, attributeSet),
        NestedScrollingParent2 {
    private lateinit var mHeader: View
    private lateinit var mTarget: View
    private var mHeaderHeight = 0

    private val mNestedScrollingParentHelper by lazy { NestedScrollingParentHelper(this) }
    private var rvScrollCount = 0F
    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount > 0) {
            mHeader = getChildAt(0)
            // 遍历出RecyclerView
            for (i in 0 until childCount) {
                if (getChildAt(i) is RecyclerView) {
                    mTarget = getChildAt(i)
                    break
                }
            }
        }
        val rv = mTarget as RecyclerView
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                rvScrollCount+= dy
            }
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mHeaderHeight = mHeader.measuredHeight
        // 需要正确的测量出RecyclerView在header折叠时的高度，不然RecyclerView显示不全
        mTarget.measure(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(
                        mHeaderHeight + mTarget.measuredHeight,
                        MeasureSpec.EXACTLY)
        )
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        // 允许竖直方向上的滑动
        return isEnabled && (axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0)
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        // 代理给NestedScrollingParentHelper
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type)
    }

    //在响应嵌套滑动钱做一些预处理，比如读取子view滑动极限状态初始化
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {

        //能否继续下滑
        val targetCanDown = target.canScrollVertically(-1)
        //能否继续上滑
        val targetCanUp = target.canScrollVertically(1)

        // dy大于0说明在向上滑动，此时需要判断header是不是还能显示
        val canScrollUp = dy > 0 && mHeaderHeight > scrollY
        // dy小于0说明在向下滑动，此时需要判断header是不是已经显示完整
        val canScrollDown = dy < 0 && rvScrollCount<=0F
        if (canScrollUp || canScrollDown) {
            scrollBy(0, dy)
            consumed[1] = dy
        }
        "NestedScrollingSimpleView onNestedPreScroll targetCanDown=$targetCanDown targetCanUp=$targetCanUp".log()
    }

    override fun onNestedScroll(
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            type: Int
    ) {


    }

    override fun onStopNestedScroll(target: View, type: Int) {
        // 代理给NestedScrollingParentHelper
        mNestedScrollingParentHelper.onStopNestedScroll(target, type)
    }

    override fun scrollTo(x: Int, y: Int) {
        val clampY = Math.max(0, Math.min(y, mHeaderHeight))
        super.scrollTo(x, clampY)
    }

}