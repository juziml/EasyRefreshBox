package com.rg.eb.nested

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.rg.eb.R
import com.rg.eb.dp
import com.rg.eb.log

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/9/21 09:55
 */
class SimpleRefreshLayout : EasyRefreshLayout {
    private val TAG = "SimpleRefreshLayout"
    lateinit var  headView:TextView
    lateinit var  footView:TextView
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        headView = TextView(context)
        val hlp = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        hlp.topToTop = 0
        hlp.leftToLeft = 0
        headView.elevation = (-1).dp
        addView(headView,hlp)

        footView = TextView(context)
        val flp = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT)
        flp.bottomToBottom = 0
        flp.leftToLeft = 0
        footView.elevation = (-1).dp
        addView(footView,flp)
    }

    override val targetViewId: Int
        get() = R.id.aer_rv

    var onPullDownLoading: OnPullDownLoading? = null
    var onPullUpLoading: OnPullUpLoading? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        val view = findViewById<View>(targetViewId)
        val clp:LayoutParams = view.layoutParams as LayoutParams
        "l=${clp.leftToLeft} top=${clp.topToTop}".log(TAG)
    }

    override var pullDownLoadListener: PullLoadListener? = object :PullLoadListener{
        override fun onReset() {
            headView.visibility = View.GONE
        }

        override fun onPulling(distance: Float) {
            if(headView.visibility != View.VISIBLE){
                headView.visibility = View.VISIBLE
            }
            headView.text = "继续下拉刷新 $distance"
        }

        override fun onWaitToRelease() {
            headView.text = "松手 刷新"
        }

        override fun onLoading() {
            headView.text = "加载中..."
            onPullDownLoading?.onPullDownLoading()
        }

        override fun onEnding() {
            headView.text = "结束中..."
        }

        override fun onCanceling() {

        }

    }
    override var pullUpLoadListener: PullLoadListener? = object :PullLoadListener{
        override fun onReset() {
            footView.visibility = View.GONE
        }

        override fun onPulling(distance: Float) {
            if(footView.visibility != View.VISIBLE){
                footView.visibility = View.VISIBLE
            }
            footView.text = "继续下拉刷新 $distance"
        }

        override fun onWaitToRelease() {
            footView.text = "松手 刷新"
        }

        override fun onLoading() {
            footView.text = "加载中..."
            onPullUpLoading?.onPullUpLoading()
        }

        override fun onEnding() {
            footView.text = "结束中..."
        }

        override fun onCanceling() {
        }

    }





    interface OnPullDownLoading {
        fun onPullDownLoading()
    }

    interface OnPullUpLoading {
        fun onPullUpLoading()
    }
}
