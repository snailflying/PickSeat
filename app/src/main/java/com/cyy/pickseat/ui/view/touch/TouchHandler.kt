package com.cyy.pickseat.ui.view.touch

import android.content.Context
import android.graphics.Matrix
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.cyy.pickseat.data.model.Seat

/**
 * 触摸处理器
 * 负责处理所有触摸相关的逻辑
 */
class TouchHandler(
    context: Context,
    private val transformController: TransformController,
    private val seatClickCallback: (Seat, Boolean) -> Unit,
    private val invalidateCallback: () -> Unit
) {
    
    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector
    private val tempPoint = FloatArray(2)
    private val inverseMatrix = Matrix()
    
    // 座位查找回调
    private var seatFinderCallback: ((Float, Float) -> Seat?)? = null
    // 座位渲染器回调（用于更新选中状态）
    private var seatRendererCallback: (() -> com.cyy.pickseat.ui.view.renderer.SeatRenderer?)? = null
    
    init {
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }
    
    /**
     * 处理触摸事件
     */
    fun onTouchEvent(event: MotionEvent, matrix: Matrix): Boolean {
        var handled = scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            handled = gestureDetector.onTouchEvent(event) || handled
        }
        
        // 更新逆矩阵用于坐标转换
        matrix.invert(inverseMatrix)
        
        return handled
    }
    
    /**
     * 设置座位查找回调
     */
    fun setSeatFinderCallback(callback: (Float, Float) -> Seat?) {
        seatFinderCallback = callback
    }
    
    /**
     * 设置座位渲染器回调
     */
    fun setSeatRendererCallback(callback: () -> com.cyy.pickseat.ui.view.renderer.SeatRenderer?) {
        seatRendererCallback = callback
    }
    
    /**
     * 将屏幕坐标转换为画布坐标
     */
    private fun screenToCanvas(screenX: Float, screenY: Float): Pair<Float, Float> {
        tempPoint[0] = screenX
        tempPoint[1] = screenY
        inverseMatrix.mapPoints(tempPoint)
        return Pair(tempPoint[0], tempPoint[1])
    }
    
    /**
     * 缩放手势监听器
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var scaleFocusX = 0f
        private var scaleFocusY = 0f
        private var lastScaleTime = 0L
        
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            transformController.stopAllAnimations()
            transformController.startNewScaleGesture() // 重置缩放历史
            scaleFocusX = detector.focusX
            scaleFocusY = detector.focusY
            lastScaleTime = System.currentTimeMillis()
            return true
        }
        
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            Log.i("aaron","ScaleInertia: onScale")
            val currentTime = System.currentTimeMillis()
            val scaleFactor = detector.scaleFactor
            
            // 记录缩放事件用于惯性计算
            transformController.recordScaleEvent(scaleFactor, scaleFocusX, scaleFocusY)
            
            // 应用缩放
            transformController.applyScale(scaleFactor, scaleFocusX, scaleFocusY)
            
            lastScaleTime = currentTime
            return true
        }
        
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            
            // 检查是否需要启动缩放惯性
            val timeSinceLastScale = System.currentTimeMillis() - lastScaleTime
            Log.i("aaron","ScaleInertia: velocity onScaleEnd timeSinceLastScale:$timeSinceLastScale")
            if (timeSinceLastScale < 200) { // 放宽时间窗口到200ms
                transformController.startScaleInertia()
            }
            
            // 约束和回弹
            transformController.constrainAndBounceIfNeeded()
        }
    }
    
    /**
     * 手势监听器
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        
        override fun onDown(e: MotionEvent): Boolean {
            transformController.stopAllAnimations()
            return true
        }
        
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val (canvasX, canvasY) = screenToCanvas(e.x, e.y)
            
            // 查找点击的座位
            val clickedSeat = seatFinderCallback?.invoke(canvasX, canvasY)
            if (clickedSeat != null && clickedSeat.status == com.cyy.pickseat.data.model.SeatStatus.AVAILABLE) {
                val seatRenderer = seatRendererCallback?.invoke()
                if (seatRenderer != null) {
                    // 切换选中状态
                    val isSelected = !seatRenderer.isSeatSelected(clickedSeat.id)
                    
                    if (isSelected) {
                        seatRenderer.addSelectedSeat(clickedSeat.id)
                    } else {
                        seatRenderer.removeSelectedSeat(clickedSeat.id)
                    }
                    
                    clickedSeat.isSelected = isSelected
                    seatClickCallback(clickedSeat, isSelected)
                    
                    // 触发重绘以立即更新座位颜色
                    Log.i("aaron", "Seat selected: ${clickedSeat.id}, isSelected: $isSelected, triggering invalidate")
                    invalidateCallback()
                    return true
                }
            }
            
            return false
        }
        
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (scaleGestureDetector.isInProgress) {
                return false
            }
            
            transformController.applyTranslation(-distanceX, -distanceY)
            return true
        }
        
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (scaleGestureDetector.isInProgress) {
                return false
            }
            
            transformController.startFling(velocityX, velocityY)
            return true
        }
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            transformController.stopAllAnimations()
            
            val currentScale = transformController.getCurrentScale()
            val targetScale = if (currentScale < 2f) 3f else transformController.getMinScale()
            
            transformController.animateScaleToPoint(targetScale, e.x, e.y)
            return true
        }
    }
}

