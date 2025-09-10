package com.cyy.pickseat.ui.view.touch

import android.util.Log
import kotlin.math.*

/**
 * 缩放惯性控制器
 * 负责处理缩放手势结束后的惯性缩放效果
 */
class ScaleInertiaController(
    private val onUpdate: (scaleFactor: Float, focusX: Float, focusY: Float) -> Unit
) {
    
    // 缩放惯性参数
    private var scaleVelocity = 0f // 缩放速度（对数空间）
    private var scaleFocusX = 0f
    private var scaleFocusY = 0f
    private var inertiaStartTime = 0L
    private var isScaling = false
    private val scaleFriction = 0.025f // 增加摩擦系数，减少惯性强度
    
    // 缩放历史记录（用于计算速度）
    private val scaleHistory = mutableListOf<ScaleEvent>()
    private val maxHistorySize = 10
    private val velocityTimeWindow = 150L // 150ms内的事件用于计算速度
    private var cumulativeScale = 1f // 累积缩放值
    
    /**
     * 记录缩放事件
     */
    fun recordScaleEvent(scaleFactor: Float, focusX: Float, focusY: Float) {
        val currentTime = System.currentTimeMillis()
        
        // 更新累积缩放
        cumulativeScale *= scaleFactor
        
        // 添加新事件（记录累积缩放值）
        scaleHistory.add(ScaleEvent(currentTime, cumulativeScale, focusX, focusY))
        
        // 清理过期事件
        scaleHistory.removeAll { currentTime - it.timestamp > velocityTimeWindow }
        
        // 限制历史记录大小
        while (scaleHistory.size > maxHistorySize) {
            scaleHistory.removeAt(0)
        }
    }
    
    /**
     * 开始缩放惯性
     */
    fun startScaleInertia(invalidateCallback: () -> Unit) {
        calculateScaleVelocity()
        
        // 添加调试信息
        Log.i("aaron","ScaleInertia: Calculated velocity = $scaleVelocity, history size = ${scaleHistory.size}")
        
        if (abs(scaleVelocity) > 0.1f) { // 提高启动阈值，减少不必要的惯性
            isScaling = true
            inertiaStartTime = System.currentTimeMillis()
            Log.i("aaron","ScaleInertia: Started with velocity $scaleVelocity")
            invalidateCallback()
        } else {
            Log.i("aaron","ScaleInertia: Velocity too low to start inertia")
        }
    }
    
    /**
     * 计算缩放速度
     */
    private fun calculateScaleVelocity() {
        if (scaleHistory.size < 3) { // 需要至少3个点来计算稳定的速度
            scaleVelocity = 0f
            return
        }
        
        // 使用最近的几个点计算平均速度
        val recentEvents = scaleHistory.takeLast(5)
        var totalVelocity = 0f
        var validSamples = 0
        
        for (i in 1 until recentEvents.size) {
            val current = recentEvents[i]
            val previous = recentEvents[i - 1]
            val timeSpan = (current.timestamp - previous.timestamp).toFloat()
            
            if (timeSpan > 0) {
                val scaleChange = ln(current.scaleFactor / previous.scaleFactor)
                val velocity = scaleChange / (timeSpan / 1000f)
                totalVelocity += velocity
                validSamples++
            }
        }
        
        if (validSamples > 0) {
            scaleVelocity = totalVelocity / validSamples
            // 使用最新的焦点
            val latest = scaleHistory.last()
            scaleFocusX = latest.focusX
            scaleFocusY = latest.focusY
            
            // 限制最大速度，并设置最小阈值
            scaleVelocity = scaleVelocity.coerceIn(-5f, 5f)
            
            // 如果速度太小，不启动惯性
            if (abs(scaleVelocity) < 0.05f) {
                scaleVelocity = 0f
            }
        } else {
            scaleVelocity = 0f
        }
    }
    
    /**
     * 更新缩放惯性（在computeScroll中调用）
     */
    fun updateScaleInertia(): Boolean {
        if (!isScaling) return false
        
        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - inertiaStartTime) / 1000f // 转换为秒
        
        // 计算当前速度（考虑摩擦）
        val currentVelocity = scaleVelocity * exp(-scaleFriction * elapsed * 60f) // 60fps
        
        // 如果速度太小，停止缩放惯性
        if (abs(currentVelocity) < 0.02f) { // 提高停止阈值，让惯性更快停止
            isScaling = false
            return false
        }
        
        // 计算缩放因子变化
        val deltaTime = 16f / 1000f // 假设60fps，每帧16ms
        val scaleChange = exp(currentVelocity * deltaTime) // 从对数空间转换回线性空间
        
        onUpdate(scaleChange, scaleFocusX, scaleFocusY)
        return true
    }
    
    /**
     * 停止缩放惯性
     */
    fun stop() {
        isScaling = false
        scaleHistory.clear()
        cumulativeScale = 1f
    }
    
    /**
     * 开始新的缩放手势（重置累积值）
     */
    fun startNewScaleGesture() {
        scaleHistory.clear()
        cumulativeScale = 1f
        isScaling = false
    }
    
    /**
     * 检查是否正在缩放
     */
    fun isScaling(): Boolean = isScaling
    
    /**
     * 清除缩放历史
     */
    fun clearHistory() {
        scaleHistory.clear()
    }
    
    /**
     * 获取当前缩放速度
     */
    fun getCurrentVelocity(): Float {
        if (!isScaling) return 0f
        
        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - inertiaStartTime) / 1000f
        return scaleVelocity * exp(-scaleFriction * elapsed * 60f)
    }
    
    /**
     * 缩放事件数据类
     */
    private data class ScaleEvent(
        val timestamp: Long,
        val scaleFactor: Float,
        val focusX: Float,
        val focusY: Float
    )
}
