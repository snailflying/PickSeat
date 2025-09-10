package com.cyy.pickseat.data.model

import com.google.gson.annotations.SerializedName

/**
 * 场馆布局数据模型
 */
data class VenueLayout(
    /** 场馆ID */
    @SerializedName("id")
    val id: String,
    
    /** 场馆名称 */
    @SerializedName("name")
    val name: String,
    
    /** 场馆类型 */
    @SerializedName("type")
    val type: VenueType = VenueType.STADIUM,
    
    /** 场馆总宽度 */
    @SerializedName("width")
    val width: Float,
    
    /** 场馆总高度 */
    @SerializedName("height")
    val height: Float,
    
    /** 座位区域列表 */
    @SerializedName("areas")
    val areas: List<SeatArea>,
    
    /** 舞台/球场位置 */
    @SerializedName("stage")
    val stage: Stage? = null,
    
    /** 场馆背景图URL */
    @SerializedName("backgroundImage")
    val backgroundImage: String? = null,
    
    /** SVG数据 */
    @SerializedName("svgData")
    val svgData: String? = null,
    
    /** 创建时间 */
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    /** 最大缩放比例 */
    @SerializedName("maxScale")
    val maxScale: Float = 5.0f,
    
    /** 最小缩放比例 */
    @SerializedName("minScale")
    val minScale: Float = 0.1f
) {
    
    /**
     * 获取所有座位
     */
    fun getAllSeats(): List<Seat> {
        return areas.flatMap { it.seats }
    }
    
    /**
     * 根据ID查找座位
     */
    fun findSeatById(seatId: String): Seat? {
        return getAllSeats().find { it.id == seatId }
    }
    
    /**
     * 根据坐标查找座位
     */
    fun findSeatByPosition(x: Float, y: Float): Seat? {
        return getAllSeats().find { it.contains(x, y) }
    }
    
    /**
     * 根据ID查找区域
     */
    fun findAreaById(areaId: String): SeatArea? {
        return areas.find { it.id == areaId }
    }
    
    /**
     * 获取总座位数
     */
    fun getTotalSeatCount(): Int {
        return getAllSeats().size
    }
    
    /**
     * 获取可选座位数
     */
    fun getAvailableSeatCount(): Int {
        return getAllSeats().count { it.status == SeatStatus.AVAILABLE }
    }
    
    /**
     * 获取已选座位数
     */
    fun getSelectedSeatCount(): Int {
        return getAllSeats().count { it.isSelected }
    }
}

/**
 * 场馆类型枚举
 */
enum class VenueType {
    /** 体育场 */
    @SerializedName("stadium")
    STADIUM,
    
    /** 剧院 */
    @SerializedName("theater")
    THEATER,
    
    /** 音乐厅 */
    @SerializedName("concert_hall")
    CONCERT_HALL,
    
    /** 会议厅 */
    @SerializedName("conference_hall")
    CONFERENCE_HALL,
    
    /** 电影院 */
    @SerializedName("cinema")
    CINEMA
}

/**
 * 舞台/球场数据模型
 */
data class Stage(
    /** 舞台名称 */
    @SerializedName("name")
    val name: String = "舞台",
    
    /** X坐标 */
    @SerializedName("x")
    val x: Float,
    
    /** Y坐标 */
    @SerializedName("y")
    val y: Float,
    
    /** 宽度 */
    @SerializedName("width")
    val width: Float,
    
    /** 高度 */
    @SerializedName("height")
    val height: Float,
    
    /** 舞台颜色 */
    @SerializedName("color")
    val color: String = "#FF6B6B"
)
