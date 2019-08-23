package quanticheart.com.cameraview.ui

import android.os.Bundle
import android.view.KeyEvent
import kotlinx.android.synthetic.main.activity_camera.*
import quanticheart.com.cameraview.R
import quanticheart.com.cameraview.broadcast.BroadcastHelper
import quanticheart.com.cameraview.extention.FLAGS_FULLSCREEN
import quanticheart.com.cameraview.ui.base.CameraBase

internal class CameraActivity : CameraBase() {

    private val IMMERSIVE_FLAG_TIMEOUT = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        ContainerFragmet.postDelayed({ ContainerFragmet.systemUiVisibility = FLAGS_FULLSCREEN }, IMMERSIVE_FLAG_TIMEOUT)
    }

    /** When key down event is triggered, relay it via local broadcast so fragments can handle it */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP -> {
                BroadcastHelper.sendClickAction(this, keyCode)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}