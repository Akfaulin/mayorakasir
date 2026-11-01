package com.example.mayoandorid.ui

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mayoandorid.R
import com.example.mayoandorid.adapter.CourierReportAdapter
import com.example.mayoandorid.api.ApiClient
import com.example.mayoandorid.model.CourierReportResponse
import com.example.mayoandorid.util.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class CourierReportActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var btnBackReport: ImageButton
    private lateinit var btnExportPdf: MaterialButton
    private lateinit var btnExportExcel: MaterialButton

    private lateinit var tvReportTotalTasks: TextView
    private lateinit var tvReportDelivered: TextView
    private lateinit var tvReportInTransit: TextView
    private lateinit var tvReportTotalQty: TextView
    private lateinit var tvReportIssues: TextView

    private lateinit var chipReportAll: TextView
    private lateinit var chipReportDelivered: TextView
    private lateinit var chipReportTransit: TextView
    private lateinit var chipReportReady: TextView

    private lateinit var etSearchReport: TextInputEditText
    private lateinit var swipeRefreshReport: SwipeRefreshLayout
    private lateinit var rvReportTasks: RecyclerView
    private lateinit var layoutEmptyReport: View

    private lateinit var reportAdapter: CourierReportAdapter
    private var currentFilter: String? = null
    private var currentSearch = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            finish()
            return
        }

        setContentView(R.layout.activity_courier_report)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val reportRoot: View = findViewById(R.id.reportRoot)
        val reportTopBar: View = findViewById(R.id.reportTopBar)
        val initialTopPadding = reportTopBar.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(reportRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            reportTopBar.updatePadding(top = systemBars.top + initialTopPadding)
            reportRoot.updatePadding(bottom = systemBars.bottom)
            insets
        }

        initViews()
        setupEvents()
        loadReport()
    }

    private fun initViews() {
        btnBackReport = findViewById(R.id.btnBackReport)
        btnExportPdf = findViewById(R.id.btnExportPdf)
        btnExportExcel = findViewById(R.id.btnExportExcel)

        tvReportTotalTasks = findViewById(R.id.tvReportTotalTasks)
        tvReportDelivered = findViewById(R.id.tvReportDelivered)
        tvReportInTransit = findViewById(R.id.tvReportInTransit)
        tvReportTotalQty = findViewById(R.id.tvReportTotalQty)
        tvReportIssues = findViewById(R.id.tvReportIssues)

        chipReportAll = findViewById(R.id.chipReportAll)
        chipReportDelivered = findViewById(R.id.chipReportDelivered)
        chipReportTransit = findViewById(R.id.chipReportTransit)
        chipReportReady = findViewById(R.id.chipReportReady)

        etSearchReport = findViewById(R.id.etSearchReport)
        swipeRefreshReport = findViewById(R.id.swipeRefreshReport)
        rvReportTasks = findViewById(R.id.rvReportTasks)
        layoutEmptyReport = findViewById(R.id.layoutEmptyReport)

        rvReportTasks.layoutManager = LinearLayoutManager(this)
        reportAdapter = CourierReportAdapter(this)
        rvReportTasks.adapter = reportAdapter
    }

    private fun setupEvents() {
        btnBackReport.setOnClickListener { finish() }

        swipeRefreshReport.setOnRefreshListener { loadReport() }

        btnExportPdf.setOnClickListener { downloadReportFile("pdf") }
        btnExportExcel.setOnClickListener { downloadReportFile("excel") }

        chipReportAll.setOnClickListener { setFilter(null) }
        chipReportDelivered.setOnClickListener { setFilter("delivered") }
        chipReportTransit.setOnClickListener { setFilter("shipped") }
        chipReportReady.setOnClickListener { setFilter("ready_to_ship") }

        etSearchReport.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s?.toString()?.trim().orEmpty()
                loadReport()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setFilter(filter: String?) {
        currentFilter = filter
        updateChipsUI()
        loadReport()
    }

    private fun updateChipsUI() {
        val activeBg = R.drawable.bg_chip_selected
        val inactiveBg = R.drawable.bg_chip_unselected
        val activeText = Color.WHITE
        val inactiveText = Color.parseColor("#334155")

        chipReportAll.setBackgroundResource(if (currentFilter == null) activeBg else inactiveBg)
        chipReportAll.setTextColor(if (currentFilter == null) activeText else inactiveText)

        chipReportDelivered.setBackgroundResource(if (currentFilter == "delivered") activeBg else inactiveBg)
        chipReportDelivered.setTextColor(if (currentFilter == "delivered") activeText else inactiveText)

        chipReportTransit.setBackgroundResource(if (currentFilter == "shipped") activeBg else inactiveBg)
        chipReportTransit.setTextColor(if (currentFilter == "shipped") activeText else inactiveText)

        chipReportReady.setBackgroundResource(if (currentFilter == "ready_to_ship") activeBg else inactiveBg)
        chipReportReady.setTextColor(if (currentFilter == "ready_to_ship") activeText else inactiveText)
    }

    private fun loadReport() {
        swipeRefreshReport.isRefreshing = true
        val api = ApiClient.getService(this)

        api.getCourierReport(
            status = currentFilter,
            search = currentSearch.ifEmpty { null }
        ).enqueue(object : retrofit2.Callback<CourierReportResponse> {
            override fun onResponse(
                call: retrofit2.Call<CourierReportResponse>,
                response: retrofit2.Response<CourierReportResponse>
            ) {
                swipeRefreshReport.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    val report = response.body()!!

                    report.summary?.let { summary ->
                        tvReportTotalTasks.text = summary.total_tasks.toString()
                        tvReportDelivered.text = summary.delivered_count.toString()
                        tvReportInTransit.text = summary.in_transit_count.toString()
                        tvReportTotalQty.text = summary.total_qty.toString()
                        tvReportIssues.text = summary.issues_count.toString()
                    }

                    val orders = report.tasks?.data ?: emptyList()
                    reportAdapter.setOrders(orders)
                    layoutEmptyReport.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    layoutEmptyReport.visibility = View.VISIBLE
                    Toast.makeText(this@CourierReportActivity, "Gagal memuat laporan pengantaran", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<CourierReportResponse>, t: Throwable) {
                swipeRefreshReport.isRefreshing = false
                layoutEmptyReport.visibility = View.VISIBLE
                Toast.makeText(this@CourierReportActivity, "Koneksi bermasalah: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun downloadReportFile(format: String) {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Menyiapkan dokumen ${format.uppercase()} resmi...")
            setCancelable(false)
            show()
        }

        var baseUrl = sessionManager.getBaseUrl()
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/"
        }
        val url = "${baseUrl}courier/reports/export-$format"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${sessionManager.getToken()}")
            .header("Accept", if (format == "pdf") "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this@CourierReportActivity, "Gagal mengunduh file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful && response.body != null) {
                    val ext = if (format == "pdf") "pdf" else "xlsx"
                    val mimeType = if (format == "pdf") "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    val fileName = "Laporan_Kurir_Mayora_${System.currentTimeMillis()}.$ext"

                    try {
                        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                        file.outputStream().use { out ->
                            response.body!!.byteStream().copyTo(out)
                        }

                        runOnUiThread {
                            progressDialog.dismiss()
                            Toast.makeText(this@CourierReportActivity, "Dokumen $format berhasil diunduh!", Toast.LENGTH_SHORT).show()
                            openDownloadedFile(file, mimeType)
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            progressDialog.dismiss()
                            Toast.makeText(this@CourierReportActivity, "Gagal menyimpan file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        progressDialog.dismiss()
                        Toast.makeText(this@CourierReportActivity, "Gagal mengunduh dari server: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun openDownloadedFile(file: File, mimeType: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(intent, "Buka Dokumen Laporan Mayora"))
        } catch (e: Exception) {
            Toast.makeText(this, "File tersimpan di: ${file.name}", Toast.LENGTH_LONG).show()
        }
    }
}
