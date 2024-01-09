package com.app.kotlin.weather.dto.public.response

data class PublicShortTermWeatherDTO (
    val response: ResponseShort
)

data class ResponseShort (
    val header: HeaderShort,
    val body : BodyShort
)

data class HeaderShort (
    val resultCode : String,
    val resultMsg : String
)

data class BodyShort (
    val dataType : String,
    val items : ItemsShort,
    val pageNo : Int,
    val numOfRows: Int,
    val totalCount: Int
)

data class ItemsShort (
    val item: List<ItemShort>
)

data class ItemShort (
    val baseDate: String,
    val baseTime: String,
    val category: String,
    val nx: Int,
    val ny: Int,
    val fcstDate: String,
    val fcstTime: String,
    val fcstValue: String
)

