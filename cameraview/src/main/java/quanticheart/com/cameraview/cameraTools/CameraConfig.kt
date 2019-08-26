@file:Suppress("DEPRECATION")

package quanticheart.com.cameraview.cameraTools

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.TextureView
import android.webkit.MimeTypeMap
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import quanticheart.com.cameraview.cameraTools.cameraUtil.AutoFitPreviewBuilder
import quanticheart.com.cameraview.cameraTools.cameraUtil.LuminosityAnalyzer
import quanticheart.com.cameraview.util.StorageUtil
import java.io.File

@Suppress("unused", "RedundantOverride", "MemberVisibilityCanBePrivate", "DEPRECATION")
abstract class CameraConfig(private val fragment: Fragment, private val surfaceCameraSelfie: TextureView) {

    protected val TAG = "Camera Info"

    /** Internal reference of the [DisplayManager] */
    private var displayManager: DisplayManager? = null

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = fragment.view?.let { view ->
            if (displayId == surfaceCameraSelfie.display.displayId) {
                Log.d("Rotation", "Rotation changed: ${view.display.rotation}")
                preview?.setTargetRotation(view.display.rotation)
                imageCapture?.setTargetRotation(view.display.rotation)
                imageAnalyzer?.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    /**
     * Vars
     */

    protected var lensFacing = CameraX.LensFacing.FRONT
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var outputDirectory: File = StorageUtil.getExternalStorageDirectory(fragment.requireContext())

    init {
        bindUserCase()
    }

    protected fun bindUserCase() {

        unbindCamera()

        /**
         * Every time the orientation of device changes, recompute layout
         */
        displayManager = surfaceCameraSelfie.context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager?.registerDisplayListener(displayListener, null)

        /**
         * Use the auto-fit preview builder to automatically handle size and orientation changes
         */
        preview = AutoFitPreviewBuilder.build(getPreviewConfig(), surfaceCameraSelfie)
        imageCapture = ImageCapture(getImageCaptureConfig())
        imageAnalyzer = getImageAnalysis()

        bind()
    }

    /**
     * Apply declared configs to CameraX using the same lifecycle owner
     */
    private fun bind() {
        unbindCamera()
        CameraX.bindToLifecycle(fragment.viewLifecycleOwner, preview, imageCapture, imageAnalyzer)
    }

    protected fun unbindCamera() {
        CameraX.unbindAll()
    }

    protected fun resetCamera() {
        bindUserCase()
    }

    fun clearCameraUserCase() {
        unbindCamera()
        displayManager?.unregisterDisplayListener(displayListener)
    }

    /**
     * Get Picture
     */
    fun getPicture(callback: OnCaptureCallback) {
        this.callback = callback
        val photoFile = StorageUtil.createFile(outputDirectory)

        flashCapture()

        // Implicit broadcasts will be ignored for devices running API
        // level >= 24, so if you only target 24+ you can remove this statement
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            fragment.requireActivity().sendBroadcast(Intent(Camera.ACTION_NEW_PICTURE, Uri.fromFile(photoFile)))
        }

        // If the folder selected is an external media directory, this is unnecessary
        // but otherwise other apps will not be able to access our images unless we
        // scan them using [MediaScannerConnection]
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(photoFile.extension)
        MediaScannerConnection.scanFile(fragment.requireActivity(), arrayOf(photoFile.absolutePath), arrayOf(mimeType), null)

        imageCapture?.let { imageCapture ->
            val metadata = ImageCapture.Metadata().apply {
                //                isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
            }
            imageCapture.takePicture(photoFile, imageSavedListener, metadata)
            imageCapture.takePicture(getImageCaptureListener())
        }
    }

    /**=============================================================================================
     *
     * Config User Case Camera
     *
     * ===========================================================================================*/

    /**
     * Set up the view finder use case to display camera preview
     */
    private fun getPreviewConfig() = PreviewConfig.Builder().apply {
        setLensFacing(lensFacing)
        setTargetRotation(surfaceCameraSelfie.display.rotation)
        setTargetAspectRatio(getRational())
        setTargetResolution(Size(surfaceCameraSelfie.width, surfaceCameraSelfie.height))
    }.build()

    /**
     * Set up the capture use case to allow users to take photos
     */
    private fun getImageCaptureConfig() = ImageCaptureConfig.Builder().apply {
        setLensFacing(lensFacing)
        setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
        setTargetAspectRatio(getRational())
        setFlashMode(FlashMode.OFF)
        // Set initial target rotation, we will have to call this again if rotation changes
        // during the lifecycle of this use case
        setTargetRotation(surfaceCameraSelfie.display.rotation)
    }.build()

    /**
     *
     */
    private fun getImageAnalysis() = ImageAnalysis(getImageAnalysisConfig()).apply {
        analyzer = LuminosityAnalyzer { luma ->
            // Values returned from our analyzer are passed to the attached listener
            // We log image analysis results here -- you should do something useful instead!
            val fps = (analyzer as LuminosityAnalyzer).framesPerSecond
            Log.i(TAG, "Average luminosity: $luma. Frames per second: ${"%.01f".format(fps)}")
        }
    }

    /**
     * Setup image analysis pipeline that computes average pixel luminance in real time
     */
    private fun getImageAnalysisConfig() = ImageAnalysisConfig.Builder().apply {
        setLensFacing(lensFacing)
        // Use a worker thread for image analysis to prevent preview glitches
        setCallbackHandler(Handler(getHandlerThread().looper))
        // In our analysis, we care more about the latest image than analyzing *every* image
        setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        // Set initial target rotation, we will have to call this again if rotation changes
        // during the lifecycle of this use case
        setTargetRotation(surfaceCameraSelfie.display.rotation)
    }.build()

    private fun getImageCaptureListener() = object : ImageCapture.OnImageCapturedListener() {
        override fun onCaptureSuccess(image: ImageProxy?, rotationDegrees: Int) {
            super.onCaptureSuccess(image, rotationDegrees)
        }

        override fun onError(useCaseError: ImageCapture.UseCaseError?, message: String?, cause: Throwable?) {
            super.onError(useCaseError, message, cause)
        }
    }

    /**=============================================================================================
     *
     * Utils for User Case Camera
     *
     * ===========================================================================================*/

    /**
    // We request aspect ratio but no resolution to match preview config but letting
    // CameraX optimize for whatever specific resolution best fits requested capture mode
     */
    private fun getRational(): Rational = Rational(surfaceCameraSelfie.width, surfaceCameraSelfie.height)

    /** Declare worker thread at the class level so it can be reused after config changes */
    private fun getHandlerThread() = HandlerThread("LuminosityAnalysis").apply { start() }

    /**=============================================================================================
     *
     * Interface for callback image
     *
     * ===========================================================================================*/

    private var callback: OnCaptureCallback? = null

    interface OnCaptureCallback {
        fun onError(error: ImageCapture.UseCaseError, message: String, exc: Throwable?) {
            Log.e("Camera Info", "Photo capture failed: $message")
            exc?.printStackTrace()
        }

        fun onSuccess(photoFile: File) {
            Log.w("Camera Info", "Photo capture succeeded: ${photoFile.absolutePath}")
        }
    }

    /**
     * Define callback that will be triggered after a photo has been taken and saved to disk
     */
    private val imageSavedListener = object : ImageCapture.OnImageSavedListener {
        override fun onError(error: ImageCapture.UseCaseError, message: String, exc: Throwable?) {
            callback?.onError(error, message, exc)
        }

        override fun onImageSaved(photoFile: File) {
            callback?.onSuccess(photoFile)
        }
    }

    /**=============================================================================================
     *
     * Abstracts for callback image
     *
     * ===========================================================================================*/

    protected abstract fun flashCapture()

    /**=============================================================================================
     *
     * Configs for CameraX View and Actions
     *
     * ===========================================================================================*/

    /**
     * Trade camera for user
     */
    @SuppressLint("RestrictedApi")
    fun flipCamera() {
        lensFacing = if (CameraX.LensFacing.FRONT == lensFacing) {
            CameraX.LensFacing.BACK
        } else {
            CameraX.LensFacing.FRONT
        }
        try {
            // Only bind use cases if we can query a camera with this orientation
            CameraX.getCameraWithLensFacing(lensFacing)
            // Unbind all use cases and bind them again with the new lens facing configuration
            resetCamera()
        } catch (exc: Exception) {
            // Do nothing
        }
    }

    /**
     * SetFlash for camera
     */
    fun flashOn() {
        preview?.enableTorch(true)
        imageCapture?.flashMode = FlashMode.OFF
    }

    fun flashOff() {
        preview?.enableTorch(false)
        imageCapture?.flashMode = FlashMode.OFF
    }

    fun flashAuto() {
        preview?.enableTorch(false)
        imageCapture?.flashMode = FlashMode.ON
    }
}