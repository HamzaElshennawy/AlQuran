package com.hifnawy.alquran.shared.model

import java.io.Serializable

data class Surah(
        val id: Int,
        val name: String,
        val start_page: Int,
        val end_page: Int,
        val makkia: Int,
        val type: Int
) : Serializable
