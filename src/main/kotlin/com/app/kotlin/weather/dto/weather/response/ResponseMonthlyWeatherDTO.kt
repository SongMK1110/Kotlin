package com.app.kotlin.weather.dto.weather.response

import io.swagger.v3.oas.annotations.media.Schema

data class ResponseSelectMonthlyWeatherDTO (
    val title: String,
    val month: String
)

@Schema(description = "월간 날씨 정보")
data class ResponseMonthlyWeatherDTO (
    @Schema(description = "월간 제목")
    val title: String,
    @Schema(description = "주간 날씨 정보")
    val weeklyWeatherInfo: List<ResponseWeeklyWeatherDTO>
)

@Schema(description = "주간 날씨 정보")
data class ResponseWeeklyWeatherDTO (
    @Schema(description = "주간 제목")
    val title: String,
    @Schema(description = "주간 기간")
    val duration: String?,
    @Schema(description = "주간 내용")
    val content: String,
    @Schema(description = "주간 평년기온")
    val avgTemperature: String,
    @Schema(description = "주간 평년강수")
    val avgPrecipitation: String
)

@Schema(description = "월간 날씨 정보")
data class ResponseMonthlyWeatherInfoDTO (
    val monthlyWeatherInfo: List<ResponseMonthlyWeatherDTO>
)
