package com.rg.eb.nested

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.RotateAnimation
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
    lateinit var  headView:View
    lateinit var  headText:TextView
    lateinit var  headBear:View
    lateinit var  footView:TextView
    val headBearAnim = ObjectAnimator()
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        headView = View.inflate(context,R.layout.header_simple,null)
        headText = headView.findViewById(R.id.hs_tv_txt)
        headBear = headView.findViewById(R.id.hs_im_bear)
        headBear.scaleY = 0.3F
        headBear.scaleX =  0.3F
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
        headBearAnim.target = headBear
        headBearAnim.setPropertyName("rotationY")
        headBearAnim.setFloatValues(0F,360F)
        headBearAnim.duration = 1000L
        headBearAnim.interpolator = AccelerateDecelerateInterpolator()
        headBearAnim.repeatCount = Animation.INFINITE
        val typeArray = context.obtainStyledAttributes(attributeSet,R.styleable.SimpleRefreshLayout)
        targetViewId = typeArray.getResourceId(R.styleable.SimpleRefreshLayout_targetViewId,-1)
        "targetViewId =$targetViewId".log(TAG)
    }
    override var targetViewId: Int

    var onPullDownLoading: OnPullDownLoading? = null
    var onPullUpLoading: OnPullUpLoading? = null
    private var pullDownPercent = 0F

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        //布局设置完完 设置下拉边界
        thresholdReleaseToLoadingDowY = headView.height.toFloat()* 1.2F
        maxPullDownY = thresholdReleaseToLoadingDowY * 1.2F
    }

    override var pullDownLoadListener: PullLoadListener? = object :PullLoadListener{
        override fun onReset() {
            headBear.scaleY = 0.3F
            headBear.scaleX =  0.3F
            headView.visibility = View.GONE
        }

        override fun onPulling(distance: Float) {
            pullDownPercent = distance / thresholdReleaseToLoadingDowY
            if(headView.visibility != View.VISIBLE){
                headView.visibility = View.VISIBLE
            }
            headText.text = "继续下拉刷新"
            if(pullDownPercent in 0.3 .. 1.0){
                headBear.scaleY = pullDownPercent
                headBear.scaleX = pullDownPercent
            }
        }

        override fun onWaitToRelease() {
            headText.text = "松手 刷新"
        }

        override fun onLoading() {
            headBearAnim.start()
            headText.text = "加载中..."
            onPullDownLoading?.onPullDownLoading()
        }

        override fun onEnding() {
            headBearAnim.cancel()
            headText.text = "结束中..."
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
