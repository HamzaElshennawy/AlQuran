package com.hifnawy.alquran.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

object TextUtil {

    fun highlightMatchingText(
            fullText: String,
            query: String,
            highlightColor: Color,
            defaultColor: Color
    ): AnnotatedString {
        if (query.isBlank()) return AnnotatedString(fullText)

        val queryChars = query.trim().lowercase().toSet()
        val highlightStyle = SpanStyle(color = highlightColor)
        val defaultStyle = SpanStyle(color = defaultColor)

        return buildAnnotatedString {
            fullText.forEach { char ->
                val isMatch = queryChars.contains(char.lowercaseChar())

                when {
                    isMatch -> withStyle(style = highlightStyle) { append(char) }
                    else    -> withStyle(style = defaultStyle) { append(char) }
                }
            }
        }
    }
}
