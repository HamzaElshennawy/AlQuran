package com.hifnawy.alquran.shared.utils

import android.content.Intent
import android.os.Build
import android.os.Bundle
import java.io.Serializable

class SerializableExt {
    companion object {

        inline fun <reified GenericType : Serializable> Intent.getTypedSerializable(key: String): GenericType? =
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializableExtra(
                            key, GenericType::class.java
                    )

                    else                                                  -> @Suppress("DEPRECATION") getSerializableExtra(key) as? GenericType
                }

        inline fun <reified GenericType : Serializable> Bundle.getTypedSerializable(key: String): GenericType? =
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(
                            key, GenericType::class.java
                    )

                    else                                                  -> @Suppress("DEPRECATION") getSerializable(key) as? GenericType
                }
    }
}