package co.com.jairocpd.ar_app.ui.ar

import android.Manifest.permission.CAMERA
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.media.CamcorderProfile
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.MenuCompat
import androidx.viewbinding.ViewBinding
import co.com.jairocpd.ar_app.R
import co.com.jairocpd.ar_app.ui.scenes.SceneActivity
import co.com.jairocpd.ar_app.util.createArCoreViewerIntent
import co.com.jairocpd.ar_app.util.redirectToApplicationSettings
import co.com.jairocpd.ar_app.util.safeStartActivity
import co.com.jairocpd.ar_app.util.screenshot
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.ArSceneView

abstract class ArActivity<T : ViewBinding>(private val inflate: (LayoutInflater) -> T) : AppCompatActivity() {

    private companion object {
        const val REQUEST_CAMERA_PERMISSION = 1
    }

    private var sessionInitializationFailed: Boolean = false

    private var installRequested: Boolean = false


    private val arCoreViewerIntent by lazy {
        createArCoreViewerIntent(
            uri = getString(R.string.scene_viewer_native_uri).toUri(),
            model = getString(R.string.scene_viewer_native_model),
            link = getString(R.string.scene_viewer_native_link),
            title = getString(R.string.scene_viewer_native_title),
        )
    }

    protected lateinit var binding: T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(inflate(layoutInflater).also { binding = it }.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        initArSession()
        try {
            arSceneView.resume()
        } catch (ex: CameraNotAvailableException) {
            sessionInitializationFailed = true
        }
        if (hasCameraPermission() && !installRequested && !sessionInitializationFailed) {
            renderNavigationBar()
            onArResumed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION && !hasCameraPermission()) {
            redirectToApplicationSettings()
        }
    }

    override fun onPause() {
        super.onPause()
        arSceneView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        arSceneView.destroy()
    }

    abstract val arSceneView: ArSceneView

    abstract val recordingIndicator: ImageView

    open val features = emptySet<Session.Feature>()

    abstract fun config(session: Session): Config

    open fun onArResumed() = Unit

    private fun hasCameraPermission() = ActivityCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED

    private fun requestCameraPermission() {
        if (!hasCameraPermission()) {
            requestPermissions(arrayOf(CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    private fun renderNavigationBar() {
        if (SDK_INT >= VERSION_CODES.O && resources.configuration.orientation != ORIENTATION_LANDSCAPE) {
            window.navigationBarColor = Color.WHITE
            window.decorView.systemUiVisibility = FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }

    private fun initArSession() {
        if (arSceneView.session != null) {
            return
        }
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }
        if (sessionInitializationFailed) {
            return
        }
        val sessionException: UnavailableException?
        try {
            val requestInstall = ArCoreApk.getInstance().requestInstall(this, !installRequested)
            if (requestInstall == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                installRequested = true
                return
            }
            installRequested = false
            val session = Session(applicationContext, features)
            session.configure(config(session))
            arSceneView.setupSession(session)
            return
        } catch (e: UnavailableException) {
            sessionException = e
        } catch (e: Exception) {
            sessionException = UnavailableException().apply { initCause(e) }
        }
        sessionInitializationFailed = true
        if (sessionException != null) {
            Toast.makeText(this, sessionException.message, Toast.LENGTH_LONG).show()
        }
        finish()
    }

}
