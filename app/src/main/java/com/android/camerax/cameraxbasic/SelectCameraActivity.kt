package com.android.camerax.cameraxbasic

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_select_camera.*
import quanticheart.com.cameraview.CameraView

class SelectCameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_camera)

        btnG.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        btnN.setOnClickListener { CameraView.startSelfie(this, 1000) }
    }
}
