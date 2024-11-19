package co.com.jairocpd.ar_app.domain.model

import android.content.Context
import android.view.MotionEvent
import co.com.jairocpd.ar_app.config.Settings
import co.com.jairocpd.ar_app.shared.ui.Coordinator
import co.com.jairocpd.ar_app.util.MaterialProperties
import co.com.jairocpd.ar_app.util.toArColor
import com.google.ar.core.Anchor
import com.google.ar.core.DepthPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory.makeOpaqueWithColor
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.BaseTransformableNode
import com.google.ar.sceneform.ux.TransformableNode
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.cos
import kotlin.math.sin
import kotlin.reflect.KClass

sealed class Nodes(
    name: String,
    coordinator: Coordinator,
    private val settings: Settings,
) : TransformableNode(coordinator) {

    interface FacingCamera

    companion object {

        private const val PLANE_ANCHORING_DISTANCE = 2F
        private const val DEFAULT_POSE_DISTANCE = 2F

        private val IDS: MutableMap<KClass<*>, AtomicLong> = mutableMapOf()

        fun Any.newId(): Long = IDS.getOrElse(this::class) { AtomicLong().also { IDS[this::class] = it } }.incrementAndGet()

        fun defaultPose(ar: ArSceneView): Pose {
            val centerX = ar.width / 2F
            val centerY = ar.height / 2F
            val hits = ar.arFrame?.hitTest(centerX, centerY)
            val planeHitPose = hits?.firstOrNull {
                when (val trackable = it.trackable) {
                    is Plane -> trackable.isPoseInPolygon(it.hitPose) && it.distance <= PLANE_ANCHORING_DISTANCE
                    is DepthPoint, is Point -> it.distance <= DEFAULT_POSE_DISTANCE
                    else -> false
                }
            }?.hitPose
            if (planeHitPose != null) return planeHitPose
            val ray = ar.scene.camera.screenPointToRay(centerX, centerY)
            val point = ray.getPoint(DEFAULT_POSE_DISTANCE)
            return Pose.makeTranslation(point.x, point.y, point.z)
        }
    }

    init {
        this.name = "$name #${newId()}"
        scaleController.apply {
            minScale = 0.25F
            maxScale = 5F
        }
        @Suppress("LeakingThis")
        if (this is FacingCamera) rotationController.isEnabled = false
    }

    var onNodeUpdate: ((Nodes) -> Any)? = null

    internal fun anchor(): Anchor? = (parent as? AnchorNode)?.anchor

    override fun getTransformationSystem(): Coordinator = super.getTransformationSystem() as Coordinator

    override fun setRenderable(renderable: Renderable?) {
        super.setRenderable(
            renderable?.apply {
                isShadowCaster = settings.shadows.get()
                isShadowReceiver = settings.shadows.get()
            },
        )
    }

    override fun onUpdate(frameTime: FrameTime) {
        onNodeUpdate?.invoke(this)
        if (this is FacingCamera) {
            facingCamera()
        }
    }

    private fun facingCamera() {
        // Buggy when dragging because TranslationController already handles it's own rotation on each update.
        if (isTransforming) return // Prevent infinite loop
        val camera = scene?.camera ?: return
        val direction = Vector3.subtract(camera.worldPosition, worldPosition)
        worldRotation = Quaternion.lookRotation(direction, Vector3.up())
    }

    open fun attach(anchor: Anchor, scene: Scene, focus: Boolean = false) {
        setParent(AnchorNode(anchor).apply { setParent(scene) })
        if (focus) {
            transformationSystem.focusNode(this)
        }
    }

    open fun detach() {
        if (this == transformationSystem.selectedNode) {
            transformationSystem.selectNode(selectionContinuation())
        }
        (parent as? AnchorNode)?.anchor?.detach()
        setParent(null)
    }

    open fun selectionContinuation(): BaseTransformableNode? = null

    open fun statusIcon(): Int = if (isActive && isEnabled && (parent as? AnchorNode)?.isTracking == true) {
        android.R.drawable.presence_online
    } else {
        android.R.drawable.presence_invisible
    }

    override fun onTap(hitTestResult: HitTestResult?, motionEvent: MotionEvent?) {
        super.onTap(hitTestResult, motionEvent)
        if (isTransforming) return // ignored when dragging over a small distance
        transformationSystem.focusNode(this)
    }
}

sealed class MaterialNode(
    name: String,
    val properties: MaterialProperties,
    coordinator: Coordinator,
    settings: Settings,
) : Nodes(name, coordinator, settings) {

    init {
        update()
    }

    fun update(block: (MaterialProperties.() -> Unit) = {}) {
        properties.update(renderable?.material, block)
    }


    fun setRotationFromGyroscope(x: Float, y: Float, z: Float) {
        localRotation = eulerToQuaternion(x, y, z)
    }
}


fun eulerToQuaternion(roll: Float, pitch: Float, yaw: Float): Quaternion {
    val cy = cos(yaw / 2.0).toFloat()
    val sy = sin(yaw / 2.0).toFloat()
    val cp = cos(pitch / 2.0).toFloat()
    val sp = sin(pitch / 2.0).toFloat()
    val cr = cos(roll / 2.0).toFloat()
    val sr = sin(roll / 2.0).toFloat()

    val x = sr * cp * cy - cr * sp * sy
    val y = cr * sp * cy + sr * cp * sy
    val z = cr * cp * sy - sr * sp * cy
    val w = cr * cp * cy + sr * sp * sy

    return Quaternion(x, y, z, w)
}






class Cube(
    context: Context,
    properties: MaterialProperties,
    coordinator: Coordinator,
    settings: Settings,
) : MaterialNode("Cube", properties, coordinator, settings) {

    companion object {
        private const val SIZE = 0.1F
        private val CENTER = Vector3(0F, SIZE / 2, 0F)
    }

    init {
        val color = properties.color.toArColor()
        makeOpaqueWithColor(context.applicationContext, color)
            .thenAccept { renderable = ShapeFactory.makeCube(Vector3.one().scaled(SIZE), CENTER, it) }
    }
}

