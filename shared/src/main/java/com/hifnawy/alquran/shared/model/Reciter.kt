package com.hifnawy.alquran.shared.model

import java.io.Serializable

/**
 * Represents a single Quran reciter.
 */
data class Reciter(
        val id: Int,
        val name: String,
        val letter: String, // Likely the first letter of the name
        val date: String, // ISO 8601 formatted date string
        val moshaf: List<Moshaf> // A list of available recitations/versions (moshaf)
) : Serializable
