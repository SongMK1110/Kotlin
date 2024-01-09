package com.app.kotlin.weather.dto.public.response


data class PublicSunDTO (
    val response: ResponseSun
)

data class ResponseSun(
    val header: HeaderSun,
    val body: BodySun
)

data class HeaderSun(
    val resultCode: String,
    val resultMsg: String
)

data class BodySun(
    val items: ItemsSun,
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Int
)

data class ItemsSun(
    val item: ItemSun
)

data class ItemSun(
    val aste: String,
    val astm: String,
    val civile: String,
    val civilm: String,
    val latitude: String,
    val latitudeNum: String,
    val location: String,
    val locdate: String,
    val longitude: String,
    val longitudeNum: String,
    val moonrise: String,
    val moonset: String,
    val moontransit: String,
    val naute: String,
    val nautm: String,
    val sunrise: String,
    val sunset: String,
    val suntransit: String
)
