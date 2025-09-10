package com.cyy.pickseat.ui.view.renderer

import android.graphics.*
import com.cyy.pickseat.data.model.SeatArea
import com.cyy.pickseat.data.model.VenueLayout

/**
 * 区域渲染器
 * 负责渲染座位区域和管理座位渲染
 */
class AreaRenderer : BaseRenderer() {
    
    private val seatRenderer = SeatRenderer()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    
    /**
     * 渲染场馆所有区域
     */
    override fun render(
        canvas: Canvas,
        matrix: Matrix,
        visibleRect: RectF,
        scaleFactor: Float
    ) {
        // 由VenueRenderer调用具体的区域渲染
    }
    
    /**
     * 渲染单个区域
     */
    fun renderArea(
        canvas: Canvas,
        area: SeatArea,
        visibleRect: RectF,
        scaleFactor: Float
    ) {
        // 检查区域是否可见
        tempRect.set(area.x, area.y, area.x + area.width, area.y + area.height)
        val isVisible = shouldRender(tempRect, visibleRect)
        if (!isVisible) {
            return
        }
        
        // 添加调试信息
        android.util.Log.i("aaron", "Rendering area: ${area.name}, scale: $scaleFactor, visible: $isVisible")
        
        // 根据缩放级别决定渲染策略
        when {
            scaleFactor < 0.3f -> renderAreaAsBlock(canvas, area)
            scaleFactor < 1f -> renderAreaWithReducedDetail(canvas, area, visibleRect)
            else -> renderAreaWithFullDetail(canvas, area, visibleRect, scaleFactor)
        }
    }
    
    /**
     * 以块状方式渲染区域（最低细节）
     */
    private fun renderAreaAsBlock(canvas: Canvas, area: SeatArea) {
        val areaColor = Color.parseColor(area.color)
        setupPaint(areaColor, style = Paint.Style.FILL)
        paint.alpha = 180
        
        canvas.drawRect(
            area.x, area.y,
            area.x + area.width, area.y + area.height,
            paint
        )
        
        // 绘制区域名称
        textPaint.color = Color.BLACK
        textPaint.textSize = 32f
        canvas.drawText(
            area.name,
            area.x + area.width / 2,
            area.y + area.height / 2,
            textPaint
        )
    }
    
    /**
     * 以简化细节绘制区域
     */
    private fun renderAreaWithReducedDetail(canvas: Canvas, area: SeatArea, visibleRect: RectF) {
        // 只绘制部分座位以提高性能
        val step = 2 // 每隔2个座位绘制一个
        
        area.seats.forEachIndexed { index, seat ->
            if (index % step == 0) {
                tempRect.set(seat.x, seat.y, seat.x + seat.width, seat.y + seat.height)
                if (shouldRender(tempRect, visibleRect)) { // 使用正确的可见性检查
                    seatRenderer.renderSeat(canvas, seat, 1f, false)
                }
            }
        }
    }
    
    /**
     * 以完整细节绘制区域
     */
    private fun renderAreaWithFullDetail(canvas: Canvas, area: SeatArea, visibleRect: RectF, scaleFactor: Float) {
        area.seats.forEach { seat ->
            tempRect.set(seat.x, seat.y, seat.x + seat.width, seat.y + seat.height)
            if (shouldRender(tempRect, visibleRect)) { // 使用正确的visibleRect
                seatRenderer.renderSeat(canvas, seat, scaleFactor, scaleFactor > 2f)
            }
        }
    }
    
    /**
     * 获取座位渲染器
     */
    fun getSeatRenderer(): SeatRenderer = seatRenderer
    
    /**
     * 查找点击的座位
     */
    fun findSeatAt(area: SeatArea, x: Float, y: Float): com.cyy.pickseat.data.model.Seat? {
        return area.seats.find { seat ->
            seat.contains(x, y)
        }
    }
}

