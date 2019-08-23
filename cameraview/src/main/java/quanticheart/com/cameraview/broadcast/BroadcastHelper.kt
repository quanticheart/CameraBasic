package quanticheart.com.cameraview.broadcast

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.KeyEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class BroadcastHelper(activity: Activity, private val callback: OnClickActionReceiver?) {

    private var broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(activity)

    /** Volume down button receiver used to trigger shutter */
    private var volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (val keyCode = intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP -> {
                    callback?.receiveAction(keyCode)
                }
            }
        }
    }

    companion object {
        val KEY_EVENT_ACTION = "key_event_action"
        val KEY_EVENT_EXTRA = "key_event_extra"
        fun sendClickAction(activity: Activity, keyCode: Int) {
            val intent = Intent(KEY_EVENT_ACTION).apply { putExtra(KEY_EVENT_EXTRA, keyCode) }
            LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)
        }
    }

    interface OnClickActionReceiver {
        fun receiveAction(keyCode: Int)
    }

    init {
        // Set up the intent filter that will receive events from our main activity
        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        with(broadcastManager) {
            registerReceiver(volumeDownReceiver, filter)
        }
    }

    fun unregisterReceiverKeyEvent() = broadcastManager.unregisterReceiver(volumeDownReceiver)


}