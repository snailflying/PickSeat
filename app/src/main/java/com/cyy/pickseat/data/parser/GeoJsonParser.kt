package com.cyy.pickseat.data.parser

import com.cyy.pickseat.data.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * GeoJSON数据解析器
 */
class GeoJsonParser {
    
    private val gson = Gson()
    
    /**
     * 解析GeoJSON数据为场馆布局
     */
    suspend fun parseVenueLayout(geoJsonString: String): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            val jsonObject = JsonParser.parseString(geoJsonString).asJsonObject
            parseVenueFromGeoJson(jsonObject)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从GeoJSON对象解析场馆布局
     */
    private fun parseVenueFromGeoJson(jsonObject: JsonObject): VenueLayout {
        val properties = jsonObject.getAsJsonObject("properties")
        val features = jsonObject.getAsJsonArray("features")
        
        val venueId = properties?.get("id")?.asString ?: "venue_${System.currentTimeMillis()}"
        val venueName = properties?.get("name")?.asString ?: "未知场馆"
        val venueType = parseVenueType(properties?.get("type")?.asString)
        
        // 解析场馆尺寸
        val bounds = calculateBounds(features)
        val width = bounds.maxX - bounds.minX + 100f // 添加边距
        val height = bounds.maxY - bounds.minY + 100f
        
        // 解析座位区域
        val areas = mutableListOf<SeatArea>()
        var stage: Stage? = null
        
        features?.forEach { feature ->
            val featureObj = feature.asJsonObject
            val geometry = featureObj.getAsJsonObject("geometry")
            val featureProperties = featureObj.getAsJsonObject("properties")
            val geometryType = geometry?.get("type")?.asString
            
            when (geometryType) {
                "Polygon", "MultiPolygon" -> {
                    val area = parseSeatArea(featureObj, bounds.minX, bounds.minY)
                    if (area != null) {
                        areas.add(area)
                    }
                }
                "Point" -> {
                    val stageData = parseStage(featureObj, bounds.minX, bounds.minY)
                    if (stageData != null) {
                        stage = stageData
                    }
                }
            }
        }
        
        return VenueLayout(
            id = venueId,
            name = venueName,
            type = venueType,
            width = width,
            height = height,
            areas = areas,
            stage = stage
        )
    }
    
    /**
     * 解析座位区域
     */
    private fun parseSeatArea(featureObj: JsonObject, offsetX: Float, offsetY: Float): SeatArea? {
        val properties = featureObj.getAsJsonObject("properties")
        val geometry = featureObj.getAsJsonObject("geometry")
        
        val areaId = properties?.get("id")?.asString ?: return null
        val areaName = properties?.get("name")?.asString ?: "未知区域"
        val areaType = parseAreaType(properties?.get("area_type")?.asString)
        val color = properties?.get("color")?.asString ?: "#FFD700"
        
        // 解析几何坐标
        val coordinates = geometry?.getAsJsonArray("coordinates")
        if (coordinates == null || coordinates.size() == 0) return null
        
        val bounds = calculatePolygonBounds(coordinates)
        
        // 生成座位
        val seats = generateSeatsForArea(
            areaId = areaId,
            bounds = bounds,
            offsetX = offsetX,
            offsetY = offsetY,
            properties = properties
        )
        
        return SeatArea(
            id = areaId,
            name = areaName,
            type = areaType,
            color = color,
            x = bounds.minX - offsetX + 50f,
            y = bounds.minY - offsetY + 50f,
            width = bounds.maxX - bounds.minX,
            height = bounds.maxY - bounds.minY,
            seats = seats
        )
    }
    
    /**
     * 解析舞台信息
     */
    private fun parseStage(featureObj: JsonObject, offsetX: Float, offsetY: Float): Stage? {
        val properties = featureObj.getAsJsonObject("properties")
        val geometry = featureObj.getAsJsonObject("geometry")
        
        if (properties?.get("type")?.asString != "stage") return null
        
        val coordinates = geometry?.getAsJsonArray("coordinates")
        if (coordinates == null || coordinates.size() < 2) return null
        
        val x = coordinates[0].asFloat - offsetX + 50f
        val y = coordinates[1].asFloat - offsetY + 50f
        val width = properties.get("width")?.asFloat ?: 200f
        val height = properties.get("height")?.asFloat ?: 100f
        val name = properties.get("name")?.asString ?: "舞台"
        val color = properties.get("color")?.asString ?: "#FF6B6B"
        
        return Stage(name, x, y, width, height, color)
    }
    
