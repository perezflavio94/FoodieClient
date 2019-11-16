package com.fpondarts.foodie.ui.my_orders

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer

import com.fpondarts.foodie.R
import com.fpondarts.foodie.ui.FoodieViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance

class ActiveOrderFragment : Fragment(), KodeinAware, OnMapReadyCallback {

    companion object {
        fun newInstance() = ActiveOrderFragment()
    }

    override val kodein by kodein()

    val factory: FoodieViewModelFactory by instance()

    private lateinit var mMap:GoogleMap

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var orderId: Long? = null

    private var deliveryMarker:Marker? = null

    private lateinit var viewModel: ActiveOrderViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this,factory).get(ActiveOrderViewModel::class.java)
        return inflater.inflate(R.layout.fragment_active_order, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val map = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        map.getMapAsync(this)
        orderId = arguments!!.getLong("orderId")

        fusedLocationProviderClient.lastLocation.addOnSuccessListener {
            mMap.addMarker(MarkerOptions().draggable(false).position(LatLng(it.latitude,it.longitude)))
        }


        viewModel.getOrder(orderId!!).observe(this, Observer {
            it?.let {

                viewModel.getDelivery(it.delivery_id).observe(this, Observer {
                    it?.let {
                        val del = it
                        deliveryMarker?.let {
                            it.position = LatLng(del.latitude, del.longitude)
                        } ?: run {
                            deliveryMarker = mMap.addMarker(
                                MarkerOptions()
                                    .draggable(false)
                                    .position(LatLng(it.latitude, it.longitude))
                            )
                        }
                    }
                })

                viewModel.getShop(it.shop_id).observe(this, Observer {
                    it?.let {
                        mMap.addMarker(
                            MarkerOptions()
                                .draggable(false)
                                .position(LatLng(it.latitude, it.longitude))
                        )
                    }
                })
            }
        })

    }

    override fun onMapReady(p0: GoogleMap?) {
        mMap = p0 as GoogleMap
    }

}
