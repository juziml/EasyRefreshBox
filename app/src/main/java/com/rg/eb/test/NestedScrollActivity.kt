package com.rg.eb.test

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
class NestedScrollActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nested_scroll)
        val rv = findViewById<RecyclerView>(R.id.rv_nestedView)
        val adapter = SimpleAdapter()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        for (i in 0..30) {
            adapter.data.add("number - $i")
        }
        adapter.notifyDataSetChanged()
    }
}