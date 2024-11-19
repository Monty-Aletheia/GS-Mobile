package com.example.windrose.ui

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.graphics.drawable.ColorDrawable
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.windrose.R
import com.example.windrose.databinding.ActivityDeviceFinderBinding
import com.example.windrose.databinding.EnterDeviceBottomSheetBinding
import com.example.windrose.ml.AutoModel2
import com.example.windrose.network.API
import com.example.windrose.network.DeviceDTO
import com.example.windrose.network.UserDeviceDTO
import com.example.windrose.network.UserDeviceListDTO
import com.example.windrose.repository.UserRepository.getUserIdByFirebaseUid
import com.example.windrose.utils.InputFormatter.inputTextToEstimatedHours
import com.example.windrose.utils.InputFormatter.openTimePickerDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class DeviceFinderActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityDeviceFinderBinding

    lateinit var model: AutoModel2
    val paint = Paint()
    lateinit var labels: List<String>
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    )
    lateinit var imageProcessor: ImageProcessor
    lateinit var bitmap: Bitmap
    lateinit var textureView: TextureView
    lateinit var imageView: ImageView

    lateinit var cameraDevice: CameraDevice
    lateinit var cameraManager: CameraManager
    lateinit var handler: Handler

    private var scores: FloatArray = floatArrayOf()
    private var locations: FloatArray = floatArrayOf()
    private var classes: FloatArray = floatArrayOf()

    private val allowedDetectionsList: List<String> =
        listOf(
            "Televisão",
            "Notebook",
            "Celular",
            "Micro-ondas",
            "Fogão",
            "Torradeira",
            "Geladeira",
            "Relógio",
            "Secador de Cabelo"
        )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        auth = Firebase.auth
        binding = ActivityDeviceFinderBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getPermission()

        labels = FileUtil.loadLabels(this, "labels.txt")
        imageProcessor =
            ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()

        model = AutoModel2.newInstance(this)

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageView = binding.imageView
        textureView = binding.textureView

        imageView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                val x = event.x
                val y = event.y

                val h = bitmap.height
                val w = bitmap.width
                var isClickedOnBox = false

                scores.forEachIndexed { index, score ->
                    if (score > 0.5) {
                        val boxIndex = index * 4
                        val top = locations[boxIndex] * h
                        val left = locations[boxIndex + 1] * w
                        val bottom = locations[boxIndex + 2] * h
                        val right = locations[boxIndex + 3] * w

                        if (x >= left && x <= right && y >= top && y <= bottom) {
                            val className = labels[classes[index].toInt()]

                            if (className in allowedDetectionsList) {
                                showBottomSheetDialog(className)
                            } else {
                                Toast.makeText(
                                    this,
                                    "Por favor, clique em um eletrodoméstico.".trimIndent(),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            isClickedOnBox = true
                        }
                    }
                }
                if (!isClickedOnBox) {
                    Log.d("BoundingBoxClick", "Clique fora de qualquer bounding box.")
                }
            }
            true
        }


        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                bitmap = textureView.bitmap!!

                val image = TensorImage.fromBitmap(bitmap)

                val outputs = model.process(image)

                locations = outputs.locationAsTensorBuffer.floatArray
                classes = outputs.categoryAsTensorBuffer.floatArray
                scores = outputs.scoreAsTensorBuffer.floatArray
                var numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)

                val h = mutable.height
                val w = mutable.width
                paint.textSize = h / 15f
                paint.strokeWidth = h / 85f
                var x = 0
                scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4

                    if (fl > 0.5) {
                        paint.setColor(colors.get(index))
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(
                            RectF(
                                locations.get(x + 1) * w,
                                locations.get(x) * h,
                                locations.get(x + 3) * w,
                                locations.get(x + 2) * h
                            ), paint
                        )
                        paint.style = Paint.Style.FILL
                        canvas.drawText(
                            labels.get(
                                classes.get(index).toInt()

                            )
                                    + " " + fl.toString(),
                            locations.get(x + 1) * w,
                            locations.get(x) * h,
                            paint
                        )
                    }
                }

                imageView.setImageBitmap(mutable)

            }

        }
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private fun showBottomSheetDialog(itemName: String) {
        val dialog = BottomSheetDialog(this)

        val sheetBinding: EnterDeviceBottomSheetBinding =
            EnterDeviceBottomSheetBinding
                .inflate(layoutInflater, null, false)

        sheetBinding.itemName.text = itemName

        val diaryUsageInput = sheetBinding.diaryUsageEditText

        val cancelButton = sheetBinding.cancelButton

        val confirmButton = sheetBinding.confirmButton
        confirmButton.isEnabled = false

        diaryUsageInput.doOnTextChanged { text, _, _, _ ->
            confirmButton.isEnabled = !text.isNullOrEmpty()
        }

        diaryUsageInput.setOnClickListener {
            openTimePickerDialog(diaryUsageInput, this)
        }

        confirmButton.setOnClickListener {

            val inputEstimatedUsageHours = sheetBinding.diaryUsageEditText.text.toString()
            val estimatedUsageHours = inputTextToEstimatedHours(inputEstimatedUsageHours)
            getUserDeviceId(itemName, estimatedUsageHours)


        }
        cancelButton.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    private fun getUserDeviceId(itemName: String, estimatedUsageHours: Double) =
        lifecycleScope.launch {

            val user = getUserIdByFirebaseUid(auth.currentUser!!.uid)
            val userId = user!!.id
            val device = getUserDevice(userId, itemName)
            val deviceId = device!!.id
            addUserDevice(userId, deviceId, estimatedUsageHours, itemName)

        }

    private suspend fun addUserDevice(
        userId: String,
        deviceId: String,
        estimatedUsageHours: Double,
        itemName: String
    ) {
        try {

            val buildService = API.buildUserDeviceService()
            val userDeviceDto = UserDeviceDTO(deviceId, estimatedUsageHours)
            val list = listOf(userDeviceDto)
            val userDeviceListDTO = UserDeviceListDTO(list)
            val response = buildService.addUserDevice(userId, userDeviceListDTO)

            if (response.isSuccessful) {
                startActivity(Intent(this, DeviceListActivity::class.java))

                Toast.makeText(
                    this,
                    "Aparelho $itemName adicionado com sucesso!".trimIndent(),
                    Toast.LENGTH_LONG
                ).show()

            } else {
                Toast.makeText(this, "Não foi possivel cadastrar o produto", Toast.LENGTH_LONG)
                    .show()
            }

        } catch (ex: Exception) {
            Toast.makeText(this, ex.message, Toast.LENGTH_LONG).show()
        }
    }

    private suspend fun getUserDevice(userId: String, itemName: String): DeviceDTO? {
        try {
            val buildService = API.buildUserDeviceService()
            val response = buildService.getUserDevicesById(userId)

            if (response.isSuccessful) {
                val devices = response.body()!!.content
                return devices.find { it.name == itemName }
            } else {
                Log.e("API_ERROR", "Failed to fetch consultations")
                return null
            }

        } catch (ex: Exception) {
            Toast.makeText(this, ex.message, Toast.LENGTH_LONG).show()
            return null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::model.isInitialized) {
            model.close()
        }
        closeCamera()
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        cameraManager.openCamera(
            cameraManager.cameraIdList[0],
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    val surfaceTexture = textureView.surfaceTexture
                    val surface = Surface(surfaceTexture)
                    val captureRequest =
                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(surface)

                    cameraDevice.createCaptureSession(
                        listOf(surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                session.setRepeatingRequest(captureRequest.build(), null, null)
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                            }
                        },
                        handler
                    )
                }

                override fun onDisconnected(camera: CameraDevice) {}
                override fun onError(camera: CameraDevice, error: Int) {}
            },
            handler
        )
    }

    private fun getPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission()
        }
    }

    private fun closeCamera() {
        try {
            cameraDevice.close()
        } catch (e: Exception) {
            Log.e("DeviceFinderActivity", "Erro ao fechar a câmera", e)
        }
    }
}