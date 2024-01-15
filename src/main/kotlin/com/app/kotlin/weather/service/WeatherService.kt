package com.app.kotlin.weather.service

import com.app.kotlin.common.CustomException
import com.app.kotlin.common.Grid
import com.app.kotlin.weather.dto.public.response.*
import com.app.kotlin.weather.dto.weather.response.*
import com.app.kotlin.weather.mapper.WeatherMapper
import com.google.gson.Gson
import org.json.XML
import org.springframework.beans.factory.annotation.Autowired
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
class WeatherService @Autowired constructor(private val weatherMapper: WeatherMapper) {

    @Value("\${public-service-key}")
    private val key = ""

    @Value("\${kakao-key}")
    private val kakaoKey = ""

    fun weatherInfo(baseDate: String, baseTime: String, gridX: Double, gridY: Double): ResponseWeatherInfoDTO {
        val result = Grid.dfsXyConv("toXY", gridX, gridY)
        val nx: Int? = result["x"]?.toInt()
        val ny: Int? = result["y"]?.toInt()
        val minusMinutes = getMinusTime(baseTime, 40)

        // 카카오 주소 정보
        val kakaoMapApiUrl = kakaoAddressApiUrl(gridX, gridY)
        val kakaoMapApi = fetchDataFromApi(kakaoMapApiUrl, "kakao")
        val kakaoMapDTO: PublicKakaoMapDTO = Gson().fromJson(kakaoMapApi, PublicKakaoMapDTO::class.java)
        val address = kakaoMapDTO.documents[1].address_name

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
        if (publicDustDTO.response.header.resultCode == "03") {
            throw CustomException("20")
        } else if (publicDTO.response.header.resultCode == "10") {
            throw CustomException("30")
        }

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
        if (publicUVDTO.response.header.resultCode == "03") {
            throw CustomException("20")
        } else if (publicDTO.response.header.resultCode == "10") {
            throw CustomException("30")
        }
        var uv = 0
        for (item in publicUVDTO.response.body.items.item) {
            uv = item.h0.toInt()
        }

        // 일몰 정보
        val sunApiUrl = sunApiUrl(baseDate, gridX, gridY)
        val sunApi = fetchDataFromApi(sunApiUrl)
        val xmlJSONObj: String = XML.toJSONObject(sunApi).toString()
        val publicSunDTO: PublicSunDTO = Gson().fromJson(xmlJSONObj, PublicSunDTO::class.java)
        if (publicSunDTO.response.header.resultCode == "03") {
            throw CustomException("20")
        } else if (publicDTO.response.header.resultCode == "10") {
            throw CustomException("30")
        }
        val sunset = publicSunDTO.response.body.items.item.sunset

        // 단기 예보 정보
        val shortTermTime = getMinusTime(getTimeRange(baseTime), 180)
        val shortTermWeatherApiUrl = shortTermWeatherApiUrl(baseDate, shortTermTime, nx, ny)
        val shortTermWeatherApi = fetchDataFromApi(shortTermWeatherApiUrl)
        val publicShortTermWeatherDTO: PublicShortTermWeatherDTO =
            Gson().fromJson(shortTermWeatherApi, PublicShortTermWeatherDTO::class.java)
        if (publicShortTermWeatherDTO.response.header.resultCode == "03") {
            throw CustomException("20")
        } else if (publicDTO.response.header.resultCode == "10") {
            throw CustomException("30")
        }
        val hour = adjustToHour(baseTime)
        var sky = ""

        var tmn = ""
        var tmx = ""
        var morningPop = 0
        var afternoonPop = 0
        var morningTmp = 100
        var morningTmpTime = ""
        var afternoonTmp = -100
        var afternoonTmpTime = ""

        var datTmn = ""
        var datTmx = ""
        var datMorningPop = 0
        var datAfternoonPop = 0
        var datMorningTmp = 100
        var datMorningTmpTime = ""
        var datAfternoonTmp = -100
        var datAfternoonTmpTime = ""

        for (item in publicShortTermWeatherDTO.response.body.items.item) {
            when {
                item.fcstDate == baseDate && item.fcstTime == hour && item.category == "PTY" && item.fcstValue != "0" -> {
                    sky = parsePty(item.fcstValue)
                }

                item.fcstDate == baseDate && item.fcstTime == hour && item.category == "SKY" -> {
                    sky = parseSky(item.fcstValue)
                }

                item.fcstDate == getAfterDate(baseDate, 1) -> {
                    when (item.category) {
                        "TMN" -> tmn = item.fcstValue.toDouble().toInt().toString()
                        "TMX" -> tmx = item.fcstValue.toDouble().toInt().toString()
                        "POP" -> {
                            if (item.fcstTime.toInt() in 0..1100) {
                                val currentPop = item.fcstValue.toIntOrNull() ?: continue
                                morningPop = maxOf<Int>(morningPop, currentPop)
                            } else if (item.fcstTime.toInt() in 1200..2300) {
                                val currentPop = item.fcstValue.toIntOrNull() ?: continue
                                afternoonPop = maxOf<Int>(afternoonPop, currentPop)
                            }
                        }
                    }
                }

                item.fcstDate == getAfterDate(baseDate, 2) -> {
                    when (item.category) {
                        "TMN" -> datTmn = item.fcstValue.toDouble().toInt().toString()
                        "TMX" -> datTmx = item.fcstValue.toDouble().toInt().toString()
                        "POP" -> {
                            if (item.fcstTime.toInt() in 0..1100) {
                                val currentPop = item.fcstValue.toIntOrNull() ?: continue
                                datMorningPop = maxOf<Int>(datMorningPop, currentPop)
                            } else if (item.fcstTime.toInt() in 1200..2300) {
                                val currentPop = item.fcstValue.toIntOrNull() ?: continue
                                datAfternoonPop = maxOf<Int>(datAfternoonPop, currentPop)
                            }
                        }
                    }
                }
            }

            /* // 강수 형태
             if (item.fcstTime == hour && item.category == "PTY" && item.fcstValue != "0") {
                 sky = parsePty(item.fcstValue)
             }

             // 하늘 상태
             if (item.fcstTime == hour && item.category == "SKY") {
                 sky = parseSky(item.fcstValue)
             }

             // 내일 최저 기온
             if (item.fcstDate == getAfterDate(baseDate, 1) && item.category == "TMN") {
                 tmn = item.fcstValue.toDouble().toInt().toString()
             }

             // 내일 최고 기온
             if (item.fcstDate == getAfterDate(baseDate, 1) && item.category == "TMX") {
                 tmx = item.fcstValue.toDouble().toInt().toString()
             }

             // 내일 오전 강수 확률
             if (item.fcstDate == getAfterDate(
                     baseDate,
                     1
                 ) && item.category == "POP" && item.fcstTime.toInt() in 0..1100
             ) {
                 val currentPop = item.fcstValue.toIntOrNull() ?: continue
                 morningPop = maxOf<Int>(morningPop, currentPop)
             }

             // 내일 오후 강수 확률
             if (item.fcstDate == getAfterDate(
                     baseDate,
                     1
                 ) && item.category == "POP" && item.fcstTime.toInt() in 1200..2300
             ) {
                 val currentPop = item.fcstValue.toIntOrNull() ?: continue
                 afternoonPop = maxOf<Int>(afternoonPop, currentPop)
             }

             // 내일 오전 최저 기온 시간
             if (item.fcstDate == getAfterDate(
                     baseDate,
                     1
                 ) && item.category == "TMP" && item.fcstTime.toInt() in 0..1100
             ) {
                 val currentTmp = item.fcstValue.toIntOrNull() ?: continue
                 if (currentTmp <= morningTmp) {
                     morningTmpTime = item.fcstTime
                     morningTmp = currentTmp
                 }
             }

             // 내일 오후 최저 기온 시간
             if (item.fcstDate == getAfterDate(
                     baseDate,
                     1
                 ) && item.category == "TMP" && item.fcstTime.toInt() in 1200..2300
             ) {
                 val currentTmp = item.fcstValue.toIntOrNull() ?: continue
                 if (currentTmp >= afternoonTmp) {
                     afternoonTmpTime = item.fcstTime
                     afternoonTmp = currentTmp
                 }
             }

             // 모레 최저 기온
             if (item.fcstDate == getAfterDate(baseDate, 2) && item.category == "TMN") {
                 datTmn = item.fcstValue.toDouble().toInt().toString()
             }

             // 모레 최고 기온
             if (item.fcstDate == getAfterDate(baseDate, 2) && item.category == "TMX") {
                 datTmx = item.fcstValue.toDouble().toInt().toString()
             }

             // 모레 오전 강수 확률
             if (item.fcstDate == getAfterDate(
                     baseDate,
                     2
                 ) && item.category == "POP" && item.fcstTime.toInt() in 0..1100
             ) {
                 val currentPop = item.fcstValue.toIntOrNull() ?: continue
                 datMorningPop = maxOf<Int>(datMorningPop, currentPop)
             }

             // 모레 오후 강수 확률
             if (item.fcstDate == getAfterDate(
                     baseDate,
                     2
                 ) && item.category == "POP" && item.fcstTime.toInt() in 1200..2300
             ) {
                 val currentPop = item.fcstValue.toIntOrNull() ?: continue
                 datAfternoonPop = maxOf<Int>(datAfternoonPop, currentPop)
             }

             // 모레 오전 최저 기온 시간
             if (item.fcstDate == getAfterDate(
                     baseDate,
                     2
                 ) && item.category == "TMP" && item.fcstTime.toInt() in 0..1100
             ) {
                 val currentTmp = item.fcstValue.toIntOrNull() ?: continue
                 if (currentTmp <= datMorningTmp) {
                     datMorningTmpTime = item.fcstTime
                     datMorningTmp = currentTmp
                 }
             }

             // 모레 오후 최저 기온 시간
             if (item.fcstDate == getAfterDate(
                     baseDate,
                     2
                 ) && item.category == "TMP" && item.fcstTime.toInt() in 1200..2300
             ) {
                 val currentTmp = item.fcstValue.toIntOrNull() ?: continue
                 if (currentTmp >= datAfternoonTmp) {
                     datAfternoonTmpTime = item.fcstTime
                     datAfternoonTmp = currentTmp
                 }
             }*/
        }

        for (item in publicShortTermWeatherDTO.response.body.items.item) {
            if (item.fcstDate == getAfterDate(
                    baseDate,
                    1
                ) && item.category == "TMP" && item.fcstTime.toInt() in 0..1100
            ) {
                val currentTmp = item.fcstValue.toIntOrNull() ?: continue
                if (currentTmp <= morningTmp) {
                    morningTmpTime = item.fcstTime
                    morningTmp = currentTmp
                }
            }

            // 내일 오후 최저 기온 시간
            if (item.fcstDate == getAfterDate(
                    baseDate,
                    1
                ) && item.category == "TMP" && item.fcstTime.toInt() in 1200..2300
            ) {
                val currentTmp = item.fcstValue.toIntOrNull() ?: continue
                if (currentTmp >= afternoonTmp) {
                    afternoonTmpTime = item.fcstTime
                    afternoonTmp = currentTmp
                }
            }

            if (item.fcstDate == getAfterDate(
                    baseDate,
                    2
                ) && item.category == "TMP" && item.fcstTime.toInt() in 0..1100
            ) {
                val currentTmp = item.fcstValue.toIntOrNull() ?: continue
                if (currentTmp <= datMorningTmp) {
                    datMorningTmpTime = item.fcstTime
                    datMorningTmp = currentTmp
                }
            }

            // 모레 오후 최저 기온 시간
            if (item.fcstDate == getAfterDate(
                    baseDate,
                    2
                ) && item.category == "TMP" && item.fcstTime.toInt() in 1200..2300
            ) {
                val currentTmp = item.fcstValue.toIntOrNull() ?: continue
                if (currentTmp >= datAfternoonTmp) {
                    datAfternoonTmpTime = item.fcstTime
                    datAfternoonTmp = currentTmp
                }
            }
        }

        var morningSky = ""
        var afternoonSky = ""
        var datMorningSky = ""
        var datAfternoonSky = ""
        for (item in publicShortTermWeatherDTO.response.body.items.item) {

            if (item.fcstDate == getAfterDate(
                    baseDate,
                    1
                ) && item.fcstTime == morningTmpTime && item.category == "PTY" && item.fcstValue != "0"
            ) {
                morningSky = parsePty(item.fcstValue)
            }

            // 내일 오전 날씨 상태
            if (item.fcstDate == getAfterDate(
                    baseDate,
                    1
                ) && item.category == "SKY" && item.fcstTime == morningTmpTime
            ) {
                morningSky = parseSky(item.fcstValue)
            }

            if (item.fcstDate == getAfterDate(
                    baseDate,
                    1
                ) && item.fcstTime == afternoonTmpTime && item.category == "PTY" && item.fcstValue != "0"
            ) {
                afternoonSky = parsePty(item.fcstValue)
            }

            // 내일 오후 날씨 상태
            if (item.fcstDate == getAfterDate(
                    baseDate,
                    1
                ) && item.category == "SKY" && item.fcstTime == afternoonTmpTime
            ) {
                afternoonSky = parseSky(item.fcstValue)
            }

            if (item.fcstDate == getAfterDate(
                    baseDate,
                    2
                ) && item.fcstTime == datMorningTmpTime && item.category == "PTY" && item.fcstValue != "0"
            ) {
                datMorningSky = parsePty(item.fcstValue)
            }

            // 모레 오전 날씨 상태
            if (item.fcstDate == getAfterDate(
                    baseDate,
                    2
                ) && item.category == "SKY" && item.fcstTime == datMorningTmpTime
            ) {
                datMorningSky = parseSky(item.fcstValue)
            }

            if (item.fcstDate == getAfterDate(
                    baseDate,
                    2
                ) && item.fcstTime == datAfternoonTmpTime && item.category == "PTY" && item.fcstValue != "0"
            ) {
                datAfternoonSky = parsePty(item.fcstValue)
            }

            // 모레 오후 날씨 상태
            if (item.fcstDate == getAfterDate(
                    baseDate,
                    2
                ) && item.category == "SKY" && item.fcstTime == datAfternoonTmpTime
            ) {
                datAfternoonSky = parseSky(item.fcstValue)
            }
        }


        val dustForecastApiUrl = dustForecastApiUrl(formatDate(baseDate))
        val dustForecastApi = fetchDataFromApi(dustForecastApiUrl)
        val publicDustForecastDTO: PublicDustForecastDTO =
            Gson().fromJson(dustForecastApi, PublicDustForecastDTO::class.java)

        val dustForecastAfterDate = getAfterDate(baseDate, 1)
        val dustForecastParseDate = formatDate(dustForecastAfterDate)
        var dustForecastPm10Val = "-"
        var dustForecastPm25Val = "-"

        for (item in publicDustForecastDTO.response.body.items) {
            // 내일 미세먼지 등급
            if (item.informData == dustForecastParseDate) {
                when (item.informCode) {
                    "PM10" -> {
                        dustForecastPm10Val = item.informGrade
                        if (dustForecastPm25Val != "-") break
                    }

                    "PM25" -> {
                        dustForecastPm25Val = item.informGrade
                        if (dustForecastPm10Val != "-") break
                    }
                }
            }
        }

        val regionGrade10Map = dustForecastPm10Val
            .split(",")
            .map { it.split(":") }
            .filter { it.size == 2 }
            .associate { (region, grade) -> region.trim() to grade.trim() }

        val regionGrade25Map = dustForecastPm25Val
            .split(",")
            .map { it.split(":") }
            .filter { it.size == 2 }
            .associate { (region, grade) -> region.trim() to grade.trim() }

        var dustForecastPm10 = regionGrade10Map["서울"]
        var dustForecastPm25 = regionGrade25Map["서울"]

        if (dustForecastPm10 == null) {
            dustForecastPm10 = "-"
        }

        if (dustForecastPm25 == null) {
            dustForecastPm25 = "-"
        }

        val datDustForecastAfterDate = getAfterDate(baseDate, 2)
        val datDustForecastParseDate = formatDate(datDustForecastAfterDate)
        var datDustForecastPm10Val = "-"
        var datDustForecastPm25Val = "-"

        for (item in publicDustForecastDTO.response.body.items) {
            // 내일 미세먼지 등급
            if (item.informData == datDustForecastParseDate) {
                when (item.informCode) {
                    "PM10" -> {
                        datDustForecastPm10Val = item.informGrade
                        if (datDustForecastPm25Val != "-") break
                    }

                    "PM25" -> {
                        datDustForecastPm25Val = item.informGrade
                        if (datDustForecastPm10Val != "-") break
                    }
                }
            }
        }

        val datRegionGrade10Map = datDustForecastPm10Val
            .split(",")
            .map { it.split(":") }
            .filter { it.size == 2 }
            .associate { (region, grade) -> region.trim() to grade.trim() }

        val datRegionGrade25Map = datDustForecastPm25Val
            .split(",")
            .map { it.split(":") }
            .filter { it.size == 2 }
            .associate { (region, grade) -> region.trim() to grade.trim() }

        var datDustForecastPm10 = datRegionGrade10Map["서울"]
        var datDustForecastPm25 = datRegionGrade25Map["서울"]

        if (datDustForecastPm10 == null) {
            datDustForecastPm10 = "-"
        }

        if (datDustForecastPm25 == null) {
            datDustForecastPm25 = "-"
        }

        val time: String = adjustToHour(getPlusTime(baseTime, 60))

        // 현재 시간 이후 시간 데이터 추출 (날씨)
        val currentAndLaterItems = publicShortTermWeatherDTO.response.body.items.item
            .filter { it.fcstDate == baseDate && it.fcstTime.toInt() >= time.toInt() }
            .groupBy { it.fcstDate to it.fcstTime }
            .mapNotNull { (key, items) ->
                val tmp = items.find { it.category == "TMP" }?.fcstValue
                val skySt = items.find { it.category == "SKY" }?.fcstValue

                if (tmp != null && skySt != null) {
                    ResponseShortTermWeatherDTO(
                        tmp = tmp,
                        sky = parseSky(skySt),
                        date = key.first,
                        time = key.second.dropLast(2)
                    )
                } else {
                    null
                }
            }


        // 현재 시간 이후 시간 데이터 추출 (강수량)
        val currentAndLaterRainItems = publicShortTermWeatherDTO.response.body.items.item
            .filter { it.fcstDate == baseDate && it.fcstTime.toInt() >= time.toInt() }
            .groupBy { it.fcstDate to it.fcstTime }
            .mapNotNull { (key, items) ->
                val pop = items.find { it.category == "POP" }?.fcstValue
                var pcp = items.find { it.category == "PCP" }?.fcstValue

                if (pcp == "강수없음") {
                    pcp = "0"
                } else if (pcp != null) {
                    pcp = pcp.removeSuffix("mm").toDouble().toInt().toString()
                }

                if (pop != null && pcp != null) {
                    ResponseShortTermRainDTO(
                        pop = pop,
                        pcp = pcp,
                        date = key.first,
                        time = key.second.dropLast(2)
                    )
                } else {
                    null
                }
            }

        // 내일, 모레 데이터 추출 (날씨)
        val tomorrowAndDatItems = publicShortTermWeatherDTO.response.body.items.item
            .filter { it.fcstDate >= getAfterDate(baseDate, 1) }
            .groupBy { it.fcstDate to it.fcstTime }
            .mapNotNull { (key, items) ->
                val tmp = items.find { it.category == "TMP" }?.fcstValue
                val skySt = items.find { it.category == "SKY" }?.fcstValue

                if (tmp != null && skySt != null) {
                    ResponseShortTermWeatherDTO(
                        tmp = tmp,
                        sky = parseSky(skySt),
                        date = key.first,
                        time = key.second.dropLast(2)
                    )
                } else {
                    null
                }
            }

        // 내일, 모레 데이터 추출 (강수량)
        val tomorrowAndDatRainItems = publicShortTermWeatherDTO.response.body.items.item
            .filter { it.fcstDate >= getAfterDate(baseDate, 1) }
            .groupBy { it.fcstDate to it.fcstTime }
            .mapNotNull { (key, items) ->
                val pop = items.find { it.category == "POP" }?.fcstValue
                var pcpTad = items.find { it.category == "PCP" }?.fcstValue

                if (pcpTad == "강수없음") {
                    pcpTad = "0"
                } else if (pcpTad != null) {
                    pcpTad = pcpTad.removeSuffix("mm").toDouble().toInt().toString()
                }

                if (pop != null && pcpTad != null) {
                    ResponseShortTermRainDTO(
                        pop = pop,
                        pcp = pcpTad,
                        date = key.first,
                        time = key.second.dropLast(2)
                    )
                } else {
                    null
                }
            }

        // 실시간 날씨 예보 set
        val shortTermWeatherList = currentAndLaterItems + tomorrowAndDatItems

        // 실시간 강수 예보 set
        val shortTermRainList = currentAndLaterRainItems + tomorrowAndDatRainItems

        // 주소 set
        val addressDTO = ResponseAddressDTO(address)

        // 현재 날씨 정보 set
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
            sky = sky
        )

