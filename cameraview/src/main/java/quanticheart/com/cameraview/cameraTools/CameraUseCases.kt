package quanticheart.com.cameraview.cameraTools

import android.content.Context.CAMERA_SERVICE
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.TextureView
import androidx.fragment.app.Fragment
import quanticheart.com.cameraview.extention.ANIMATION_FAST_MILLIS
import quanticheart.com.cameraview.extention.ANIMATION_SLOW_MILLIS

@Suppress("unused")
class CameraUseCases(private val fragment: Fragment, private val surfaceCameraSelfie: TextureView) :
        CameraConfig(fragment, surfaceCameraSelfie) {

    /**
     * Flash camera
     */
    override fun flashCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Display flash animation to indicate that photo was captured
            fragment.view?.rootView?.postDelayed({
                fragment.view?.rootView?.foreground = ColorDrawable(Color.WHITE)
                fragment.view?.rootView?.postDelayed(
                        { fragment.view?.rootView?.foreground = null }, ANIMATION_FAST_MILLIS)
            }, ANIMATION_SLOW_MILLIS)
        }
    }

    /**
     * get display metrics
     * Get screen metrics used to setup camera for full screen resolution
     */
    private fun getMetrix() = DisplayMetrics().also { surfaceCameraSelfie.display.getRealMetrics(it) }

    /**
     * get flash frontal support
     * @link https://medium.com/google-developers/detecting-camera-features-with-camera2-61675bb7d1bf
     */
    fun hasFlashForFrontCamera(): Boolean {
        try {
            val cManager = fragment.context?.getSystemService(CAMERA_SERVICE) as CameraManager
            for (cameraId in cManager.cameraIdList) {
                val characteristics = cManager.getCameraCharacteristics(cameraId)
                val cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
                if (cOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                        Log.e("Support frontal flash", "yes")
                        return true
                    } else {
                        Log.e("Support frontal flash", "no")
                    }
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return false
    }

}