package com.fpondarts.foodie.data.repository

import android.util.JsonReader
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fpondarts.foodie.data.db.FoodieDatabase
import com.fpondarts.foodie.data.db.entity.*
import com.fpondarts.foodie.data.parser.RoutesParser
import com.fpondarts.foodie.data.repository.interfaces.OrderRepository
import com.fpondarts.foodie.data.repository.interfaces.RepositoryInterface
import com.fpondarts.foodie.data.repository.interfaces.ShopRepository
import com.fpondarts.foodie.model.Coordinates
import com.fpondarts.foodie.model.Directions
import com.fpondarts.foodie.network.DirectionsApi
import com.fpondarts.foodie.network.FcmApi
import com.fpondarts.foodie.network.FoodieApi
import com.fpondarts.foodie.network.SafeApiRequest
import com.fpondarts.foodie.network.request.ChangePasswordRequest
import com.fpondarts.foodie.network.request.StateChangeRequest
import com.fpondarts.foodie.network.request.UpdateFcmRequest
import com.fpondarts.foodie.network.request.UpdatePictureRequest
import com.fpondarts.foodie.network.response.SuccessResponse
import com.fpondarts.foodie.util.Coroutines
import com.fpondarts.foodie.util.exception.FoodieApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import java.lang.Exception
import org.json.JSONObject
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.fpondarts.foodie.network.fcm_data.FcmMessageData


