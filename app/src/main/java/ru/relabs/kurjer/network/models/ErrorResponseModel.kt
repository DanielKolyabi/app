package ru.relabs.kurjer.network.models

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.HttpException

/**
 * Created by ProOrange on 05.09.2018.
 */

abstract class ResponseWithErrorModel {
    abstract val error: ResponseErrorModel?
}

data class ResponseErrorModel(
        val code: Int,
        val message: String
)

data class ErrorModel(
     val error: ResponseErrorModel?
)

object ErrorUtils{
    fun getError(e: HttpException): ResponseErrorModel{
        try {
            val serializedError = e.response().errorBody()?.string()
            return Gson().fromJson(serializedError, ErrorModel::class.java).error!!
        }catch (e: Exception){
            e.printStackTrace()
        }

        return ResponseErrorModel(-1, "Не известная ошибка.")
    }
}