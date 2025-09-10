package com.cyy.pickseat.utils

import android.graphics.RectF
import kotlin.math.*

/**
 * 性能优化工具类
 */
object PerformanceUtils {
    
    /**
     * 计算两个矩形是否相交
     */
    fun intersects(rect1: RectF, rect2: RectF): Boolean {
        return rect1.left < rect2.right && 
               rect2.left < rect1.right && 
               rect1.top < rect2.bottom && 
               rect2.top < rect1.bottom
    }
    
    /**
     * 计算两点之间的距离
     */
    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * 检查点是否在矩形内
     */
    fun contains(rectX: Float, rectY: Float, rectWidth: Float, rectHeight: Float, pointX: Float, pointY: Float): Boolean {
        return pointX >= rectX && 
               pointX <= rectX + rectWidth && 
               pointY >= rectY && 
               pointY <= rectY + rectHeight
    }
    
    /**
     * 检查点是否在圆形内
     */
    fun containsCircle(centerX: Float, centerY: Float, radius: Float, pointX: Float, pointY: Float): Boolean {
        val distance = distance(centerX, centerY, pointX, pointY)
        return distance <= radius
    }
    
    /**
     * 限制值在指定范围内
     */
    fun clamp(value: Float, min: Float, max: Float): Float {
        return maxOf(min, minOf(max, value))
    }
    
    /**
     * 线性插值
     */
    fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + fraction * (end - start)
    }
    
    /**
     * 获取内存使用情况（MB）
     */
    fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    }
    
    /**
     * 获取可用内存（MB）
     */
    fun getAvailableMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() / 1024 / 1024
    }
    
    /**
     * 计算适合的缩放比例
     */
    fun calculateFitScale(contentWidth: Float, contentHeight: Float, viewWidth: Float, viewHeight: Float): Float {
        if (contentWidth <= 0 || contentHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return 1f
        }
        
        val scaleX = viewWidth / contentWidth
        val scaleY = viewHeight / contentHeight
        return minOf(scaleX, scaleY)
    }
    
    /**
     * 计算居中位置
     */
    fun calculateCenterPosition(contentSize: Float, viewSize: Float, scale: Float): Float {
        return (viewSize - contentSize * scale) / 2f
    }
    
    /**
     * 对象池 - 用于复用对象减少GC
     */
    class ObjectPool<T>(
        private val factory: () -> T,
        private val reset: (T) -> Unit = {},
        private val maxSize: Int = 10
    ) {
        private val pool = mutableListOf<T>()
        
        fun acquire(): T {
            return if (pool.isNotEmpty()) {
                pool.removeLastOrNull() ?: factory()
            } else {
                factory()
            }
        }
        
        fun release(obj: T) {
            if (pool.size < maxSize) {
                reset(obj)
                pool.add(obj)
            }
        }
        
        fun clear() {
            pool.clear()
        }
    }
    
    /**
     * 简单的FPS计算器
     */
    class FpsCounter {
        private var frameCount = 0
        private var lastTime = System.currentTimeMillis()
        private var fps = 0
        
        fun onFrame() {
            frameCount++
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTime >= 1000) {
                fps = frameCount
                frameCount = 0
                lastTime = currentTime
            }
        }
        
        fun getFps(): Int = fps
    }
    
    /**
     * 四叉树 - 用于空间索引优化
     */
    class QuadTree(
        private val bounds: RectF,
        private val maxObjects: Int = 10,
        private val maxLevels: Int = 5,
        private val level: Int = 0
    ) {
        private val objects = mutableListOf<QuadObject>()
        private var nodes: Array<QuadTree>? = null
        
        fun clear() {
            objects.clear()
            nodes?.forEach { it.clear() }
            nodes = null
        }
        
        fun split() {
            val subWidth = bounds.width() / 2
            val subHeight = bounds.height() / 2
            val x = bounds.left
            val y = bounds.top
            
            nodes = arrayOf(
                QuadTree(RectF(x + subWidth, y, x + bounds.width(), y + subHeight), maxObjects, maxLevels, level + 1),
                QuadTree(RectF(x, y, x + subWidth, y + subHeight), maxObjects, maxLevels, level + 1),
                QuadTree(RectF(x, y + subHeight, x + subWidth, y + bounds.height()), maxObjects, maxLevels, level + 1),
                QuadTree(RectF(x + subWidth, y + subHeight, x + bounds.width(), y + bounds.height()), maxObjects, maxLevels, level + 1)
            )
        }
        
        fun getIndex(rect: RectF): Int {
            val nodes = this.nodes ?: return -1
            
            val verticalMidpoint = bounds.left + bounds.width() / 2
            val horizontalMidpoint = bounds.top + bounds.height() / 2
            
            val topQuadrant = rect.top < horizontalMidpoint && rect.bottom < horizontalMidpoint
            val bottomQuadrant = rect.top > horizontalMidpoint
            
            return when {
                rect.left < verticalMidpoint && rect.right < verticalMidpoint -> {
                    if (topQuadrant) 1 else if (bottomQuadrant) 2 else -1
                }
                rect.left > verticalMidpoint -> {
                    if (topQuadrant) 0 else if (bottomQuadrant) 3 else -1
                }
                else -> -1
            }
        }
        
        fun insert(obj: QuadObject) {
            val nodes = this.nodes
            if (nodes != null) {
                val index = getIndex(obj.bounds)
                if (index != -1) {
                    nodes[index].insert(obj)
                    return
                }
            }
            
            objects.add(obj)
            
            if (objects.size > maxObjects && level < maxLevels) {
                if (nodes == null) {
                    split()
                }
                
                val iterator = objects.iterator()
                while (iterator.hasNext()) {
                    val obj = iterator.next()
                    val index = getIndex(obj.bounds)
                    if (index != -1) {
                        nodes!![index].insert(obj)
                        iterator.remove()
                    }
                }
            }
        }
        
        fun retrieve(returnObjects: MutableList<QuadObject>, rect: RectF) {
            val index = getIndex(rect)
            if (index != -1 && nodes != null) {
                nodes!![index].retrieve(returnObjects, rect)
            }
            
            returnObjects.addAll(objects)
        }
    }
    
    /**
     * 四叉树对象接口
     */
    interface QuadObject {
        val bounds: RectF
    }
}
