package com.rg.eb

import android.content.res.Resources
import android.util.TypedValue

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/5/21 15:37
 */
private val TAG = "EasyRefreshBox"
fun String.log(secondTag:String=""){
    println("$TAG:$secondTag$this")
}
val Float.dp
get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,this,Resources.getSystem()
    .displayMetrics)
val Int.dp:Float
get() = this.toFloat().dp
