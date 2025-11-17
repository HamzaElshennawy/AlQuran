package com.hifnawy.alquran.view.screens

sealed class Screen(val route: String) {
    object Reciters: Screen("reciters_screen")
    object Surahs: Screen("surahs_screen")
}