package com.app.kotlin.weather.dto.weather.response

data class ResponseMonthlyWeatherDTO (
    val month: String,
    val mTitle: String,
    val week : String,
    val wTitle: String,
    val content: String
)
