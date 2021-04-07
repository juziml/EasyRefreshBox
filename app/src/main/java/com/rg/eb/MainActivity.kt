package com.rg.eb

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.widget.NestedScrollView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {
    val swipeRefreshLayout: SwipeRefreshLayout? = null
    val nestedScrollView: NestedScrollView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val easyRefreshBox = findViewById<EasyRefreshBox>(R.id.easyRefreshBox)
        easyRefreshBox.pullDownRefreshListener = object : EasyRefreshBox.PullDownRefreshListener {
            override fun onPrepare() {

            }

            override fun onPulling() {

            }

            override fun onEffective() {

            }

            override fun onRefreshing() {
                "onRefreshing".log()
                easyRefreshBox.postDelayed({
                    easyRefreshBox.refreshComplete()
                }, 3000L)
            }

            override fun onEnding() {

            }

        }
    }
}