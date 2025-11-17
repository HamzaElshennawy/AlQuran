package com.hifnawy.alquran.shared.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hifnawy.alquran.shared.R
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.warn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

object QuranAPI {

    private val ioCoroutineScope = CoroutineScope(Dispatchers.IO)

    private suspend fun sendRESTRequest(
            url: String,
            responseHandler: ((error: Boolean, errorType: KClass<out Exception>?, responseMessage: String) -> Unit)?
    ) {
        val client: OkHttpClient = OkHttpClient().newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            // .retryOnConnectionFailure(true)
            .build()
        val request: Request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("Accept", "application/json")
            .build()
        try {
            ioCoroutineScope
                .async {
                    client
                        .newCall(request)
                        .execute()
                }
                .await()
                .apply {
                    responseHandler?.invoke(false, null, body.string())
                }
        } catch (ex: Exception) {
            Timber.warn(ex.message, ex)
            responseHandler?.invoke(true, ex::class, "Connection failed with error: $ex")
        }
    }

    suspend fun getRecitersList(context: Context): List<Reciter> {
        var reciters = emptyList<Reciter>()

        sendRESTRequest("${context.getString(R.string.API_BASE_URL)}/${context.getString(R.string.API_RECITERS)}?language=ar") { error, _, responseMessage ->
            if (error) {
                Timber.warn(responseMessage)
                return@sendRESTRequest
            }
            val recitersJsonArray = JSONObject(responseMessage).getJSONArray(context.getString(R.string.API_RECITERS)).toString()

            reciters = Gson().fromJson(recitersJsonArray, object : TypeToken<List<Reciter>>() {}.type)
        }

        Timber.debug(reciters.joinToString(separator = "\n") { it.toString() })

        return reciters
    }

    suspend fun getSurahs(context: Context): List<Surah> {
        var surahs = emptyList<Surah>()

        sendRESTRequest("${context.getString(R.string.API_BASE_URL)}/${context.getString(R.string.API_SURAHS)}?language=ar") { error, _, responseMessage ->
            if (error) {
                Timber.warn(responseMessage)
                return@sendRESTRequest
            }
            val surahsJsonArray = JSONObject(responseMessage).getJSONArray(context.getString(R.string.API_SURAHS)).toString()

            surahs = Gson().fromJson(surahsJsonArray, object : TypeToken<List<Surah>>() {}.type)
        }

        Timber.debug(surahs.joinToString(separator = "\n") { it.toString() })

        return surahs
    }
}