class DeliveryRepository(
    private val api:FoodieApi,
    private val directionsApi: DirectionsApi,
    private val fcmApi: FcmApi,
    private val db:FoodieDatabase
): SafeApiRequest()
    ,PositionUpdater
    ,RepositoryInterface{

    val apiError = MutableLiveData<FoodieApiException>().apply {
        value = null
    }

    var token:String? = null
    var userId:Long? = null

    var currentUser = MutableLiveData<Delivery>().apply {
        value = null
    }

    val isWorking = MutableLiveData<Boolean>().apply {
        value = false
    }

    var current_order:Long = -1

    fun refreshUser(){
        initUser(this.token!!,this.userId!!)
    }

    fun initUser(token:String, id:Long){
        if (token==null || id == null){
            return
        }
        this.token = token
        userId = id
        Coroutines.io{
            try {
                db.getOrderDao().nukeTable()
                val user = apiRequest{ api.getDelivery(token,id) }
                currentUser.postValue(user)
                if (user.state == "working"){
                    current_order = user.current_order!!
                    isWorking.postValue(true)
                }

            } catch (e:FoodieApiException) {
                apiError.postValue(e)
                if (e.code > 500){
                    initUser(token,id)
                }
            }
        }

    }

    fun getRoute(origin: LatLng, destination:LatLng, waypoint:LatLng? ): LiveData<Directions>{
        val response = MutableLiveData<Directions>().apply {
            value = null
        }
        Coroutines.io{
            try{
                val originStr = origin.latitude.toString() + "," + origin.longitude.toString()
                val destinationStr = destination.latitude.toString() + "," + destination.longitude.toString()

                var waypointStr = waypoint?.latitude.toString() + "," + waypoint?.longitude.toString()

                if (waypoint == null)
                    waypointStr = ""
                val directions = apiRequest{ directionsApi.getRoute(originStr,destinationStr,waypointStr) }

                response.postValue(directions)


            } catch (e:FoodieApiException){
                apiError.postValue(e)
            }


        }

        return response
    }

    fun updateFcmToken(token:String):LiveData<Boolean>{
        val liveBool = MutableLiveData<Boolean>().apply {
            value = null
        }
        Coroutines.io {
            while (liveBool.value!! == null){
                try {
                    val res = apiRequest { api.patchFcmToken(token!!,userId!!, UpdateFcmRequest(token)) }
                    liveBool.postValue(true)
                } catch (e: FoodieApiException){
                    if (e.code!=500){
                        liveBool.postValue(false)
                        apiError.postValue(e)
                    }
                }
            }
        }
        return liveBool
    }


    fun getCurrentOffers():LiveData<List<Offer>>{
        val offers = MutableLiveData<List<Offer>>().apply {
            value = null
        }
        Coroutines.io {
            try{
                val apiResponse = apiRequest { api.getCurrentOffers(token!!,userId!!) }
                offers.postValue(apiResponse)
            } catch (e:FoodieApiException){
                apiError.postValue(e)
                offers.postValue(ArrayList<Offer>())
            }
        }
        return offers
    }

    fun acceptOffer(offer_id:Long):LiveData<Boolean>{

        val successResponse = MutableLiveData<Boolean>().apply{
            value = null
        }

        Coroutines.io {
            try{
                val apiResponse = apiRequest {api.changeOfferState(token!!,userId!!,offer_id, StateChangeRequest("accepted")) }
                successResponse.postValue(true)
            } catch (e:FoodieApiException){
                apiError.postValue(e)
                successResponse.postValue(false)
            }
        }

        return successResponse
    }

    fun rejectOffer(offer_id:Long):LiveData<Boolean>{
        val successResponse = MutableLiveData<Boolean>().apply{
            value = null
        }
        Coroutines.io{
            try{
                val apiResponse = apiRequest{api.changeOfferState(token!!,userId!!,offer_id,
                    StateChangeRequest("rejected")
                )}
                successResponse.postValue(true)
            } catch (e:FoodieApiException){
                apiError.postValue(e)
                successResponse.postValue(false)
            }
        }
        return successResponse
    }

    override fun getOrder(order_id:Long):LiveData<Order>{
        val liveData = MutableLiveData<Order>().apply {
            value = null
        }
        Coroutines.io{
            try{
                val response = apiRequest { api.getOrder(token!!,order_id) }
                liveData.postValue(response)

            } catch (e:FoodieApiException){
                apiError.postValue(e)
            }
        }
        return liveData
    }

    override fun getOrderItems(order_id:Long):LiveData<List<OrderItem>>{
        var liveData = db.getOrderItemDao().getOrderItems(order_id)
        if (liveData.value.isNullOrEmpty()){
            liveData = MutableLiveData<List<OrderItem>>().apply {
                value = null
            }
            Coroutines.io{
                try {
                    val apiResponse = apiRequest { api.getOrderItems(token!!,order_id) }
                    liveData.postValue(apiResponse)
                    db.getOrderItemDao().upsert(apiResponse)
                } catch (e:FoodieApiException){
                    apiError.postValue(e)
                }
            }
        }
        return liveData
    }

    override fun getMenu(shop_id:Long):LiveData<List<MenuItem>>{
        val menu = db.getMenuItemDao().loadMenu(shop_id)
        if (menu.value.isNullOrEmpty()){
            Coroutines.io {
                try {
                    val apiResponse = apiRequest { api.getMenu(token!!, shop_id) }
                    db.getMenuItemDao().upsert(apiResponse)
                } catch (e: FoodieApiException) {
                    apiError.postValue(e)
                }
            }
        }
        return menu
    }

    override fun getMenuItem(product_id:Long):LiveData<MenuItem>{
        val item = db.getMenuItemDao().loadItem(product_id)
        if (item.value == null){
            Coroutines.io{
                try{
                    val apiResponse = apiRequest { api.getProduct(token!!,product_id) }
                    db.getMenuItemDao().upsert(apiResponse)
                } catch (e :FoodieApiException){
                    apiError.postValue(e)
                }
            }
        }
        return item
    }

    fun changeOrderState(order_id:Long,state:String="delivered"):LiveData<Boolean>{
        val liveData = MutableLiveData<Boolean>().apply {
            value = null
        }
        Coroutines.io{
            try {
                val apiResponse = apiRequest { api.finishOrder(token!!,order_id,StateChangeRequest(state)) }
                liveData.postValue(true)
            } catch (e:FoodieApiException){
                apiError.postValue(e)
                liveData.postValue(false)
            }
        }
        return liveData
    }

    override fun updatePosition (latitude:Double,longitude:Double):LiveData<Boolean>{
        val liveData = MutableLiveData<Boolean>().apply {
            value = null
        }
        Coroutines.io {
            token?.let {
                try {
                    val apiResponse = apiRequest {
                        api.updateCoordinates(token!!,userId!!,
                            Coordinates(latitude ,longitude)
                        )
                    }
                    liveData.postValue(true)
                } catch (e:FoodieApiException){
                    apiError.postValue(e)
                    liveData.postValue(false)
                }
            }
        }
            return liveData
    }



    fun refreshOrder(order_id: Long):LiveData<Order>{
        val liveData = db.getOrderDao().getOrder(order_id)
        Coroutines.io {
            try {
                val response = apiRequest { api.getOrder(token!!,order_id) }
                db.getOrderDao().upsert(response)
            } catch (e:FoodieApiException){
                if (e.code == 500){
                    delay(500)
                    refreshOrder(order_id)
                }
            }
        }
        return liveData
    }

    override fun getDeliveredByMe():LiveData<List<Order>>{
        val liveData = db.getOrderDao().getAll()

        if (liveData.value.isNullOrEmpty()){
            Coroutines.io {
                try {
                    val response = apiRequest { api.getDeliveredBy(token!!,userId!!) }
                    db.getOrderDao().upsert(response)
                } catch (e: FoodieApiException){
                    if (e.code == 500){
                        delay(500)
                        getDeliveredByMe()
                    }
                }
            }


        }


        return liveData
    }

    override fun getShop(id:Long):LiveData<Shop>{
        val shop = db.getShopDao().loadShop(id)
        shop.value?: Coroutines.io {
            try{
                val fetched = apiRequest { api.getShop(token!!,id) }
                db.getShopDao().upsert(fetched)
            } catch (e: FoodieApiException){
                apiError.postValue(e)
            }

        }
        return shop
    }

    fun changePassword(new_pass:String):LiveData<SuccessResponse>{
        val live = MutableLiveData<SuccessResponse>().apply {
            value = null
        }
        Coroutines.io{
            try {
                val apiResponse = apiRequest { api.changePassword(token!!,userId!!,
                    ChangePasswordRequest(new_pass)
                ) }
                live.postValue(apiResponse)
            } catch (e:FoodieApiException){
                apiError.postValue(e)
                live.postValue(SuccessResponse(e.message,400))
            }
        }
        return live
    }


    fun updatePic(url:String):LiveData<SuccessResponse>{
        val live = MutableLiveData<SuccessResponse>().apply {
            value = null
         }
        Coroutines.io{
            try{
                val apiResp = apiRequest { api.updateUserPicture(token!!,userId!!,
                    UpdatePictureRequest(url)
                ) }
                live.postValue(apiResp)
            } catch (e: FoodieApiException){
                apiError.postValue(e)
            }
        }
        return live
    }

    override fun getUser(user_id:Long):LiveData<User>{
        val liveUser = MutableLiveData<User>().apply {
            value = null
        }
        Coroutines.io {
            try {
                val response = apiRequest { api.getUserById(token!!,user_id) }
                liveUser.postValue(response)
            } catch (e:FoodieApiException) {
                liveUser.postValue(null)
                apiError.postValue(e)
            }
        }
        return liveUser
    }

    fun notifyOrderDelivered(userFbId:String,order_id:Long):LiveData<Boolean>{
        val live = MutableLiveData<Boolean>().apply {
            value = null
        }
        Coroutines.io{
            try {
                val jsonObject = JsonObject()
                jsonObject.addProperty("to","/topics/$userFbId")
                val data = JsonObject()
                data.addProperty("title","Orden $order_id")
                data.addProperty("message","La orden ha sido entregada")
                jsonObject.add("data",data)
                val res = apiRequest { fcmApi.pushNotification(jsonObject) }
                live.postValue(true)
            } catch (e:Exception){
                live.postValue(false)
            }
        }
        return live
    }


    fun notifyOrderPickedUp(userFbId:String,order_id:Long):LiveData<Boolean>{
        val live = MutableLiveData<Boolean>().apply {
            value = null
        }
        Coroutines.io{
            try {
                val jsonObject = JsonObject()
                jsonObject.addProperty("to","/topics/$userFbId")
                val data = JsonObject()
                data.addProperty("title","Orden $order_id")
                data.addProperty("message","La orden ha sido recogida de la tienda")
                jsonObject.add("data",data)
                val res = apiRequest { fcmApi.pushNotification(jsonObject) }
                live.postValue(true)
            } catch (e:Exception){
                live.postValue(false)
            }
        }
        return live
    }




}