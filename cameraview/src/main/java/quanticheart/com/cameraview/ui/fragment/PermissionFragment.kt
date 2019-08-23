package quanticheart.com.cameraview.ui.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import quanticheart.com.cameraview.R
import quanticheart.com.cameraview.ui.base.FragmentCameraBase

class PermissionFragment : FragmentCameraBase() {

    override fun setViewID(): Int? = R.layout.fragment_permission

    override fun onViewFinishLoad(view: View, savedInstanceState: Bundle?) {
        if (!hasPermissions(requireContext())) {
            // Request camera-related permissions
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        } else {
            // If permissions have already been granted, proceed
            goToCamera()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Take the user to the success fragment when permission is granted
                goToCamera()
            } else {
                activity?.finish()
            }
        }
    }
}