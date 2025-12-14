package com.hifnawy.alquran.shared.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Surah(
        val id: Int = -1,
        val name: String = "",
        @SerializedName("start_page")
        val startPage: Int = -1,
        @SerializedName("end_page")
        val endPage: Int = -1,
        val makkia: Int = -1,
        val type: Int = -1,
        var uri: String? = null
) : Serializable
