package com.example.mayoandorid.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import androidx.cardview.widget.CardView
import com.example.mayoandorid.model.BulkPickupRequest
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mayoandorid.R
import com.example.mayoandorid.adapter.TaskAdapter
import com.example.mayoandorid.api.ApiClient
import com.example.mayoandorid.model.ApiResponse
import com.example.mayoandorid.model.CourierDashboardResponse
import com.example.mayoandorid.model.Fleet
import com.example.mayoandorid.model.IssueRequest
import com.example.mayoandorid.model.Order
import com.example.mayoandorid.model.PickupRequest
import com.example.mayoandorid.model.TaskResponse
import com.example.mayoandorid.util.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CourierMainActivity : AppCompatActivity(), TaskAdapter.OnTaskActionListener {

    private lateinit var sessionManager: SessionManager
    private lateinit var tvCourierName: TextView
    private lateinit var tvFleetModel: TextView
    private lateinit var tvFleetPlate: TextView
    private lateinit var tvFleetCapacity: TextView
    private lateinit var tvCountReady: TextView
    private lateinit var tvCountInTransit: TextView
    private lateinit var tvCountDelivered: TextView
    private lateinit var tvCountIssues: TextView
    private lateinit var chipAll: TextView
    private lateinit var chipReady: TextView
    private lateinit var chipTransit: TextView
    private lateinit var chipDelivered: TextView
    private lateinit var chipIssue: TextView
    private lateinit var cardFilterReady: View
    private lateinit var cardFilterTransit: View
    private lateinit var cardFilterDelivered: View
    private lateinit var cardFilterIssue: View
    private lateinit var etSearch: TextInputEditText
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvTasks: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var btnLogout: ImageButton
    private lateinit var btnReport: ImageButton
    private lateinit var cardReportBanner: View

    private lateinit var layoutSelectAll: View
    private lateinit var cbSelectAll: CheckBox
    private lateinit var tvSelectAllLabel: TextView
    private lateinit var tvSelectAllCount: TextView
    private lateinit var cardBulkAction: CardView
    private lateinit var tvBulkSelectedCount: TextView
    private lateinit var btnCancelSelection: TextView
    private lateinit var btnBulkPickup: MaterialButton

    private lateinit var taskAdapter: TaskAdapter
    private var currentFilter = "all"
    private var currentSearch = ""
    private var activeFleet: Fleet? = null

    private val podLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadDashboardData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_courier_main)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val mainRoot: View = findViewById(R.id.mainRoot)
        val topBarLayout: View = findViewById(R.id.topBarLayout)
        val initialTopPadding = topBarLayout.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            topBarLayout.updatePadding(top = systemBars.top + initialTopPadding)
            mainRoot.updatePadding(bottom = systemBars.bottom)
            insets
        }

        // Secret long click on top bar header to adjust API Server URL during testing
        topBarLayout.setOnLongClickListener {
            showServerConfigDialog()
            true
        }

        initViews()
        setupEvents()
        loadDashboardData()
    }

    private fun initViews() {
        tvCourierName = findViewById(R.id.tvCourierName)
        tvFleetModel = findViewById(R.id.tvFleetModel)
        tvFleetPlate = findViewById(R.id.tvFleetPlate)
        tvFleetCapacity = findViewById(R.id.tvFleetCapacity)

        tvCountReady = findViewById(R.id.tvCountReady)
        tvCountInTransit = findViewById(R.id.tvCountInTransit)
        tvCountDelivered = findViewById(R.id.tvCountDelivered)
        tvCountIssues = findViewById(R.id.tvCountIssues)

        cardFilterReady = findViewById(R.id.cardFilterReady)
        cardFilterTransit = findViewById(R.id.cardFilterTransit)
        cardFilterDelivered = findViewById(R.id.cardFilterDelivered)
        cardFilterIssue = findViewById(R.id.cardFilterIssue)

        chipAll = findViewById(R.id.chipAll)
        chipReady = findViewById(R.id.chipReady)
        chipTransit = findViewById(R.id.chipTransit)
        chipDelivered = findViewById(R.id.chipDelivered)
        chipIssue = findViewById(R.id.chipIssue)

        etSearch = findViewById(R.id.etSearch)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvTasks = findViewById(R.id.rvTasks)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        btnLogout = findViewById(R.id.btnLogout)
        btnReport = findViewById(R.id.btnReport)
        cardReportBanner = findViewById(R.id.cardReportBanner)

        layoutSelectAll = findViewById(R.id.layoutSelectAll)
        cbSelectAll = findViewById(R.id.cbSelectAll)
        tvSelectAllLabel = findViewById(R.id.tvSelectAllLabel)
        tvSelectAllCount = findViewById(R.id.tvSelectAllCount)
        cardBulkAction = findViewById(R.id.cardBulkAction)
        tvBulkSelectedCount = findViewById(R.id.tvBulkSelectedCount)
        btnCancelSelection = findViewById(R.id.btnCancelSelection)
        btnBulkPickup = findViewById(R.id.btnBulkPickup)

        tvCourierName.text = sessionManager.getUserName()

        rvTasks.layoutManager = LinearLayoutManager(this)
        taskAdapter = TaskAdapter(this, this)
        rvTasks.adapter = taskAdapter
    }

    private fun setupEvents() {
        swipeRefresh.setOnRefreshListener { loadDashboardData() }

        btnReport.setOnClickListener {
            startActivity(Intent(this, CourierReportActivity::class.java))
        }

        cardReportBanner.setOnClickListener {
            startActivity(Intent(this, CourierReportActivity::class.java))
        }

        cbSelectAll.setOnClickListener {
            taskAdapter.selectAll(cbSelectAll.isChecked, taskAdapter.isPodSelection())
        }

        btnCancelSelection.setOnClickListener {
            taskAdapter.clearSelection()
        }

        btnBulkPickup.setOnClickListener {
            val selected = taskAdapter.getSelectedOrders()
            if (selected.isEmpty()) return@setOnClickListener

            if (taskAdapter.isPodSelection()) {
                openBulkPod(selected)
            } else {
                val plate = activeFleet?.plate_number ?: "AG 9421 PQR"
                val count = selected.size
                val totalWeight = selected.sumOf { if (it.package_weight > 0) it.package_weight else (it.shipment?.weight_kg ?: 25.0) }
                val totalKoli = selected.sumOf { if (it.total_qty > 0) it.total_qty else 1 }

                AlertDialog.Builder(this)
                    .setTitle("Serah Terima dari Gudang")
                    .setMessage("Konfirmasi serah terima fisik $count pesanan sekaligus dari gudang dan muat ke mobil box armada ($plate)?\n\n• Jumlah Muatan: $totalKoli Koli\n• Estimasi Berat: ${String.format("%.1f", totalWeight)} kg\n• Armada: $plate")
                    .setPositiveButton("Konfirmasi & Muat ($count)") { _, _ ->
                        executeBulkPickup(selected.map { it.id }, plate)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Keluar Akun")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi Kurir?")
                .setPositiveButton("Ya, Keluar") { _, _ ->
                    sessionManager.logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }


        // Chip Filters
        chipAll.setOnClickListener { setFilter("all") }
        chipReady.setOnClickListener { setFilter("ready_to_pick") }
        chipTransit.setOnClickListener { setFilter("in_transit") }
        chipDelivered.setOnClickListener { setFilter("delivered") }
        chipIssue.setOnClickListener { setFilter("issue") }

        // Cards filters
        cardFilterReady.setOnClickListener { setFilter("ready_to_pick") }
        cardFilterTransit.setOnClickListener { setFilter("in_transit") }
        cardFilterDelivered.setOnClickListener { setFilter("delivered") }
        cardFilterIssue.setOnClickListener { setFilter("issue") }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s?.toString()?.trim().orEmpty()
                loadTasks()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        taskAdapter.setFilterMode(filter)
        taskAdapter.clearSelection()
        updateChipsUI()
        loadTasks()
    }

    private fun updateChipsUI() {
        val activeBg = R.drawable.bg_chip_selected
        val inactiveBg = R.drawable.bg_chip_unselected
        val activeText = Color.WHITE
        val inactiveText = Color.parseColor("#334155")

        chipAll.setBackgroundResource(if (currentFilter == "all") activeBg else inactiveBg)
        chipAll.setTextColor(if (currentFilter == "all") activeText else inactiveText)

        chipReady.setBackgroundResource(if (currentFilter == "ready_to_pick") activeBg else inactiveBg)
        chipReady.setTextColor(if (currentFilter == "ready_to_pick") activeText else inactiveText)

        chipTransit.setBackgroundResource(if (currentFilter == "in_transit") activeBg else inactiveBg)
        chipTransit.setTextColor(if (currentFilter == "in_transit") activeText else inactiveText)

        chipDelivered.setBackgroundResource(if (currentFilter == "delivered") activeBg else inactiveBg)
        chipDelivered.setTextColor(if (currentFilter == "delivered") activeText else inactiveText)

        chipIssue.setBackgroundResource(if (currentFilter == "issue") activeBg else inactiveBg)
        chipIssue.setTextColor(if (currentFilter == "issue") activeText else inactiveText)
    }

    private fun loadDashboardData() {
        swipeRefresh.isRefreshing = true
        val api = ApiClient.getService(this)

        api.getDashboard().enqueue(object : Callback<CourierDashboardResponse> {
            override fun onResponse(call: Call<CourierDashboardResponse>, response: Response<CourierDashboardResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val dash = response.body()!!
                    dash.summary?.let { summary ->
                        tvCountReady.text = summary.ready_to_pick.toString()
                        tvCountInTransit.text = summary.in_transit.toString()
                        tvCountDelivered.text = summary.delivered_today.toString()
                        tvCountIssues.text = summary.reported_issues.toString()
                    }

                    dash.active_fleet?.let { fleet ->
                        activeFleet = fleet
                        tvFleetModel.text = fleet.model_name ?: "Daihatsu Gran Max Box 1.5 DOHC"
                        tvFleetPlate.text = fleet.plate_number ?: "AG 9421 PQR"
                        tvFleetCapacity.text = "Kapasitas: ${fleet.capacity_kg} kg · Mobil Box Operasional Multi-Drop"
                    }
                }
                loadTasks()
            }

            override fun onFailure(call: Call<CourierDashboardResponse>, t: Throwable) {
                loadTasks()
                Toast.makeText(this@CourierMainActivity, "Gagal memuat dashboard: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadTasks() {
        val api = ApiClient.getService(this)
        api.getTasks(currentFilter, currentSearch).enqueue(object : Callback<TaskResponse> {
            override fun onResponse(call: Call<TaskResponse>, response: Response<TaskResponse>) {
                swipeRefresh.isRefreshing = false
                if (response.isSuccessful && response.body() != null) {
                    val tasks = response.body()!!.data ?: emptyList()
                    taskAdapter.setOrders(tasks)
                    layoutEmpty.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    layoutEmpty.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<TaskResponse>, t: Throwable) {
                swipeRefresh.isRefreshing = false
                Toast.makeText(this@CourierMainActivity, "Gagal memuat daftar tugas: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onPickup(order: Order) {
        val code = order.calculation_code ?: order.id.toString()
        val plate = activeFleet?.plate_number ?: "AG 9421 PQR"

        AlertDialog.Builder(this)
            .setTitle("Serah Terima dari Gudang")
            .setMessage("Konfirmasi serah terima fisik paket pesanan #$code dari gudang dan muat ke mobil box ($plate)?")
            .setPositiveButton("Konfirmasi & Muat") { _, _ ->
                swipeRefresh.isRefreshing = true
                val api = ApiClient.getService(this)
                api.pickupTask(order.id, PickupRequest(plate)).enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        swipeRefresh.isRefreshing = false
                        if (response.isSuccessful) {
                            Toast.makeText(this@CourierMainActivity, "Paket berhasil diterima dan dimuat ke mobil box!", Toast.LENGTH_SHORT).show()
                            loadDashboardData()
                        } else {
                            Toast.makeText(this@CourierMainActivity, "Gagal serah terima paket.", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(this@CourierMainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onSelectionChanged(selectedCount: Int, totalSelectableCount: Int, isPodMode: Boolean) {
        val canSelect = totalSelectableCount > 0 && (currentFilter == "all" || (isPodMode && currentFilter == "in_transit") || (!isPodMode && currentFilter == "ready_to_pick"))
        if (canSelect) {
            layoutSelectAll.visibility = View.VISIBLE
            tvSelectAllCount.text = "$selectedCount/$totalSelectableCount"
            tvSelectAllLabel.text = if (isPodMode) "Pilih Semua Pesanan untuk Rekam POD" else "Pilih Semua Pesanan Siap Muat"
            cbSelectAll.setOnCheckedChangeListener(null)
            cbSelectAll.isChecked = (selectedCount > 0 && selectedCount == totalSelectableCount)
            cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
                taskAdapter.selectAll(isChecked, isPodMode)
            }
        } else {
            layoutSelectAll.visibility = View.GONE
        }

        if (selectedCount > 0) {
            cardBulkAction.visibility = View.VISIBLE
            if (isPodMode) {
                tvBulkSelectedCount.text = "$selectedCount pesanan dipilih (POD)"
                btnBulkPickup.text = "Rekam POD Sekaligus ($selectedCount)"
            } else {
                tvBulkSelectedCount.text = "$selectedCount pesanan dipilih (Muat)"
                btnBulkPickup.text = "Muat ke Armada ($selectedCount)"
            }
        } else {
            cardBulkAction.visibility = View.GONE
        }
    }

    private fun openBulkPod(selectedOrders: List<Order>) {
        if (selectedOrders.isEmpty()) return
        val intent = Intent(this, PodActivity::class.java).apply {
            putExtra("orders", ArrayList(selectedOrders))
            putExtra("order", selectedOrders.first())
        }
        podLauncher.launch(intent)
    }

    private fun executeBulkPickup(orderIds: List<Long>, plate: String) {
        if (orderIds.isEmpty()) return

        swipeRefresh.isRefreshing = true
        val api = ApiClient.getService(this)
        api.pickupSelectedTasks(BulkPickupRequest(orderIds, plate)).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@CourierMainActivity, "${orderIds.size} pesanan berhasil diserahterimakan dan dimuat!", Toast.LENGTH_SHORT).show()
                    taskAdapter.clearSelection()
                    loadDashboardData()
                } else {
                    fallbackSequentialPickup(orderIds, plate)
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                fallbackSequentialPickup(orderIds, plate)
            }
        })
    }

    private fun fallbackSequentialPickup(orderIds: List<Long>, plate: String) {
        val api = ApiClient.getService(this)
        var completed = 0
        var failed = 0

        for (id in orderIds) {
            api.pickupTask(id, PickupRequest(plate)).enqueue(object : Callback<ApiResponse> {
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
                        swipeRefresh.isRefreshing = false
                        if (completed > 0) {
                            Toast.makeText(this@CourierMainActivity, "$completed pesanan berhasil dimuat ke armada!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CourierMainActivity, "Gagal serah terima paket.", Toast.LENGTH_SHORT).show()
                        }
                        taskAdapter.clearSelection()
                        loadDashboardData()
                    }
                }
            })
        }
    }

    override fun onSubmitPod(order: Order) {
        val intent = Intent(this, PodActivity::class.java).apply {
            putExtra("orders", arrayListOf(order))
            putExtra("order", order)
        }
        podLauncher.launch(intent)
    }

    override fun onReportIssue(order: Order) {
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_report_issue, null)
        builder.setView(dialogView)

        val dialog = builder.create()

        val spIssueType: Spinner = dialogView.findViewById(R.id.spIssueType)
        val etIssueNotes: EditText = dialogView.findViewById(R.id.etIssueNotes)
        val btnCancel: MaterialButton = dialogView.findViewById(R.id.btnCancelIssue)
        val btnSubmit: MaterialButton = dialogView.findViewById(R.id.btnSubmitIssue)

        val issues = arrayOf(
            "Toko Tutup / Tidak Ada Orang",
            "Alamat Tidak Ditemukan",
            "Reseller Menolak Menerima",
            "Kendala Armada / Ban Bocor",
            "Cuaca Ekstrem / Banjir",
            "Lainnya"
        )
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, issues)
        spIssueType.adapter = spinnerAdapter

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val selectedIssue = spIssueType.selectedItem.toString()
            val notes = etIssueNotes.text.toString().trim()

            if (notes.isEmpty()) {
                etIssueNotes.error = "Catatan kendala wajib diisi"
                etIssueNotes.requestFocus()
                return@setOnClickListener
            }

            dialog.dismiss()
            swipeRefresh.isRefreshing = true

            val api = ApiClient.getService(this)
            api.reportIssue(order.id, IssueRequest(selectedIssue, notes)).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    swipeRefresh.isRefreshing = false
                    if (response.isSuccessful) {
                        Toast.makeText(this@CourierMainActivity, "Laporan kendala berhasil dikirim ke Admin/Gudang.", Toast.LENGTH_SHORT).show()
                        loadDashboardData()
                    } else {
                        Toast.makeText(this@CourierMainActivity, "Gagal mengirim laporan kendala.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    swipeRefresh.isRefreshing = false
                    Toast.makeText(this@CourierMainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        dialog.show()
    }

    override fun onViewPod(order: Order) {
        val intent = Intent(this, PodDetailActivity::class.java).apply {
            putExtra("order", order)
        }
        startActivity(intent)
    }

    private fun showServerConfigDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_server_url, null)
        builder.setView(dialogView)

        val dialog = builder.create()

        val etServerUrl: EditText = dialogView.findViewById(R.id.etServerUrl)
        val btnPresetEmulator: MaterialButton = dialogView.findViewById(R.id.btnPresetEmulator)
        val btnPresetWifi: MaterialButton = dialogView.findViewById(R.id.btnPresetWifi)
        val btnPresetUsb: MaterialButton = dialogView.findViewById(R.id.btnPresetUsb)
        val btnCancel: MaterialButton = dialogView.findViewById(R.id.btnCancelServer)
        val btnSave: MaterialButton = dialogView.findViewById(R.id.btnSaveServer)

        etServerUrl.setText(sessionManager.getBaseUrl())

        btnPresetWifi.setOnClickListener { etServerUrl.setText("http://192.168.100.110:8000/api/") }
        btnPresetUsb.setOnClickListener { etServerUrl.setText("http://127.0.0.1:8000/api/") }
        btnPresetEmulator.setOnClickListener { etServerUrl.setText("http://10.0.2.2:8000/api/") }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newUrl = etServerUrl.text.toString().trim()
            if (newUrl.isNotEmpty()) {
                sessionManager.setBaseUrl(newUrl)
                ApiClient.reset()
                Toast.makeText(this, "URL Server diperbarui", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadDashboardData()
            }
        }

        dialog.show()
    }
}