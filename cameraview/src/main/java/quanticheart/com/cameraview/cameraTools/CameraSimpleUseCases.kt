package quanticheart.com.cameraview.cameraTools

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import quanticheart.com.cameraview.cameraTools.cameraUtil.LuminosityAnalyzer
import quanticheart.com.cameraview.extention.ANIMATION_FAST_MILLIS
import quanticheart.com.cameraview.extention.ANIMATION_SLOW_MILLIS
import quanticheart.com.cameraview.util.StorageUtil
import java.io.File

class CameraSimpleUseCases(private val fragment: Fragment, private val lifecycleOwner: LifecycleOwner, private val surfaceCameraSelfie: TextureView) {

    private var lensFacing = CameraX.LensFacing.FRONT
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var outputDirectory: File = StorageUtil.getExternalStorageDirectory(fragment.requireContext())

    init {
        // Every time the provided texture view changes, recompute layout
        surfaceCameraSelfie.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
        startCamera()
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = surfaceCameraSelfie.width / 2f
        val centerY = surfaceCameraSelfie.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (surfaceCameraSelfie.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        surfaceCameraSelfie.setTransform(matrix)
    }

    private fun startCamera() {

        CameraX.unbindAll()

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(Rational(1, 1))
            setTargetResolution(Size(surfaceCameraSelfie.width, surfaceCameraSelfie.height))
            setLensFacing(lensFacing)
        }.build()

        // Build the viewfinder use case
        preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview?.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = surfaceCameraSelfie.parent as ViewGroup
            parent.removeView(surfaceCameraSelfie)
            parent.addView(surfaceCameraSelfie, 0)

            surfaceCameraSelfie.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(lifecycleOwner, preview)

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
                .apply {
                    setTargetAspectRatio(Rational(1, 1))
                    setLensFacing(lensFacing)
                    // We don't set a resolution for image capture; instead, we
                    // select a capture mode which will infer the appropriate
                    // resolution based autoToOn aspect ration and requested mode
                    setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                }.build()

        // Build the image capture use case and attach button click listener
        imageCapture = ImageCapture(imageCaptureConfig)

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(lifecycleOwner, preview)

        // Setup image analysis pipeline that computes average pixel luminance
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            // Use a worker thread for image analysis to prevent glitches
            val analyzerThread = HandlerThread(
                    "LuminosityAnalysis"
            ).apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            // In our analysis, we care more about the latest image than
            // analyzing *every* image
            setImageReaderMode(
                    ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE
            )
        }.build()

        // Build the image analysis use case and instantiate our analyzer
        imageAnalyzer = ImageAnalysis(analyzerConfig).apply {
            analyzer = LuminosityAnalyzer()

        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        CameraX.bindToLifecycle(lifecycleOwner, preview, imageCapture, imageAnalyzer)
    }

    /**
     * Get Picture
     */
    fun getPicture(callback: OnCaptureCallback) {
        this.callback = callback
        imageCapture?.let { imageCapture ->
            // Create output file to hold the image
            val photoFile = File(fragment.requireActivity().externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")
            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {
                // Mirror image when using the front camera
                isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
            }

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(photoFile, imageSavedListener, metadata)

            // We can only change the foreground Drawable using API level 23+ API
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

}