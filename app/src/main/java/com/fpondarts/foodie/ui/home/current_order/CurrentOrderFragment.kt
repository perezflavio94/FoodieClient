package com.fpondarts.foodie.ui.home.current_order

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager

import com.fpondarts.foodie.R
import com.fpondarts.foodie.databinding.CurrentOrderFragmentBinding
import com.fpondarts.foodie.model.NamedOrderItem
import com.fpondarts.foodie.model.OrderItem
import com.fpondarts.foodie.ui.FoodieViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.current_order_fragment.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class CurrentOrderFragment : BottomSheetDialogFragment(), KodeinAware,
    OnOrderItemClickListener {


    override fun onItemClick(id:Long) {
        viewModel?.removeFromOrder(id)
        adapterList.removeIf {
            it.id == id
        }
        current_order_recycler_view.adapter!!.notifyDataSetChanged()
    }


    override val kodein by kodein()

    val factory: FoodieViewModelFactory by instance()

    companion object {
        fun newInstance() = CurrentOrderFragment()
    }



    private var adapterList = ArrayList<NamedOrderItem>()
    private var viewModel: CurrentOrderViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: CurrentOrderFragmentBinding = DataBindingUtil.inflate(inflater,R.layout.current_order_fragment,container,false)
        viewModel = ViewModelProviders.of(this,factory).get(CurrentOrderViewModel::class.java)
        binding.viewModel = viewModel
        return inflater.inflate(R.layout.current_order_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel!!.getCurrentOrder()


        button_fin_pedido.setOnClickListener(View.OnClickListener {
            if (viewModel!!.repository.currentOrder!!.isEmpty()){
                Toast.makeText(activity,"El pedido esta vacío",Toast.LENGTH_LONG).show()
            } else {
                Navigation.findNavController(parentFragment!!.view!!).navigate(R.id.deliveryAddressFragment)
                dismiss()
            }

        })

        current_order_recycler_view.apply {
            layoutManager = LinearLayoutManager(activity)
        }

        MutableLiveData<MutableCollection<OrderItem>>().apply {
            value = viewModel!!.getCurrentOrder().items!!.values
        }.observe(this, Observer{
            it?.let{

                adapterList = ArrayList<NamedOrderItem>()
                val order = viewModel!!.getCurrentOrder()
                for (item in it){
                    val name = order.names.get(item.product_id)
                    val price = order.prices.get(item.product_id)
                    adapterList.add(NamedOrderItem(item.product_id,item.units,name!!,price))

                }

                current_order_recycler_view.adapter =
                    CurrentOrderAdapter(adapterList, this)
                current_order_recycler_view.adapter!!.notifyDataSetChanged()
            }

        })
    }

}
