package quanticheart.com.cameraview.ui.base

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import quanticheart.com.cameraview.R
import quanticheart.com.cameraview.ui.fragment.PermissionFragmentDirections

abstract class FragmentCameraBase : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setViewID()?.let { view ->
            return inflater.inflate(view, container, false)
        } ?: run { return super.onCreateView(inflater, container, savedInstanceState) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onViewFinishLoad(view, savedInstanceState)
    }

    abstract fun setViewID(): Int?

    abstract fun onViewFinishLoad(view: View, savedInstanceState: Bundle?)

    fun goToPermission(view: View) {
        val bundle = Bundle().apply {
            putString("KEY_TITLE", "Winds of Winter")
        }
        view.findNavController().navigate(R.id.permisionFragment, bundle)
    }

    fun goToCamera() {
        Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                PermissionFragmentDirections.actionPermisionFragmentToCameraFragment())
    }

    fun goToPreview(view: View, fromFile: Uri) {
        val bundle = Bundle().apply {
            putString("KEY_TITLE", fromFile.toString())
        }
        view.findNavController().navigate(R.id.previewFragment, bundle)
    }

    protected val PERMISSIONS_REQUEST_CODE = 10
    protected val PERMISSIONS_REQUIRED = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO)

    /** Convenience method used to check if all permissions required by this app are granted */
    protected fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}