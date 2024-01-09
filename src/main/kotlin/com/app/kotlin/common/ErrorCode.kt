package com.app.kotlin.common

enum class ErrorCode(val code: Int, val message: String) {
    FAIL(999, "실패"),
    BAD_REQUEST(10, ""),
    NO_DATA(20, "데이터가 없습니다."),
    CURRENT_DAY(30, "최근 1일 간의 자료만 제공합니다."),
    PUBLIC_API_ERROR(100, "공공 API 요청 오류."),
    SERVER_ERROR(500, "")
}
