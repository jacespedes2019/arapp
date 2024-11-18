package co.com.jairocpd.ar_app.ui.scenes

import android.content.ContentValues.TAG
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.net.toUri
import co.com.jairocpd.ar_app.R
import co.com.jairocpd.ar_app.config.Settings
import co.com.jairocpd.ar_app.databinding.ActivitySceneBinding
import co.com.jairocpd.ar_app.databinding.DialogInputBinding
import co.com.jairocpd.ar_app.domain.model.Coordinator
import co.com.jairocpd.ar_app.domain.model.Cube
import co.com.jairocpd.ar_app.domain.model.MaterialNode
import co.com.jairocpd.ar_app.domain.model.Nodes
import co.com.jairocpd.ar_app.ui.ar.ArActivity
import co.com.jairocpd.ar_app.util.MaterialProperties
import co.com.jairocpd.ar_app.util.SimpleSeekBarChangeListener
import co.com.jairocpd.ar_app.util.behavior
import co.com.jairocpd.ar_app.util.format
import co.com.jairocpd.ar_app.util.formatDistance
import co.com.jairocpd.ar_app.util.formatRotation
import co.com.jairocpd.ar_app.util.formatTranslation
import co.com.jairocpd.ar_app.util.toArColor
import co.com.jairocpd.ar_app.util.toggle
import co.com.jairocpd.ar_app.util.update
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.ar.core.Anchor
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Config
import com.google.ar.core.Config.AugmentedFaceMode
import com.google.ar.core.Config.CloudAnchorMode
import com.google.ar.core.Config.DepthMode
import com.google.ar.core.Config.FocusMode
import com.google.ar.core.Config.LightEstimationMode
import com.google.ar.core.Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
import com.google.ar.core.Config.UpdateMode
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingFailureReason.BAD_STATE
import com.google.ar.core.TrackingFailureReason.CAMERA_UNAVAILABLE
import com.google.ar.core.TrackingFailureReason.EXCESSIVE_MOTION
import com.google.ar.core.TrackingFailureReason.INSUFFICIENT_FEATURES
import com.google.ar.core.TrackingFailureReason.INSUFFICIENT_LIGHT
import com.google.ar.core.TrackingFailureReason.NONE
import com.google.ar.core.TrackingState
import com.google.ar.core.TrackingState.PAUSED
import com.google.ar.core.TrackingState.STOPPED
import com.google.ar.core.TrackingState.TRACKING
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.PlaneRenderer
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import co.com.jairocpd.ar_app.domain.model.DetectedObject
import co.com.jairocpd.ar_app.ml.ObjectDetectionModel
import com.google.ar.sceneform.math.Quaternion


class SceneActivity : ArActivity<ActivitySceneBinding>(ActivitySceneBinding::inflate) {

    private val coordinator by lazy { Coordinator(this, ::onArTap, ::onNodeSelected, ::onNodeFocused) }
    private val model: SceneViewModel by viewModels()
    private val settings by lazy { Settings.instance(this) }

    private val setOfMaterialViews by lazy {
        with(bottomSheetNode.body) {
            setOf(
                colorValue,
                colorLabel,
            )
        }
    }

    //Giroscopio
    private lateinit var sensorManager: SensorManager
    private var gyroscopeSensor: Sensor? = null
    private var gyroscopeListener: SensorEventListener? = null



    override val arSceneView: ArSceneView get() = binding.arSceneView

    override val recordingIndicator: ImageView get() = bottomSheetScene.header.recording

    private val bottomSheetScene get() = binding.bottomSheetScene

    private val bottomSheetNode get() = binding.bottomSheetNode

