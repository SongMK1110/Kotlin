package com.app.kotlin.weather.mapper

import com.app.kotlin.weather.dto.weather.response.ResponseSelectMonthlyWeatherDTO
import com.app.kotlin.weather.dto.weather.response.ResponseWeeklyWeatherDTO

interface WeatherMapper {

    fun monthlyWeather(): List<ResponseSelectMonthlyWeatherDTO>
    fun weeklyWeather(month: String): List<ResponseWeeklyWeatherDTO>

}