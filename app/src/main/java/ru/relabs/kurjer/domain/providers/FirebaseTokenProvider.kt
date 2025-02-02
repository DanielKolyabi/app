package ru.relabs.kurjer.domain.providers

import android.os.Parcelable
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.parcelize.Parcelize
import ru.relabs.kurjer.domain.storage.AppPreferences
import ru.relabs.kurjer.utils.Either
import ru.relabs.kurjer.utils.instanceIdAsync
import java.io.Serializable

@Parcelize
data class FirebaseToken(val token: String) : Parcelable, Serializable

class FirebaseTokenProvider(
    private val appPreferences: AppPreferences
) {
    suspend fun set(token: FirebaseToken) {
        appPreferences.saveFirebaseToken(token)
    }

    suspend fun get(): Either<Exception, FirebaseToken> = Either.of {
        val savedToken = appPreferences.getFirebaseToken()
        if(savedToken != null){
            savedToken
        }else{

            val token = FirebaseMessaging.getInstance().instanceIdAsync().let{
                FirebaseToken(it)
            }
            set(token)
            token
        }
    }
}