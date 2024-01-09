package com.app.kotlin.common

import java.lang.Math.pow
import kotlin.math.*

class Grid {
    companion object {
        private const val RE = 6371.00877 // 지구 반경(km)
        private const val GRID = 5.0 // 격자 간격(km)
        private const val SLAT1 = 30.0 // 투영 위도1(degree)
        private const val SLAT2 = 60.0 // 투영 위도2(degree)
        private const val OLON = 126.0 // 기준점 경도(degree)
        private const val OLAT = 38.0 // 기준점 위도(degree)
        private const val XO = 43 // 기준점 X좌표(GRID)
        private const val YO = 136 // 기준점 Y좌표(GRID)

        // LCC DFS 좌표변환 ( code : "toXY"(위경도->좌표, v1:위도, v2:경도), "toLL"(좌표->위경도,v1:x, v2:y) )
        fun dfsXyConv(code: String, v1: Double, v2: Double): Map<String, Double> {
            val DEGRAD = PI / 180.0
            val RADDEG = 180.0 / PI

            val re = RE / GRID
            val slat1 = SLAT1 * DEGRAD
            val slat2 = SLAT2 * DEGRAD
            val olon = OLON * DEGRAD
            val olat = OLAT * DEGRAD

            var sn = tan(PI * 0.25 + slat2 * 0.5) / tan(PI * 0.25 + slat1 * 0.5)
            sn = ln(cos(slat1) / cos(slat2)) / ln(sn)
            var sf = tan(PI * 0.25 + slat1 * 0.5)
            sf = sf.pow(sn) * cos(slat1) / sn
            var ro = tan(PI * 0.25 + olat * 0.5)
            ro = re * sf / ro.pow(sn)
            return if (code == "toXY") {
                var ra = tan(PI * 0.25 + v1 * DEGRAD * 0.5)
                ra = re * sf / ra.pow(sn)
                var theta = v2 * DEGRAD - olon
                if (theta > PI) theta -= 2.0 * PI
                if (theta < -PI) theta += 2.0 * PI
                theta *= sn
                val x = floor(ra * sin(theta) + XO + 0.5)
                val y = floor(ro - ra * cos(theta) + YO + 0.5)
                mapOf("lat" to v1, "lng" to v2, "x" to x, "y" to y)
            } else {
                val xn = v1 - XO
                val yn = ro - v2 + YO
                val ra = sqrt(xn.pow(2) + yn.pow(2))
                val alat = (re * sf / ra).pow(1.0 / sn)
                val theta = if (abs(xn) <= 0.0) 0.0 else atan2(xn, yn)
                val alon = (theta / sn + olon)
                mapOf("x" to v1, "y" to v2, "lat" to alat * RADDEG, "lng" to alon * RADDEG)
            }
        }
    }
}

