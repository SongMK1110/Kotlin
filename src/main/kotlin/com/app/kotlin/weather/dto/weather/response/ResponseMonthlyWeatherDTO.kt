package com.app.kotlin.weather.dto.weather.response

data class ResponseSelectMonthlyWeatherDTO (
    val title: String,
    val month: String
)

data class ResponseMonthlyWeatherDTO (
    val title: String,
    val weeklyWeatherInfo: List<ResponseWeeklyWeatherDTO>
)

data class ResponseWeeklyWeatherDTO (
    val title: String,
    val duration: String?,
    val content: String,
    val avgTemperature: String,
    val avgPrecipitation: String
)
data class ResponseMonthlyWeatherInfoDTO (
    val monthlyWeatherInfo: List<ResponseMonthlyWeatherDTO>
)
