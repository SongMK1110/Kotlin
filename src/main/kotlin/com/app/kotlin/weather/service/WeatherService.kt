package com.app.kotlin.weather.service

import com.app.kotlin.common.CustomException
import com.app.kotlin.common.Grid
import com.app.kotlin.weather.dto.public.response.*
import com.app.kotlin.weather.dto.weather.response.ResponseWeatherDTO
import com.app.kotlin.weather.dto.weather.response.ResponseWeatherInfoDTO
import com.app.kotlin.weather.mapper.WeatherMapper
import com.google.gson.Gson
import org.json.XML
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow

@Service
class WeatherService(private val weatherMapper: WeatherMapper) {

    @Value("\${public-service-key}")
    private val key: String = ""

    fun weatherInfo(baseDate: String, baseTime: String, gridX: Double, gridY: Double): ResponseWeatherInfoDTO {
        val result = Grid.dfsXyConv("toXY", gridX, gridY)
        val nx: Int? = result["x"]?.toInt()
        val ny: Int? = result["y"]?.toInt()
        val minusMinutes = getMinusTime(baseTime, 40)

        // 일간 예보 정보
        val weatherApiUrl = weatherApiUrl(baseDate, minusMinutes, nx, ny)
        val weatherApi = fetchDataFromApi(weatherApiUrl)
        val publicDTO: PublicDTO = Gson().fromJson(weatherApi, PublicDTO::class.java)
        if (publicDTO.response.header.resultCode == "03") {
            throw CustomException("20")
        } else if (publicDTO.response.header.resultCode == "10") {
            throw CustomException("30")
        }


        var currentTemp = ""
        var humidity = ""
        var windDirection = ""
        var windVelocity = ""

        for (item in publicDTO.response.body.items.item) {
            when (item.category) {
                "T1H" -> {
                    currentTemp = item.obsrValue
                }

                "REH" -> {
                    humidity = item.obsrValue
                }

                "VEC" -> {
                    windDirection = item.obsrValue
                }

                "WSD" -> {
                    windVelocity = item.obsrValue
                }
            }
        }

        val windChill = String.format("%.1f", parseWindChill(currentTemp, windVelocity))
        windDirection = parseWindDirection(windDirection)

        // 전날 기온
        val yesterdayDate = getPreviousDate(baseDate, 1)
        val yesterdayTime = getPlusTime(baseTime, 40)
        val yesterdayWeatherApiUrl = weatherApiUrl(yesterdayDate, yesterdayTime, nx, ny)
        val yesterdayWeatherApi = fetchDataFromApi(yesterdayWeatherApiUrl)
        val yesterdayPublicDTO: PublicDTO = Gson().fromJson(yesterdayWeatherApi, PublicDTO::class.java)
        if (yesterdayPublicDTO.response.header.resultCode == "03") {
            throw CustomException("20")
        } else if (yesterdayPublicDTO.response.header.resultCode == "10") {
            throw CustomException("30")
        }

        var yesterdayTemp = ""
        for (item in yesterdayPublicDTO.response.body.items.item) {
            when (item.category) {
                "T1H" -> {
                    yesterdayTemp = item.obsrValue
                }
            }
        }

        val tempComparison = String.format("%.1f", (currentTemp.toDouble() - yesterdayTemp.toDouble()))

        // 미세먼지 정보
        val dustApiUrl = dustApiUrl()
        val dustApi = fetchDataFromApi(dustApiUrl)
        val publicDustDTO: PublicDustDTO = Gson().fromJson(dustApi, PublicDustDTO::class.java)

        var pm10Grade = ""
        var pm25Grade = ""

        for (item in publicDustDTO.response.body.items) {
            when (item.stationName) {
                "중구" -> {
                    pm10Grade = item.pm10Grade
                    pm25Grade = item.pm25Grade
                }
            }
        }

        // 자외선 정보
        val uvTime = baseDate + baseTime.substring(0, 2)
        val uvApiUrl = uvApiUrl(uvTime)
        val uvApi = fetchDataFromApi(uvApiUrl)
        val publicUVDTO: PublicUVDTO = Gson().fromJson(uvApi, PublicUVDTO::class.java)
        var uv = 0
        for (item in publicUVDTO.response.body.items.item) {
            uv = item.h0.toInt()
        }

        // 일몰 정보
        val sunApiUrl = sunApiUrl(baseDate, gridX, gridY)
        val sunApi = fetchDataFromApi(sunApiUrl)
        val xmlJSONObj: String = XML.toJSONObject(sunApi).toString()
        val publicSunDTO: PublicSunDTO = Gson().fromJson(xmlJSONObj, PublicSunDTO::class.java)
        val sunset = publicSunDTO.response.body.items.item.sunset

        // 단기 예보 정보
        val shortTermWeatherApiUrl = shortTermWeatherApiUrl(baseDate, nx, ny)
        val shortTermWeatherApi = fetchDataFromApi(shortTermWeatherApiUrl)
        val publicShortTermWeatherDTO: PublicShortTermWeatherDTO = Gson().fromJson(shortTermWeatherApi, PublicShortTermWeatherDTO::class.java)
        val hour = adjustToHour(baseTime)
        var sky = ""
        for (item in publicShortTermWeatherDTO.response.body.items.item) {
            if(item.category == "SKY" && item.fcstTime == hour){
                sky = item.fcstValue
                break
            }
        }

        val responseWeatherDTO = ResponseWeatherDTO(
            currentTemp = currentTemp,
            humidity = humidity,
            windDirection = windDirection,
            windVelocity = windVelocity,
            windChill = windChill,
            pm10Grade = parseGrade(pm10Grade),
            pm25Grade = parseGrade(pm25Grade),
            tempComparison = tempComparison,
            uv = parseUV(uv),
            sunset = formatTime(sunset),
            sky = parseSky(sky.toInt())
        )

        return ResponseWeatherInfoDTO(responseWeatherDTO)
    }

