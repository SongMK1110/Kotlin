package com.app.kotlin.common

data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) {
    companion object {
        fun <T> success(data: T? = null): ApiResponse<T> {
            return ApiResponse(0, "SUCCESS", data)
        }

        fun <T> error(errorCode: ErrorCode): ApiResponse<T> {
            return ApiResponse(errorCode.code, errorCode.message, null)
        }
    }
}

