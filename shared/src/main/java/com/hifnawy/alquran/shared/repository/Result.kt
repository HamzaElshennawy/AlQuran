package com.hifnawy.alquran.shared.repository

import java.io.Serializable

sealed interface Error : Serializable
typealias RootError = Error

sealed interface Result<out D, out E : RootError> : Serializable {
    data class Success<out D, out E : RootError>(val data: D) : Result<D, E>, Serializable
    data class Error<out D, out E : RootError>(val error: E) : Result<D, E>, Serializable
}

sealed interface DataError : Error, Serializable {

    val errorMessage: String

    sealed interface NetworkError : DataError, Serializable {

        val errorCode: Int

        data class TooManyRequests(override val errorCode: Int, override val errorMessage: String) : NetworkError, Serializable
        data class PayloadTooLarge(override val errorCode: Int, override val errorMessage: String) : NetworkError, Serializable
        data class RequestTimeout(override val errorCode: Int, override val errorMessage: String) : NetworkError, Serializable
        data class Unreachable(override val errorCode: Int, override val errorMessage: String) : NetworkError, Serializable
        data class Unauthorized(override val errorCode: Int, override val errorMessage: String) : NetworkError, Serializable
        data class Forbidden(override val errorCode: Int, override val errorMessage: String) : NetworkError, Serializable
        data class NotFound(override val errorCode: Int, override val errorMessage: String) : NetworkError, Serializable
        data class ServerError(override val errorCode: Int, override val errorMessage: String) : NetworkError, Serializable
        data class Unknown(override val errorCode: Int, override val errorMessage: String) : NetworkError, Serializable
    }

    sealed interface LocalError : DataError, Serializable {
        data class DiskFull(override val errorMessage: String) : LocalError, Serializable
    }

    sealed interface ParseError : DataError, Serializable {
        data class JsonSyntaxException(override val errorMessage: String) : ParseError, Serializable
    }
}
