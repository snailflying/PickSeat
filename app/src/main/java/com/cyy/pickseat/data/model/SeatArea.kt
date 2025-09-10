package com.cyy.pickseat.data.model

import com.google.gson.annotations.SerializedName

/**
 * 座位区域数据模型
 */
data class SeatArea(
    /** 区域ID */
    @SerializedName("id")
    val id: String,
    
    /** 区域名称 */
    @SerializedName("name")
    val name: String,
    
    /** 区域类型 */
    @SerializedName("type")
    val type: AreaType = AreaType.NORMAL,
    
    /** 区域颜色（16进制） */
    @SerializedName("color")
    val color: String = "#FFD700",
    
    /** 区域左上角X坐标 */
    @SerializedName("x")
    val x: Float,
    
    /** 区域左上角Y坐标 */
    @SerializedName("y")
    val y: Float,
    
    /** 区域宽度 */
    @SerializedName("width")
    val width: Float,
    
    /** 区域高度 */
    @SerializedName("height")
    val height: Float,
    
    /** 区域内座位列表 */
    @SerializedName("seats")
    val seats: List<Seat> = emptyList(),
    
    /** 区域描述 */
    @SerializedName("description")
    val description: String = ""
) {
    
    /**
     * 检查点是否在区域范围内
     */
    fun contains(pointX: Float, pointY: Float): Boolean {
        return pointX >= x && pointX <= x + width &&
               pointY >= y && pointY <= y + height
    }
    
    /**
     * 获取区域中心点X坐标
     */
    fun getCenterX(): Float = x + width / 2
    
    /**
     * 获取区域中心点Y坐标
     */
    fun getCenterY(): Float = y + height / 2
    
    /**
     * 获取可选座位数量
     */
    fun getAvailableSeatCount(): Int {
        return seats.count { it.status == SeatStatus.AVAILABLE }
    }
    
    /**
     * 获取已选座位数量
     */
    fun getSelectedSeatCount(): Int {
        return seats.count { it.isSelected }
    }
}

/**
 * 区域类型枚举
 */
enum class AreaType {
    /** 普通区 */
    @SerializedName("normal")
    NORMAL,
    
    /** VIP区 */
    @SerializedName("vip")
    VIP,
    
    /** 残疾人专用区 */
    @SerializedName("disabled")
    DISABLED,
    
    /** 贵宾区 */
    @SerializedName("premium")
    PREMIUM,
    
    /** 包厢区 */
    @SerializedName("box")
    BOX
}
