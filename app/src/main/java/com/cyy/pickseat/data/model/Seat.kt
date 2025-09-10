package com.cyy.pickseat.data.model

import com.google.gson.annotations.SerializedName

/**
 * 座位数据模型
 */
data class Seat(
    /** 座位ID */
    @SerializedName("id")
    val id: String,
    
    /** 座位行号 */
    @SerializedName("row")
    val row: String,
    
    /** 座位列号 */
    @SerializedName("column")
    val column: String,
    
    /** 座位显示名称 */
    @SerializedName("name")
    val name: String,
    
    /** X坐标 */
    @SerializedName("x")
    val x: Float,
    
    /** Y坐标 */
    @SerializedName("y")
    val y: Float,
    
    /** 座位宽度 */
    @SerializedName("width")
    val width: Float = 30f,
    
    /** 座位高度 */
    @SerializedName("height")
    val height: Float = 30f,
    
    /** 座位状态 */
    @SerializedName("status")
    val status: SeatStatus = SeatStatus.AVAILABLE,
    
    /** 座位价格 */
    @SerializedName("price")
    val price: Double = 0.0,
    
    /** 所属区域ID */
    @SerializedName("areaId")
    val areaId: String,
    
    /** 是否被选中 */
    var isSelected: Boolean = false
) {
    
    /**
     * 检查点是否在座位范围内
     */
    fun contains(pointX: Float, pointY: Float): Boolean {
        return pointX >= x && pointX <= x + width &&
               pointY >= y && pointY <= y + height
    }
    
    /**
     * 获取座位中心点X坐标
     */
    fun getCenterX(): Float = x + width / 2
    
    /**
     * 获取座位中心点Y坐标
     */
    fun getCenterY(): Float = y + height / 2
}

/**
 * 座位状态枚举
 */
enum class SeatStatus {
    /** 可选 */
    @SerializedName("available")
    AVAILABLE,
    
    /** 已售 */
    @SerializedName("sold")
    SOLD,
    
    /** 不可选 */
    @SerializedName("unavailable")
    UNAVAILABLE,
    
    /** 维修中 */
    @SerializedName("maintenance")
    MAINTENANCE
}
