package com.cyy.pickseat.ui.view.touch

import android.content.Context
import android.graphics.Matrix
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
    private val seatClickCallback: (Seat, Boolean) -> Unit
) {
    
    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector
    private val tempPoint = FloatArray(2)
    private val inverseMatrix = Matrix()
    
    // 座位查找回调
    private var seatFinderCallback: ((Float, Float) -> Seat?)? = null
    
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
        
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            transformController.stopAllAnimations()
            scaleFocusX = detector.focusX
            scaleFocusY = detector.focusY
            return true
        }
        
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            transformController.applyScale(scaleFactor, scaleFocusX, scaleFocusY)
            return true
        }
        
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
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
                // 切换选中状态
                val isSelected = !clickedSeat.isSelected
                clickedSeat.isSelected = isSelected
                seatClickCallback(clickedSeat, isSelected)
                return true
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
