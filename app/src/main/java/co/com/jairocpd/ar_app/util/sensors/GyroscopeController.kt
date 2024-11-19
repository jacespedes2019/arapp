package co.com.jairocpd.ar_app.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

class GyroscopeController(
    context: Context,
    private val smoothingFactor: Float = 0.5f, // Ajustable
    private val onRotationUpdate: (x: Float, y: Float, z: Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var smoothedRotationX = 0f
    private var smoothedRotationY = 0f
    private var smoothedRotationZ = 0f

    fun start() {
        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        // Lee los valores actuales del giroscopio
        val rawRotationRateX = event.values[0]
        val rawRotationRateY = event.values[1]
        val rawRotationRateZ = event.values[2]

        // Aplica suavizado con promedio móvil exponencial
        smoothedRotationX += smoothingFactor * (rawRotationRateX - smoothedRotationX)
        smoothedRotationY += smoothingFactor * (rawRotationRateY - smoothedRotationY)
        smoothedRotationZ += smoothingFactor * (rawRotationRateZ - smoothedRotationZ)

        // Callback con los valores actualizados
        onRotationUpdate(smoothedRotationX, smoothedRotationY, smoothedRotationZ)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
