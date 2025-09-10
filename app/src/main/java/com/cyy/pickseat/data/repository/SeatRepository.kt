package com.cyy.pickseat.data.repository

import android.content.Context
import com.cyy.pickseat.data.model.VenueLayout
import com.cyy.pickseat.data.parser.GeoJsonParser
import com.cyy.pickseat.data.parser.SvgParser
import com.cyy.pickseat.utils.MockDataGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 座位数据仓库
 * 负责数据的加载、缓存和管理
 */
class SeatRepository(private val context: Context) {
    
    private val geoJsonParser = GeoJsonParser()
    private val svgParser = SvgParser(context)
    private val cacheDir = File(context.cacheDir, "venues")
    
    init {
        // 确保缓存目录存在
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    /**
     * 从SVG数据加载场馆布局
     */
    suspend fun loadVenueFromSvg(svgData: String): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            svgParser.parseVenueLayout(svgData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从GeoJSON数据加载场馆布局
     */
    suspend fun loadVenueFromGeoJson(geoJsonData: String): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            geoJsonParser.parseVenueLayout(geoJsonData)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 从Assets文件加载场馆布局
     */
    suspend fun loadVenueFromAssets(fileName: String): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open(fileName)
            val data = inputStream.bufferedReader().use { it.readText() }
            
            when {
                fileName.endsWith(".svg", ignoreCase = true) -> {
                    svgParser.parseVenueLayout(data)
                }
                fileName.endsWith(".json", ignoreCase = true) || 
                fileName.endsWith(".geojson", ignoreCase = true) -> {
                    geoJsonParser.parseVenueLayout(data)
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 生成Mock场馆数据
     */
    suspend fun generateMockVenue(type: VenueType): VenueLayout = withContext(Dispatchers.IO) {
        when (type) {
            VenueType.STADIUM -> MockDataGenerator.generateLargeStadium()
            VenueType.THEATER -> MockDataGenerator.generateLargeTheater()
            VenueType.CONCERT_HALL -> MockDataGenerator.generateConcertHall()
        }
    }
    
    /**
     * 缓存场馆数据到本地
     */
    suspend fun cacheVenueLayout(venueId: String, layout: VenueLayout) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(cacheDir, "$venueId.json")
            val jsonData = MockDataGenerator.venueLayoutToJson(layout)
            cacheFile.writeText(jsonData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 从缓存加载场馆数据
     */
    suspend fun loadVenueFromCache(venueId: String): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(cacheDir, "$venueId.json")
            if (cacheFile.exists()) {
                val jsonData = cacheFile.readText()
                MockDataGenerator.venueLayoutFromJson(jsonData)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 清除缓存
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取缓存大小（字节）
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 检查是否有缓存数据
     */
    suspend fun hasCachedVenue(venueId: String): Boolean = withContext(Dispatchers.IO) {
        val cacheFile = File(cacheDir, "$venueId.json")
        cacheFile.exists()
    }
    
    /**
     * 获取所有缓存的场馆ID
     */
    suspend fun getCachedVenueIds(): List<String> = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()
                ?.filter { it.extension == "json" }
                ?.map { it.nameWithoutExtension }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 场馆类型枚举
     */
    enum class VenueType {
        STADIUM,    // 体育场
        THEATER,    // 剧院
        CONCERT_HALL // 音乐厅
    }
}
