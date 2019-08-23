package quanticheart.com.cameraview.cameraTools

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.Camera
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.TextureView
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import quanticheart.com.cameraview.cameraTools.cameraUtil.AutoFitPreviewBuilder
import quanticheart.com.cameraview.cameraTools.cameraUtil.LuminosityAnalyzer
import quanticheart.com.cameraview.extention.ANIMATION_FAST_MILLIS
import quanticheart.com.cameraview.extention.ANIMATION_SLOW_MILLIS
import quanticheart.com.cameraview.util.StorageUtil
import java.io.File

class CameraUseCases(private val fragment: Fragment, private val lifecycleOwner: LifecycleOwner, private val surfaceCameraSelfie: TextureView) {

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

    /** Declare worker thread at the class level so it can be reused after config changes */
    private val analyzerThread = HandlerThread("LuminosityAnalysis").apply { start() }

    private var lensFacing = CameraX.LensFacing.FRONT
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var outputDirectory: File = StorageUtil.getExternalStorageDirectory(fragment.requireContext())

    init {
        bindUserCase()
    }

    private fun bindUserCase() {

        /**
         * Clean for init
         */
        CameraX.unbindAll()

        /**
         * Every time the orientation of device changes, recompute layout
         */

        displayManager = surfaceCameraSelfie.context
                .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager?.registerDisplayListener(displayListener, null)

        /**
         * Use the auto-fit preview builder to automatically handle size and orientation changes
         */

        preview = AutoFitPreviewBuilder.build(getPreviewConfig(), surfaceCameraSelfie)
        imageCapture = ImageCapture(getImageCaptureConfig())
        imageAnalyzer = getImageAnalysis()

        /**
         * Apply declared configs to CameraX using the same lifecycle owner
         */
        CameraX.bindToLifecycle(lifecycleOwner, preview, imageCapture, imageAnalyzer)
    }

    /**
     * Trade camera
     */
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
            CameraX.unbindAll()
            bindUserCase()
        } catch (exc: Exception) {
            // Do nothing
        }
    }

    /**
     * Get Picture
     */
    fun getPicture(callback: OnCaptureCallback) {
        this.callback = callback
        imageCapture?.let { imageCapture ->
            val photoFile = StorageUtil.createFile(outputDirectory)
            val metadata = ImageCapture.Metadata().apply {
                //                    isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
            }
            imageCapture.takePicture(photoFile, imageSavedListener, metadata)
            flashCapture()
        }
    }

    private var callback: OnCaptureCallback? = null

    interface OnCaptureCallback {
        fun onError(error: ImageCapture.UseCaseError, message: String, exc: Throwable?)
        fun onSuccess(photoFile: File)
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

    /**
     * Fun for kill listener display
     */
    fun unregisterDisplayListener() {
        displayManager?.unregisterDisplayListener(displayListener)
    }

    /**
     * Flash camera
     */
    private fun flashCapture() {
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
     * Utils for class
     */
    private fun getDeviceCameraSupport(): List<Camera.Size> {
        val camera = Camera.open()
        val cameraParameters = camera.getParameters()
        return cameraParameters.getSupportedPictureSizes()
    }

    /**
     * get display metrics
     * Get screen metrics used to setup camera for full screen resolution
     */
    private fun getMetrix() = DisplayMetrics().also { surfaceCameraSelfie.display.getRealMetrics(it) }

    /**
    // We request aspect ratio but no resolution to match preview config but letting
    // CameraX optimize for whatever specific resolution best fits requested capture mode
     */
    private fun getRational(): Rational = Rational(surfaceCameraSelfie.width, surfaceCameraSelfie.height)

    /**
     * Set up the view finder use case to display camera preview
     */
    private fun getPreviewConfig() = PreviewConfig.Builder().apply {
        setLensFacing(lensFacing)
        setTargetRotation(surfaceCameraSelfie.display.rotation)
        setTargetAspectRatio(Rational(1, 1))
        setTargetResolution(Size(surfaceCameraSelfie.width, surfaceCameraSelfie.height))
    }.build()

    /**
     * Set up the capture use case to allow users to take photos
     */
    private fun getImageCaptureConfig() = ImageCaptureConfig.Builder().apply {
        setLensFacing(lensFacing)
        setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
        setTargetAspectRatio(getRational())
        setFlashMode(FlashMode.AUTO)
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
            Log.d(ContentValues.TAG, "Average luminosity: $luma. Frames per second: ${"%.01f".format(fps)}")
        }
    }

    /**
     * Setup image analysis pipeline that computes average pixel luminance in real time
     */
    private fun getImageAnalysisConfig() = ImageAnalysisConfig.Builder().apply {
        setLensFacing(lensFacing)
        // Use a worker thread for image analysis to prevent preview glitches
        setCallbackHandler(Handler(analyzerThread.looper))
        // In our analysis, we care more about the latest image than analyzing *every* image
        setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        // Set initial target rotation, we will have to call this again if rotation changes
        // during the lifecycle of this use case
        setTargetRotation(surfaceCameraSelfie.display.rotation)
    }.build()

}