package com.fpondarts.foodie.ui.delivery.working


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.fpondarts.foodie.R

/**
 * A simple [Fragment] subclass.
 */
class WorkingFragment : Fragment() {


    var order_id:Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        //order_id = arguments!!.getLong("order_id")

        return inflater.inflate(R.layout.fragment_working, container, false)
    }


}
