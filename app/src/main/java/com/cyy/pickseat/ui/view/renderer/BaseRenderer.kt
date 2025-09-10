package com.cyy.pickseat.ui.view.renderer

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF

/**
 * 渲染器基类
 */
abstract class BaseRenderer {
    
    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val tempRect = RectF()
    
    /**
     * 渲染内容
     * @param canvas 画布
     * @param matrix 变换矩阵
     * @param visibleRect 可见区域
     * @param scaleFactor 缩放因子
     */
    abstract fun render(
        canvas: Canvas,
        matrix: Matrix,
        visibleRect: RectF,
        scaleFactor: Float
    )
    
    /**
     * 检查是否需要渲染（性能优化）
     */
    protected fun shouldRender(bounds: RectF, visibleRect: RectF): Boolean {
        return RectF.intersects(bounds, visibleRect)
    }
    
    /**
     * 设置画笔属性
     */
    protected fun setupPaint(
        color: Int,
        strokeWidth: Float = 0f,
        style: Paint.Style = Paint.Style.FILL
    ) {
        paint.color = color
        paint.strokeWidth = strokeWidth
        paint.style = style
    }
}

