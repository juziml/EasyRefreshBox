package com.rg.eb

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

/**
 *@Desc:
 *-
 *-
 *create by zhusw on 4/7/21 15:17
 */
class SimpleAdapter: BaseQuickAdapter<String, BaseViewHolder> {
    constructor():super(R.layout.item_word,null)
    override fun convert(holder: BaseViewHolder, item: String) {
        
    }
}