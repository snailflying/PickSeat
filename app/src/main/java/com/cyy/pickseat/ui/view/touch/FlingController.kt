package com.cyy.pickseat.ui.view.touch

import kotlin.math.*

/**
 * 惯性滚动控制器
 * 负责处理惯性滚动的物理计算
 */
class FlingController(
    private val onUpdate: (dx: Float, dy: Float) -> Unit
) {
    
    // 惯性滚动参数
    private var flingVelocityX = 0f
    private var flingVelocityY = 0f
    private var flingStartTime = 0L
    private var isFlinging = false
    private val friction = 0.015f // 摩擦系数
    
    /**
     * 开始惯性滚动
     */
    fun startFling(velocityX: Float, velocityY: Float, invalidateCallback: () -> Unit) {
        flingVelocityX = velocityX
        flingVelocityY = velocityY
        flingStartTime = System.currentTimeMillis()
        isFlinging = true
        
        // 启动滚动循环
        startFlingLoop(invalidateCallback)
    }
    
    /**
     * 停止惯性滚动
     */
    fun stop() {
        isFlinging = false
    }
    
    /**
     * 检查是否正在滚动
     */
    fun isFlinging(): Boolean = isFlinging
    
    /**
     * 更新惯性滚动（在computeScroll中调用）
     */
    fun updateFling(): Boolean {
        if (!isFlinging) return false
        
        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - flingStartTime) / 1000f // 转换为秒
        
        // 计算当前速度（考虑摩擦）
        val currentVelocityX = flingVelocityX * exp(-friction * elapsed * 60f) // 60fps
        val currentVelocityY = flingVelocityY * exp(-friction * elapsed * 60f)
        
        // 如果速度太小，停止惯性滚动
        if (abs(currentVelocityX) < 50f && abs(currentVelocityY) < 50f) {
            isFlinging = false
            return false
        }
        
        // 更新位置
        val deltaTime = 16f / 1000f // 假设60fps，每帧16ms
        val dx = currentVelocityX * deltaTime
        val dy = currentVelocityY * deltaTime
        
        onUpdate(dx, dy)
        return true
    }
    
    /**
     * 启动滚动循环（简化版本）
     */
    private fun startFlingLoop(invalidateCallback: () -> Unit) {
        // 简化实现，依赖于computeScroll的调用
        invalidateCallback()
    }
    
    /**
     * 计算剩余滚动距离
     */
    fun getRemainingDistance(): Pair<Float, Float> {
        if (!isFlinging) return Pair(0f, 0f)
        
        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - flingStartTime) / 1000f
        
        // 计算剩余距离（积分计算）
        val remainingX = flingVelocityX * exp(-friction * elapsed * 60f) / (friction * 60f)
        val remainingY = flingVelocityY * exp(-friction * elapsed * 60f) / (friction * 60f)
        
        return Pair(remainingX, remainingY)
    }
    
    /**
     * 获取当前速度
     */
    fun getCurrentVelocity(): Pair<Float, Float> {
        if (!isFlinging) return Pair(0f, 0f)
        
        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - flingStartTime) / 1000f
        
        val currentVelocityX = flingVelocityX * exp(-friction * elapsed * 60f)
        val currentVelocityY = flingVelocityY * exp(-friction * elapsed * 60f)
        
        return Pair(currentVelocityX, currentVelocityY)
    }
}
