package com.rg.eb

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {
    val swipeRefreshLayout: SwipeRefreshLayout? = null
    val nestedScrollView: NestedScrollView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rv = findViewById<RecyclerView>(R.id.rv_content)
        val adapter = SimpleAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        for(i in 0..30){
            adapter.data.add("number - i")
        }
        adapter.notifyDataSetChanged()

        val easyRefreshBox = findViewById<EasyRefreshBox>(R.id.easyRefreshBox)
        easyRefreshBox.pullDownRefreshListener = object : EasyRefreshBox.PullDownRefreshListener {
            override fun onPrepare() {
                "onPrepare".log()
            }

            override fun onPulling(percent: Float) {
                "onPulling".log()
            }

            override fun onEffective() {
                "onEffective".log()
            }

            override fun onRefreshing() {
                "onRefreshing".log()
                easyRefreshBox.postDelayed({
                    easyRefreshBox.refreshComplete()
                }, 3000L)
            }

            override fun onEnding() {
                "onEnding".log()
            }

        }
    }
}