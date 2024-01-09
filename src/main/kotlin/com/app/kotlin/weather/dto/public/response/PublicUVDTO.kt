package com.app.kotlin.weather.dto.public.response

data class PublicUVDTO(
    val response: ResponseUV
)

data class ResponseUV (
    val header: HeaderUV,
    val body : BodyUV
)

data class HeaderUV (
    val resultCode : String,
    val resultMsg : String
)

data class BodyUV (
    val dataType : String,
    val items : ItemsUV,
    val pageNo : Int,
    val numOfRows: Int,
    val totalCount: Int
)

data class ItemsUV (
    val item: List<ItemUV>
)

data class ItemUV (
    val code: String,
    val areaNo: String,
    val date: String,
    val h0: String,
    val h3: String,
    val h6: String,
    val h9: String,
    val h12: String,
    val h15: String,
    val h18: String,
    val h21: String,
    val h24: String,
    val h27: String,
    val h30: String,
    val h33: String,
    val h36: String,
    val h39: String,
    val h42: String,
    val h45: String,
    val h48: String,
    val h51: String,
    val h54: String,
    val h57: String,
    val h60: String,
    val h63: String,
    val h66: String,
    val h69: String,
    val h72: String,
    val h75: String
)

