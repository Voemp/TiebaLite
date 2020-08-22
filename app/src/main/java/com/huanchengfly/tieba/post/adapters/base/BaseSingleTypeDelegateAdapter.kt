package com.huanchengfly.tieba.post.adapters.base

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.alibaba.android.vlayout.LayoutHelper
import com.huanchengfly.tieba.post.components.MyViewHolder

abstract class BaseSingleTypeDelegateAdapter<Item>(
        context: Context,
        layoutHelper: LayoutHelper
) : BaseDelegateAdapter<Item>(
        context, layoutHelper
) {
    protected abstract fun getItemLayoutId(): Int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder = MyViewHolder(context, getItemLayoutId())

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.setItemOnClickListener(View.OnClickListener {
            onItemClickListener?.onClick(holder, getItem(position), position)
        })
        holder.setItemOnLongClickListener(View.OnLongClickListener {
            onItemLongClickListener?.onLongClick(holder, getItem(position), position) ?: false
        })
        convert(holder, getItem(position), position)
    }

    protected abstract fun convert(viewHolder: MyViewHolder, item: Item, position: Int)
}