package quanticheart.com.cameraview.project

import android.view.View
import androidx.navigation.findNavController
import quanticheart.com.cameraview.R

object ProjectActivity {

    fun goToPermission(view: View) {
        view.findNavController().navigate(R.id.permisionFragment)
    }

    fun goToCamera(view: View) {
        view.findNavController().navigate(R.id.cameraFragment)
    }

    fun goToPreview(view: View) {
        view.findNavController().navigate(R.id.previewFragment)
    }
}