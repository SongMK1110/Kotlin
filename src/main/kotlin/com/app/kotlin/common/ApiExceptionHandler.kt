package com.app.kotlin.common

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class APIExceptionHandler {

    private val log = KotlinLogging.logger {}

    @ExceptionHandler(CustomException::class)
    @ResponseStatus(HttpStatus.OK)
    fun handleCustomException(ex: CustomException): ApiResponse<String> {
        log.error("CustomException: ${ex.stackTraceToString()}")
        return when (ex.message) {
            "20" -> ApiResponse.error(ErrorCode.NO_DATA)
            "30" -> ApiResponse.error(ErrorCode.CURRENT_DAY)
            "100" -> ApiResponse.error(ErrorCode.PUBLIC_API_ERROR)
            else -> ApiResponse.error(ErrorCode.FAIL)
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.OK)
    fun handleValidationException(ex: MethodArgumentNotValidException): ApiResponse<String> {
        val bindingResult = ex.bindingResult
        if (bindingResult.hasErrors()) {
            val errorMsg = bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "오류"
            return ApiResponse.error(ErrorCode.BAD_REQUEST)
        } else {
            return ApiResponse.error(ErrorCode.BAD_REQUEST)
        }
    }
}



