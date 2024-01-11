package com.app.kotlin.weather.dto.public.response

data class PublicDustForecastDTO (
    val response: ResponseDustForecast
)

data class ResponseDustForecast (
    val header: HeaderDustForecast,
    val body : BodyDustForecast
)

data class HeaderDustForecast (
    val resultCode : String,
    val resultMsg : String
)

data class BodyDustForecast (
    val dataType : String,
    val items : List<ItemDustForecast>,
    val pageNo : Int,
    val numOfRows: Int,
    val totalCount: Int
)

data class ItemDustForecast (
    val imageUrl1: String,
    val imageUrl2: String,
    val imageUrl3: String,
    val imageUrl4: String,
    val imageUrl5: String,
    val imageUrl6: String,
    val informCode: String,
    val informCause: String,
    val informOverall: String,
    val informData: String,
    val informGrade: String,
    val dataTime: String
)
