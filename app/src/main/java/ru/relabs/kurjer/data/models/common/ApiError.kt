package ru.relabs.kurjer.data.models.common

import com.google.gson.annotations.SerializedName
import ru.relabs.kurjer.utils.Either

/**
 * Created by Daniil Kurchanov on 20.11.2019.
 */
typealias EitherE<R> = Either<DomainException, R>

sealed class DomainException : Exception() {
    data class ApiException(val error: ApiError) : DomainException()
    object CanceledException : DomainException()
    object UnknownException : DomainException()
}

data class ApiErrorContainer(
    @SerializedName("error") val error: ApiError?
)

data class ApiError(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") private val dataInternal: Map<String, Any>?
) {
    val details: Map<String, Any>
        get() = dataInternal ?: emptyMap()

    //TODO: Add error codes
    companion object {
        const val ERROR_INVALID_DATE_TIME = 5

    }
}
