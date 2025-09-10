package com.cyy.pickseat.ui.view.renderer

import android.graphics.*
import com.cyy.pickseat.data.model.Seat
import com.cyy.pickseat.data.model.SeatStatus

/**
 * 座位渲染器
 * 负责渲染单个座位的视觉效果
 */
class SeatRenderer : BaseRenderer() {
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    
    private val selectedSeats = mutableSetOf<String>()
    
    /**
     * 渲染座位
     */
    fun renderSeat(
        canvas: Canvas,
        seat: Seat,
        scaleFactor: Float,
        showText: Boolean = false
    ) {
        // 设置座位颜色
        val seatColor = when {
            selectedSeats.contains(seat.id) -> Color.parseColor("#4CAF50") // 绿色-已选
            seat.status == SeatStatus.SOLD -> Color.parseColor("#F44336") // 红色-已售
            seat.status == SeatStatus.UNAVAILABLE -> Color.parseColor("#9E9E9E") // 灰色-不可选
            seat.status == SeatStatus.MAINTENANCE -> Color.parseColor("#FF9800") // 橙色-维修
            else -> Color.parseColor("#2196F3") // 蓝色-可选
        }
        
        setupPaint(seatColor)
        
        // 绘制座位（圆形）
        val radius = minOf(seat.width, seat.height) / 2
        val centerX = seat.x + seat.width / 2
        val centerY = seat.y + seat.height / 2
        
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // 绘制座位号
        if (showText && scaleFactor > 2f) {
            textPaint.color = Color.WHITE
            textPaint.textSize = 16f / scaleFactor
            canvas.drawText(
                seat.name,
                centerX,
                centerY + textPaint.textSize / 3,
                textPaint
            )
        }
    }
    
    /**
     * 批量渲染座位
     */
    override fun render(
        canvas: Canvas,
        matrix: Matrix,
        visibleRect: RectF,
        scaleFactor: Float
    ) {
        // 这个方法由具体的区域渲染器调用
        // 这里提供基础的渲染逻辑
    }
    
    /**
     * 设置选中的座位
     */
    fun setSelectedSeats(seats: Set<String>) {
        selectedSeats.clear()
        selectedSeats.addAll(seats)
    }
    
    /**
     * 添加选中座位
     */
    fun addSelectedSeat(seatId: String) {
        selectedSeats.add(seatId)
    }
    
    /**
     * 移除选中座位
     */
    fun removeSelectedSeat(seatId: String) {
        selectedSeats.remove(seatId)
    }
    
    /**
     * 清空选中座位
     */
    fun clearSelectedSeats() {
        selectedSeats.clear()
    }
    
    /**
     * 检查座位是否被选中
     */
    fun isSeatSelected(seatId: String): Boolean {
        return selectedSeats.contains(seatId)
    }
}

