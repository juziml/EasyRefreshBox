package com.rg.eb.nested

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rg.eb.R
import com.rg.eb.SimpleAdapter

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/8/21 11:57
 */
class NestedEasyRefreshActivity : AppCompatActivity() {
    private lateinit var rv: RecyclerView
    private val adapter = SimpleAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_easy_refresh)
        rv = findViewById(R.id.aer_rv)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        setNewData()

        val easyRefreshLayout = findViewById<EasyRefreshLayout>(R.id.aer_easy_refresh_layout)
        easyRefreshLayout.openPullDownRefresh = true
        easyRefreshLayout.openPullUpLoadMore = true
        easyRefreshLayout.pullDownRefreshListener = object :PullDownRefreshListener{

            override fun onReset() {

            }

            override fun onPulling(percent: Float) {
            }
            override fun onWaitToRelease() {

            }
            override fun onLoading() {
                easyRefreshLayout.postDelayed({
                    setNewData()
                    easyRefreshLayout.pullDownRefreshComplete()
                },2000L)

            }
            override fun onEnding() {

            }
        }
    }

    fun setNewData() {
        adapter.data.clear()
        for (i in 0..30) {
            adapter.data.add("number - $i")
        }
        adapter.notifyDataSetChanged()
    }

}