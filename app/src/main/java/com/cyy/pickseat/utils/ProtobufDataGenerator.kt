package com.cyy.pickseat.utils

import android.content.Context
import com.cyy.pickseat.data.model.*
import com.cyy.pickseat.data.parser.ProtobufParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Protobuf数据生成工具
 * 用于生成示例protobuf数据文件
 */
object ProtobufDataGenerator {
    
    /**
     * 生成示例protobuf数据并保存到assets目录
     */
    suspend fun generateExampleProtobufData(context: Context) = withContext(Dispatchers.IO) {
        try {
            val protobufParser = ProtobufParser(context)
            
            // 创建示例场馆布局
            val venueLayout = createExampleVenueLayout()
            
            // 生成SVG protobuf数据
            val svgContent = context.assets.open("data/venue_example.svg").bufferedReader().use { it.readText() }
            val svgProtobuf = protobufParser.createSvgProtobuf(svgContent, venueLayout)
            
            // 生成GeoJSON protobuf数据
            val geoJsonContent = context.assets.open("data/venue_example.geojson").bufferedReader().use { it.readText() }
            val geoJsonProtobuf = protobufParser.createGeoJsonProtobuf(geoJsonContent, venueLayout)
            
            // 保存到内部存储（因为assets是只读的）
            val internalDir = File(context.filesDir, "protobuf_data")
            if (!internalDir.exists()) {
                internalDir.mkdirs()
            }
            
            // 保存SVG protobuf
            val svgFile = File(internalDir, "venue_example_svg.pb")
            FileOutputStream(svgFile).use { fos ->
                svgProtobuf.writeTo(fos)
            }
            
            // 保存GeoJSON protobuf
            val geoJsonFile = File(internalDir, "venue_example_geojson.pb")
            FileOutputStream(geoJsonFile).use { fos ->
                geoJsonProtobuf.writeTo(fos)
            }
            
            android.util.Log.i("ProtobufGenerator", "Generated protobuf files:")
            android.util.Log.i("ProtobufGenerator", "SVG: ${svgFile.absolutePath} (${svgFile.length()} bytes)")
            android.util.Log.i("ProtobufGenerator", "GeoJSON: ${geoJsonFile.absolutePath} (${geoJsonFile.length()} bytes)")
            
        } catch (e: Exception) {
            android.util.Log.e("ProtobufGenerator", "Error generating protobuf data", e)
        }
    }
    
    /**
     * 创建示例场馆布局
     */
    private fun createExampleVenueLayout(): VenueLayout {
        // 创建座位数据
        val areaA = createSeatArea("area-a", "北看台A区", 200f, 100f, 1600f, 300f, "#FFD700", 3000)
        val areaB = createSeatArea("area-b", "南看台B区", 200f, 1100f, 1600f, 300f, "#FF9800", 3000)
        val areaC = createSeatArea("area-c", "西看台C区", 50f, 450f, 500f, 600f, "#2196F3", 2500)
        val areaD = createSeatArea("area-d", "东看台D区", 1450f, 450f, 500f, 600f, "#9C27B0", 2500)
        val areaVip = createSeatArea("area-vip", "VIP包厢区", 800f, 200f, 400f, 200f, "#F44336", 200)
        
        // 创建舞台
        val stage = Stage(
            name = "足球场",
            x = 600f,
            y = 500f,
            width = 800f,
            height = 500f,
            color = "#4CAF50"
        )
        
        return VenueLayout(
            id = "example-stadium",
            name = "示例体育场",
            type = VenueType.STADIUM,
            width = 2000f,
            height = 1500f,
            areas = listOf(areaA, areaB, areaC, areaD, areaVip),
            stage = stage,
            backgroundImage = null,
            svgData = null,
            createdAt = System.currentTimeMillis(),
            maxScale = 5f,
            minScale = 0.1f
        )
    }
    
    /**
     * 创建座位区域
     */
    private fun createSeatArea(
        id: String,
        name: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        color: String,
        seatCount: Int
    ): SeatArea {
        val seats = mutableListOf<Seat>()
        
        // 计算座位布局
        val seatsPerRow = when {
            seatCount <= 500 -> 20
            seatCount <= 2000 -> 40
            else -> 50
        }
        val rowCount = (seatCount + seatsPerRow - 1) / seatsPerRow
        
        val seatWidth = width / seatsPerRow
        val seatHeight = height / rowCount
        
        var seatIndex = 0
        for (row in 0 until rowCount) {
            for (col in 0 until seatsPerRow) {
                if (seatIndex >= seatCount) break
                
                val seatX = x + col * seatWidth
                val seatY = y + row * seatHeight
                
                seats.add(
                    Seat(
                        id = "${id}-${row}-${col}",
                        row = "${row + 1}",
                        column = "${col + 1}",
                        name = "${row + 1}-${col + 1}",
                        x = seatX,
                        y = seatY,
                        width = seatWidth * 0.8f, // 留一些间距
                        height = seatHeight * 0.8f,
                        status = SeatStatus.AVAILABLE,
                        price = when (id) {
                            "area-vip" -> 800.0
                            "area-a", "area-b" -> 200.0
                            else -> 150.0
                        },
                        areaId = id
                    )
                )
                seatIndex++
            }
            if (seatIndex >= seatCount) break
        }
        
        return SeatArea(
            id = id,
            name = name,
            type = if (id.contains("vip")) AreaType.VIP else AreaType.NORMAL,
            color = color,
            x = x,
            y = y,
            width = width,
            height = height,
            seats = seats,
            description = "由ProtobufDataGenerator生成的示例数据"
        )
    }
    
    /**
     * 检查protobuf文件是否存在
     */
    fun hasProtobufFiles(context: Context): Boolean {
        val internalDir = File(context.filesDir, "protobuf_data")
        val svgFile = File(internalDir, "venue_example_svg.pb")
        val geoJsonFile = File(internalDir, "venue_example_geojson.pb")
        return svgFile.exists() && geoJsonFile.exists()
    }
    
    /**
     * 获取protobuf文件路径
     */
    fun getProtobufFilePaths(context: Context): Pair<String, String> {
        val internalDir = File(context.filesDir, "protobuf_data")
        val svgFile = File(internalDir, "venue_example_svg.pb")
        val geoJsonFile = File(internalDir, "venue_example_geojson.pb")
        return Pair(svgFile.absolutePath, geoJsonFile.absolutePath)
    }
}
