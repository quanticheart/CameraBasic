package quanticheart.com.cameraview.ui.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.camera.core.ImageCapture
import kotlinx.android.synthetic.main.fragment_camera_selfie.*
import quanticheart.com.cameraview.R
import quanticheart.com.cameraview.broadcast.BroadcastHelper
import quanticheart.com.cameraview.cameraTools.CameraConfig
import quanticheart.com.cameraview.cameraTools.CameraUseCases
import quanticheart.com.cameraview.extention.simulateClick
import quanticheart.com.cameraview.ui.base.FragmentCameraBase
import quanticheart.com.cameraview.util.CompressUtil
import java.io.File

@Suppress("DEPRECATION")
class CameraFragment : FragmentCameraBase() {

    /**
     * Var for init KeyReceiver
     */
    private var broadcastReceiver: BroadcastHelper? = null
    private var cameraCase: CameraUseCases? = null

    /**
     * OnCreate
     */
    override fun setViewID(): Int? = R.layout.fragment_camera_selfie

    override fun onViewFinishLoad(view: View, savedInstanceState: Bundle?) {
        setupBroadcastReceiver()
        setupDisplay()
    }

    /**
     * Setup Broadcast action
     */
    private fun setupBroadcastReceiver() {
        broadcastReceiver = BroadcastHelper(requireActivity(), object : BroadcastHelper.OnClickActionReceiver {
            override fun receiveAction(keyCode: Int) {
                btnCameraSelfieGetPhoto.simulateClick()
            }
        })
    }

    private fun setupDisplay() {
        surfaceCameraSelfie.post {
            // Build UI controls and bind all camera use cases
            cameraCase = CameraUseCases(this, surfaceCameraSelfie)
            updateCameraUi()
        }
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes */
    @SuppressLint("RestrictedApi")
    private fun updateCameraUi() {

        // Listener for button used to capture photo
        btnCameraSelfieGetPhoto.setOnClickListener {
            btnCameraSelfieGetPhoto.isEnabled = false
            cameraCase?.getPicture(object : CameraConfig.OnCaptureCallback {
                override fun onError(error: ImageCapture.UseCaseError, message: String, exc: Throwable?) {
                    super.onError(error, message, exc)
                    btnCameraSelfieGetPhoto.isEnabled = true
                }

                override fun onSuccess(photoFile: File) {
                    super.onSuccess(photoFile)
                    saveFile(photoFile)
                }
            })
        }

        //Listener flash btn
        btnCameraSelfieFlash.apply {
            visibility = if (cameraCase?.hasFlashForFrontCamera() == true) View.VISIBLE else View.GONE
            setOnClickListener {
                setFlashStatusTrade()
            }
        }

        btnCameraSelfieTrade.setOnClickListener {
            cameraCase?.flipCamera()
        }
    }

    private var flashStatus = 0
    private val flashOff = 0
    private val flashAuto = 1
    private val flashOn = 2

    private fun setFlashStatusTrade() {
        flashStatus = when (flashStatus) {
            flashOff -> {
                cameraCase?.flashAuto()
                btnCameraSelfieFlash.flashOffToAuto()
                flashAuto
            }
            flashAuto -> {
                cameraCase?.flashOn()
                btnCameraSelfieFlash.flashAutoToOn()
                flashOn
            }
            flashOn -> {
                cameraCase?.flashOff()
                btnCameraSelfieFlash.flashOnToOff()
                flashOff
            }
            else -> {
                cameraCase?.flashOff()
                btnCameraSelfieFlash.flashOnToOff()
                flashOff
            }
        }
    }

    private fun saveFile(photoFile: File) {
        goToPreview(view!!, Uri.parse(CompressUtil.init(requireActivity(), photoFile.absolutePath)))
        btnCameraSelfieGetPhoto.isEnabled = true
    }

    /**
     * Activity LifeCycle
     */
    override fun onDestroyView() {
        super.onDestroyView()
        broadcastReceiver?.unregisterReceiverKeyEvent()
        cameraCase?.clearCameraUserCase()
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since user could have removed them
        //  while the app was autoToOn paused state
        if (!hasPermissions(requireContext())) {
            goToPermission(view!!)
        }
        flashStatus = flashOn
        setFlashStatusTrade()
    }

}
