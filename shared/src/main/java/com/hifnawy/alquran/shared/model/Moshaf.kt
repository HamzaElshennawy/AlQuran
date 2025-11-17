package com.hifnawy.alquran.shared.model

import java.io.Serializable

/**
 * Represents a specific recitation version (moshaf) by a reciter.
 */
data class Moshaf(
        val id: Int,
        val name: String, // Name of the recitation (e.g., "حفص عن عاصم - مرتل")
        val server: String, // Base URL for the audio files
        val surah_total: Int, // Total number of surahs available in this moshaf
        val moshaf_type: Int,
        // A comma-separated string of surah numbers available for this moshaf
        val surah_list: String
) : Serializable