        // 내일 날씨 정보 set
        val responseTomorrowWeatherDTO = ResponseTomorrowWeatherDTO(
            morningTemp = tmn,
            morningSky = morningSky,
            morningPoP = morningPop.toString(),
            morningPm10Grade = dustForecastPm10,
            morningPm25Grade = dustForecastPm25,
            afternoonTemp = tmx,
            afternoonSky = afternoonSky,
            afternoonPoP = afternoonPop.toString(),
            afternoonPm10Grade = dustForecastPm10,
            afternoonPm25Grade = dustForecastPm25
        )

        // 모레 날씨 정보 set
        val responseDatWeatherDTO = ResponseDatWeatherDTO(
            morningTemp = datTmn,
            morningSky = datMorningSky,
            morningPoP = datMorningPop.toString(),
            morningPm10Grade = datDustForecastPm10,
            morningPm25Grade = datDustForecastPm25,
            afternoonTemp = datTmx,
            afternoonSky = datAfternoonSky,
            afternoonPoP = datAfternoonPop.toString(),
            afternoonPm10Grade = datDustForecastPm10,
            afternoonPm25Grade = datDustForecastPm25
        )

        return ResponseWeatherInfoDTO(
            addressInfo = addressDTO,
            weatherInfo = responseWeatherDTO,
            tomorrowWeatherInfo = responseTomorrowWeatherDTO,
            datWeatherInfo = responseDatWeatherDTO,
            shortTermWeatherInfo = shortTermWeatherList,
            shortTermRainInfo = shortTermRainList
        )
    }


    fun monthlyWeatherInfo(): ResponseMonthlyWeatherInfoDTO {
        val monthlyWeatherList = weatherMapper.monthlyWeather().map { item ->
            ResponseMonthlyWeatherDTO(
                title = item.title,
                weeklyWeatherInfo = weatherMapper.weeklyWeather(item.month)
            )
        }
        return ResponseMonthlyWeatherInfoDTO(monthlyWeatherInfo = monthlyWeatherList)
    }


    // 예보 시간 변경
    fun getTimeRange(input: String): String {
        val time = LocalTime.parse(input, DateTimeFormatter.ofPattern("HHmm"))
        val timeRanges = listOf(
            LocalTime.parse("02:10", DateTimeFormatter.ofPattern("HH:mm")),
            LocalTime.parse("05:10", DateTimeFormatter.ofPattern("HH:mm")),
            LocalTime.parse("08:10", DateTimeFormatter.ofPattern("HH:mm")),
            LocalTime.parse("11:10", DateTimeFormatter.ofPattern("HH:mm")),
            LocalTime.parse("14:10", DateTimeFormatter.ofPattern("HH:mm")),
            LocalTime.parse("17:10", DateTimeFormatter.ofPattern("HH:mm")),
            LocalTime.parse("20:10", DateTimeFormatter.ofPattern("HH:mm")),
            LocalTime.parse("23:10", DateTimeFormatter.ofPattern("HH:mm"))
        )

        // 수정된 부분
        val index = timeRanges.indexOfFirst { it > time }
        val startTime = if (index > 0) timeRanges[index - 1] else timeRanges.last()

        return startTime.format(DateTimeFormatter.ofPattern("HHmm"))
    }

    // 하늘 상태
    fun parseSky(sky: String): String {
        return when (sky) {
            "4" -> "흐림"
            "3" -> "구름많음"
            "1" -> "맑음"
            else -> "알 수 없음"
        }
    }

    // 강수 상태
    fun parsePty(sky: String): String {
        return when (sky) {
            "1" -> "비"
            "2" -> "비/눈"
            "3" -> "눈"
            "4" -> "소나기"
            else -> "알 수 없음"
        }
    }

    // 등급 계산 (미세먼지, 초미세먼지)
    fun parseGrade(grade: String?): String {
        return when (grade) {
            "1" -> "좋음"
            "2" -> "보통"
            "3" -> "나쁨"
            "4" -> "매우나쁨"
            else -> "-"
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

    // 다음 날
    fun getAfterDate(inputDate: String, plusDate: Long): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val date = LocalDate.parse(inputDate, formatter)
        val afterDate = date.plusDays(plusDate)

        return formatter.format(afterDate)
    }

    // 일몰 시간 포맷
    fun formatTime(inputTime: String): String {
        val formatter = DateTimeFormatter.ofPattern("HHmm")
        val parsedTime = LocalTime.parse(inputTime, formatter)
        val formattedTime = DateTimeFormatter.ofPattern("HH:mm").format(parsedTime)
        return formattedTime
    }

    // 날짜 포맷
    fun formatDate(inputDate: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val parsedDate = LocalDate.parse(inputDate, formatter)
        val formattedDate = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(parsedDate)
        return formattedDate
    }

    // ex) 13:10 -> 13:00
    fun adjustToHour(inputTime: String): String {
        val formatter = DateTimeFormatter.ofPattern("HHmm")
        val parsedTime = LocalTime.parse(inputTime, formatter)
        val adjustedTime = LocalTime.of(parsedTime.hour, 0)
        return formatter.format(adjustedTime)
    }

    // 공공 API
    fun fetchDataFromApi(urlBuilder: String, api: String = ""): String {
        var rd: BufferedReader? = null
        var conn: HttpURLConnection? = null
        try {
            val url = URL(urlBuilder)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            if (api == "kakao") {
                conn.setRequestProperty("Authorization", "KakaoAK $kakaoKey")
            }
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
    fun shortTermWeatherApiUrl(date: String, time: String, nx: Int?, ny: Int?): String {
        return "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst" +
                "?${URLEncoder.encode("serviceKey", StandardCharsets.UTF_8)}=$key" +
                "&${URLEncoder.encode("pageNo", StandardCharsets.UTF_8)}=1" +
                "&${URLEncoder.encode("numOfRows", StandardCharsets.UTF_8)}=1000" +
                "&${URLEncoder.encode("dataType", StandardCharsets.UTF_8)}=json" +
                "&${URLEncoder.encode("base_date", StandardCharsets.UTF_8)}=$date" +
                "&${URLEncoder.encode("base_time", StandardCharsets.UTF_8)}=$time" +
                "&${URLEncoder.encode("nx", StandardCharsets.UTF_8)}=$nx" +
                "&${URLEncoder.encode("ny", StandardCharsets.UTF_8)}=$ny"

    }

    // 내일 모레 미세먼지 API
    fun dustForecastApiUrl(date: String): String {
        return "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getMinuDustFrcstDspth" +
                "?${URLEncoder.encode("serviceKey", StandardCharsets.UTF_8)}=$key" +
                "&${URLEncoder.encode("returnType", StandardCharsets.UTF_8)}=json" +
                "&${URLEncoder.encode("numOfRows", StandardCharsets.UTF_8)}=100" +
                "&${URLEncoder.encode("pageNo", StandardCharsets.UTF_8)}=1" +
                "&${URLEncoder.encode("searchDate", StandardCharsets.UTF_8)}=$date" +
                "&${URLEncoder.encode("InformCode", StandardCharsets.UTF_8)}=PM10"
    }

    // 카카오 주소 API
    fun kakaoAddressApiUrl(gridX: Double, gridY: Double): String {
        return "https://dapi.kakao.com/v2/local/geo/coord2regioncode" +
                "?${URLEncoder.encode("x", StandardCharsets.UTF_8)}=$gridY" +
                "&${URLEncoder.encode("y", StandardCharsets.UTF_8)}=$gridX"
//                "&${URLEncoder.encode("input_coord", StandardCharsets.UTF_8)}=WGS84"
    }
}