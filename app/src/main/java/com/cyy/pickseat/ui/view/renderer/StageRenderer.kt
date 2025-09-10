package com.cyy.pickseat.ui.view.renderer

import android.graphics.*
import com.cyy.pickseat.data.model.Stage

/**
 * 舞台渲染器
 * 负责渲染舞台/球场等中心区域
 */
class StageRenderer : BaseRenderer() {
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    
    /**
     * 渲染舞台
     */
    override fun render(
        canvas: Canvas,
        matrix: Matrix,
        visibleRect: RectF,
        scaleFactor: Float
    ) {
        // 由VenueRenderer调用具体的舞台渲染
    }
    
    /**
     * 渲染单个舞台
     */
    fun renderStage(
        canvas: Canvas,
        stage: Stage,
        visibleRect: RectF,
        scaleFactor: Float
    ) {
        // 检查舞台是否在可见区域内
        tempRect.set(stage.x, stage.y, stage.x + stage.width, stage.y + stage.height)
        if (!shouldRender(tempRect, visibleRect)) {
            return
        }
        
        // 绘制舞台背景
        val stageColor = Color.parseColor(stage.color)
        setupPaint(stageColor, style = Paint.Style.FILL)
        
        canvas.drawRect(
            stage.x, stage.y,
            stage.x + stage.width, stage.y + stage.height,
            paint
        )
        
        // 绘制舞台边框
        setupPaint(Color.BLACK, strokeWidth = 3f, style = Paint.Style.STROKE)
        canvas.drawRect(
            stage.x, stage.y,
            stage.x + stage.width, stage.y + stage.height,
            paint
        )
        
        // 绘制舞台文字
        if (scaleFactor > 0.5f) {
            textPaint.color = Color.WHITE
            textPaint.textSize = (24f / scaleFactor).coerceAtLeast(16f)
            
            // 添加文字阴影效果
            textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK)
            
            canvas.drawText(
                stage.name,
                stage.x + stage.width / 2,
                stage.y + stage.height / 2 + textPaint.textSize / 3,
                textPaint
            )
            
            // 清除阴影
            textPaint.clearShadowLayer()
        }
    }
    
    /**
     * 渲染舞台装饰效果
     */
    fun renderStageDecorations(
        canvas: Canvas,
        stage: Stage,
        scaleFactor: Float
    ) {
        if (scaleFactor < 1f) return
        
        // 绘制舞台光晕效果
        val gradient = RadialGradient(
            stage.x + stage.width / 2,
            stage.y + stage.height / 2,
            stage.width / 2 + 20f,
            intArrayOf(
                Color.TRANSPARENT,
                Color.parseColor("#33" + stage.color.substring(1)) // 半透明版本
            ),
            floatArrayOf(0.8f, 1f),
            Shader.TileMode.CLAMP
        )
        
        paint.shader = gradient
        paint.alpha = 100
        
        canvas.drawRect(
            stage.x - 20f, stage.y - 20f,
            stage.x + stage.width + 20f, stage.y + stage.height + 20f,
            paint
        )
        
        paint.shader = null
        paint.alpha = 255
    }
}
