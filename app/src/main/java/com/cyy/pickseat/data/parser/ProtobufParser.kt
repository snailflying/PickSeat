package com.cyy.pickseat.data.parser

import android.content.Context
import com.cyy.pickseat.data.model.*
import com.cyy.pickseat.proto.VenueProto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Protobuf数据解析器
 * 支持解析protobuf格式的SVG和GeoJSON数据
 */
class ProtobufParser(private val context: Context) {
    
    private val svgParser = SvgParser(context)
    private val geoJsonParser = GeoJsonParser()
    
    /**
     * 解析protobuf格式的SVG数据
     */
    suspend fun parseSvgFromProtobuf(inputStream: InputStream): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            val svgData = VenueProto.SvgData.parseFrom(inputStream)
            
            // 如果protobuf中已经包含了VenueLayout，直接使用
            if (svgData.hasVenueLayout()) {
                convertProtobufToVenueLayout(svgData.venueLayout)
            } else {
                // 否则解析SVG内容
                svgParser.parseVenueLayout(svgData.svgContent)
            }
        } catch (e: Exception) {
            android.util.Log.e("ProtobufParser", "Failed to parse SVG protobuf data", e)
            null
        }
    }
    
    /**
     * 解析protobuf格式的GeoJSON数据
     */
    suspend fun parseGeoJsonFromProtobuf(inputStream: InputStream): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            val geoJsonData = VenueProto.GeoJsonData.parseFrom(inputStream)
            
            // 如果protobuf中已经包含了VenueLayout，直接使用
            if (geoJsonData.hasVenueLayout()) {
                convertProtobufToVenueLayout(geoJsonData.venueLayout)
            } else {
                // 否则解析GeoJSON内容
                geoJsonParser.parseVenueLayout(geoJsonData.geoJsonContent)
            }
        } catch (e: Exception) {
            android.util.Log.e("ProtobufParser", "Failed to parse GeoJSON protobuf data", e)
            null
        }
    }
    
    /**
     * 将VenueLayout转换为protobuf格式
     */
    fun convertVenueLayoutToProtobuf(venueLayout: VenueLayout): VenueProto.VenueLayout {
        val builder = VenueProto.VenueLayout.newBuilder()
            .setId(venueLayout.id)
            .setName(venueLayout.name)
            .setWidth(venueLayout.width)
            .setHeight(venueLayout.height)
            .setMaxScale(venueLayout.maxScale)
            .setMinScale(venueLayout.minScale)
            .setCreatedAt(venueLayout.createdAt)
            .addAllAreas(venueLayout.areas.map { convertSeatAreaToProtobuf(it) })
        
        // 添加可选字段
        venueLayout.stage?.let { stage ->
            builder.setStage(convertStageToProtobuf(stage))
        }
        
        venueLayout.backgroundImage?.let { backgroundImage ->
            builder.setBackgroundImage(backgroundImage)
        }
        
        venueLayout.svgData?.let { svgData ->
            builder.setSvgData(svgData)
        }
        
        return builder.build()
    }
    
    /**
     * 将protobuf格式转换为VenueLayout
     */
    private fun convertProtobufToVenueLayout(protoLayout: VenueProto.VenueLayout): VenueLayout {
        return VenueLayout(
            id = protoLayout.id,
            name = protoLayout.name,
            type = VenueType.STADIUM, // 默认类型
            width = protoLayout.width,
            height = protoLayout.height,
            areas = protoLayout.areasList.map { convertProtobufToSeatArea(it) },
            stage = if (protoLayout.hasStage()) convertProtobufToStage(protoLayout.stage) else null,
            backgroundImage = if (protoLayout.backgroundImage.isNotEmpty()) protoLayout.backgroundImage else null,
            svgData = if (protoLayout.svgData.isNotEmpty()) protoLayout.svgData else null,
            createdAt = protoLayout.createdAt,
            maxScale = protoLayout.maxScale,
            minScale = protoLayout.minScale
        )
    }
    
    /**
     * 将SeatArea转换为protobuf格式
     */
    private fun convertSeatAreaToProtobuf(seatArea: SeatArea): VenueProto.SeatArea {
        return VenueProto.SeatArea.newBuilder()
            .setId(seatArea.id)
            .setName(seatArea.name)
            .setX(seatArea.x)
            .setY(seatArea.y)
            .setWidth(seatArea.width)
            .setHeight(seatArea.height)
            .setColor(seatArea.color)
            .setDescription(seatArea.description)
            .addAllSeats(seatArea.seats.map { convertSeatToProtobuf(it) })
            .build()
    }
    
    /**
     * 将protobuf格式转换为SeatArea
     */
    private fun convertProtobufToSeatArea(protoArea: VenueProto.SeatArea): SeatArea {
        return SeatArea(
            id = protoArea.id,
            name = protoArea.name,
            type = AreaType.NORMAL, // 默认类型
            color = protoArea.color,
            x = protoArea.x,
            y = protoArea.y,
            width = protoArea.width,
            height = protoArea.height,
            seats = protoArea.seatsList.map { convertProtobufToSeat(it) },
            description = protoArea.description
        )
    }
    
    /**
     * 将Seat转换为protobuf格式
     */
    private fun convertSeatToProtobuf(seat: Seat): VenueProto.Seat {
        return VenueProto.Seat.newBuilder()
            .setId(seat.id)
            .setRow(seat.row)
            .setColumn(seat.column)
            .setName(seat.name)
            .setX(seat.x)
            .setY(seat.y)
            .setWidth(seat.width)
            .setHeight(seat.height)
            .setStatus(convertSeatStatusToProtobuf(seat.status))
            .setPrice(seat.price)
            .setAreaId(seat.areaId)
            .setIsSelected(seat.isSelected)
            .build()
    }
    
    /**
     * 将protobuf格式转换为Seat
     */
    private fun convertProtobufToSeat(protoSeat: VenueProto.Seat): Seat {
        return Seat(
            id = protoSeat.id,
            row = protoSeat.row,
            column = protoSeat.column,
            name = protoSeat.name,
            x = protoSeat.x,
            y = protoSeat.y,
            width = protoSeat.width,
            height = protoSeat.height,
            status = convertProtobufToSeatStatus(protoSeat.status),
            price = protoSeat.price,
            areaId = protoSeat.areaId,
            isSelected = protoSeat.isSelected
        )
    }
    
    /**
     * 将Stage转换为protobuf格式
     */
    private fun convertStageToProtobuf(stage: Stage): VenueProto.Stage {
        return VenueProto.Stage.newBuilder()
            .setName(stage.name)
            .setX(stage.x)
            .setY(stage.y)
            .setWidth(stage.width)
            .setHeight(stage.height)
            .setColor(stage.color)
            .build()
    }
    
    /**
     * 将protobuf格式转换为Stage
     */
    private fun convertProtobufToStage(protoStage: VenueProto.Stage): Stage {
        return Stage(
            name = protoStage.name,
            x = protoStage.x,
            y = protoStage.y,
            width = protoStage.width,
            height = protoStage.height,
            color = protoStage.color
        )
    }
    
    /**
     * 将SeatStatus转换为protobuf格式
     */
    private fun convertSeatStatusToProtobuf(status: SeatStatus): VenueProto.SeatStatus {
        return when (status) {
            SeatStatus.AVAILABLE -> VenueProto.SeatStatus.AVAILABLE
            SeatStatus.SOLD -> VenueProto.SeatStatus.OCCUPIED
            SeatStatus.UNAVAILABLE -> VenueProto.SeatStatus.RESERVED
            SeatStatus.MAINTENANCE -> VenueProto.SeatStatus.MAINTENANCE
        }
    }
    
    /**
     * 将protobuf格式转换为SeatStatus
     */
    private fun convertProtobufToSeatStatus(protoStatus: VenueProto.SeatStatus): SeatStatus {
        return when (protoStatus) {
            VenueProto.SeatStatus.AVAILABLE -> SeatStatus.AVAILABLE
            VenueProto.SeatStatus.OCCUPIED -> SeatStatus.SOLD
            VenueProto.SeatStatus.RESERVED -> SeatStatus.UNAVAILABLE
            VenueProto.SeatStatus.MAINTENANCE -> SeatStatus.MAINTENANCE
            else -> SeatStatus.AVAILABLE
        }
    }
    
    /**
     * 创建包含SVG内容的protobuf数据
     */
    fun createSvgProtobuf(svgContent: String, venueLayout: VenueLayout? = null): VenueProto.SvgData {
        val builder = VenueProto.SvgData.newBuilder()
            .setSvgContent(svgContent)
            .setMetadata("Generated by PickSeat app")
            .setTimestamp(System.currentTimeMillis())
        
        venueLayout?.let { layout ->
            builder.setVenueLayout(convertVenueLayoutToProtobuf(layout))
        }
        
        return builder.build()
    }
    
    /**
     * 创建包含GeoJSON内容的protobuf数据
     */
    fun createGeoJsonProtobuf(geoJsonContent: String, venueLayout: VenueLayout? = null): VenueProto.GeoJsonData {
        val builder = VenueProto.GeoJsonData.newBuilder()
            .setGeoJsonContent(geoJsonContent)
            .setMetadata("Generated by PickSeat app")
            .setTimestamp(System.currentTimeMillis())
        
        venueLayout?.let { layout ->
            builder.setVenueLayout(convertVenueLayoutToProtobuf(layout))
        }
        
        return builder.build()
    }
    
    /**
     * 将protobuf数据保存到字节数组
     */
    fun saveProtobufToBytes(svgData: VenueProto.SvgData): ByteArray {
        return svgData.toByteArray()
    }
    
    /**
     * 将protobuf数据保存到字节数组
     */
    fun saveProtobufToBytes(geoJsonData: VenueProto.GeoJsonData): ByteArray {
        return geoJsonData.toByteArray()
    }
    
    /**
     * 从字节数组解析SVG protobuf数据
     */
    suspend fun parseSvgFromBytes(bytes: ByteArray): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            val svgData = VenueProto.SvgData.parseFrom(bytes)
            if (svgData.hasVenueLayout()) {
                convertProtobufToVenueLayout(svgData.venueLayout)
            } else {
                svgParser.parseVenueLayout(svgData.svgContent)
            }
        } catch (e: Exception) {
            android.util.Log.e("ProtobufParser", "Failed to parse SVG from bytes", e)
            null
        }
    }
    
    /**
     * 从字节数组解析GeoJSON protobuf数据
     */
    suspend fun parseGeoJsonFromBytes(bytes: ByteArray): VenueLayout? = withContext(Dispatchers.IO) {
        try {
            val geoJsonData = VenueProto.GeoJsonData.parseFrom(bytes)
            if (geoJsonData.hasVenueLayout()) {
                convertProtobufToVenueLayout(geoJsonData.venueLayout)
            } else {
                geoJsonParser.parseVenueLayout(geoJsonData.geoJsonContent)
            }
        } catch (e: Exception) {
            android.util.Log.e("ProtobufParser", "Failed to parse GeoJSON from bytes", e)
            null
        }
    }
}