    // 하늘 상태
    fun parseSky(sky: Int): String {
        return when {
            sky >= 9 -> "흐림"
            sky >= 6 -> "구름맑음"
            sky >= 0 -> "맑음"
            else -> "알 수 없음"
        }
    }

    // 등급 계산 (미세먼지, 초미세먼지)
    fun parseGrade(grade: String): String {
        return when (grade) {
            "1" -> "좋음"
            "2" -> "보통"
            "3" -> "나쁨"
            "4" -> "매우나쁨"
            else -> "알 수 없음"
        }
    }

    // 자외선 단계
    fun parseUV(uv: Int): String {
        return when {
            uv >= 11 -> "위험"
            uv >= 8 -> "매우 높음"
            uv >= 6 -> "높음"
            uv >= 3 -> "보통"
            uv >= 0 -> "낮음"
            else -> "알 수 없음"
        }
    }


    // 체감 온도 계산
    fun parseWindChill(currentTemp: String, windVelocity: String): Double {
        val t: Double = currentTemp.toDouble()
        val w: Double = windVelocity.toDouble() * 3.6
        val num: Double = (13.12 + 0.6215 * t - 11.37 * w.pow(0.16) + 0.3965 * w.pow(0.16) * t)
        return num
    }

    // 풍향 계산
    fun parseWindDirection(windDirection: String): String {
        val result = ((windDirection.toDouble() + 22.5 * 0.5) / 22.5).toInt()

        return when (result) {
            0 -> "북"
            1 -> "북북동"
            2 -> "북동"
            3 -> "동북동"
            4 -> "동"
            5 -> "동동남"
            6 -> "동남"
            7 -> "남동남"
            8 -> "남"
            9 -> "남남서"
            10 -> "남서"
            11 -> "서남서"
            12 -> "서"
            13 -> "서북서"
            14 -> "북서"
            15 -> "북북서"
            16 -> "북"
            else -> "알 수 없음"
        }
    }

    // 분 빼기
    fun getMinusTime(inputTime: String, minusTime: Long): String {
        val formatter = DateTimeFormatter.ofPattern("HHmm")
        val time = LocalTime.parse(inputTime, formatter)
        val previousTime = time.minusMinutes(minusTime)

        return formatter.format(previousTime)
    }

    // 분 더하기
    fun getPlusTime(inputTime: String, plusTime: Long): String {
        val formatter = DateTimeFormatter.ofPattern("HHmm")
        val time = LocalTime.parse(inputTime, formatter)
        val previousTime = time.plusMinutes(plusTime)

        return formatter.format(previousTime)
    }

    // 전날
    fun getPreviousDate(inputDate: String, minusDate: Long): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val date = LocalDate.parse(inputDate, formatter)
        val previousDate = date.minusDays(minusDate)

