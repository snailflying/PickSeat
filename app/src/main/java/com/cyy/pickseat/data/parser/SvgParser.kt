package com.cyy.pickseat.data.parser

import android.content.Context
import com.cyy.pickseat.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*
import kotlin.random.Random

/**
 * SVG数据解析器
 * 简化实现，通过解析SVG基本信息生成座位数据
 */
class SvgParser(private val context: Context) {
    
    /**
     * 解析SVG数据为场馆布局
     */
    suspend fun parseVenueLayout(svgString: String): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            // 解析SVG基本信息
            val width = extractDimension(svgString, "width") ?: 1000f
            val height = extractDimension(svgString, "height") ?: 800f
            val viewBox = extractViewBox(svgString)
            
            // 使用解析出的尺寸生成场馆数据
            generateVenueFromSvgInfo(width, height, viewBox, svgString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从SVG信息生成场馆布局
     */
    private fun generateVenueFromSvgInfo(
        width: Float, 
        height: Float, 
        viewBox: FloatArray?, 
        svgData: String
    ): VenueLayout {
        val venueId = "svg_venue_${System.currentTimeMillis()}"
        val venueName = "SVG场馆"
        
        // 使用viewBox或原始尺寸
        val actualWidth = viewBox?.get(2) ?: width
        val actualHeight = viewBox?.get(3) ?: height
        
        // 创建舞台
        val stage = Stage(
            name = "舞台",
            x = actualWidth / 2 - 100f,
            y = 50f,
            width = 200f,
            height = 80f,
            color = "#FF6B6B"
        )
        
        // 生成座位区域
        val areas = generateSvgAreas(actualWidth, actualHeight, stage)
        
        return VenueLayout(
            id = venueId,
            name = venueName,
            type = VenueType.THEATER,
            width = actualWidth,
            height = actualHeight,
            areas = areas,
            stage = stage,
            svgData = svgData,
            maxScale = 6f,
            minScale = 0.1f
        )
    }
    
    /**
     * 生成SVG场馆的座位区域
     */
    private fun generateSvgAreas(width: Float, height: Float, stage: Stage): List<SeatArea> {
        val areas = mutableListOf<SeatArea>()
        
        // 主厅区域（舞台下方）
        val mainHallY = stage.y + stage.height + 50f
        val mainHallHeight = height - mainHallY - 100f
        
        if (mainHallHeight > 100f) {
            areas.add(generateSeatArea(
                id = "main_hall",
                name = "主厅",
                x = 100f,
                y = mainHallY,
                width = width - 200f,
                height = mainHallHeight,
                type = AreaType.NORMAL,
                color = "#FFD93D"
            ))
        }
        
        // 左右包厢区域
        val boxWidth = 80f
        val boxHeight = 60f
        val boxY = stage.y + stage.height + 20f
        
        // 左侧包厢
        for (i in 0 until 5) {
            areas.add(generateSeatArea(
                id = "left_box_$i",
                name = "左包厢${i + 1}",
                x = 20f,
                y = boxY + i * (boxHeight + 10f),
                width = boxWidth,
                height = boxHeight,
                type = AreaType.BOX,
                color = "#F38BA8"
            ))
        }
        
        // 右侧包厢
        for (i in 0 until 5) {
            areas.add(generateSeatArea(
                id = "right_box_$i",
                name = "右包厢${i + 1}",
                x = width - boxWidth - 20f,
                y = boxY + i * (boxHeight + 10f),
                width = boxWidth,
                height = boxHeight,
                type = AreaType.BOX,
                color = "#F38BA8"
            ))
        }
        
        // 楼座区域
        val balconyY = height - 150f
        if (balconyY > mainHallY + 100f) {
            areas.add(generateSeatArea(
                id = "balcony",
                name = "楼座",
                x = 150f,
                y = balconyY,
                width = width - 300f,
                height = 120f,
                type = AreaType.PREMIUM,
                color = "#4ECDC4"
            ))
        }
        
        return areas
    }
    
    /**
     * 生成座位区域
     */
    private fun generateSeatArea(
        id: String,
        name: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        type: AreaType,
        color: String
    ): SeatArea {
        val seats = generateSeatsForArea(id, x, y, width, height, type)
        
        return SeatArea(
            id = id,
            name = name,
            type = type,
            color = color,
            x = x,
            y = y,
            width = width,
            height = height,
            seats = seats
        )
    }
    
    /**
     * 为区域生成座位
     */
    private fun generateSeatsForArea(
        areaId: String,
        areaX: Float,
        areaY: Float,
        areaWidth: Float,
        areaHeight: Float,
        areaType: AreaType
    ): List<Seat> {
        val seats = mutableListOf<Seat>()
        
        val seatSize = when (areaType) {
            AreaType.BOX -> 20f
            AreaType.PREMIUM -> 25f
            else -> 24f
        }
        
        val spacing = seatSize + 5f
        val rowSpacing = seatSize + 10f
        
        val cols = ((areaWidth - 20f) / spacing).toInt().coerceAtLeast(1)
        val rows = ((areaHeight - 20f) / rowSpacing).toInt().coerceAtLeast(1)
        
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val seatId = "${areaId}_${row + 1}_${col + 1}"
                val seatName = "${row + 1}排${col + 1}号"
                val seatX = areaX + 10f + col * spacing
                val seatY = areaY + 10f + row * rowSpacing
                
                // 随机设置座位状态
                val status = when (Random.nextInt(20)) {
                    0, 1 -> SeatStatus.SOLD
                    19 -> SeatStatus.UNAVAILABLE
                    else -> SeatStatus.AVAILABLE
                }
                
                // 根据区域类型设置价格
                val basePrice = when (areaType) {
                    AreaType.BOX -> 800.0
                    AreaType.PREMIUM -> 300.0
                    AreaType.VIP -> 500.0
                    else -> 150.0
                }
                
                val price = basePrice + (rows - row) * 20.0 // 前排更贵
                
                seats.add(
                    Seat(
                        id = seatId,
                        row = (row + 1).toString(),
                        column = (col + 1).toString(),
                        name = seatName,
                        x = seatX,
                        y = seatY,
                        width = seatSize,
                        height = seatSize,
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
     * 从SVG字符串中提取尺寸信息
     */
    private fun extractDimension(svgString: String, attribute: String): Float? {
        val regex = Regex("$attribute\\s*=\\s*[\"']([^\"']+)[\"']")
        val match = regex.find(svgString)
        val value = match?.groupValues?.get(1) ?: return null
        
        // 移除单位并转换为数字
        val numericValue = value.replace(Regex("[^0-9.]"), "")
        return numericValue.toFloatOrNull()
    }
    
    /**
     * 从SVG字符串中提取viewBox信息
     */
    private fun extractViewBox(svgString: String): FloatArray? {
        val regex = Regex("viewBox\\s*=\\s*[\"']([^\"']+)[\"']")
        val match = regex.find(svgString)
        val value = match?.groupValues?.get(1) ?: return null
        
        val parts = value.trim().split(Regex("\\s+"))
        if (parts.size >= 4) {
            return try {
                floatArrayOf(
                    parts[0].toFloat(), // x
                    parts[1].toFloat(), // y
                    parts[2].toFloat(), // width
                    parts[3].toFloat()  // height
                )
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        return null
    }
}