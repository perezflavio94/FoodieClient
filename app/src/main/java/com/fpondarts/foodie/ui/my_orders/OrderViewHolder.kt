package com.fpondarts.foodie.ui.my_orders

import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fpondarts.foodie.R
import com.fpondarts.foodie.data.db.entity.Order
import com.google.android.material.card.MaterialCardView

class OrderViewHolder(inflater:LayoutInflater, parent:ViewGroup):
    RecyclerView.ViewHolder(inflater.inflate(R.layout.card_delivered_order,parent,false)) {

    private var mOrderId: TextView? = null;
    private var mDate: TextView? = null;
    private var mPrice: TextView? = null;
    private lateinit var mViewOrder: ImageView
    private lateinit var mCard: MaterialCardView

    init{
        mOrderId = itemView.findViewById(R.id.tv_order_id)
        mDate = itemView.findViewById(R.id.tv_order_date)
        mPrice = itemView.findViewById(R.id.tv_earnings)
        mCard = itemView.findViewById(R.id.order_card)
        mViewOrder = itemView.findViewById(R.id.view_button)
    }


    fun bind(order: Order, listener: OnMyOrderClickListener, active:Boolean){
        mOrderId!!.text = "Orden # ${order.order_id}"
        mDate!!.text = order.created_at.substring(0,17)
        val number2digits :Double = Math.round(order.price * 100.0) / 100.0
        mPrice!!.text = "$${number2digits.toString()}"
        mViewOrder.setOnClickListener(View.OnClickListener {
            listener.onActiveOrderClick(active,order.order_id,order.shop_id,order.delivery_id)
        })
    }
}