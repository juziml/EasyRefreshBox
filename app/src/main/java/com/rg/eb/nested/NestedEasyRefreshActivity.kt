package com.rg.eb.nested

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rg.eb.R
import com.rg.eb.SimpleAdapter
import com.rg.eb.log

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
        easyRefreshLayout.pullDownLoadListener = object :PullLoadListener{

            override fun onReset() {

            }

            override fun onPulling(percent: Float) {
            }
            override fun onWaitToRelease() {

            }
            override fun onLoading() {
                easyRefreshLayout.postDelayed({
                    setNewData()
                    easyRefreshLayout.pullDownLoadComplete()
                },2000L)

            }
            override fun onEnding() {

            }

            override fun onCanceling() {

            }
        }
        easyRefreshLayout.pullUpLoadListener = object :PullLoadListener{

            override fun onReset() {

            }

            override fun onPulling(percent: Float) {
            }
            override fun onWaitToRelease() {

            }
            override fun onLoading() {
                "onLoading".log("EasyRefreshLayout")
                easyRefreshLayout.postDelayed({
                    addData()
                    easyRefreshLayout.pullUpLoadComplete()
                    "data add success ".log("EasyRefreshLayout")
                },2000L)

            }
            override fun onEnding() {

            }

            override fun onCanceling() {

            }
        }
    }

    fun setNewData() {
        adapter.data.clear()
        for (i in 0..10) {
            adapter.data.add("number - $i")
        }
        adapter.notifyDataSetChanged()
    }

    fun addData() {
        val size = adapter.data.size
        for (i in size..size+10) {
            adapter.data.add("number - $i")
        }
        adapter.notifyDataSetChanged()
    }
}