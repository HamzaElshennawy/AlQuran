package com.hifnawy.alquran.utils

import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import kotlin.reflect.KClass

object RuntimeTypeAdapterFactoryEx {

    /**
     * Recursively finds all sealed subclasses of the given sealed class.
     *
     * @return A list of all sealed subclasses, including nested sealed classes.
     */
    private val <T : Any> KClass<T>.allSealedLeafSubclasses: List<KClass<out T>>
        get() = sealedSubclasses.flatMap { sub ->
            when {
                sub.isSealed -> sub.allSealedLeafSubclasses
                else         -> listOf(sub)
            }
        }

    /**
     * Returns the type field name used by the RuntimeTypeAdapterFactory.
     *
     * @return The type field name.
     */
    val RuntimeTypeAdapterFactory<*>.registeredTypeFieldName: String
        get() {
            val clazz = this::class.java
            val typeField = clazz.getDeclaredField("typeFieldName").apply { isAccessible = true }
            return typeField.get(this) as String
        }

    /**
     * Returns a string representation of the registered subtypes in the RuntimeTypeAdapterFactory.
     *
     * @return A string containing the registered subtypes and their corresponding labels.
     */
    @Suppress("UNCHECKED_CAST")
    val RuntimeTypeAdapterFactory<*>.registeredSubtypes: String
        get() {
            val clazz = this::class.java

            val labelToSubtypeField = clazz.getDeclaredField("labelToSubtype").apply { isAccessible = true }
            val subtypeToLabelField = clazz.getDeclaredField("subtypeToLabel").apply { isAccessible = true }

            val labelToSubtype = labelToSubtypeField.get(this) as Map<String, Class<*>>
            val subtypeToLabel = subtypeToLabelField.get(this) as Map<Class<*>, String>

            return buildString {
                appendLine("Subtype - Label:")
                labelToSubtype.forEach { label, subtype ->
                    appendLine("$label → ${subtype.name}")
                }

                appendLine("Label - Subtype:")
                subtypeToLabel.forEach { subtype, label ->
                    appendLine("${subtype.name} → $label")
                }
            }
        }

    /**
     * Extension function to automatically register all sealed subclasses
     * with Gson's RuntimeTypeAdapterFactory.
     *
     * @return A configured RuntimeTypeAdapterFactory for the sealed class.
     */
    @Suppress("UNCHECKED_CAST")
    val <T : Any> KClass<T>.registerSealedSubtypes: RuntimeTypeAdapterFactory<T>
        get() = registerSealedSubtypes("${this.simpleName?.lowercase()}_type")

    /**
     * Extension function to automatically register all sealed subclasses
     * with Gson's RuntimeTypeAdapterFactory.
     *
     * @param typeFieldName The name of the JSON field that holds the subtype tag (e.g., "type").
     *
     * @return A configured RuntimeTypeAdapterFactory for the sealed class.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> KClass<T>.registerSealedSubtypes(typeFieldName: String): RuntimeTypeAdapterFactory<T> {
        require(this.isSealed)

        var factory = RuntimeTypeAdapterFactory.of(this.java, typeFieldName)

        this.allSealedLeafSubclasses.forEach { kSubclass ->
            val tag = kSubclass.simpleName?.lowercase()
            factory = factory.registerSubtype(kSubclass.java as Class<T>, tag)
        }

        return factory
    }
}
