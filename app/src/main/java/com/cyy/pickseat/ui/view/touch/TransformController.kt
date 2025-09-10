package com.cyy.pickseat.ui.view.touch

import android.animation.ValueAnimator
import android.graphics.Matrix
import android.view.animation.DecelerateInterpolator
import kotlin.math.*

/**
 * 变换控制器
 * 负责管理缩放、平移、动画等变换操作
 */
class TransformController(
    private val viewWidth: () -> Float,
    private val viewHeight: () -> Float,
    private val contentWidth: () -> Float,
    private val contentHeight: () -> Float,
    private val invalidateCallback: () -> Unit,
    private val onScaleChangeCallback: ((Float, Float, Float) -> Unit)? = null
) {
    
    // 变换参数
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    private var maxScale = 5f
    private var minScale = 0.1f
    
    // 变换矩阵
    private val matrix = Matrix()
    
    // 动画
    private var scaleAnimator: ValueAnimator? = null
    private val animationInterpolator = DecelerateInterpolator(2f)
    
    // 惯性滚动
    private val flingController = FlingController { dx, dy ->
        translateX += dx
        translateY += dy
        constrainTranslation()
        updateMatrix()
        invalidateCallback()
    }
    
    /**
     * 更新惯性滚动（在computeScroll中调用）
     */
    fun updateFling(): Boolean {
        return flingController.updateFling()
    }
    
    /**
     * 设置缩放范围
     */
    fun setScaleRange(minScale: Float, maxScale: Float) {
        this.minScale = minScale
        this.maxScale = maxScale
    }
    
    /**
     * 获取当前变换矩阵
     */
    fun getMatrix(): Matrix = matrix
    
    /**
     * 获取当前缩放因子
     */
    fun getCurrentScale(): Float = scaleFactor
    
    /**
     * 获取最小缩放因子
     */
    fun getMinScale(): Float = minScale
    
    /**
     * 获取最大缩放因子
     */
    fun getMaxScale(): Float = maxScale
    
    /**
     * 应用缩放
     */
    fun applyScale(scale: Float, focusX: Float, focusY: Float) {
        val newScale = (scaleFactor * scale).coerceIn(minScale, maxScale)
        
        if (abs(newScale - scaleFactor) > 0.001f) {
            val scaleChange = newScale / scaleFactor
            translateX = focusX - (focusX - translateX) * scaleChange
            translateY = focusY - (focusY - translateY) * scaleChange
            
            scaleFactor = newScale
            constrainTranslation()
            updateMatrix()
            
            onScaleChangeCallback?.invoke(scaleFactor, minScale, maxScale)
            invalidateCallback()
        }
    }
    
    /**
     * 应用平移
     */
    fun applyTranslation(dx: Float, dy: Float) {
        translateX += dx
        translateY += dy
        constrainTranslation()
        updateMatrix()
        invalidateCallback()
    }
    
    /**
     * 开始惯性滚动
     */
    fun startFling(velocityX: Float, velocityY: Float) {
        flingController.startFling(velocityX, velocityY, invalidateCallback)
    }
    
    /**
     * 动画缩放到指定比例和焦点
     */
    fun animateScaleToPoint(targetScale: Float, focusX: Float, focusY: Float) {
        val constrainedScale = targetScale.coerceIn(minScale, maxScale)
        val scaleChange = constrainedScale / scaleFactor
        
        val targetTranslateX = focusX - (focusX - translateX) * scaleChange
        val targetTranslateY = focusY - (focusY - translateY) * scaleChange
        
        animateToTransform(constrainedScale, targetTranslateX, targetTranslateY, 400)
    }
    
    /**
     * 重置到适合屏幕的状态
     */
    fun resetToFitScreen() {
        val vWidth = viewWidth()
        val vHeight = viewHeight()
        val cWidth = contentWidth()
        val cHeight = contentHeight()
        
        if (vWidth > 0 && vHeight > 0 && cWidth > 0 && cHeight > 0) {
            val scaleX = vWidth / cWidth
            val scaleY = vHeight / cHeight
            val targetScale = minOf(scaleX, scaleY).coerceIn(minScale, maxScale)
            
            val targetTranslateX = (vWidth - cWidth * targetScale) / 2
            val targetTranslateY = (vHeight - cHeight * targetScale) / 2
            
            animateToTransform(targetScale, targetTranslateX, targetTranslateY, 500)
        }
    }
    
    /**
     * 约束并回弹（如果需要）
     */
    fun constrainAndBounceIfNeeded() {
        val constrainedScale = scaleFactor.coerceIn(minScale, maxScale)
        if (abs(constrainedScale - scaleFactor) > 0.001f) {
            animateToTransform(constrainedScale, translateX, translateY, 200)
        }
    }
    
    /**
     * 停止所有动画
     */
    fun stopAllAnimations() {
        scaleAnimator?.cancel()
        flingController.stop()
    }
    
    /**
     * 限制平移范围
     */
    private fun constrainTranslation() {
        val vWidth = viewWidth()
        val vHeight = viewHeight()
        val scaledWidth = contentWidth() * scaleFactor
        val scaledHeight = contentHeight() * scaleFactor
        
        // 计算允许的平移范围
        val maxTranslateX = if (scaledWidth > vWidth) 0f else (vWidth - scaledWidth) / 2f
        val minTranslateX = if (scaledWidth > vWidth) vWidth - scaledWidth else (vWidth - scaledWidth) / 2f
        val maxTranslateY = if (scaledHeight > vHeight) 0f else (vHeight - scaledHeight) / 2f
        val minTranslateY = if (scaledHeight > vHeight) vHeight - scaledHeight else (vHeight - scaledHeight) / 2f
        
        translateX = translateX.coerceIn(minTranslateX, maxTranslateX)
        translateY = translateY.coerceIn(minTranslateY, maxTranslateY)
    }
    
    /**
     * 更新变换矩阵
     */
    private fun updateMatrix() {
        matrix.reset()
        matrix.postScale(scaleFactor, scaleFactor)
        matrix.postTranslate(translateX, translateY)
    }
    
    /**
     * 动画到指定变换状态
     */
    private fun animateToTransform(
        targetScale: Float,
        targetTranslateX: Float,
        targetTranslateY: Float,
        duration: Long = 300
    ) {
        scaleAnimator?.cancel()
        
        val startScale = scaleFactor
        val startTranslateX = translateX
        val startTranslateY = translateY
        
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = animationInterpolator
            
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                
                scaleFactor = startScale + (targetScale - startScale) * fraction
                translateX = startTranslateX + (targetTranslateX - startTranslateX) * fraction
                translateY = startTranslateY + (targetTranslateY - startTranslateY) * fraction
                
                constrainTranslation()
                updateMatrix()
                invalidateCallback()
                
                onScaleChangeCallback?.invoke(scaleFactor, minScale, maxScale)
            }
        }
        
        scaleAnimator = animator
        animator.start()
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        stopAllAnimations()
    }
}
