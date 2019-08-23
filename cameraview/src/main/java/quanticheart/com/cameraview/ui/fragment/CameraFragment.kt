package quanticheart.com.cameraview.ui.fragment

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import androidx.camera.core.ImageCapture
import kotlinx.android.synthetic.main.fragment_camera_selfie.*
import quanticheart.com.cameraview.R
import quanticheart.com.cameraview.broadcast.BroadcastHelper
import quanticheart.com.cameraview.cameraTools.CameraUseCases
import quanticheart.com.cameraview.extention.simulateClick
import quanticheart.com.cameraview.ui.base.FragmentCameraBase
import quanticheart.com.cameraview.util.CompressionImageTask
import quanticheart.com.cameraview.util.TestC
import java.io.File

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
            updateCameraUi()
//            bindCameraUseCases()
            cameraCase = CameraUseCases(this, viewLifecycleOwner, surfaceCameraSelfie)
        }

        // Determine the output directory
    }


    /** Method used to re-draw the camera UI controls, called every time configuration changes */
    @SuppressLint("RestrictedApi")
    private fun updateCameraUi() {

        // Listener for button used to capture photo
        btnCameraSelfieGetPhoto.setOnClickListener {
            // Get a stable reference of the modifiable image capture use case

            cameraCase?.getPicture(object : CameraUseCases.OnCaptureCallback {
                override fun onError(
                        error: ImageCapture.UseCaseError, message: String, exc: Throwable?) {
                    Log.e(TAG, "Photo capture failed: $message")
                    exc?.printStackTrace()
                }

                override fun onSuccess(photoFile: File) {
                    saveFile(photoFile)
                }
            })
        }
    }

    private fun saveFile(photoFile: File) {
        Log.d(TAG, "Photo capture succeeded: ${photoFile.absolutePath}")


        // We can only change the foreground Drawable using API level 23+ API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Update the gallery thumbnail with latest picture taken
            //                setGalleryThumbnail(photoFile)
        }

        // Implicit broadcasts will be ignored for devices running API
        // level >= 24, so if you only target 24+ you can remove this statement
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            requireActivity().sendBroadcast(
                    Intent(Camera.ACTION_NEW_PICTURE, Uri.fromFile(photoFile)))
        }

        // If the folder selected is an external media directory, this is unnecessary
        // but otherwise other apps will not be able to access our images unless we
        // scan them using [MediaScannerConnection]
        val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(photoFile.extension)
        MediaScannerConnection.scanFile(
                context, arrayOf(photoFile.absolutePath), arrayOf(mimeType), null)

//        CompressionImageTask.compressImage(requireActivity(), photoFile)
        TestC.compressImage(requireActivity(), photoFile.absolutePath)
        goToPreview(view!!, Uri.fromFile(photoFile))
    }

    /**
     * Activity LifeCycle
     */
    override fun onDestroyView() {
        super.onDestroyView()
        broadcastReceiver?.unregisterReceiverKeyEvent()
        cameraCase?.unregisterDisplayListener()
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since user could have removed them
        //  while the app was on paused state
        if (!hasPermissions(requireContext())) {
            goToPermission(view!!)
        }
    }

}
