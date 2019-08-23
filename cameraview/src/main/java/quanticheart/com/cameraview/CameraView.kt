package quanticheart.com.cameraview

import android.app.Activity
import android.content.Intent
import quanticheart.com.cameraview.contants.CameraKeys
import quanticheart.com.cameraview.ui.CameraActivity

object CameraView {
    fun startSelfie(activity: Activity, requestCode: Int) {
        val intent = Intent(activity, CameraActivity::class.java)
        intent.action = CameraKeys.SELFIE_MODE
        activity.startActivityForResult(intent, requestCode)
    }
}