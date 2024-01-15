package com.app.kotlin.weather.controller

import com.app.kotlin.common.ApiResponse
import com.app.kotlin.weather.dto.weather.response.ResponseMonthlyWeatherDTO
import com.app.kotlin.weather.dto.weather.response.ResponseTomorrowWeatherDTO
import com.app.kotlin.weather.dto.weather.response.ResponseWeatherInfoDTO
import com.app.kotlin.weather.service.WeatherService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class WeatherController(private val weatherService: WeatherService) {

    @GetMapping("/weather-info")
    @Operation(summary = "일간 예보 정보 조회")
    fun weatherInfo(
        @Parameter(description = "날짜", example = "yyyyMMdd") @Valid @NotBlank(message = "날짜를 입력해주세요") @RequestParam baseDate: String,
        @Parameter(description = "시간", example = "HHmm") @Valid @NotBlank(message = "시간을 입력해주세요") @RequestParam baseTime: String,
        @Parameter(description = "위도") @Valid @NotNull(message = "위도를 입력해주세요") @RequestParam gridX: Double,
        @Parameter(description = "경도") @Valid @NotNull(message = "경도를 입력해주세요") @RequestParam gridY: Double
    ): ApiResponse<ResponseWeatherInfoDTO> {
        return ApiResponse.success(weatherService.weatherInfo(baseDate, baseTime, gridX, gridY))
    }

    @GetMapping("/test")
    fun test(): List<ResponseMonthlyWeatherDTO> {
        return weatherService.test()
    }

}