        return formatter.format(previousDate)
    }

    // 일몰 시간 포맷
    fun formatTime(inputTime: String): String {
        val formatter = DateTimeFormatter.ofPattern("HHmm")
        val parsedTime = LocalTime.parse(inputTime, formatter)
        val formattedTime = DateTimeFormatter.ofPattern("HH:mm").format(parsedTime)
        return formattedTime
    }

    // ex) 13:10 -> 13:00
    fun adjustToHour(inputTime: String): String {
        val formatter = DateTimeFormatter.ofPattern("HHmm")
        val parsedTime = LocalTime.parse(inputTime, formatter)
        val adjustedTime = LocalTime.of(parsedTime.hour, 0)
        return formatter.format(adjustedTime)
    }

    // 공공 API
    fun fetchDataFromApi(urlBuilder: String): String {
        var rd: BufferedReader? = null
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlBuilder)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Content-type", "application/json")

            rd = if (conn.responseCode in 200..300) {
                BufferedReader(InputStreamReader(conn.inputStream))
            } else {
                BufferedReader(InputStreamReader(conn.errorStream))
            }

            val sb = StringBuilder()
            var line: String?
            while (rd.readLine().also { line = it } != null) {
                sb.append(line)
            }

            return sb.toString()

        } catch (e: IOException) {
            throw CustomException("100")
        } finally {
            rd?.close()
            conn?.disconnect()
        }
    }

    // 일간 예보 API
    fun weatherApiUrl(baseDate: String, minusMinutes: String, nx: Int?, ny: Int?): String {
        return "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst" +
                "?${URLEncoder.encode("serviceKey", StandardCharsets.UTF_8)}=$key" +
                "&${URLEncoder.encode("pageNo", StandardCharsets.UTF_8)}=1" +
                "&${URLEncoder.encode("numOfRows", StandardCharsets.UTF_8)}=1000" +
                "&${URLEncoder.encode("dataType", StandardCharsets.UTF_8)}=json" +
                "&${URLEncoder.encode("base_date", StandardCharsets.UTF_8)}=${
                    URLEncoder.encode(
                        baseDate,
                        StandardCharsets.UTF_8
                    )
                }" +
                "&${URLEncoder.encode("base_time", StandardCharsets.UTF_8)}=${
                    URLEncoder.encode(
                        minusMinutes,
                        StandardCharsets.UTF_8
                    )
                }" +
                "&${URLEncoder.encode("nx", StandardCharsets.UTF_8)}=${nx}" +
                "&${URLEncoder.encode("ny", StandardCharsets.UTF_8)}=${ny}"
    }

    // 미세먼지 API
    fun dustApiUrl(): String {
        return "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty" +
                "?${URLEncoder.encode("serviceKey", StandardCharsets.UTF_8)}=$key" +
                "&${URLEncoder.encode("returnType", StandardCharsets.UTF_8)}=json" +
                "&${URLEncoder.encode("numOfRows", StandardCharsets.UTF_8)}=100" +
                "&${URLEncoder.encode("pageNo", StandardCharsets.UTF_8)}=1" +
                "&${URLEncoder.encode("sidoName", StandardCharsets.UTF_8)}=${
                    URLEncoder.encode(
                        "서울",
                        StandardCharsets.UTF_8
                    )
                }" +
                "&${URLEncoder.encode("ver", StandardCharsets.UTF_8)}=1.0"
    }

    // 자외선 API
    fun uvApiUrl(time: String): String {
        return "https://apis.data.go.kr/1360000/LivingWthrIdxServiceV4/getUVIdxV4" +
                "?${URLEncoder.encode("serviceKey", StandardCharsets.UTF_8)}=$key" +
                "&${URLEncoder.encode("pageNo", StandardCharsets.UTF_8)}=1" +
                "&${URLEncoder.encode("numOfRows", StandardCharsets.UTF_8)}=10" +
                "&${URLEncoder.encode("dataType", StandardCharsets.UTF_8)}=json" +
                "&${URLEncoder.encode("areaNo", StandardCharsets.UTF_8)}=1100000000" +
                "&${URLEncoder.encode("time", StandardCharsets.UTF_8)}=$time"
    }

    // 일몰 API
    fun sunApiUrl(date: String, gridX: Double, gridY: Double): String {
        return "https://apis.data.go.kr/B090041/openapi/service/RiseSetInfoService/getLCRiseSetInfo" +
                "?${URLEncoder.encode("serviceKey", StandardCharsets.UTF_8)}=$key" +
                "&${URLEncoder.encode("locdate", StandardCharsets.UTF_8)}=$date" +
                "&${URLEncoder.encode("longitude", StandardCharsets.UTF_8)}=$gridY" +
                "&${URLEncoder.encode("latitude", StandardCharsets.UTF_8)}=$gridX" +
                "&${URLEncoder.encode("dnYn", StandardCharsets.UTF_8)}=Y"
    }

    // 단기예보 API
    fun shortTermWeatherApiUrl(date: String, nx: Int?, ny: Int?): String {
        return "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst" +
                "?${URLEncoder.encode("serviceKey", StandardCharsets.UTF_8)}=$key" +
                "&${URLEncoder.encode("pageNo", StandardCharsets.UTF_8)}=1" +
                "&${URLEncoder.encode("numOfRows", StandardCharsets.UTF_8)}=1000" +
                "&${URLEncoder.encode("dataType", StandardCharsets.UTF_8)}=json" +
                "&${URLEncoder.encode("base_date", StandardCharsets.UTF_8)}=$date" +
                "&${URLEncoder.encode("base_time", StandardCharsets.UTF_8)}=0500" +
                "&${URLEncoder.encode("nx", StandardCharsets.UTF_8)}=$nx" +
                "&${URLEncoder.encode("ny", StandardCharsets.UTF_8)}=$ny"

    }

}