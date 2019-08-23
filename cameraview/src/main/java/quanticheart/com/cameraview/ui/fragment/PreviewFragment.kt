package quanticheart.com.cameraview.ui.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_camera_selfie_preview.*
import quanticheart.com.cameraview.R
import java.io.File

class PreviewFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_camera_selfie_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val string = arguments?.getString("KEY_TITLE")
        Log.e("URI", string!!)

        Glide.with(view)
                .load(File(Uri.parse(string).path))
                .into(imgCameraSelfie)

        btnCameraSelfieCancel.setOnClickListener { fragmentManager?.popBackStack() }
    }

}