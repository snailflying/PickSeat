package com.cyy.pickseat.utils

import android.content.Context
import com.cyy.pickseat.data.model.*
import com.cyy.pickseat.data.parser.GeoJsonParser
import com.cyy.pickseat.data.parser.ProtobufParser
import com.cyy.pickseat.data.parser.SvgParser
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.math.*
import kotlin.random.Random

/**
 * 数据源类型枚举
 */
enum class DataSourceType {
    /** 程序生成的Mock数据（10W+座位） */
    MOCK_GENERATED,
    /** SVG格式数据 */
    SVG_ASSETS,
    /** GeoJSON格式数据 */
    GEOJSON_ASSETS,
    /** Protobuf格式数据 */
    PROTOBUF_ASSETS
}

/**
 * Mock数据生成器 - 支持多种数据源
 */
object MockDataGenerator {
    
    private val gson = Gson()
    
    /**
     * 根据数据源类型加载场馆数据
     */
    suspend fun loadVenueData(context: Context, dataSourceType: DataSourceType): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            when (dataSourceType) {
                DataSourceType.MOCK_GENERATED -> generateLargeStadium()
                DataSourceType.SVG_ASSETS -> loadFromSvgAssets(context)
                DataSourceType.GEOJSON_ASSETS -> loadFromGeoJsonAssets(context)
                DataSourceType.PROTOBUF_ASSETS -> loadFromProtobufAssets(context)
            }
        } catch (e: Exception) {
            android.util.Log.e("MockDataGenerator", "Error loading venue data for $dataSourceType", e)
            null
        }
    }
    
    /**
     * 从SVG assets加载数据
     */
    private suspend fun loadFromSvgAssets(context: Context): VenueLayout? {
        return try {
            val svgParser = SvgParser(context)
            val svgContent = context.assets.open("data/venue_example.svg").bufferedReader().use { it.readText() }
            val venueLayout = svgParser.parseVenueLayout(svgContent)
            android.util.Log.i("MockDataGenerator", "Loaded SVG data: ${venueLayout?.name}, seats: ${venueLayout?.getTotalSeatCount()}")
            venueLayout
        } catch (e: Exception) {
            android.util.Log.e("MockDataGenerator", "Error loading SVG assets", e)
            null
        }
    }
    
    /**
     * 从GeoJSON assets加载数据
     */
    private suspend fun loadFromGeoJsonAssets(context: Context): VenueLayout? {
        return try {
            val geoJsonParser = GeoJsonParser()
            val geoJsonContent = context.assets.open("data/venue_example.geojson").bufferedReader().use { it.readText() }
            val venueLayout = geoJsonParser.parseVenueLayout(geoJsonContent)
            android.util.Log.i("MockDataGenerator", "Loaded GeoJSON data: ${venueLayout?.name}, seats: ${venueLayout?.getTotalSeatCount()}")
            venueLayout
        } catch (e: Exception) {
            android.util.Log.e("MockDataGenerator", "Error loading GeoJSON assets", e)
            null
        }
    }
    
    /**
     * 从Protobuf assets加载数据
     */
    private suspend fun loadFromProtobufAssets(context: Context): VenueLayout? {
        return try {
            val protobufParser = ProtobufParser(context)
            
            // 首先确保protobuf文件存在
            if (!ProtobufDataGenerator.hasProtobufFiles(context)) {
                android.util.Log.i("MockDataGenerator", "Generating protobuf files...")
                ProtobufDataGenerator.generateExampleProtobufData(context)
            }
            
            // 加载SVG protobuf数据（也可以选择GeoJSON）
            val (svgPath, _) = ProtobufDataGenerator.getProtobufFilePaths(context)
            val svgFile = File(svgPath)
            
            if (svgFile.exists()) {
                val venueLayout = protobufParser.parseSvgFromProtobuf(FileInputStream(svgFile))
                android.util.Log.i("MockDataGenerator", "Loaded Protobuf data: ${venueLayout?.name}, seats: ${venueLayout?.getTotalSeatCount()}")
                venueLayout
            } else {
                android.util.Log.w("MockDataGenerator", "Protobuf file not found: $svgPath")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("MockDataGenerator", "Error loading Protobuf assets", e)
            null
        }
    }
    
    /**
     * 获取数据源描述
     */
    fun getDataSourceDescription(dataSourceType: DataSourceType): String {
        return when (dataSourceType) {
            DataSourceType.MOCK_GENERATED -> "程序生成 (10W+座位)"
            DataSourceType.SVG_ASSETS -> "SVG格式 (Assets)"
            DataSourceType.GEOJSON_ASSETS -> "GeoJSON格式 (Assets)"
            DataSourceType.PROTOBUF_ASSETS -> "Protobuf格式 (二进制)"
        }
    }
    
    /**
     * 生成大型体育场数据（10W+座位）
     */
    suspend fun generateLargeStadium(): VenueLayout = withContext(Dispatchers.IO) {
        val venueId = "stadium_${System.currentTimeMillis()}"
        val venueName = "国家体育场"
        val venueWidth = 2000f
        val venueHeight = 1500f
        
        val areas = mutableListOf<SeatArea>()
        
        // 创建舞台/球场
        val stage = Stage(
            name = "足球场",
            x = venueWidth / 2 - 200f,
            y = venueHeight / 2 - 100f,
            width = 400f,
            height = 200f,
            color = "#4CAF50"
        )
        
        // 生成多个看台区域
        areas.addAll(generateStadiumAreas(venueWidth, venueHeight, stage))
        
        VenueLayout(
            id = venueId,
            name = venueName,
            type = VenueType.STADIUM,
            width = venueWidth,
            height = venueHeight,
            areas = areas,
            stage = stage,
            maxScale = 8f,
            minScale = 0.05f
        )
    }
    
    /**
     * 生成大型剧院数据
     */
    suspend fun generateLargeTheater(): VenueLayout = withContext(Dispatchers.IO) {
        val venueId = "theater_${System.currentTimeMillis()}"
        val venueName = "国家大剧院"
        val venueWidth = 1200f
        val venueHeight = 1000f
        
        val areas = mutableListOf<SeatArea>()
        
        // 创建舞台
        val stage = Stage(
            name = "舞台",
            x = venueWidth / 2 - 150f,
            y = 50f,
            width = 300f,
            height = 100f,
            color = "#FF6B6B"
        )
        
        // 生成剧院区域
        areas.addAll(generateTheaterAreas(venueWidth, venueHeight, stage))
        
        VenueLayout(
            id = venueId,
            name = venueName,
            type = VenueType.THEATER,
            width = venueWidth,
            height = venueHeight,
            areas = areas,
            stage = stage,
            maxScale = 6f,
            minScale = 0.1f
        )
    }
    
    /**
     * 生成音乐厅数据
     */
    suspend fun generateConcertHall(): VenueLayout = withContext(Dispatchers.IO) {
        val venueId = "concert_${System.currentTimeMillis()}"
        val venueName = "音乐厅"
        val venueWidth = 800f
        val venueHeight = 600f
        
        val areas = mutableListOf<SeatArea>()
        
        // 创建舞台
        val stage = Stage(
            name = "演奏台",
            x = venueWidth / 2 - 100f,
            y = 30f,
            width = 200f,
            height = 80f,
            color = "#9C27B0"
        )
        
        // 生成音乐厅区域
        areas.addAll(generateConcertAreas(venueWidth, venueHeight, stage))
        
        VenueLayout(
            id = venueId,
            name = venueName,
            type = VenueType.CONCERT_HALL,
            width = venueWidth,
            height = venueHeight,
            areas = areas,
            stage = stage,
            maxScale = 5f,
            minScale = 0.2f
        )
    }
    
    /**
     * 生成体育场看台区域
     */
    private fun generateStadiumAreas(venueWidth: Float, venueHeight: Float, stage: Stage): List<SeatArea> {
        val areas = mutableListOf<SeatArea>()
        val centerX = venueWidth / 2
        val centerY = venueHeight / 2
        
        // 四个主要看台
        val stands = listOf(
            // 北看台
            StandInfo("north", "北看台", centerX - 400f, 50f, 800f, 300f, AreaType.NORMAL, "#2196F3"),
            // 南看台
            StandInfo("south", "南看台", centerX - 400f, venueHeight - 350f, 800f, 300f, AreaType.NORMAL, "#2196F3"),
            // 东看台
            StandInfo("east", "东看台", venueWidth - 350f, centerY - 250f, 300f, 500f, AreaType.VIP, "#FF6B6B"),
            // 西看台
            StandInfo("west", "西看台", 50f, centerY - 250f, 300f, 500f, AreaType.VIP, "#FF6B6B")
        )
        
        stands.forEach { standInfo ->
            val seats = generateStandSeats(standInfo)
            areas.add(
                SeatArea(
                    id = standInfo.id,
                    name = standInfo.name,
                    type = standInfo.type,
                    color = standInfo.color,
                    x = standInfo.x,
                    y = standInfo.y,
                    width = standInfo.width,
                    height = standInfo.height,
                    seats = seats
                )
            )
        }
        
        // 添加更多小区域以达到10W+座位
        areas.addAll(generateAdditionalStadiumAreas(venueWidth, venueHeight))
        
        return areas
    }
    
    /**
     * 生成剧院区域
     */
    private fun generateTheaterAreas(venueWidth: Float, venueHeight: Float, stage: Stage): List<SeatArea> {
        val areas = mutableListOf<SeatArea>()
        
        // 一楼池座
        areas.add(generateTheaterSection(
            "orchestra", "池座", 
            100f, 200f, venueWidth - 200f, 300f,
            AreaType.NORMAL, "#FFD93D", 25, 50
        ))
        
        // 二楼包厢
        val boxWidth = 120f
        val boxHeight = 80f
        for (i in 0 until 20) {
            val x = 50f + (i % 10) * (boxWidth + 20f)
            val y = if (i < 10) 550f else 650f
            areas.add(generateTheaterSection(
                "box_$i", "包厢${i + 1}",
                x, y, boxWidth, boxHeight,
                AreaType.BOX, "#F38BA8", 4, 6
            ))
        }
        
        // 三楼楼座
        areas.add(generateTheaterSection(
            "balcony", "楼座",
            150f, 750f, venueWidth - 300f, 200f,
            AreaType.PREMIUM, "#4ECDC4", 20, 40
        ))
        
        return areas
    }
    
    /**
     * 生成音乐厅区域
     */
    private fun generateConcertAreas(venueWidth: Float, venueHeight: Float, stage: Stage): List<SeatArea> {
        val areas = mutableListOf<SeatArea>()
        
        // 主厅座位 - 扇形排列
        val mainHallSeats = generateFanShapedSeats(
            centerX = venueWidth / 2,
            centerY = stage.y + stage.height + 50f,
            startRadius = 100f,
            endRadius = 400f,
            startAngle = -60f,
            endAngle = 60f,
            rows = 30,
            seatsPerRow = 35
        )
        
        areas.add(
            SeatArea(
                id = "main_hall",
                name = "主厅",
                type = AreaType.NORMAL,
                color = "#9C27B0",
                x = 100f,
                y = 150f,
                width = venueWidth - 200f,
                height = venueHeight - 200f,
                seats = mainHallSeats
            )
        )
        
        return areas
    }
    
    /**
     * 生成看台座位
     */
    private fun generateStandSeats(standInfo: StandInfo): List<Seat> {
        val seats = mutableListOf<Seat>()
        val rows = (standInfo.height / 35f).toInt() // 每行35像素
        val seatsPerRow = (standInfo.width / 30f).toInt() // 每座位30像素
        
        for (row in 0 until rows) {
            for (seatNum in 0 until seatsPerRow) {
                val seatId = "${standInfo.id}_${row + 1}_${seatNum + 1}"
                val seatName = "${row + 1}排${seatNum + 1}号"
                val x = standInfo.x + seatNum * 30f + 5f
                val y = standInfo.y + row * 35f + 5f
                
                // 设置座位状态（随机分布）
                val status = when (Random.nextInt(20)) {
                    0, 1, 2 -> SeatStatus.SOLD
                    19 -> SeatStatus.UNAVAILABLE
                    else -> SeatStatus.AVAILABLE
                }
                
                // 设置价格（根据区域类型和位置）
                val price = calculateSeatPrice(standInfo.type, row, rows)
                
                seats.add(
                    Seat(
                        id = seatId,
                        row = (row + 1).toString(),
                        column = (seatNum + 1).toString(),
                        name = seatName,
                        x = x,
                        y = y,
                        width = 25f,
                        height = 25f,
                        status = status,
                        price = price,
                        areaId = standInfo.id
                    )
                )
            }
        }
        
        return seats
    }
    
    /**
     * 生成剧院区域座位
     */
    private fun generateTheaterSection(
        id: String, name: String,
        x: Float, y: Float, width: Float, height: Float,
        type: AreaType, color: String,
        rows: Int, seatsPerRow: Int
    ): SeatArea {
        val seats = mutableListOf<Seat>()
        val seatWidth = width / seatsPerRow
        val seatHeight = height / rows
        
        for (row in 0 until rows) {
            for (seatNum in 0 until seatsPerRow) {
                val seatId = "${id}_${row + 1}_${seatNum + 1}"
                val seatName = "${row + 1}排${seatNum + 1}号"
                val seatX = x + seatNum * seatWidth
                val seatY = y + row * seatHeight
                
                val status = when (Random.nextInt(15)) {
                    0, 1 -> SeatStatus.SOLD
                    14 -> SeatStatus.UNAVAILABLE
                    else -> SeatStatus.AVAILABLE
                }
                
                val price = calculateSeatPrice(type, row, rows)
                
                seats.add(
                    Seat(
                        id = seatId,
                        row = (row + 1).toString(),
                        column = (seatNum + 1).toString(),
                        name = seatName,
                        x = seatX,
                        y = seatY,
                        width = seatWidth * 0.8f,
                        height = seatHeight * 0.8f,
                        status = status,
                        price = price,
                        areaId = id
                    )
                )
            }
        }
        
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
     * 生成扇形座位排列
     */
    private fun generateFanShapedSeats(
        centerX: Float, centerY: Float,
        startRadius: Float, endRadius: Float,
        startAngle: Float, endAngle: Float,
        rows: Int, seatsPerRow: Int
    ): List<Seat> {
        val seats = mutableListOf<Seat>()
        val radiusStep = (endRadius - startRadius) / rows
        val angleStep = (endAngle - startAngle) / seatsPerRow
        
        for (row in 0 until rows) {
            val radius = startRadius + row * radiusStep
            for (seatNum in 0 until seatsPerRow) {
                val angle = Math.toRadians((startAngle + seatNum * angleStep).toDouble())
                val seatX = centerX + radius * cos(angle).toFloat()
                val seatY = centerY + radius * sin(angle).toFloat()
                
                val seatId = "fan_${row + 1}_${seatNum + 1}"
                val seatName = "${row + 1}排${seatNum + 1}号"
                
                val status = when (Random.nextInt(18)) {
                    0, 1 -> SeatStatus.SOLD
                    17 -> SeatStatus.UNAVAILABLE
                    else -> SeatStatus.AVAILABLE
                }
                
                val price = 100.0 + (rows - row) * 10.0 // 前排更贵
                
                seats.add(
                    Seat(
                        id = seatId,
                        row = (row + 1).toString(),
                        column = (seatNum + 1).toString(),
                        name = seatName,
                        x = seatX - 12f,
                        y = seatY - 12f,
                        width = 24f,
                        height = 24f,
                        status = status,
                        price = price,
                        areaId = "main_hall"
                    )
                )
            }
        }
        
        return seats
    }
    
    /**
     * 生成额外的体育场区域以达到10W+座位
     */
    private fun generateAdditionalStadiumAreas(venueWidth: Float, venueHeight: Float): List<SeatArea> {
        val areas = mutableListOf<SeatArea>()
        
        // 生成上层看台
        val upperStands = listOf(
            StandInfo("upper_north", "北上层看台", 200f, 20f, 600f, 150f, AreaType.PREMIUM, "#4ECDC4"),
            StandInfo("upper_south", "南上层看台", 200f, venueHeight - 170f, 600f, 150f, AreaType.PREMIUM, "#4ECDC4"),
            StandInfo("upper_east", "东上层看台", venueWidth - 170f, 200f, 150f, 300f, AreaType.PREMIUM, "#4ECDC4"),
            StandInfo("upper_west", "西上层看台", 20f, 200f, 150f, 300f, AreaType.PREMIUM, "#4ECDC4")
        )
        
        upperStands.forEach { standInfo ->
            val seats = generateStandSeats(standInfo)
            areas.add(
                SeatArea(
                    id = standInfo.id,
                    name = standInfo.name,
                    type = standInfo.type,
                    color = standInfo.color,
                    x = standInfo.x,
                    y = standInfo.y,
                    width = standInfo.width,
                    height = standInfo.height,
                    seats = seats
                )
            )
        }
        
        // 生成角落区域
        val cornerAreas = generateCornerAreas(venueWidth, venueHeight)
        areas.addAll(cornerAreas)
        
        return areas
    }
    
    /**
     * 生成角落区域
     */
    private fun generateCornerAreas(venueWidth: Float, venueHeight: Float): List<SeatArea> {
        val areas = mutableListOf<SeatArea>()
        val cornerSize = 120f
        
        val corners = listOf(
            Triple("corner_nw", "西北角", Pair(30f, 30f)),
            Triple("corner_ne", "东北角", Pair(venueWidth - cornerSize - 30f, 30f)),
            Triple("corner_sw", "西南角", Pair(30f, venueHeight - cornerSize - 30f)),
            Triple("corner_se", "东南角", Pair(venueWidth - cornerSize - 30f, venueHeight - cornerSize - 30f))
        )
        
        corners.forEach { (id, name, position) ->
            areas.add(generateTheaterSection(
                id, name,
                position.first, position.second, cornerSize, cornerSize,
                AreaType.DISABLED, "#95E1D3", 8, 8
            ))
        }
        
        return areas
    }
    
    /**
     * 计算座位价格
     */
    private fun calculateSeatPrice(areaType: AreaType, row: Int, totalRows: Int): Double {
        val basePrice = when (areaType) {
            AreaType.VIP -> 500.0
            AreaType.PREMIUM -> 300.0
            AreaType.BOX -> 800.0
            AreaType.DISABLED -> 200.0
            AreaType.NORMAL -> 150.0
        }
        
        // 前排价格更高
        val rowMultiplier = 1.0 + (totalRows - row) * 0.1
        return basePrice * rowMultiplier
    }
    
    /**
     * 将场馆布局转换为JSON字符串
     */
    fun venueLayoutToJson(layout: VenueLayout): String {
        return gson.toJson(layout)
    }
    
    /**
     * 从JSON字符串解析场馆布局
     */
    fun venueLayoutFromJson(json: String): VenueLayout? {
        return try {
            gson.fromJson(json, VenueLayout::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 看台信息数据类
     */
    private data class StandInfo(
        val id: String,
        val name: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val type: AreaType,
        val color: String
    )
}
