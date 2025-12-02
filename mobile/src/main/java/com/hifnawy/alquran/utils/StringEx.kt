package com.hifnawy.alquran.utils

object StringEx {
    val String.snakeCase: String
        get() = this
            .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1_$2")
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(Regex("([0-9])([A-Z])"), "$1_$2")
            .lowercase()
}