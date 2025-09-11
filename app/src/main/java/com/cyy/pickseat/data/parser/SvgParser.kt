package com.cyy.pickseat.data.parser

import android.content.Context
import com.cyy.pickseat.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*
import kotlin.random.Random

/**
 * SVG数据解析器
 * 真正解析SVG文件内容，提取区域和座位信息
 */
class SvgParser(private val context: Context) {
    
    /**
     * 解析SVG数据为场馆布局
     */
    suspend fun parseVenueLayout(svgString: String): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            android.util.Log.i("SvgParser", "开始解析SVG数据，长度: ${svgString.length}")
            
            // 解析SVG基本信息
            val width = extractDimension(svgString, "width") ?: 2000f
            val height = extractDimension(svgString, "height") ?: 1500f
            val viewBox = extractViewBox(svgString)
            
            // 解析场馆名称
            val venueName = extractVenueName(svgString) ?: "SVG场馆"
            
            // 解析座位区域
            val areas = parseSeatAreas(svgString)
            
            // 解析舞台（足球场）
            val stage = parseStage(svgString)
            
            val result = VenueLayout(
                id = "svg_venue_${System.currentTimeMillis()}",
                name = venueName,
                type = VenueType.STADIUM,
                width = width,
                height = height,
                areas = areas,
                stage = stage,
                svgData = svgString,
                maxScale = 6f,
                minScale = 0.1f
            )
            
            android.util.Log.i("SvgParser", "解析完成，场馆名称: ${result.name}, 区域数量: ${result.areas.size}, 座位总数: ${result.getTotalSeatCount()}")
            result
        } catch (e: Exception) {
            android.util.Log.e("SvgParser", "解析SVG失败", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 解析场馆名称
     */
    private fun extractVenueName(svgString: String): String? {
        // 尝试从metadata中提取
        val metadataRegex = "<name>([^<]+)</name>".toRegex()
        val match = metadataRegex.find(svgString)
        return match?.groupValues?.get(1)
    }
    
    /**
     * 解析舞台信息
     */
    private fun parseStage(svgString: String): Stage? {
        // 查找足球场矩形
        val stageRegex = """<rect\s+x="(\d+)"\s+y="(\d+)"\s+width="(\d+)"\s+height="(\d+)"\s+fill="#4CAF50"""".toRegex()
        val match = stageRegex.find(svgString)
        
        return if (match != null) {
            val x = match.groupValues[1].toFloat()
            val y = match.groupValues[2].toFloat()
            val width = match.groupValues[3].toFloat()
            val height = match.groupValues[4].toFloat()
            
            Stage(
                name = "足球场",
                x = x,
                y = y,
                width = width,
                height = height,
                color = "#4CAF50"
            )
        } else {
            null
        }
    }
    
    /**
     * 解析座位区域
     */
    private fun parseSeatAreas(svgString: String): List<SeatArea> {
        val areas = mutableListOf<SeatArea>()
        
        // 查找所有带id的group元素
        val groupRegex = """<g\s+id="([^"]+)"\s+data-name="([^"]+)"\s+data-color="([^"]+)">""".toRegex()
        val matches = groupRegex.findAll(svgString)
        
        matches.forEach { match ->
            val areaId = match.groupValues[1]
            val areaName = match.groupValues[2]
            val areaColor = match.groupValues[3]
            
            // 查找该区域的主矩形
            val areaContent = extractGroupContent(svgString, areaId)
            val rectRegex = """<rect\s+x="(\d+)"\s+y="(\d+)"\s+width="(\d+)"\s+height="(\d+)"""".toRegex()
            val rectMatch = rectRegex.find(areaContent)
            
            if (rectMatch != null) {
                val x = rectMatch.groupValues[1].toFloat()
                val y = rectMatch.groupValues[2].toFloat()
                val width = rectMatch.groupValues[3].toFloat()
                val height = rectMatch.groupValues[4].toFloat()
                
                // 生成座位
                val seats = generateSeatsForArea(areaId, areaName, x, y, width, height)
                
                val areaType = when {
                    areaName.contains("VIP") -> AreaType.VIP
                    else -> AreaType.NORMAL
                }
                
                areas.add(
                    SeatArea(
                        id = areaId,
                        name = areaName,
                        type = areaType,
                        color = areaColor,
                        x = x,
                        y = y,
                        width = width,
                        height = height,
                        seats = seats
                    )
                )
            }
        }
        
        return areas
    }
    
    /**
     * 提取group内容
     */
    private fun extractGroupContent(svgString: String, groupId: String): String {
        val startTag = """<g\s+id="$groupId"[^>]*>""".toRegex()
        val startMatch = startTag.find(svgString) ?: return ""
        val startIndex = startMatch.range.last + 1
        
        var depth = 1
        var currentIndex = startIndex
        
        while (currentIndex < svgString.length && depth > 0) {
            when {
                svgString.substring(currentIndex).startsWith("<g") -> depth++
                svgString.substring(currentIndex).startsWith("</g>") -> depth--
            }
            currentIndex++
        }
        
        return if (depth == 0) {
            svgString.substring(startIndex, currentIndex - 4) // 不包括</g>
        } else {
            ""
        }
    }
    
    /**
     * 为区域生成座位
     */
    private fun generateSeatsForArea(
        areaId: String,
        areaName: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ): List<Seat> {
        val seats = mutableListOf<Seat>()
        
        val seatWidth = 25f
        val seatHeight = 25f
        val seatSpacing = 30f
        val rowSpacing = 35f
        
        val seatsPerRow = maxOf(1, (width / seatSpacing).toInt())
        val rows = maxOf(1, (height / rowSpacing).toInt())
        
        val price = when {
            areaName.contains("VIP") -> 500.0
            areaName.contains("A区") || areaName.contains("B区") -> 200.0
            else -> 150.0
        }
        
        val padding = 20f
        val startX = x + padding
        val startY = y + padding
        
        for (row in 0 until rows) {
            for (seat in 0 until seatsPerRow) {
                val seatId = "${areaId}_${row + 1}_${seat + 1}"
                val seatName = "${row + 1}排${seat + 1}号"
                val seatX = startX + seat * seatSpacing
                val seatY = startY + row * rowSpacing
                
                // 随机设置一些座位状态
                val status = when ((row + seat) % 12) {
                    0, 1 -> SeatStatus.SOLD
                    11 -> SeatStatus.UNAVAILABLE
                    else -> SeatStatus.AVAILABLE
                }
                
                seats.add(
                    Seat(
                        id = seatId,
                        row = (row + 1).toString(),
                        column = (seat + 1).toString(),
                        name = seatName,
                        x = seatX,
                        y = seatY,
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
     * 提取SVG尺寸
     */
    private fun extractDimension(svgString: String, dimension: String): Float? {
        val regex = """$dimension="(\d+)"""".toRegex()
        val match = regex.find(svgString)
        return match?.groupValues?.get(1)?.toFloatOrNull()
    }
    
    /**
     * 提取viewBox信息
     */
    private fun extractViewBox(svgString: String): FloatArray? {
        val viewBoxRegex = """viewBox="([\d\s.-]+)"""".toRegex()
        val match = viewBoxRegex.find(svgString) ?: return null
        
        val values = match.groupValues[1].split(Regex("\\s+"))
        return if (values.size >= 4) {
            floatArrayOf(
                values[0].toFloatOrNull() ?: 0f,
                values[1].toFloatOrNull() ?: 0f,
                values[2].toFloatOrNull() ?: 0f,
                values[3].toFloatOrNull() ?: 0f
            )
        } else null
    }
}