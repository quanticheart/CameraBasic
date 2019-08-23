package quanticheart.com.cameraview.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.util.Log
import quanticheart.com.cameraview.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object StorageUtil {

    /**Check If SD Card is present or not method */
    private val isSDCardPresent: Boolean = (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)

    /** Folder name */
    private const val directoryName = "Movida/"

    /**
     * Create file in external card, if not exits, create in app Repo
     *
     * @param context for verify app package
     */
    fun getExternalStorageDirectory(context: Context): File {
        //Get File if SD card is present
        if (isSDCardPresent) {
            val apkStorage = File(Environment.getExternalStorageDirectory().toString() + "/" + directoryName)
            //If File is not present create directory
            if (!apkStorage.exists()) {
                apkStorage.mkdir()
                Log.w(ContentValues.TAG, "Directory Created.")
            } else {
                Log.w(ContentValues.TAG, "$apkStorage Directory Exists.")
            }
            return apkStorage
        } else
            Log.w(ContentValues.TAG, "SDCard No Exists.")
        return getOutputDirectory(context)
    }

    /** Use external media if it is available, our app's file directory otherwise */
    private fun getOutputDirectory(context: Context): File {
        val appContext = context.applicationContext
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else appContext.filesDir
    }


    private const val TAG = "CameraXBasic"
    private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
    private const val PHOTO_EXTENSION = ".jpg"

//    fun setGalleryThumbnail(view: ImageView, file: File) {
//        // Run the operations in the view's thread
//        view.post {
//
//            // Remove thumbnail padding
//            view.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())
//
//            // Load thumbnail into circular button using Glide
//            Glide.with(view)
//                    .load(file)
//                    .apply(RequestOptions.circleCropTransform())
//                    .into(view)
//        }
//    }

    /** Helper function used to create a timestamped file */
    fun createFile(baseFolder: File) =
            File(baseFolder, SimpleDateFormat(FILENAME, Locale.US)
                    .format(System.currentTimeMillis()) + PHOTO_EXTENSION)
}