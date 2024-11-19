package co.com.jairocpd.ar_app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import co.com.jairocpd.ar_app.domain.model.DetectedObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ObjectDetectionModel(private val context: Context) {

    private lateinit var tflite: Interpreter
    private lateinit var labels: List<String>
    private var imageProcessor: ImageProcessor

    init {
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        loadModelAndLabels()
    }

    private fun loadModelAndLabels() {
        val modelPath = "ssd_mobilenet_v1_1_metadata_1.tflite"
        tflite = Interpreter(loadModelFile(modelPath))
        labels = context.assets.open("labels.txt").bufferedReader().readLines()
    }

    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
    }

    fun detectObjects(bitmap: Bitmap): List<DetectedObject> {
        // Preprocesar la imagen
        val resizedBitmap = resizeBitmap(bitmap, 300, 300)
        val tensorImage = TensorImage.fromBitmap(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)

        val inputBuffer = processedImage.buffer
        val outputLocations = Array(1) { Array(10) { FloatArray(4) } }
        val outputClasses = Array(1) { FloatArray(10) }
        val outputScores = Array(1) { FloatArray(10) }
        val outputCount = FloatArray(1)

        val outputs = mapOf(
            0 to outputLocations,
            1 to outputClasses,
            2 to outputScores,
            3 to outputCount
        )

        tflite.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)

        val detections = mutableListOf<DetectedObject>()
        val count = outputCount[0].toInt()

        for (i in 0 until count) {
            val confidence = outputScores[0][i]
            if (confidence > 0.5) {
                val labelIndex = outputClasses[0][i].toInt()
                val label = labels.getOrNull(labelIndex) ?: continue
                val boundingBox = RectF(
                    outputLocations[0][i][1] * resizedBitmap.width,
                    outputLocations[0][i][0] * resizedBitmap.height,
                    outputLocations[0][i][3] * resizedBitmap.width,
                    outputLocations[0][i][2] * resizedBitmap.height
                )
                detections.add(DetectedObject(boundingBox, label, confidence))
            }
        }
        return detections
    }

    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    fun close() {
        tflite.close()
    }
}
