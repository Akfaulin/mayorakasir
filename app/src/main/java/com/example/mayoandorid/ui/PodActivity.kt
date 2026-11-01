package com.example.mayoandorid.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import com.example.mayoandorid.R
import com.example.mayoandorid.api.ApiClient
import com.example.mayoandorid.model.ApiResponse
import com.example.mayoandorid.model.BulkPodRequest
import com.example.mayoandorid.model.Order
import com.example.mayoandorid.model.PodRequest
import com.example.mayoandorid.util.ImageUtils
import com.example.mayoandorid.view.SignatureView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PodActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1001
    }

    private var orders: List<Order> = emptyList()

    private lateinit var tvOrderTitle: TextView
    private lateinit var cardSelectedOrdersSummary: CardView
    private lateinit var tvBulkSummaryHeader: TextView
    private lateinit var tvBulkOrdersList: TextView

    private lateinit var btnBack: ImageButton
    private lateinit var btnTakePhotoCamera: MaterialButton
    private lateinit var btnChooseGallery: MaterialButton
    private lateinit var btnClearPhoto: MaterialButton
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var layoutPhotoPlaceholder: View
    private lateinit var signatureView: SignatureView
    private lateinit var btnClearSignature: MaterialButton
    private lateinit var btnSubmitPod: MaterialButton
    private lateinit var etRecipientName: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var progressBarPod: ProgressBar
    private lateinit var podScrollView: NestedScrollView

    private var capturedPhotoBitmap: Bitmap? = null
    private var tempCameraFile: File? = null
    private var tempCameraUri: Uri? = null

    // Direct camera capture launcher
    private val cameraLauncher: ActivityResultLauncher<Uri> = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess && tempCameraFile != null && tempCameraFile!!.exists()) {
            val bmp = ImageUtils.decodeAndRotateFile(tempCameraFile!!.absolutePath, 1024)
            if (bmp != null) {
                capturedPhotoBitmap = bmp
                ivPhotoPreview.setImageBitmap(capturedPhotoBitmap)
                layoutPhotoPlaceholder.visibility = View.GONE
                btnClearPhoto.visibility = View.VISIBLE
            }
        }
    }

    // Gallery launcher
    private val galleryLauncher: ActivityResultLauncher<String> = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bmp = ImageUtils.uriToBitmap(this, uri, 1024)
            if (bmp != null) {
                capturedPhotoBitmap = bmp
                ivPhotoPreview.setImageBitmap(capturedPhotoBitmap)
                layoutPhotoPlaceholder.visibility = View.GONE
                btnClearPhoto.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pod)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val podRoot: View = findViewById(R.id.podRoot)
        val podHeaderBar: View = findViewById(R.id.podHeaderBar)
        podScrollView = findViewById(R.id.podScrollView)
        val initialTopPadding = podHeaderBar.paddingTop

        var lastImeBottom = 0

        ViewCompat.setOnApplyWindowInsetsListener(podRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            // Fixed header status bar inset
            podHeaderBar.updatePadding(top = systemBars.top + initialTopPadding)

            // Dynamic bottom inset: when keyboard opens, ime.bottom is non-zero
            val bottomPadding = maxOf(systemBars.bottom, ime.bottom)
            podScrollView.updatePadding(bottom = bottomPadding)

            if (ime.bottom > 0 && ime.bottom != lastImeBottom) {
                // Keyboard popped up: auto-scroll to the active input so it is never covered
                podScrollView.postDelayed({
                    val focused = currentFocus
                    if (focused != null && (focused == etRecipientName || focused == etNotes)) {
                        scrollToTargetView(focused)
                    } else {
                        scrollToTargetView(etRecipientName)
                    }
                }, 150)
            }
            lastImeBottom = ime.bottom

            insets
        }

        @Suppress("UNCHECKED_CAST")
        val ordersFromIntent = intent.getSerializableExtra("orders") as? ArrayList<Order>
        val singleOrder = intent.getSerializableExtra("order") as? Order

        orders = when {
            !ordersFromIntent.isNullOrEmpty() -> ordersFromIntent
            singleOrder != null -> listOf(singleOrder)
            else -> emptyList()
        }

        if (orders.isEmpty()) {
            Toast.makeText(this, "Data pesanan tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupEvents()
    }

    private fun initViews() {
        tvOrderTitle = findViewById(R.id.tvOrderTitle)
        cardSelectedOrdersSummary = findViewById(R.id.cardSelectedOrdersSummary)
        tvBulkSummaryHeader = findViewById(R.id.tvBulkSummaryHeader)
        tvBulkOrdersList = findViewById(R.id.tvBulkOrdersList)

        btnBack = findViewById(R.id.btnBack)
        btnTakePhotoCamera = findViewById(R.id.btnTakePhotoCamera)
        btnChooseGallery = findViewById(R.id.btnChooseGallery)
        btnClearPhoto = findViewById(R.id.btnClearPhoto)
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview)
        layoutPhotoPlaceholder = findViewById(R.id.layoutPhotoPlaceholder)
        signatureView = findViewById(R.id.signatureView)
        btnClearSignature = findViewById(R.id.btnClearSignature)
        btnSubmitPod = findViewById(R.id.btnSubmitPod)
        etRecipientName = findViewById(R.id.etRecipientName)
        etNotes = findViewById(R.id.etNotes)
        progressBarPod = findViewById(R.id.progressBarPod)

        val firstOrder = orders.first()
        val store = firstOrder.store_name ?: "Toko Reseller"

        if (orders.size > 1) {
            tvOrderTitle.text = "${orders.size} Pesanan Sekaligus · $store"
            cardSelectedOrdersSummary.visibility = View.VISIBLE
            tvBulkSummaryHeader.text = "📦 Serah Terima Sekaligus (${orders.size} Pesanan)"
            val orderCodes = orders.joinToString(", ") { "#" + (it.calculation_code ?: it.id.toString()) }
            tvBulkOrdersList.text = orderCodes

            btnSubmitPod.text = "Simpan POD (${orders.size} Pesanan)"

            val defaultRecipient = firstOrder.recipient_name ?: (firstOrder.store_name ?: "")
            etRecipientName.setText(defaultRecipient)
            etNotes.setText("Serah terima sekaligus untuk ${orders.size} pesanan: $orderCodes")
        } else {
            val code = firstOrder.calculation_code ?: firstOrder.id.toString()
            tvOrderTitle.text = "#$code · $store"
            cardSelectedOrdersSummary.visibility = View.GONE
            btnSubmitPod.text = "Simpan Bukti Pengiriman (POD)"

            val defaultRecipient = firstOrder.recipient_name ?: (firstOrder.store_name ?: "")
            etRecipientName.setText(defaultRecipient)
        }
    }

    private fun setupEvents() {
        btnBack.setOnClickListener { finish() }

        btnTakePhotoCamera.setOnClickListener { checkCameraPermissionAndOpen() }
        btnChooseGallery.setOnClickListener { galleryLauncher.launch("image/*") }

        btnClearPhoto.setOnClickListener {
            capturedPhotoBitmap = null
            ivPhotoPreview.setImageDrawable(null)
            layoutPhotoPlaceholder.visibility = View.VISIBLE
            btnClearPhoto.visibility = View.GONE
        }

        btnClearSignature.setOnClickListener { signatureView.clear() }
        btnSubmitPod.setOnClickListener { submitPod() }

        etRecipientName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                podScrollView.postDelayed({
                    scrollToTargetView(etRecipientName)
                }, 150)
            }
        }
        etRecipientName.setOnClickListener {
            podScrollView.postDelayed({
                scrollToTargetView(etRecipientName)
            }, 150)
        }

        etNotes.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                podScrollView.postDelayed({
                    scrollToTargetView(etNotes)
                }, 150)
            }
        }
        etNotes.setOnClickListener {
            podScrollView.postDelayed({
                scrollToTargetView(etNotes)
            }, 150)
        }
    }

    private fun scrollToTargetView(targetView: View) {
        var top = 0
        var current: View? = targetView
        while (current != null && current != podScrollView) {
            top += current.top
            current = current.parent as? View
        }
        podScrollView.smoothScrollTo(0, maxOf(0, top - 80))
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Izin kamera diperlukan untuk mengambil foto langsung.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openCamera() {
        try {
            val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: cacheDir
            tempCameraFile = File(dir, "POD_${System.currentTimeMillis()}.jpg")
            tempCameraUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", tempCameraFile!!)
            cameraLauncher.launch(tempCameraUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menyiapkan kamera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitPod() {
        if (orders.isEmpty()) return
        val recipientName = etRecipientName.text?.toString()?.trim().orEmpty()
        val notes = etNotes.text?.toString()?.trim().orEmpty()

        if (recipientName.isEmpty()) {
            etRecipientName.error = "Nama penerima wajib diisi"
            etRecipientName.requestFocus()
            return
        }

        val photoBmp = capturedPhotoBitmap
        if (photoBmp == null) {
            Toast.makeText(this, "Foto bukti serah terima wajib diambil (bisa foto langsung atau galeri)!", Toast.LENGTH_LONG).show()
            return
        }

        if (!signatureView.hasSignature()) {
            Toast.makeText(this, "Tanda tangan penerima pada kotak putih wajib diisi!", Toast.LENGTH_LONG).show()
            return
        }

        btnSubmitPod.isEnabled = false
        progressBarPod.visibility = View.VISIBLE

        val photoBase64 = ImageUtils.bitmapToBase64(photoBmp, 80)
        val signatureBase64 = signatureView.getSignatureBase64()

        val orderIds = orders.map { it.id }
        val api = ApiClient.getService(this)

        if (orderIds.size > 1) {
            val bulkRequest = BulkPodRequest(
                order_ids = orderIds,
                recipient_name = recipientName,
                pod_photo = photoBase64,
                pod_signature = signatureBase64,
                notes = notes
            )
            api.submitBulkPod(bulkRequest).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        btnSubmitPod.isEnabled = true
                        progressBarPod.visibility = View.GONE
                        Toast.makeText(this@PodActivity, "Bukti Pengiriman (POD) untuk ${orderIds.size} pesanan berhasil direkam!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        fallbackSequentialPod(orderIds, recipientName, photoBase64, signatureBase64, notes)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    fallbackSequentialPod(orderIds, recipientName, photoBase64, signatureBase64, notes)
                }
            })
        } else {
            val req = PodRequest(recipientName, photoBase64, signatureBase64, notes)
            api.submitPod(orderIds[0], req).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    btnSubmitPod.isEnabled = true
                    progressBarPod.visibility = View.GONE

                    if (response.isSuccessful) {
                        Toast.makeText(this@PodActivity, "Bukti Pengiriman (POD) berhasil direkam. Pesanan selesai!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@PodActivity, "Gagal merekam POD: ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    btnSubmitPod.isEnabled = true
                    progressBarPod.visibility = View.GONE
                    Toast.makeText(this@PodActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun fallbackSequentialPod(
        orderIds: List<Long>,
        recipientName: String,
        photoBase64: String?,
        signatureBase64: String?,
        notes: String?
    ) {
        val api = ApiClient.getService(this)
        var completed = 0
        var failed = 0

        for (id in orderIds) {
            api.submitPod(id, PodRequest(recipientName, photoBase64, signatureBase64, notes)).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) completed++ else failed++
                    checkFinish()
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    failed++
                    checkFinish()
                }

                private fun checkFinish() {
                    if (completed + failed >= orderIds.size) {
                        btnSubmitPod.isEnabled = true
                        progressBarPod.visibility = View.GONE
                        if (completed > 0) {
                            Toast.makeText(this@PodActivity, "Bukti Pengiriman (POD) untuk $completed pesanan berhasil direkam!", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this@PodActivity, "Gagal merekam POD. Periksa koneksi ke server.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }
    }
}
