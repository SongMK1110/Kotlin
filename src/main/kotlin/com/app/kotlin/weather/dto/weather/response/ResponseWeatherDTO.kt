package com.app.kotlin.weather.dto.weather.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "일간 예보 정보 응답 DTO")
data class ResponseWeatherDTO(
    @Schema(description = "현재 기온")
    val currentTemp: String,
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

@Schema(description = "내일 날씨 정보 응답 DTO")
data class ResponseTomorrowWeatherDTO(
    @Schema(description = "오전 기온")
    val morningTemp: String,
    @Schema(description = "오전 하늘 상태")
    val morningSky: String,
    @Schema(description = "오전 강수 확률")
    val morningPoP: String,
    @Schema(description = "오전 미세먼지 등급")
    val morningPm10Grade: String,
    @Schema(description = "오전 초미세먼지 등급")
    val morningPm25Grade: String,
    @Schema(description = "오후 기온")
    val afternoonTemp: String,
    @Schema(description = "오후 하늘 상태")
    val afternoonSky: String,
    @Schema(description = "오후 강수 확률")
    val afternoonPoP: String,
    @Schema(description = "오후 미세먼지")
    val afternoonPm10Grade: String,
    @Schema(description = "오후 초미세먼지")
    val afternoonPm25Grade: String
)

@Schema(description = "모레 날씨 정보 응답 DTO")
data class ResponseDatWeatherDTO(
    @Schema(description = "오전 기온")
    val morningTemp: String,
    @Schema(description = "오전 하늘 상태")
    val morningSky: String,
    @Schema(description = "오전 강수 확률")
    val morningPoP: String,
    @Schema(description = "오전 미세먼지 등급")
    val morningPm10Grade: String,
    @Schema(description = "오전 초미세먼지 등급")
    val morningPm25Grade: String,
    @Schema(description = "오후 기온")
    val afternoonTemp: String,
    @Schema(description = "오후 하늘 상태")
    val afternoonSky: String,
    @Schema(description = "오후 강수 확률")
    val afternoonPoP: String,
    @Schema(description = "오후 미세먼지")
    val afternoonPm10Grade: String,
    @Schema(description = "오후 초미세먼지")
    val afternoonPm25Grade: String
)

@Schema(description = "시간 별 날씨 정보 응답 DTO")
data class ResponseShortTermWeatherDTO(
    @Schema(description = "기온")
    val tmp: String,
    @Schema(description = "하늘 상태")
    val sky: String,
    @Schema(description = "시간")
    val time: String,
    @Schema(description = "날짜")
    val date: String
)


@Schema(description = "날씨 정보")
data class ResponseWeatherInfoDTO(
    @Schema(description = "날씨 정보")
    val weatherInfo: ResponseWeatherDTO,
    @Schema(description = "내일 날씨 정보")
    val tomorrowWeatherInfo: ResponseTomorrowWeatherDTO,
    @Schema(description = "모레 날씨 정보")
    val datWeatherInfo: ResponseDatWeatherDTO,
    @Schema(description = "시간 별 날씨 정보")
    val shortTermWeatherInfo: List<ResponseShortTermWeatherDTO>?

)
