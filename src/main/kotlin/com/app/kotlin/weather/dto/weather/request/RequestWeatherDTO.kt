package com.app.kotlin.weather.dto.weather.request


import jakarta.validation.constraints.NotBlank


data class RequestWeatherDTO(
    @field:NotBlank(message = "name을 입력해주세요")
    val name: String?,
    @field:NotBlank(message = "text을 입력해주세요")
    val text: String?,
    val number: Int
)
