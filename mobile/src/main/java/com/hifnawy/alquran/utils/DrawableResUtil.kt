package com.hifnawy.alquran.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.DrawableRes

object DrawableResUtil {
    @DrawableRes
    @SuppressLint("DiscouragedApi")
    fun getSurahDrawableId(context: Context, surahId: Int? = null): Int {
        val surahNum = surahId.toString().padStart(3, '0')
        val resourceName = surahId?.let { "surah_$surahNum" } ?: "surah_name"

        return context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
}
