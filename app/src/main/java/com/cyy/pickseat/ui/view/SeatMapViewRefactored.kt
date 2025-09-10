package com.cyy.pickseat.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.cyy.pickseat.data.model.Seat
import com.cyy.pickseat.data.model.VenueLayout
import com.cyy.pickseat.ui.view.renderer.VenueRenderer
import com.cyy.pickseat.ui.view.touch.TouchHandler
import com.cyy.pickseat.ui.view.touch.TransformController
import kotlinx.coroutines.*

/**
 * 重构后的座位图自定义视图组件
 * 采用分层架构：渲染层、触摸层、控制层分离
 */
class SeatMapViewRefactored @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 渲染层
    private val venueRenderer = VenueRenderer()
    
    // 控制层
    private lateinit var transformController: TransformController
    
    // 触摸层
    private lateinit var touchHandler: TouchHandler
    
    // 可见区域计算
    private val visibleRect = RectF()
    private val inverseMatrix = Matrix()
    
    // 协程作用域
    private val renderScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // 回调接口
    private var onSeatClickListener: OnSeatClickListener? = null
    private var onScaleChangeListener: OnScaleChangeListener? = null
    
    // 性能监控
    private var isRenderingEnabled = true
    
    init {
        setupView()
        setupControllers()
    }
    
    /**
     * 初始化视图
     */
    private fun setupView() {
        isClickable = true
        isFocusable = true
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }
    
    /**
     * 初始化控制器
     */
    private fun setupControllers() {
        // 初始化变换控制器
        transformController = TransformController(
            viewWidth = { width.toFloat() },
            viewHeight = { height.toFloat() },
            contentWidth = { venueRenderer.venueLayout?.width ?: 0f },
            contentHeight = { venueRenderer.venueLayout?.height ?: 0f },
            invalidateCallback = { invalidate() },
            onScaleChangeCallback = { current, min, max ->
                onScaleChangeListener?.onScaleChanged(current, min, max)
            }
        )
        
        // 初始化触摸处理器
        touchHandler = TouchHandler(
            context = context,
            transformController = transformController,
            seatClickCallback = { seat, isSelected ->
                onSeatClickListener?.onSeatClick(seat, isSelected)
            }
        )
        
        // 设置座位查找回调
        touchHandler.setSeatFinderCallback { x, y ->
            venueRenderer.findSeatAt(x, y)
        }
        
        // 设置座位渲染器回调
        touchHandler.setSeatRendererCallback {
            venueRenderer.getSeatRenderer()
        }
    }
    
    /**
     * 设置场馆布局数据
     */
    fun setVenueLayout(layout: VenueLayout) {
        venueRenderer.setVenueLayout(layout)
        transformController.setScaleRange(layout.minScale, layout.maxScale)
        
        // 重置变换
        post { transformController.resetToFitScreen() }
        
        // 触发重绘
        invalidate()
    }
    
    /**
     * 重置变换到初始状态
     */
    fun resetTransform() {
        transformController.resetToFitScreen()
    }
    
    /**
     * 刷新视图
     */
    fun refresh() {
        venueRenderer.getSeatRenderer().clearSelectedSeats()
        invalidate()
    }
    
    /**
     * 获取选中的座位
     */
    fun getSelectedSeats(): List<Seat> {
        return venueRenderer.getSelectedSeats()
    }
    
    /**
     * 清空选中状态
     */
    fun clearSelection() {
        venueRenderer.getSeatRenderer().clearSelectedSeats()
        invalidate()
    }
    
    /**
     * 启用/禁用渲染
     */
    fun setRenderingEnabled(enabled: Boolean) {
        isRenderingEnabled = enabled
        invalidate()
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            post { transformController.resetToFitScreen() }
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isRenderingEnabled) return
        
        // 计算可见区域
        calculateVisibleRect()
        
        // 执行渲染
        val startTime = System.currentTimeMillis()
        venueRenderer.render(
            canvas = canvas,
            matrix = transformController.getMatrix(),
            visibleRect = visibleRect,
            scaleFactor = transformController.getCurrentScale()
        )
        val renderTime = System.currentTimeMillis() - startTime
        
        // 可以在这里添加性能监控逻辑
        if (renderTime > 16) { // 超过16ms（60fps阈值）
            // 记录性能问题
        }
    }
    
    /**
     * 计算可见区域
     */
    private fun calculateVisibleRect() {
        visibleRect.set(0f, 0f, width.toFloat(), height.toFloat())
        if (transformController.getMatrix().invert(inverseMatrix)) {
            inverseMatrix.mapRect(visibleRect)
        }
        
        // 添加调试信息
        Log.i("aaron", "VisibleRect: $visibleRect, Scale: ${transformController.getCurrentScale()}")
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.i("aaron","ScaleInertia: onTouchEvent")
        return touchHandler.onTouchEvent(event, transformController.getMatrix()) || super.onTouchEvent(event)
    }
    
    override fun computeScroll() {
        super.computeScroll()
        // 更新惯性效果
        val hasFling = transformController.updateFling()
        val hasScaleInertia = transformController.updateScaleInertia()
        
        if (hasFling || hasScaleInertia) {
            invalidate()
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
    
    /**
     * 获取渲染统计信息
     */
    fun getRenderStats(): VenueRenderer.RenderStats {
        return venueRenderer.getRenderStats()
    }
    
    /**
     * 设置背景颜色
     */
    fun setVenueBackgroundColor(color: Int) {
        venueRenderer.setBackgroundColor(color)
        invalidate()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        renderScope.cancel()
        transformController.cleanup()
    }
}
