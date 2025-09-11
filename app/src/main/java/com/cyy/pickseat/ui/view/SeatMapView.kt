package com.cyy.pickseat.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.cyy.pickseat.data.model.*
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * 座位图自定义视图组件
 * 支持10W+座位的高性能渲染
 */
class SeatMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 绘制相关
    private val seatPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // 数据
    private var venueLayout: VenueLayout? = null
    private val selectedSeats = mutableSetOf<String>()
    
    // 变换矩阵
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    private var scaleFactor = 1f
    private var translateX = 0f
    private var translateY = 0f
    
    // 缩放和手势
    private var maxScale = 5f
    private var minScale = 0.1f
    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector
    
    // 惯性滚动
    private val scroller: OverScroller
    private var isScrolling = false
    private var lastScrollTime = 0L
    
    // 自定义惯性滚动参数
    private var flingVelocityX = 0f
    private var flingVelocityY = 0f
    private var flingStartTime = 0L
    private var isCustomFling = false
    private val friction = 0.015f // 摩擦系数
    
    // 动画相关
    private var scaleAnimator: ValueAnimator? = null
    private var translateAnimator: ValueAnimator? = null
    private val animationInterpolator = DecelerateInterpolator(2f)
    
    // 边界限制
    private var contentBounds = RectF()
    private var viewBounds = RectF()
    
    // 性能优化
    private val visibleRect = RectF()
    private val tempRect = RectF()
    private val tempPoint = FloatArray(2)
    
    // 渲染优化
    private var isRenderingEnabled = true
    private var renderJob: Job? = null
    private val renderScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 回调接口
    private var onSeatClickListener: OnSeatClickListener? = null
    private var onScaleChangeListener: OnScaleChangeListener? = null
    
    init {
        // 初始化画笔
        setupPaints()
        
        // 初始化手势检测器
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
        
        // 初始化惯性滚动
        scroller = OverScroller(context, DecelerateInterpolator())
        
        // 设置视图属性
        isClickable = true
        isFocusable = true
        
        // 启用硬件加速
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    
    /**
     * 设置画笔属性
     */
    private fun setupPaints() {
        seatPaint.apply {
            style = Paint.Style.FILL
            strokeWidth = 2f
        }
        
        textPaint.apply {
            color = Color.BLACK
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        
        stagePaint.apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FF6B6B")
        }
        
        backgroundPaint.apply {
            color = Color.parseColor("#F5F5F5")
        }
    }
    
    /**
     * 设置场馆布局数据
     */
    fun setVenueLayout(layout: VenueLayout) {
        venueLayout = layout
        maxScale = layout.maxScale
        minScale = layout.minScale
        
        // 重置变换
        resetTransform()
        
        // 触发重绘
        invalidate()
    }
    
    
    /**
     * 刷新视图
     */
    fun refresh() {
        selectedSeats.clear()
        invalidate()
    }
    
    /**
     * 获取选中的座位
     */
    fun getSelectedSeats(): List<Seat> {
        val layout = venueLayout ?: return emptyList()
        return selectedSeats.mapNotNull { seatId ->
            layout.findSeatById(seatId)
        }
    }
    
    /**
     * 清空选中状态
     */
    fun clearSelection() {
        selectedSeats.clear()
        invalidate()
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewBounds.set(0f, 0f, w.toFloat(), h.toFloat())
        if (venueLayout != null) {
            updateContentBounds()
            resetTransform()
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isRenderingEnabled) return
        
        val layout = venueLayout ?: return
        
        // 绘制背景
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // 应用变换矩阵
        canvas.save()
        canvas.concat(matrix)
        
        // 计算可见区域
        calculateVisibleRect()
        
        // 绘制舞台
        layout.stage?.let { stage ->
            drawStage(canvas, stage)
        }
        
        // 绘制座位区域
        layout.areas.forEach { area ->
            if (isAreaVisible(area)) {
                drawSeatArea(canvas, area)
            }
        }
        
        canvas.restore()
    }
    
    /**
     * 绘制舞台
     */
    private fun drawStage(canvas: Canvas, stage: Stage) {
        stagePaint.color = Color.parseColor(stage.color)
        canvas.drawRect(stage.x, stage.y, stage.x + stage.width, stage.y + stage.height, stagePaint)
        
        // 绘制舞台文字
        if (scaleFactor > 0.5f) {
            textPaint.color = Color.WHITE
            textPaint.textSize = 24f / scaleFactor
            canvas.drawText(
                stage.name,
                stage.x + stage.width / 2,
                stage.y + stage.height / 2 + textPaint.textSize / 3,
                textPaint
            )
        }
    }
    
    /**
     * 绘制座位区域
     */
    private fun drawSeatArea(canvas: Canvas, area: SeatArea) {
        // 根据缩放级别决定渲染细节 - 简化策略，避免座位数量突然变化
        when {
            scaleFactor < 0.2f -> drawAreaAsBlock(canvas, area) // 极低缩放时只显示区域块
            else -> drawAreaWithFullDetail(canvas, area) // 其他情况都显示所有座位
        }
    }
    
    /**
     * 以块状方式绘制区域（最低细节）
     */
    private fun drawAreaAsBlock(canvas: Canvas, area: SeatArea) {
        seatPaint.color = Color.parseColor(area.color)
        seatPaint.alpha = 180
        canvas.drawRect(area.x, area.y, area.x + area.width, area.y + area.height, seatPaint)
        
        // 绘制区域名称
        textPaint.color = Color.BLACK
        textPaint.textSize = 32f / scaleFactor
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
    private fun drawAreaWithReducedDetail(canvas: Canvas, area: SeatArea) {
        // 只绘制部分座位以提高性能
        val step = (2f / scaleFactor).toInt().coerceAtLeast(1)
        
        area.seats.forEachIndexed { index, seat ->
            if (index % step == 0 && isSeatVisible(seat)) {
                drawSeat(canvas, seat, false) // 不显示座位号
            }
        }
    }
    
    /**
     * 以完整细节绘制区域
     */
    private fun drawAreaWithFullDetail(canvas: Canvas, area: SeatArea) {
        area.seats.forEach { seat ->
            if (isSeatVisible(seat)) {
                drawSeat(canvas, seat, scaleFactor > 2f) // 放大时显示座位号
            }
        }
    }
    
    /**
     * 绘制单个座位
     */
    private fun drawSeat(canvas: Canvas, seat: Seat, showText: Boolean) {
        // 设置座位颜色
        seatPaint.color = when {
            seat.isSelected -> Color.parseColor("#4CAF50") // 绿色-已选
            seat.status == SeatStatus.SOLD -> Color.parseColor("#F44336") // 红色-已售
            seat.status == SeatStatus.UNAVAILABLE -> Color.parseColor("#9E9E9E") // 灰色-不可选
            seat.status == SeatStatus.MAINTENANCE -> Color.parseColor("#FF9800") // 橙色-维修
            else -> Color.parseColor("#2196F3") // 蓝色-可选
        }
        
        // 绘制座位
        val radius = minOf(seat.width, seat.height) / 2
        canvas.drawCircle(
            seat.x + seat.width / 2,
            seat.y + seat.height / 2,
            radius,
            seatPaint
        )
        
        // 绘制座位号
        if (showText && scaleFactor > 2f) {
            textPaint.color = Color.WHITE
            textPaint.textSize = 16f / scaleFactor
            canvas.drawText(
                seat.name,
                seat.x + seat.width / 2,
                seat.y + seat.height / 2 + textPaint.textSize / 3,
                textPaint
            )
        }
    }
    
    /**
     * 计算可见区域
     */
    private fun calculateVisibleRect() {
        visibleRect.set(0f, 0f, width.toFloat(), height.toFloat())
        if (matrix.invert(inverseMatrix)) {
            inverseMatrix.mapRect(visibleRect)
        }
    }
    
    /**
     * 检查区域是否可见
     */
    private fun isAreaVisible(area: SeatArea): Boolean {
        tempRect.set(area.x, area.y, area.x + area.width, area.y + area.height)
        return RectF.intersects(visibleRect, tempRect)
    }
    
    /**
     * 检查座位是否可见
     */
    private fun isSeatVisible(seat: Seat): Boolean {
        tempRect.set(seat.x, seat.y, seat.x + seat.width, seat.y + seat.height)
        return RectF.intersects(visibleRect, tempRect)
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
     * 更新内容边界
     */
    private fun updateContentBounds() {
        val layout = venueLayout ?: return
        contentBounds.set(0f, 0f, layout.width, layout.height)
    }
    
    /**
     * 限制平移范围
     */
    private fun constrainTranslation() {
        val layout = venueLayout ?: return
        
        val scaledWidth = layout.width * scaleFactor
        val scaledHeight = layout.height * scaleFactor
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        // 计算允许的平移范围
        val maxTranslateX = if (scaledWidth > viewWidth) 0f else (viewWidth - scaledWidth) / 2f
        val minTranslateX = if (scaledWidth > viewWidth) viewWidth - scaledWidth else (viewWidth - scaledWidth) / 2f
        val maxTranslateY = if (scaledHeight > viewHeight) 0f else (viewHeight - scaledHeight) / 2f
        val minTranslateY = if (scaledHeight > viewHeight) viewHeight - scaledHeight else (viewHeight - scaledHeight) / 2f
        
        translateX = translateX.coerceIn(minTranslateX, maxTranslateX)
        translateY = translateY.coerceIn(minTranslateY, maxTranslateY)
    }
    
    /**
     * 平滑动画到指定的变换状态
     */
    private fun animateToTransform(
        targetScale: Float,
        targetTranslateX: Float,
        targetTranslateY: Float,
        duration: Long = 300
    ) {
        // 停止当前动画
        scaleAnimator?.cancel()
        translateAnimator?.cancel()
        
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
                invalidate()
                
                onScaleChangeListener?.onScaleChanged(scaleFactor, minScale, maxScale)
            }
        }
        
        scaleAnimator = animator
        animator.start()
    }
    
    /**
     * 处理惯性滚动
     */
    override fun computeScroll() {
        super.computeScroll()
        
        if (isCustomFling) {
            val currentTime = System.currentTimeMillis()
            val elapsed = (currentTime - flingStartTime) / 1000f // 转换为秒
            
            // 计算当前速度（考虑摩擦）
            val currentVelocityX = flingVelocityX * exp(-friction * elapsed * 60f) // 60fps
            val currentVelocityY = flingVelocityY * exp(-friction * elapsed * 60f)
            
            // 如果速度太小，停止惯性滚动
            if (abs(currentVelocityX) < 50f && abs(currentVelocityY) < 50f) {
                isCustomFling = false
                isScrolling = false
                return
            }
            
            // 更新位置
            val deltaTime = 16f / 1000f // 假设60fps，每帧16ms
            translateX += currentVelocityX * deltaTime
            translateY += currentVelocityY * deltaTime
            
            constrainTranslation()
            updateMatrix()
            invalidate()
            
            isScrolling = true
        } else if (scroller.computeScrollOffset()) {
            val newX = scroller.currX.toFloat()
            val newY = scroller.currY.toFloat()
            
            translateX = newX
            translateY = newY
            
            constrainTranslation()
            updateMatrix()
            invalidate()
            
            isScrolling = true
        } else if (isScrolling) {
            isScrolling = false
        }
    }
    
    /**
     * 开始惯性滚动
     */
    private fun startFling(velocityX: Float, velocityY: Float) {
        // 停止任何现有的滚动
        scroller.forceFinished(true)
        
        // 使用自定义惯性滚动
        // 直接使用velocity，方向应该与手指滑动方向一致
        flingVelocityX = velocityX
        flingVelocityY = velocityY
        flingStartTime = System.currentTimeMillis()
        isCustomFling = true
        
        invalidate()
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = scaleGestureDetector.onTouchEvent(event)
        if (!scaleGestureDetector.isInProgress) {
            handled = gestureDetector.onTouchEvent(event) || handled
        }
        return handled || super.onTouchEvent(event)
    }
    
    /**
     * 缩放手势监听器
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var scaleFocusX = 0f
        private var scaleFocusY = 0f
        
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            // 停止惯性滚动
            scroller.forceFinished(true)
            // 停止动画
            scaleAnimator?.cancel()
            translateAnimator?.cancel()
            
            scaleFocusX = detector.focusX
            scaleFocusY = detector.focusY
            return true
        }
        
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = detector.scaleFactor
            val newScale = (scaleFactor * scale).coerceIn(minScale, maxScale)
            
            if (abs(newScale - scaleFactor) > 0.001f) {
                // 使用固定的焦点进行缩放，避免跳跃
                val scaleChange = newScale / scaleFactor
                translateX = scaleFocusX - (scaleFocusX - translateX) * scaleChange
                translateY = scaleFocusY - (scaleFocusY - translateY) * scaleChange
                
                scaleFactor = newScale
                constrainTranslation()
                updateMatrix()
                
                onScaleChangeListener?.onScaleChanged(scaleFactor, minScale, maxScale)
                invalidate()
            }
            return true
        }
        
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            
            // 如果缩放超出边界，平滑回弹
            val constrainedScale = scaleFactor.coerceIn(minScale, maxScale)
            if (abs(constrainedScale - scaleFactor) > 0.001f) {
                val targetTranslateX = translateX
                val targetTranslateY = translateY
                animateToTransform(constrainedScale, targetTranslateX, targetTranslateY, 200)
            }
        }
    }
    
    /**
     * 手势监听器
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            // 停止惯性滚动
            scroller.forceFinished(true)
            isCustomFling = false
            return true
        }
        
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            // 将屏幕坐标转换为画布坐标
            tempPoint[0] = e.x
            tempPoint[1] = e.y
            if (matrix.invert(inverseMatrix)) {
                inverseMatrix.mapPoints(tempPoint)
            }
            
            // 查找点击的座位
            val layout = venueLayout ?: return false
            val clickedSeat = layout.findSeatByPosition(tempPoint[0], tempPoint[1])
            
            if (clickedSeat != null && clickedSeat.status == SeatStatus.AVAILABLE) {
                // 切换选中状态
                if (selectedSeats.contains(clickedSeat.id)) {
                    selectedSeats.remove(clickedSeat.id)
                    clickedSeat.isSelected = false
                } else {
                    selectedSeats.add(clickedSeat.id)
                    clickedSeat.isSelected = true
                }
                
                onSeatClickListener?.onSeatClick(clickedSeat, clickedSeat.isSelected)
                invalidate()
                return true
            }
            
            return false
        }
        
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (scaleGestureDetector.isInProgress) {
                return false
            }
            
            translateX -= distanceX
            translateY -= distanceY
            constrainTranslation()
            updateMatrix()
            invalidate()
            
            lastScrollTime = System.currentTimeMillis()
            return true
        }
        
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (scaleGestureDetector.isInProgress) {
                return false
            }
            
            // 启动惯性滚动
            // 重要：GestureDetector的velocity方向：
            // velocityX > 0 表示向右快速滑动
            // velocityY > 0 表示向下快速滑动
            // 我们希望内容继续向同一方向移动
            startFling(velocityX, velocityY)
            return true
        }
        
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // 停止当前动画和滚动
            scroller.forceFinished(true)
            isCustomFling = false
            scaleAnimator?.cancel()
            
            // 双击缩放
            val targetScale = if (scaleFactor < 2f) 3f else minScale
            animateScaleToPoint(targetScale, e.x, e.y)
            return true
        }
    }
    
    /**
     * 动画缩放到指定比例和焦点
     */
    private fun animateScaleToPoint(targetScale: Float, focusX: Float, focusY: Float) {
        val constrainedScale = targetScale.coerceIn(minScale, maxScale)
        val scaleChange = constrainedScale / scaleFactor
        
        val targetTranslateX = focusX - (focusX - translateX) * scaleChange
        val targetTranslateY = focusY - (focusY - translateY) * scaleChange
        
        animateToTransform(constrainedScale, targetTranslateX, targetTranslateY, 400)
    }
    
    /**
     * 重置变换到初始状态（带动画）
     */
    fun resetTransform() {
        val layout = venueLayout ?: return
        
        // 计算适合屏幕的初始缩放比例
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        if (viewWidth > 0 && viewHeight > 0) {
            val scaleX = viewWidth / layout.width
            val scaleY = viewHeight / layout.height
            val targetScale = minOf(scaleX, scaleY).coerceIn(minScale, maxScale)
            
            // 居中显示
            val targetTranslateX = (viewWidth - layout.width * targetScale) / 2
            val targetTranslateY = (viewHeight - layout.height * targetScale) / 2
            
            animateToTransform(targetScale, targetTranslateX, targetTranslateY, 500)
        }
    }
    
    /**
     * 座位点击监听接口
     */
    interface OnSeatClickListener {
        fun onSeatClick(seat: Seat, isSelected: Boolean)
    }
    
    /**
     * 缩放变化监听接口
     */
    interface OnScaleChangeListener {
        fun onScaleChanged(currentScale: Float, minScale: Float, maxScale: Float)
    }
    
    /**
     * 设置座位点击监听器
     */
    fun setOnSeatClickListener(listener: OnSeatClickListener) {
        onSeatClickListener = listener
    }
    
    /**
     * 设置缩放变化监听器
     */
    fun setOnScaleChangeListener(listener: OnScaleChangeListener) {
        onScaleChangeListener = listener
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        renderScope.cancel()
        
        // 清理动画资源
        scaleAnimator?.cancel()
        translateAnimator?.cancel()
        scroller.forceFinished(true)
        isCustomFling = false
    }
}
