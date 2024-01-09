package com.app.kotlin.weather.dto.weather.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "일간 예보 정보 응답 DTO")
data class ResponseWeatherDTO(
    @Schema(description = "현재 기온")
    val currentTemp : String,
    @Schema(description = "체감 온도")
    val windChill: String,
    @Schema(description = "습도")
    val humidity: String,
    @Schema(description = "풍향")
    val windDirection: String,
    @Schema(description = "풍속")
    val windVelocity: String,
    @Schema(description = "미세먼지 등급")
    val pm10Grade: String,
    @Schema(description = "초미세먼지 등급")
    val pm25Grade: String,
    @Schema(description = "전날 온도 비교")
    val tempComparison: String,
    @Schema(description = "자외선 지수")
    val uv: String,
    @Schema(description = "일몰 시간")
    val sunset: String,
    @Schema(description = "하늘 상태")
    val sky: String
)


@Schema(description = "날씨 정보")
data class ResponseWeatherInfoDTO(
    @Schema(description = "날씨 정보")
    val weatherInfo: ResponseWeatherDTO,
)
