package com.restujulian07.scanner_rec

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import com.restujulian07.scanner_rec.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    // View Binding
    private lateinit var binding        : ActivityMainBinding

    private lateinit var mCameraSource  : CameraSource
    private lateinit var textRecognizer : TextRecognizer
    private val tag                     : String = "TAG"
    private var RESULT_NIK              : String? = "false"
    private var RESULT_NAMA             : String? = "false"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Create text recognition
        textRecognizer = TextRecognizer.Builder(this).build()
        if (!textRecognizer.isOperational)
        {
            Toast.makeText(this, "Dependencies are not loaded yet...please try after few moment!!", Toast.LENGTH_SHORT).show()
            Log.e(tag, "Dependencies are downloading....try after few moment")
            return
        }

        //  Init camera source to use high resolution and auto focus
        mCameraSource = CameraSource.Builder(applicationContext, textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 1024)
                .setAutoFocusEnabled(true)
                .setRequestedFps(2.0f)
                .build()

        binding.surfaceCameraPreview.holder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder)
            {
                try {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.CAMERA), 123)
                    } else {
                        mCameraSource.start(binding.surfaceCameraPreview.holder)
                    }
                } catch (e: Exception) {
                    toast("Error:  ${e.message}")
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { }

            override fun surfaceDestroyed(holder: SurfaceHolder)
            {
                mCameraSource.stop()
            }
        })

        textRecognizer.setProcessor(object : Detector.Processor<TextBlock> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<TextBlock>) {
                val items = detections.detectedItems

                if (items.size() <= 0) {
                    return
                }

                binding.tvResult.post {
                    val stringBuilder = StringBuilder()
                    for (i in 0 until items.size()) {
                        val item = items.valueAt(i)
                        stringBuilder.append(item.value)
                        stringBuilder.append("\n")
                    }
                    generateIDCard(stringBuilder.toString())
                }
            }
        })
    }

    // Method detector
    fun generateIDCard(text: String) {

        if (text.contains("NIK")) {
            val arrNik1 = text.split("NIK").toTypedArray()
            for (nikIn in arrNik1)
            {
                if (nikIn.contains("Nama")) {
                    val arrNik2 = nikIn.split("Nama").toTypedArray()
                    if (arrNik2.count() > 1) {
                        val nik         = arrNik2[0].replace(":", "")
                        if (nik.count() == 18) {
                            RESULT_NIK  = nik.replace("o", "0")
                                    .replace("L", "1")
                                    .replace("l", "1")
                                    .replace("i", "1")
                                    .replace("I", "1")
                                    .replace("O", "0")
                                    .replace("D", "0")
                                    .replace("?", "7")

                            val arrNama1 = text.split(nik).toTypedArray()
                            if (arrNama1.count() > 1) {
                                if (arrNama1[1].contains("Nama")) {
                                    val arrNama2 = arrNama1[1].replace("Nama", "|").split("|").toTypedArray()
                                    if (arrNama2.count() > 1) {
                                        if (arrNama2[1].contains("Tempat/Tgl Lahir")) {
                                            val arrNama3 = arrNama2[1].split("Tempat/Tgl Lahir").toTypedArray()
                                            if (arrNama3.count() > 0) {
                                                if (arrNama3[0].length > 2) {
                                                    RESULT_NAMA = if (arrNama3[0].replace(":", "").contains("-")) {
                                                        "false"
                                                    } else {
                                                        if (arrNama3[0].replace(":", "").contains(",")) {
                                                            "false"
                                                        } else {
                                                            if (arrNama3[0].replace(":", "").contains(":")) {
                                                                "false"
                                                            } else {
                                                                arrNama3[0].replace(":", "")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (RESULT_NIK != "false" && RESULT_NAMA != "false") {
            if(RESULT_NIK?.length == 18) {
                binding.nik.text    = RESULT_NIK
                binding.nama.text   = RESULT_NAMA

                binding.simpan.setOnClickListener {
                    if (RESULT_NIK != "false" && RESULT_NAMA != "false") {
                        Toast.makeText(this, "${RESULT_NIK} ${RESULT_NAMA}", Toast.LENGTH_SHORT).show()
                        val intent = intent
                        finish()
                        startActivity(intent)
                    }
                }
            }
            // binding.tvResult.text = "NIK : ${RESULT_NIK} / Nama : ${RESULT_NAMA}"
        } else {
            binding.tvResult.text   = "RESULT"
        }
    }

    // Method toast
    fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "CAMERA Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "CAMERA Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
