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
        setContentView(R.layout.activity_main_v2)

        val rv = findViewById<RecyclerView>(R.id.rv_contentV2)
        val adapter = SimpleAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        for (i in 0..10) {
            adapter.data.add("number - $i")
        }
        adapter.notifyDataSetChanged()

        val easyRefreshBox = findViewById<EasyRefreshBoxV2>(R.id.easyRefreshBoxV2)
        easyRefreshBox.pullDownRefreshListener = object : EasyRefreshBoxV2.PullDownRefreshListener {
            override fun onPrepare() {
                "pullDown onPrepare".log()
            }

            override fun onPulling(percent: Float) {
                "pullDown onPulling".log()
            }

            override fun onEffective() {
                "pullDown onEffective".log()
            }

            override fun onRefreshing() {
                "pullDown onRefreshing".log()
                easyRefreshBox.postDelayed({
                    adapter.data.clear()
                    for (i in 0..10) {
                        adapter.data.add("number - $i")
                    }
                    adapter.notifyDataSetChanged()
                    easyRefreshBox.refreshComplete()
                }, 3000L)
            }

            override fun onEnding() {
                "pullDown onEnding".log()
            }

        }
        easyRefreshBox.pullUpLoadMoreListener = object : EasyRefreshBoxV2.PullUpLoadMoreListener {
            override fun onPrepare() {
                "pullUp onPrepare".log()
            }

            override fun onLoading() {
                "pullUp onLoading".log()
                easyRefreshBox.postDelayed({
                    for (i in adapter.data.size - 1..adapter.data.size + 10) {
                        adapter.data.add("number - $i")
                    }
                    adapter.notifyDataSetChanged()
                    easyRefreshBox.loadMoreComplete()
                }, 2000L)
            }

            override fun onEnding() {
                "pullUp onEnding".log()
            }

        }
    }
}