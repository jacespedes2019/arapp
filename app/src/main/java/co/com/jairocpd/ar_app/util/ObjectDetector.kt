package co.com.jairocpd.ar_app.util

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import org.tensorflow.lite.support.image.TensorImage

class ObjectDetector(
    private val image: Bitmap,
    private val idAnalyzer: (DetectedObject) -> Unit
) {

    companion object {
        var iscRunning = false
    }

    private val localModel by lazy {
        LocalModel.Builder()
            .setAssetFilePath("ssd_mobilenet_v1_1_metadata_1.tflite")
            .build()
    }

    private val options by lazy {
        CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f)
            .setMaxPerObjectLabelCount(3)
            .build()
    }

    private val objectDetector by lazy { ObjectDetection.getClient(options) }

    fun useCustomObjectDetector() {
        if (!iscRunning) {
            iscRunning = true
            val tensorImage = TensorImage.fromBitmap(image)

            objectDetector.process(InputImage.fromBitmap(image, 0))
                .addOnSuccessListener { results ->
                    for (result in results) {
                        val boundingBox = result.boundingBox
                        val labels = result.labels
                        if (labels.isNotEmpty()) {
                            val detectedObject = DetectedObject(
                                boundingBox = RectF(
                                    boundingBox.left.toFloat(),
                                    boundingBox.top.toFloat(),
                                    boundingBox.right.toFloat(),
                                    boundingBox.bottom.toFloat()
                                ),
                                labels = labels.map { it.text },
                                confidence = labels.first().confidence
                            )
                            idAnalyzer(detectedObject)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ObjectDetector", "Error al procesar la detección", e)
                }
                .addOnCompleteListener {
                    iscRunning = false
                }
        }
    }
}

data class DetectedObject(
    val boundingBox: RectF,
    val labels: List<String>,
    val confidence: Float
)