    // Inicializa el modelo
    private lateinit var tflite: Interpreter
    private lateinit var labels: List<String>
    private lateinit var imageProcessor: ImageProcessor
    private var isCubePlaced = false
    private val nodeList = mutableListOf<Nodes>()
    private lateinit var objectDetectionModel: ObjectDetectionModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initSceneBottomSheet()
        initNodeBottomSheet()
        initAr()
        initWithIntent(intent)
        // Inicializar el SensorManager y el sensor del giroscopio
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        // Carga el modelo desde los assets
        objectDetectionModel = ObjectDetectionModel(this)
    }

    // Función para cargar el archivo del modelo
    private fun loadModelFile(modelPath: String): MappedByteBuffer {
        val assetFileDescriptor = assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun onDestroy() {
        gyroscopeListener?.let { sensorManager.unregisterListener(it) }
        objectDetectionModel.close()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        restartApp()
    }

    private fun restartApp() {
        val intent = Intent(this, SceneActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish() // Finaliza la actividad actual para evitar que quede en el historial
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        initWithIntent(intent)
    }

    override fun onBackPressed() {
        if (coordinator.selectedNode != null) {
            coordinator.selectNode(null)
        } else {
            super.onBackPressed()
        }
    }

    override fun config(session: Session): Config = Config(session).apply {
        lightEstimationMode = LightEstimationMode.DISABLED
        planeFindingMode = HORIZONTAL_AND_VERTICAL
        updateMode = UpdateMode.LATEST_CAMERA_IMAGE
        cloudAnchorMode = CloudAnchorMode.ENABLED
        augmentedFaceMode = AugmentedFaceMode.DISABLED
        focusMode = FocusMode.AUTO
        if (session.isDepthModeSupported(DepthMode.AUTOMATIC)) {
            depthMode = DepthMode.AUTOMATIC
        }
    }

    override fun onArResumed() {
        bottomSheetScene.behavior().update(state = STATE_EXPANDED, isHideable = false)
    }

    private fun initWithIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        intent.data?.let {
            Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
            this.intent = null
        }
    }

    private fun initSceneBottomSheet() = with(bottomSheetScene) {
        behavior().state = STATE_HIDDEN
        header.root.setOnClickListener { behavior().toggle() }
        header.add.setOnClickListener {
            val session = arSceneView.session
            val camera = arSceneView.arFrame?.camera ?: return@setOnClickListener
            if (session == null || camera.trackingState != TRACKING) return@setOnClickListener
            createNodeAndAddToScene(anchor = { session.createAnchor(Nodes.defaultPose(arSceneView)) }, focus = false)
        }

        model.selection.observe(this@SceneActivity) {
            body.apply {
                cube.isSelected = it == Cube::class
            }
        }

        body.apply {
            cube.setOnClickListener { model.selection.value = Cube::class }
            colorValue.setOnColorChangeListener { color ->
                arSceneView.planeRenderer.material?.thenAccept {
                    it.setFloat3(PlaneRenderer.MATERIAL_COLOR, color.toArColor())
                }
                settings.pointCloud.updateMaterial(arSceneView) { this.color = color }
                settings.reticle.updateMaterial(arSceneView) { this.color = color }
            }
            colorValue.post { colorValue.setColor(MaterialProperties.DEFAULT.color) }
        }
    }

    private fun initNodeBottomSheet() = with(bottomSheetNode) {
        behavior().apply {
            skipCollapsed = true
            addBottomSheetCallback(object : BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    bottomSheet.requestLayout()
                    if (newState == STATE_HIDDEN) {
                        coordinator.selectNode(null)
                    }
                }
            })
            state = STATE_HIDDEN
        }
        header.apply {
            root.setOnClickListener { coordinator.selectNode(null) }
            delete.setOnClickListener { coordinator.focusedNode?.detach()
                isCubePlaced = false}
        }

        body.apply {
            colorValue.setOnColorChangeListener { focusedMaterialNode()?.update { color = it } }
            }
    }

    private fun focusedMaterialNode() = (coordinator.focusedNode as? MaterialNode)

    private fun materialProperties() = with(bottomSheetNode.body) {
        MaterialProperties(
            color = if (focusedMaterialNode() != null) colorValue.getColor() else bottomSheetScene.body.colorValue.getColor(),
        )
    }

    private fun initAr() = with(arSceneView) {
        scene.addOnUpdateListener { onArUpdate() }
        scene.addOnPeekTouchListener { hitTestResult, motionEvent ->
            coordinator.onTouch(hitTestResult, motionEvent)
        }
        settings.apply {
            sunlight.applyTo(this@with)
            shadows.applyTo(this@with)
            planes.applyTo(this@with)
            selection.applyTo(coordinator.selectionVisualizer)
            reticle.initAndApplyTo(this@with)
            pointCloud.initAndApplyTo(this@with)
        }
    }

    private fun onArTap(motionEvent: MotionEvent) {
        val frame = arSceneView.arFrame ?: return
        if (frame.camera.trackingState != TRACKING) {
            coordinator.selectNode(null)
            return
        }

        if(isCubePlaced) {
            // Se tocó una parte vacía de la pantalla
            clearAllObjects()
            coordinator.selectNode(null)
            return
        }
    }

    private fun clearAllObjects() {
        // Elimina cada nodo de la escena
        nodeList.forEach { it.detach() }
        nodeList.clear() // Limpia la lista

        // Restablece la bandera
        isCubePlaced = false

        // Mensaje opcional
        Toast.makeText(this, "Todos los objetos han sido eliminados", Toast.LENGTH_SHORT).show()
    }



    private fun createNodeAndAddToScene(anchor: () -> Anchor, focus: Boolean = true) {
        val node = when (model.selection.value) {
            Cube::class -> Cube(this, materialProperties(), coordinator, settings)
            else -> return
        }

        node.attach(anchor(), arSceneView.scene, focus)

        // Establecer la rotación inicial del nodo en 0
        node.localRotation = Quaternion.axisAngle(Vector3(0f, 1f, 0f), 0f)

        nodeList.add(node) // Agrega el nodo a la lista
    }



    private fun onArUpdate() {
        val frame = arSceneView.arFrame
        val camera = frame?.camera
        val state = camera?.trackingState
        val reason = camera?.trackingFailureReason

        onArUpdateStatusText(state, reason)
        onArUpdateStatusIcon(state, reason)
        detectObjects()
    }

    private fun detectObjects() {
        val frame = arSceneView.arFrame ?: return
        copyPixelFromView { bitmap ->
            val detections = objectDetectionModel.detectObjects(bitmap)
            detections.forEach { detectedObject ->
                if (detectedObject.label == "keyboard" && !isCubePlaced) {
                    handleDetectedObject(detectedObject)
                    isCubePlaced = true
                }
            }
        }
    }


    private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun handleDetectedObject(detectedObject: DetectedObject) {
        Log.d(TAG, "Objeto detectado: ${detectedObject.label}")
        Toast.makeText(this, "Detectado: ${detectedObject.label}", Toast.LENGTH_SHORT).show()
        // Crear un anchor y dibujar un cubo en la posición detectada.
        val anchor = createAnchorFromObject(detectedObject) ?: return
        createNodeAndAddToScene(
            anchor = { anchor },
            focus = true
        )
    }

    private fun createAnchorFromObject(detectedObject: DetectedObject): Anchor? {
        val session = arSceneView.session ?: return null
        val frame = arSceneView.arFrame ?: return null

        // Obtener las coordenadas centrales del bounding box
        val centerX = detectedObject.boundingBox.centerX()
        val centerY = detectedObject.boundingBox.centerY()

        // Transformar las coordenadas del bounding box al tamaño del viewport de la AR Scene
        val scaleX = arSceneView.width / 300f // 300 es el ancho del bitmap redimensionado
        val scaleY = arSceneView.height / 300f // 300 es la altura del bitmap redimensionado
        val viewportX = centerX * scaleX
        val viewportY = centerY * scaleY

        // Realizar un hit test en el espacio AR usando las coordenadas transformadas
        val hitResult = frame.hitTest(viewportX, viewportY).firstOrNull() ?: return null

        // Obtener la pose original del hit result
        val hitPose = hitResult.hitPose

        // Ajustar la rotación del Pose para que el cubo no esté girado
        val correctedPose = hitPose.compose(Pose.makeRotation(0f, 0f, 0f, 1f)) // Quaternion para rotación identidad

        // Crear y devolver el ancla con la pose corregida
        return session.createAnchor(correctedPose)
    }


    private fun copyPixelFromView(callback: (Bitmap) -> Unit) {
        val view = arSceneView
        if (view.width == 0 || view.height == 0) {
            Log.e(TAG, "ArSceneView aún no está inicializado: ancho o alto = 0")
            return
        }

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(view, bitmap, { result ->
            if (result == PixelCopy.SUCCESS) {
                callback(bitmap)
            } else {
                Log.e(TAG, "Fallo al copiar los píxeles de la vista AR.")
            }
        }, Handler(mainLooper))
    }



    private fun onArUpdateStatusText(state: TrackingState?, reason: TrackingFailureReason?) = bottomSheetScene.header.label.setText(
        when (state) {
            TRACKING -> R.string.tracking_success
            PAUSED -> when (reason) {
                NONE -> R.string.tracking_failure_none
                BAD_STATE -> R.string.tracking_failure_bad_state
                INSUFFICIENT_LIGHT -> R.string.tracking_failure_insufficient_light
                EXCESSIVE_MOTION -> R.string.tracking_failure_excessive_motion
                INSUFFICIENT_FEATURES -> R.string.tracking_failure_insufficient_features
                CAMERA_UNAVAILABLE -> R.string.tracking_failure_camera_unavailable
                null -> 0
            }
            STOPPED -> R.string.tracking_stopped
            null -> 0
        },
    )

    private fun onArUpdateStatusIcon(state: TrackingState?, reason: TrackingFailureReason?) = bottomSheetScene.header.status.setImageResource(
        when (state) {
            TRACKING -> android.R.drawable.presence_online
            PAUSED -> when (reason) {
                NONE -> android.R.drawable.presence_invisible
                BAD_STATE, CAMERA_UNAVAILABLE -> android.R.drawable.presence_busy
                INSUFFICIENT_LIGHT, EXCESSIVE_MOTION, INSUFFICIENT_FEATURES -> android.R.drawable.presence_away
                null -> 0
            }
            STOPPED -> android.R.drawable.presence_offline
            null -> 0
        },
    )



    private fun onNodeUpdate(node: Nodes) = with(bottomSheetNode) {
        when {
            node != coordinator.selectedNode || node != coordinator.focusedNode || behavior().state == STATE_HIDDEN -> Unit
            else -> {
                with(header) {
                    status.setImageResource(node.statusIcon())
                    distance.text = arSceneView.arFrame?.camera.formatDistance(this@SceneActivity, node)
                    delete.isEnabled = !node.isTransforming
                }
                with(body) {
                    positionValue.text = node.worldPosition.format(this@SceneActivity)
                    rotationValue.text = node.worldRotation.format(this@SceneActivity)
                    scaleValue.text = node.worldScale.format(this@SceneActivity)
                }
            }
        }
    }

    private fun onNodeSelected(old: Nodes? = coordinator.selectedNode, new: Nodes?) {
        old?.onNodeUpdate = null
        new?.onNodeUpdate = ::onNodeUpdate
    }

    private fun onNodeFocused(node: Nodes?) {
        val nodeSheetBehavior = bottomSheetNode.behavior()
        val sceneBehavior = bottomSheetScene.behavior()
        when (node) {
            null -> {
                nodeSheetBehavior.state = STATE_HIDDEN
                if ((bottomSheetScene.root.tag as? Boolean) == true) {
                    bottomSheetScene.root.tag = false
                    sceneBehavior.state = STATE_EXPANDED
                }
                // Detener el sensor del giroscopio cuando no hay nodo seleccionado
                gyroscopeListener?.let { sensorManager.unregisterListener(it) }
                gyroscopeListener = null
            }
            coordinator.selectedNode -> {
                with(bottomSheetNode.header) {
                    name.text = node.name
                }
                with(bottomSheetNode.body) {
                    (node as? MaterialNode)?.properties?.let {
                        colorValue.setColor(it.color)
                    }
                }
                val materialVisibility = if (node is MaterialNode) VISIBLE else GONE
                setOfMaterialViews.forEach { it.visibility = materialVisibility }
                nodeSheetBehavior.state = STATE_EXPANDED
                if (sceneBehavior.state != STATE_COLLAPSED) {
                    sceneBehavior.state = STATE_COLLAPSED
                    bottomSheetScene.root.tag = true
                }
                // Activar el control del giroscopio
                activateGyroscopeControl(node)
            }
            else -> Unit
        }
    }

    private val smoothingFactor = 0.1f // Factor de suavización (ajustable entre 0 y 1)
    private var smoothedRotationX = 0f
    private var smoothedRotationY = 0f
    private var smoothedRotationZ = 0f

    private fun activateGyroscopeControl(node: Nodes) {
        if (node !is MaterialNode) return // Solo aplicamos el control a MaterialNodes

        gyroscopeListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.sensor.type != Sensor.TYPE_GYROSCOPE) return

                // Lee los valores actuales del giroscopio
                val rawRotationRateX = event.values[0] // Velocidad angular alrededor del eje X
                val rawRotationRateY = event.values[1] // Velocidad angular alrededor del eje Y
                val rawRotationRateZ = event.values[2] // Velocidad angular alrededor del eje Z

                // Suaviza los datos utilizando el promedio móvil exponencial
                smoothedRotationX = smoothedRotationX + smoothingFactor * (rawRotationRateX - smoothedRotationX)
                smoothedRotationY = smoothedRotationY + smoothingFactor * (rawRotationRateY - smoothedRotationY)
                smoothedRotationZ = smoothedRotationZ + smoothingFactor * (rawRotationRateZ - smoothedRotationZ)

                // Aplica los valores suavizados directamente al nodo
                node.setRotationFromGyroscope(smoothedRotationX, smoothedRotationY, smoothedRotationZ)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Registrar el listener del giroscopio
        gyroscopeSensor?.let {
            sensorManager.registerListener(gyroscopeListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }




}
