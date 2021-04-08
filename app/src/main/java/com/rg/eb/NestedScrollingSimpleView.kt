package com.rg.eb
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat

/**
 *@Desc:
 *create by zhusw on 4/8/21 11:31
 */
class NestedScrollingSimpleView(context: Context, attributeSet: AttributeSet):ConstraintLayout(context,attributeSet)
        ,NestedScrollingParent3 {
    private val nestedScrollingParentHelper by lazy {
        NestedScrollingParentHelper(this)
    }
    init {
    }
    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        nestedScrollingParentHelper.onNestedScrollAccepted(child,target,axes,type)
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        nestedScrollingParentHelper.onNestedScrollAccepted(child,target,axes)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        "NestedScrollingSimpleView onStartNestedScroll axex=$axes".log()
            return true
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean {
        "NestedScrollingSimpleView onStartNestedScroll axex=$axes".log()
        return true
    }

    override fun onNestedFling(target: View, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
      return false
    }

    override fun getNestedScrollAxes(): Int {
        return nestedScrollingParentHelper.nestedScrollAxes
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (type == ViewCompat.TYPE_TOUCH) {
            "NestedScrollingSimpleView onNestedPreScroll====1 dy=$dy".log()
            onNestedPreScroll(target, dx, dy, consumed)
        }
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        "NestedScrollingSimpleView onNestedPreScroll=====2 dy=$dy".log()
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {
        "NestedScrollingSimpleView onNestedScroll dyUnconsumed=$dyUnconsumed".log()
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) {
        "NestedScrollingSimpleView onNestedScroll===2 dyUnconsumed=$dyUnconsumed".log()
    }

    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) {
        "NestedScrollingSimpleView onNestedScroll===3 dyUnconsumed=$dyUnconsumed".log()
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
       return false
    }

    override fun onStopNestedScroll(target: View, type: Int) {
       nestedScrollingParentHelper.onStopNestedScroll(target,type)
    }

    override fun onStopNestedScroll(target: View) {
        nestedScrollingParentHelper.onStopNestedScroll(target)
    }


}