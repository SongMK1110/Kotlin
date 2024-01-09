package com.app.kotlin.weather.controller

import com.app.kotlin.common.ApiResponse
import com.app.kotlin.weather.dto.weather.response.ResponseWeatherInfoDTO
import com.app.kotlin.weather.service.WeatherService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class WeatherController(private val weatherService: WeatherService) {

    @GetMapping("/weather-info")
    @Operation(summary = "일간 예보 정보 조회")
    fun weatherInfo(
        @Parameter(description = "날짜", example = "yyyyMMdd") baseDate: String,
        @Parameter(description = "시간", example = "HHmm") baseTime: String,
        @Parameter(description = "위도") gridX: Double,
        @Parameter(description = "경도") gridY: Double
    ): ApiResponse<ResponseWeatherInfoDTO> {
        return ApiResponse.success(weatherService.weatherInfo(baseDate, baseTime, gridX, gridY))
    }
}
