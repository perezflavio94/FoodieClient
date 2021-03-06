package com.fpondarts.foodie.ui.home.confirm_order

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController

import com.fpondarts.foodie.R
import com.fpondarts.foodie.databinding.ConfirmOrderFragmentBinding
import com.fpondarts.foodie.ui.auth.AuthListener
import com.fpondarts.foodie.ui.FoodieViewModelFactory
import kotlinx.android.synthetic.main.card_choose_discount.*
import kotlinx.android.synthetic.main.confirm_order_fragment.*
import kotlinx.android.synthetic.main.confirm_order_fragment.view.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance
import kotlin.math.round

class ConfirmOrderFragment : Fragment(), AuthListener, KodeinAware {



    override fun onStarted() {


    }

    override fun onSuccess() {


    }

    override fun onFailure(message: String) {
        Toast.makeText(activity,message,Toast.LENGTH_LONG).show()
    }

    companion object {
        fun newInstance() =
            ConfirmOrderFragment()
    }


    override val kodein by kodein()

    val factory: FoodieViewModelFactory by instance()

    private var viewModel: ConfirmOrderViewModel? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: ConfirmOrderFragmentBinding = DataBindingUtil.inflate(inflater,R.layout.confirm_order_fragment,container,false)
        val viewModel = ViewModelProviders.of(this,factory).get(ConfirmOrderViewModel::class.java)
        binding.viewModel = viewModel
        this.viewModel = viewModel
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        rb_money_price.isChecked = true

        val price_with_discount = viewModel!!.repository.currentOrder!!.price * 0.8

        tv_price_with_discount.text = "$${(round(100.0 * price_with_discount) / 100.0).toString()}"

        button_confirm_order.setOnClickListener(View.OnClickListener {

            val discount = discount_checkbox.isChecked

            if (rb_money_price.isChecked){
                viewModel!!.confirmOrder(discount,false).observe(this, Observer {
                    it?.let {
                        if (it) {

                            val user_lat = viewModel!!.repository.currentOrder!!.latitude
                            val user_lon = viewModel!!.repository.currentOrder!!.longitude
                            val shop_id = viewModel!!.repository.currentOrder!!.shopId
                            val bundle = bundleOf("order_id" to viewModel!!.repository.currentOrder!!.id
                                ,"user_lat" to user_lat
                                ,"user_lon" to user_lon
                                ,"shop_id" to shop_id)

                            findNavController().navigate(R.id.action_confirmOrderFragment_to_deliveryMapFragment,bundle,
                                NavOptions.Builder().setPopUpTo(
                                    R.id.nav_home,
                                    false
                                ).build())
                        } else {
                            Toast.makeText(
                                activity,
                                "No pudo confirmarse la orden",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
            } else {
                viewModel!!.confirmOrder(discount,true).observe(this, Observer {
                    it?.let {
                        if (it) {

                            val user_lat = viewModel!!.repository.currentOrder!!.latitude
                            val user_lon = viewModel!!.repository.currentOrder!!.longitude
                            val shop_id = viewModel!!.repository.currentOrder!!.shopId

                            val bundle = bundleOf("order_id" to viewModel!!.repository.currentOrder!!.id
                                ,"user_lat" to user_lat
                                ,"user_lon" to user_lon
                                ,"shop_id" to shop_id
                                ,"isFavour" to true)

                            findNavController().navigate(R.id.action_confirmOrderFragment_to_deliveryMapFragment,bundle,
                                NavOptions.Builder().setPopUpTo(
                                    R.id.nav_home,
                                    false
                                ).build())
                        } else {
                            Toast.makeText(
                                activity,
                                "No pudo confirmarse la orden",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
            }

        })

    }

}
