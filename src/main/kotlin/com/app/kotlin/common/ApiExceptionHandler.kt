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
        if (ex.message == "20") {
            return ApiResponse.error(ErrorCode.NO_DATA)
        } else if (ex.message == "30") {
            return ApiResponse.error(ErrorCode.CURRENT_DAY)
        }
        return ApiResponse.error(ErrorCode.FAIL)
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



