package com.fpondarts.foodie.ui.home.current_order

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fpondarts.foodie.model.NamedOrderItem
import com.fpondarts.foodie.model.OrderItem

class CurrentOrderAdapter(private val list : Collection<NamedOrderItem>, val listener: OnOrderItemClickListener)
    : RecyclerView.Adapter<CurrentOrderViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrentOrderViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return CurrentOrderViewHolder(inflater, parent)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: CurrentOrderViewHolder, position: Int) {
        val item: NamedOrderItem = list.elementAt(position)
        holder.bind(item,listener)
    }


}
