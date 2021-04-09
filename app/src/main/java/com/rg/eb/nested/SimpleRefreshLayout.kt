package com.rg.eb.nested

import android.content.Context
import android.util.AttributeSet
import com.rg.eb.R

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/9/21 09:55
 */
class SimpleRefreshLayout : EasyRefreshLayout {

    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {



    }

    override val targetViewId: Int
        get() = R.id.aer_rv
    override var pullDownLoadListener: PullLoadListener? = object :PullLoadListener{
        override fun onReset() {

        }

        override fun onPulling(distance: Float) {
        }

        override fun onWaitToRelease() {
        }

        override fun onLoading() {
            onPullDownLoading?.onPullDownLoading()
        }

        override fun onEnding() {
        }

        override fun onCanceling() {
        }

    }
    override var pullUpLoadListener: PullLoadListener? = object :PullLoadListener{
        override fun onReset() {

        }

        override fun onPulling(distance: Float) {
        }

        override fun onWaitToRelease() {
        }

        override fun onLoading() {
            onPullUpLoading?.onPullUpLoading()
        }

        override fun onEnding() {
        }

        override fun onCanceling() {
        }

    }

    var onPullDownLoading: OnPullDownLoading? = null
    var onPullUpLoading: OnPullUpLoading? = null


    interface OnPullDownLoading {
        fun onPullDownLoading()
    }

    interface OnPullUpLoading {
        fun onPullUpLoading()
    }
}
