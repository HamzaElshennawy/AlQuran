package com.hifnawy.alquran.shared.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import com.hifnawy.alquran.shared.R
import androidx.core.graphics.withTranslation

object ImageUtil {

    fun (@receiver:DrawableRes Int).drawTextOn(
            context: Context,
            text: String,
            subText: String = "",
            @FontRes fontFace: Int = R.font.decotype_thuluth_2,
            fontSize: Float,
            fontMargin: Int
    ): Bitmap {
        var bitmap = BitmapFactory.decodeResource(context.resources, this)
        val bitmapConfig = bitmap.config as Bitmap.Config
        bitmap = bitmap.copy(bitmapConfig, true)
        val canvas = Canvas(bitmap)
        val textWidth = canvas.width - fontMargin
        val fontFamily = context.resources.getFont(fontFace)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontFamily
            color = Color.WHITE
            textSize = fontSize
        }
        val subTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontFamily
            color = Color.YELLOW
            textSize = fontSize * 0.7f
        }
        val textStaticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 2f)
            .build()
        var allTextHeight = textStaticLayout.height
        lateinit var subTextStaticLayout: StaticLayout
        if (subText.isNotEmpty()) {
            subTextStaticLayout = StaticLayout.Builder
                .obtain(subText, 0, subText.length, subTextPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 2f)
                .build()
            allTextHeight += subTextStaticLayout.height
        }
        val x = (bitmap.width - textWidth) / 2f
        var y = (bitmap.height - allTextHeight) / 2f

        canvas.withTranslation(x, y) {
            textStaticLayout.draw(this)

            if (subText.isNotEmpty()) {
                y = ((bitmap.height - textStaticLayout.height) * 1.5f) - (subTextStaticLayout.height * 0.5f)

                translate(x, y)
                subTextStaticLayout.draw(this)
            }

        }

        return bitmap
    }
}
