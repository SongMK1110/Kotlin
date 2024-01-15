package com.app.kotlin.weather.dto.public.response

data class PublicKakaoMapDTO(
    val meta: Meta,
    val documents: List<Document>
)

data class Meta(
    val total_count: Int,
)

data class Document(
    val region_type: String,
    val address_name: String,
    val region_1depth_name: String,
    val region_2depth_name: String,
    val region_3depth_name: String,
    val region_4depth_name: String,
    val code: String,
    val x: Double,
    val y: Double
)

//data class Documents(
//    val road_address: RoadAddress,
//    val address: Address
//)

//data class RoadAddress(
//    val address_name: String,
//    val region_1depth_name: String,
//    val region_2depth_name: String,
//    val region_3depth_name: String,
//    val road_name: String,
//    val underground_yn: String,
//    val main_building_no: String,
//    val sub_building_no: String,
//    val building_name: String,
//    val zone_no: String
//)
//
//data class Address(
//    val address_name: String,
//    val region_1depth_name: String,
//    val region_2depth_name: String,
//    val region_3depth_name: String,
//    val mountain_yn: String,
//    val main_address_no: String,
//    val sub_address_no: String,
//    val zip_code: String
//)