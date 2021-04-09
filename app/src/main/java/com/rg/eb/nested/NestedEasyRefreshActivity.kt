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

        val simpleRefreshLayout = findViewById<SimpleRefreshLayout>(R.id.aer_simple_refresh_layout)
        simpleRefreshLayout.openPullDownRefresh = true
        simpleRefreshLayout.openPullUpLoadMore = true
        simpleRefreshLayout.onPullUpLoading = object : SimpleRefreshLayout.OnPullUpLoading {
            override fun onPullUpLoading() {
                val lastPosition = adapter.data.size - 1
                simpleRefreshLayout.postDelayed({
                    addData()
                    simpleRefreshLayout.pullUpLoadComplete()
                    "data add success ".log("EasyRefreshLayout")
                    rv.smoothScrollToPosition(lastPosition + 1)
                }, 2000L)
            }
        }
        simpleRefreshLayout.onPullDownLoading = object : SimpleRefreshLayout.OnPullDownLoading {
            override fun onPullDownLoading() {
                simpleRefreshLayout.postDelayed({
                    setNewData()
                    simpleRefreshLayout.pullDownLoadComplete()
                }, 2000L)
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
        for (i in size..size + 10) {
            adapter.data.add("number - $i")
        }
        adapter.notifyDataSetChanged()
    }
}