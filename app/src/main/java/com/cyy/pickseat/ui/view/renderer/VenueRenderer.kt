package com.cyy.pickseat.ui.view.renderer

import android.graphics.*
import com.cyy.pickseat.data.model.Seat
import com.cyy.pickseat.data.model.VenueLayout

/**
 * 场馆渲染器
 * 负责协调所有渲染器，管理整个场馆的渲染流程
 */
class VenueRenderer : BaseRenderer() {
    
    private val areaRenderer = AreaRenderer()
    private val stageRenderer = StageRenderer()
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#F5F5F5")
    }
    
    var venueLayout: VenueLayout? = null
        private set
    
    /**
     * 设置场馆布局
     */
    fun setVenueLayout(layout: VenueLayout) {
        venueLayout = layout
    }
    
    /**
     * 主渲染方法
     */
    override fun render(
        canvas: Canvas,
        matrix: Matrix,
        visibleRect: RectF,
        scaleFactor: Float
    ) {
        val layout = venueLayout ?: return
        
        // 应用变换矩阵
        canvas.save()
        canvas.concat(matrix)
        
        // 绘制背景
        renderBackground(canvas, layout)
        
        // 绘制舞台
        layout.stage?.let { stage ->
            stageRenderer.renderStage(canvas, stage, visibleRect, scaleFactor)
            if (scaleFactor > 1.5f) {
                stageRenderer.renderStageDecorations(canvas, stage, scaleFactor)
            }
        }
        
        // 绘制座位区域
        layout.areas.forEach { area ->
            areaRenderer.renderArea(canvas, area, visibleRect, scaleFactor)
        }
        
        canvas.restore()
    }
    
    /**
     * 渲染背景
     */
    private fun renderBackground(canvas: Canvas, layout: VenueLayout) {
        canvas.drawRect(0f, 0f, layout.width, layout.height, backgroundPaint)
    }
    
    /**
     * 查找点击的座位
     */
    fun findSeatAt(x: Float, y: Float): Seat? {
        val layout = venueLayout ?: return null
        
        // 遍历所有区域查找座位
        for (area in layout.areas) {
            val seat = areaRenderer.findSeatAt(area, x, y)
            if (seat != null) {
                return seat
            }
        }
        
        return null
    }
    
    /**
     * 获取座位渲染器
     */
    fun getSeatRenderer(): SeatRenderer = areaRenderer.getSeatRenderer()
    
    /**
     * 获取区域渲染器
     */
    fun getAreaRenderer(): AreaRenderer = areaRenderer
    
    /**
     * 获取舞台渲染器
     */
    fun getStageRenderer(): StageRenderer = stageRenderer
    
    /**
     * 设置背景颜色
     */
    fun setBackgroundColor(color: Int) {
        backgroundPaint.color = color
    }
    
    /**
     * 获取渲染统计信息
     */
    fun getRenderStats(): RenderStats {
        val layout = venueLayout ?: return RenderStats()
        
        val totalSeats = layout.getTotalSeatCount()
        val visibleAreas = layout.areas.size
        val hasStage = layout.stage != null
        
        return RenderStats(
            totalSeats = totalSeats,
            visibleAreas = visibleAreas,
            hasStage = hasStage
        )
    }
    
    /**
     * 渲染统计数据类
     */
    data class RenderStats(
        val totalSeats: Int = 0,
        val visibleAreas: Int = 0,
        val hasStage: Boolean = false,
        val renderTime: Long = 0L
    )
}
