package com.cyy.pickseat.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cyy.pickseat.R
import com.cyy.pickseat.data.model.Seat
import com.cyy.pickseat.data.model.VenueLayout
import com.cyy.pickseat.databinding.ActivityMainBinding
import com.cyy.pickseat.ui.view.SeatMapViewRefactored
import com.cyy.pickseat.utils.MockDataGenerator
import com.cyy.pickseat.data.parser.ProtobufParser
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 主界面Activity
 * 展示选座功能和Demo
 */
class MainActivity : AppCompatActivity(), 
    SeatMapViewRefactored.OnSeatClickListener, 
    SeatMapViewRefactored.OnScaleChangeListener {

    private lateinit var binding: ActivityMainBinding
    private var currentVenueLayout: VenueLayout? = null
    private val selectedSeats = mutableListOf<Seat>()
    private lateinit var protobufParser: ProtobufParser
    
    // 性能监控
    private var lastFrameTime = System.currentTimeMillis()
    private var frameCount = 0
    private var fps = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 初始化protobuf解析器
        protobufParser = ProtobufParser(this)
        
        setupViews()
        setupEventListeners()
        loadDefaultVenue()
        startPerformanceMonitoring()
    }
    
    /**
     * 初始化视图
     */
    private fun setupViews() {
        // 设置座位图监听器
        binding.seatMapView.setOnSeatClickListener(this)
        binding.seatMapView.setOnScaleChangeListener(this)
        
        // 初始化UI状态
        updateSelectionInfo()
        updateResetButtonVisibility(false)
    }
    
    /**
     * 设置事件监听器
     */
    private fun setupEventListeners() {
        // 场馆选择按钮
        binding.btnSelectVenue.setOnClickListener {
            showVenueSelectionDialog()
        }
        
        // 刷新按钮
        binding.btnRefresh.setOnClickListener {
            refreshSeatMap()
            // 演示protobuf功能
            demonstrateProtobufUsage()
        }
        
        // 回到全局视图按钮
        binding.btnResetView.setOnClickListener {
            binding.seatMapView.resetTransform()
        }
        
        // 清空选择按钮
        binding.btnClearSelection.setOnClickListener {
            clearSelection()
        }
        
        // 确认选座按钮
        binding.btnConfirmSelection.setOnClickListener {
            confirmSelection()
        }
        
        // 长按显示性能面板
        binding.seatMapView.setOnLongClickListener {
            togglePerformancePanel()
            true
        }
    }
    
    /**
     * 加载默认场馆
     */
    private fun loadDefaultVenue() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                // 生成大型体育场数据
                val stadium = MockDataGenerator.generateLargeStadium()
                loadVenueLayout(stadium)
            } catch (e: Exception) {
                e.printStackTrace()
                showError("加载场馆数据失败: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }
    
    /**
     * 显示场馆选择对话框
     */
    private fun showVenueSelectionDialog() {
        val venueTypes = arrayOf("大型体育场 (10W+座位)", "国家大剧院 (5W+座位)", "音乐厅 (2W+座位)")
        
        AlertDialog.Builder(this)
            .setTitle("选择场馆类型")
            .setItems(venueTypes) { _, which ->
                loadVenueByType(which)
            }
            .show()
    }
    
    /**
     * 根据类型加载场馆
     */
    private fun loadVenueByType(type: Int) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val venue = when (type) {
                    0 -> MockDataGenerator.generateLargeStadium()
                    1 -> MockDataGenerator.generateLargeTheater()
                    2 -> MockDataGenerator.generateConcertHall()
                    else -> MockDataGenerator.generateLargeStadium()
                }
                loadVenueLayout(venue)
            } catch (e: Exception) {
                e.printStackTrace()
                showError("加载场馆失败: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }
    
    /**
     * 加载场馆布局
     */
    private fun loadVenueLayout(layout: VenueLayout) {
        currentVenueLayout = layout
        binding.seatMapView.setVenueLayout(layout)
        binding.tvTitle.text = layout.name
        
        // 清空之前的选择
        selectedSeats.clear()
        updateSelectionInfo()
        
        // 显示场馆信息
        Toast.makeText(
            this,
            "已加载 ${layout.name}，共 ${layout.getTotalSeatCount()} 个座位",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    /**
     * 刷新座位图
     */
    private fun refreshSeatMap() {
        binding.seatMapView.refresh()
        selectedSeats.clear()
        updateSelectionInfo()
        Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 清空选择
     */
    private fun clearSelection() {
        binding.seatMapView.clearSelection()
        selectedSeats.clear()
        updateSelectionInfo()
    }
    
    /**
     * 确认选座
     */
    private fun confirmSelection() {
        if (selectedSeats.isEmpty()) {
            Toast.makeText(this, "请先选择座位", Toast.LENGTH_SHORT).show()
            return
        }
        
        val seatNames = selectedSeats.joinToString(", ") { it.name }
        val totalPrice = selectedSeats.sumOf { it.price }
        
        AlertDialog.Builder(this)
            .setTitle("确认选座")
            .setMessage("选择座位：$seatNames\n总价：¥${totalPrice.roundToInt()}")
            .setPositiveButton("确认") { _, _ ->
                Toast.makeText(this, "选座成功！", Toast.LENGTH_LONG).show()
                clearSelection()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示/隐藏加载状态
     */
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    /**
     * 更新选座信息
     */
    private fun updateSelectionInfo() {
        val count = selectedSeats.size
        val totalPrice = selectedSeats.sumOf { it.price }
        
        binding.tvSelectedInfo.text = "已选择 $count 个座位"
        binding.tvTotalPrice.text = "总价：¥${totalPrice.roundToInt()}"
        
        // 更新按钮状态
        binding.btnClearSelection.isEnabled = count > 0
        binding.btnConfirmSelection.isEnabled = count > 0
    }
    
    /**
     * 更新重置按钮可见性
     */
    private fun updateResetButtonVisibility(show: Boolean) {
        binding.btnResetView.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    /**
     * 切换性能面板显示
     */
    private fun togglePerformancePanel() {
        val isVisible = binding.performancePanel.visibility == View.VISIBLE
        binding.performancePanel.visibility = if (isVisible) View.GONE else View.VISIBLE
    }
    
    /**
     * 开始性能监控
     */
    private fun startPerformanceMonitoring() {
        val runnable = object : Runnable {
            override fun run() {
                updatePerformanceInfo()
                binding.root.postDelayed(this, 1000) // 每秒更新一次
            }
        }
        binding.root.post(runnable)
    }
    
    /**
     * 更新性能信息
     */
    private fun updatePerformanceInfo() {
        // 计算FPS
        val currentTime = System.currentTimeMillis()
        frameCount++
        if (currentTime - lastFrameTime >= 1000) {
            fps = frameCount
            frameCount = 0
            lastFrameTime = currentTime
        }
        
        // 获取内存使用情况
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        
        // 获取座位数量
        val seatCount = currentVenueLayout?.getTotalSeatCount() ?: 0
        
        // 更新UI
        binding.tvFps.text = "FPS: $fps"
        binding.tvMemory.text = "内存: ${usedMemory}MB"
        binding.tvSeatCount.text = "座位: $seatCount"
    }
    
    // SeatMapViewRefactored.OnSeatClickListener 实现
    override fun onSeatClick(seat: Seat, isSelected: Boolean) {
        if (isSelected) {
            selectedSeats.add(seat)
        } else {
            selectedSeats.remove(seat)
        }
        updateSelectionInfo()
    }
    
    // SeatMapViewRefactored.OnScaleChangeListener 实现
    override fun onScaleChanged(currentScale: Float, minScale: Float, maxScale: Float) {
        // 当放大超过1.5倍时显示重置按钮
        updateResetButtonVisibility(currentScale > 1.5f)
    }
    
    /**
     * 演示protobuf功能
     */
    private fun demonstrateProtobufUsage() {
        lifecycleScope.launch {
            try {
                currentVenueLayout?.let { layout ->
                    // 1. 将VenueLayout转换为protobuf格式
                    val protobufLayout = protobufParser.convertVenueLayoutToProtobuf(layout)
                    android.util.Log.i("ProtobufDemo", "Converted layout to protobuf: ${protobufLayout.serializedSize} bytes")
                    
                    // 2. 创建SVG protobuf数据
                    val svgProtobuf = protobufParser.createSvgProtobuf(
                        svgContent = "<svg>...</svg>",
                        venueLayout = layout
                    )
                    
                    // 3. 保存为字节数组
                    val svgBytes = protobufParser.saveProtobufToBytes(svgProtobuf)
                    android.util.Log.i("ProtobufDemo", "SVG protobuf size: ${svgBytes.size} bytes")
                    
                    // 4. 从字节数组解析
                    val parsedLayout = protobufParser.parseSvgFromBytes(svgBytes)
                    android.util.Log.i("ProtobufDemo", "Parsed layout: ${parsedLayout?.name}")
                    
                    // 5. 创建GeoJSON protobuf数据
                    val geoJsonProtobuf = protobufParser.createGeoJsonProtobuf(
                        geoJsonContent = """{"type":"FeatureCollection","features":[]}""",
                        venueLayout = layout
                    )
                    
                    val geoJsonBytes = protobufParser.saveProtobufToBytes(geoJsonProtobuf)
                    android.util.Log.i("ProtobufDemo", "GeoJSON protobuf size: ${geoJsonBytes.size} bytes")
                    
                    Toast.makeText(this@MainActivity, "Protobuf演示完成，请查看日志", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProtobufDemo", "Error demonstrating protobuf", e)
                Toast.makeText(this@MainActivity, "Protobuf演示出错: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