    /**
     * 为区域生成座位
     */
    private fun generateSeatsForArea(
        areaId: String,
        bounds: Bounds,
        offsetX: Float,
        offsetY: Float,
        properties: JsonObject?
    ): List<Seat> {
        val seats = mutableListOf<Seat>()
        
        val rows = properties?.get("rows")?.asInt ?: calculateRows(bounds)
        val seatsPerRow = properties?.get("seats_per_row")?.asInt ?: calculateSeatsPerRow(bounds)
        val price = properties?.get("price")?.asDouble ?: 50.0
        
        val seatWidth = 30f
        val seatHeight = 30f
        val seatSpacing = 35f
        val rowSpacing = 40f
        
        val startX = bounds.minX - offsetX + 50f
        val startY = bounds.minY - offsetY + 50f
        
        for (row in 0 until rows) {
            for (seat in 0 until seatsPerRow) {
                val seatId = "${areaId}_${row + 1}_${seat + 1}"
                val seatName = "${row + 1}排${seat + 1}号"
                val x = startX + seat * seatSpacing
                val y = startY + row * rowSpacing
                
                // 随机设置一些座位为已售或不可选状态
                val status = when ((row + seat) % 10) {
                    0, 1 -> SeatStatus.SOLD
                    9 -> SeatStatus.UNAVAILABLE
                    else -> SeatStatus.AVAILABLE
                }
                
                seats.add(
                    Seat(
                        id = seatId,
                        row = (row + 1).toString(),
                        column = (seat + 1).toString(),
                        name = seatName,
                        x = x,
                        y = y,
                        width = seatWidth,
                        height = seatHeight,
                        status = status,
                        price = price,
                        areaId = areaId
                    )
                )
            }
        }
        
        return seats
    }
    
    /**
     * 计算几何边界
     */
    private fun calculateBounds(features: com.google.gson.JsonArray?): Bounds {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        
        features?.forEach { feature ->
            val featureObj = feature.asJsonObject
            val geometry = featureObj.getAsJsonObject("geometry")
            val coordinates = geometry?.getAsJsonArray("coordinates")
            
            coordinates?.forEach { coord ->
                if (coord.isJsonArray) {
                    processCoordinateArray(coord.asJsonArray) { x, y ->
                        minX = minOf(minX, x)
                        minY = minOf(minY, y)
                        maxX = maxOf(maxX, x)
                        maxY = maxOf(maxY, y)
                    }
                }
            }
        }
        
        return Bounds(minX, minY, maxX, maxY)
    }
    
    /**
     * 计算多边形边界
     */
    private fun calculatePolygonBounds(coordinates: com.google.gson.JsonArray): Bounds {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        
        processCoordinateArray(coordinates) { x, y ->
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
        }
        
        return Bounds(minX, minY, maxX, maxY)
    }
    
    /**
     * 递归处理坐标数组
     */
    private fun processCoordinateArray(
        coordinates: com.google.gson.JsonArray,
        callback: (Float, Float) -> Unit
    ) {
        coordinates.forEach { coord ->
            when {
                coord.isJsonArray -> {
                    val coordArray = coord.asJsonArray
                    if (coordArray.size() >= 2 && coordArray[0].isJsonPrimitive && coordArray[1].isJsonPrimitive) {
                        callback(coordArray[0].asFloat, coordArray[1].asFloat)
                    } else {
                        processCoordinateArray(coordArray, callback)
                    }
                }
            }
        }
    }
    
    /**
     * 解析场馆类型
     */
    private fun parseVenueType(typeString: String?): VenueType {
        return when (typeString?.lowercase()) {
            "stadium" -> VenueType.STADIUM
            "theater" -> VenueType.THEATER
            "concert_hall" -> VenueType.CONCERT_HALL
            "conference_hall" -> VenueType.CONFERENCE_HALL
            "cinema" -> VenueType.CINEMA
            else -> VenueType.STADIUM
        }
    }
    
    /**
     * 解析区域类型
     */
    private fun parseAreaType(typeString: String?): AreaType {
        return when (typeString?.lowercase()) {
            "vip" -> AreaType.VIP
            "disabled" -> AreaType.DISABLED
            "premium" -> AreaType.PREMIUM
            "box" -> AreaType.BOX
            else -> AreaType.NORMAL
        }
    }
    
    /**
     * 根据边界计算行数
     */
    private fun calculateRows(bounds: Bounds): Int {
        val height = bounds.maxY - bounds.minY
        return maxOf(1, (height / 40f).toInt()) // 每行40像素间距
    }
    
    /**
     * 根据边界计算每行座位数
     */
    private fun calculateSeatsPerRow(bounds: Bounds): Int {
        val width = bounds.maxX - bounds.minX
        return maxOf(1, (width / 35f).toInt()) // 每座位35像素间距
    }
    
    /**
     * 边界数据类
     */
    private data class Bounds(
        val minX: Float,
        val minY: Float,
        val maxX: Float,
        val maxY: Float
    )
}
