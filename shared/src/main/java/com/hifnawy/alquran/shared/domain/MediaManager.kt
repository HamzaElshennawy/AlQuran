package com.hifnawy.alquran.shared.domain

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.shared.model.Surah
import com.hifnawy.alquran.shared.repository.QuranAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object MediaManager : LifecycleOwner {

    var onMediaReady: ((
            reciter: Reciter,
            surah: Surah,
            surahUri: Uri,
            surahDrawable: Drawable?
    ) -> Unit)? = null

    private var reciters: List<Reciter> = emptyList()
    private var surahs: List<Surah> = emptyList()
    private var surahsUri: List<Uri> = emptyList()
    private var currentReciter: Reciter? = null
    private var currentSurah: Surah? = null
    private val lifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    init {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun stopLifecycle() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun whenRecitersReady(context: Context, onReady: (reciters: List<Reciter>) -> Unit) = when {
        reciters.isEmpty() -> {
            lifecycleScope.launch(Dispatchers.IO) {
                reciters =
                        async { QuranAPI.getRecitersList(context) }.await().sortedBy { reciter -> reciter.id }
                withContext(Dispatchers.Main) {
                    onReady(reciters)
                }
            }

            false
        }

        else               -> {
            onReady(reciters)

            true
        }
    }

    fun whenSurahsReady(context: Context, onReady: (surahs: List<Surah>) -> Unit): Boolean = when {
        surahs.isEmpty() -> {
            lifecycleScope.launch(Dispatchers.IO) {
                surahs = async { QuranAPI.getSurahs(context) }.await().sortedBy { surah -> surah.id }
                withContext(Dispatchers.Main) {
                    onReady(surahs)
                }
            }

            false
        }

        else             -> {
            onReady(surahs)

            true
        }
    }

    fun whenReady(context: Context, onReady: (reciters: List<Reciter>, surahs: List<Surah>) -> Unit) = when {
        reciters.isEmpty() || surahs.isEmpty() -> {
            lifecycleScope.launch(Dispatchers.IO) {
                if (reciters.isEmpty()) reciters = async { QuranAPI.getRecitersList(context) }.await().sortedBy { reciter -> reciter.id }
                if (surahs.isEmpty()) surahs = async { QuranAPI.getSurahs(context) }.await().sortedBy { surah -> surah.id }

                withContext(Dispatchers.Main) {
                    onReady(reciters, surahs)
                }
            }

            false
        }

        else                                   -> {
            onReady(reciters, surahs)
            true
        }
    }

    fun whenReady(context: Context, reciterID: Int, onReady: (reciters: List<Reciter>, surahs: List<Surah>, surahsUri: List<Uri>) -> Unit) = when {
        reciters.isEmpty() || surahs.isEmpty() -> {
            lifecycleScope.launch(Dispatchers.IO) {
                if (reciters.isEmpty()) reciters = async { QuranAPI.getRecitersList(context) }.await().sortedBy { reciter -> reciter.id }

                if (surahs.isEmpty()) {
                    val reciter = reciters.first { it.id == reciterID }
                    val moshaf = reciter.moshaf.first()
                    val moshafSurahs = moshaf.surah_list.split(",").map { it.toInt() }

                    surahs = async { QuranAPI.getSurahs(context) }.await().filter { it.id in moshafSurahs }.sortedBy { surah -> surah.id }

                    surahsUri = getReciterSurahUris(reciterID, surahs)
                }

                withContext(Dispatchers.Main) {
                    onReady(reciters, surahs, surahsUri)
                }
            }

            false
        }

        else                                   -> {
            surahsUri = getReciterSurahUris(reciterID, surahs)

            onReady(reciters, surahs, surahsUri)

            true
        }
    }

    fun processSurah(context: Context, reciter: Reciter, surah: Surah) {
        @SuppressLint("DiscouragedApi")
        val drawableId = context.resources.getIdentifier("surah_${surah.id.toString().padStart(3, '0')}", "drawable", context.packageName)
        val moshaf = reciter.moshaf.first()
        val surahNum = surah.id.toString().padStart(3, '0')
        val surahUri = "${moshaf.server}$surahNum.mp3"

        currentReciter = reciter
        currentSurah = surah

        onMediaReady?.invoke(
                reciter,
                surah,
                surahUri.toUri(),
                AppCompatResources.getDrawable(context, drawableId)
        )
    }

    fun processNextSurah(context: Context) {
        val reciter = currentReciter ?: return
        val surah = currentSurah ?: return

        currentSurah = surahs.find {
            it.id == when (surah.id) {
                surahs.last().id -> 1
                else             -> surah.id + 1
            }
        }?.also { newSurah ->
            processSurah(context, reciter, newSurah)
        }
    }

    fun processPreviousSurah(context: Context) {
        val reciter = currentReciter ?: return
        val surah = currentSurah ?: return

        currentSurah = surahs.find {
            it.id == when (surah.id) {
                surahs.first().id -> surahs.last().id
                else              -> surah.id - 1
            }
        }?.also { newSurah ->
            processSurah(context, reciter, newSurah)
        }
    }

    private fun getReciterSurahUris(reciterID: Int, reciterSurahs: List<Surah>): List<Uri> {
        val reciter = reciters.first { it.id == reciterID }
        val moshaf = reciter.moshaf.first()
        val moshafServer = moshaf.server

        return reciterSurahs.map {
            val surahNum = it.id.toString().padStart(3, '0')
            "${moshafServer}$surahNum.mp3".toUri()
        }
    }
}
