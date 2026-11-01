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
import com.example.mayoandorid.adapter.ReportCustomerAdapter
import com.example.mayoandorid.adapter.ReportDailyAdapter
import com.example.mayoandorid.adapter.ReportProductAdapter
import com.example.mayoandorid.model.ReportDailyItem
import com.example.mayoandorid.model.ReportDestinationItem
import com.example.mayoandorid.api.ApiClient
import com.example.mayoandorid.model.CourierReportResponse
import com.example.mayoandorid.model.Order
import com.example.mayoandorid.model.ReportCustomerItem
import com.example.mayoandorid.model.ReportProductItem
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

    private lateinit var tabReportOrders: TextView
    private lateinit var tabReportDaily: TextView
    private lateinit var tabReportProducts: TextView
    private lateinit var tabReportCustomers: TextView

    private lateinit var chipReportAll: TextView
    private lateinit var chipReportDelivered: TextView
    private lateinit var chipReportTransit: TextView
    private lateinit var chipReportReady: TextView

    private lateinit var etSearchReport: TextInputEditText
    private lateinit var swipeRefreshReport: SwipeRefreshLayout
    private lateinit var rvReportTasks: RecyclerView
    private lateinit var layoutEmptyReport: View

    private lateinit var reportAdapter: CourierReportAdapter
    private lateinit var dailyAdapter: ReportDailyAdapter
    private lateinit var productAdapter: ReportProductAdapter
    private lateinit var customerAdapter: ReportCustomerAdapter

    private var activeViewMode = "orders" // "orders", "products", "customers"
    private var currentFilter: String? = null
    private var currentSearch = ""

    private var cachedOrders: List<Order> = emptyList()
    private var cachedDaily: List<ReportDailyItem> = emptyList()
    private var cachedProducts: List<ReportProductItem> = emptyList()
    private var cachedCustomers: List<ReportCustomerItem> = emptyList()

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

        tabReportOrders = findViewById(R.id.tabReportOrders)
        tabReportDaily = findViewById(R.id.tabReportDaily)
        tabReportProducts = findViewById(R.id.tabReportProducts)
        tabReportCustomers = findViewById(R.id.tabReportCustomers)

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
        dailyAdapter = ReportDailyAdapter(this)
        productAdapter = ReportProductAdapter()
        customerAdapter = ReportCustomerAdapter(this)

        rvReportTasks.adapter = reportAdapter
    }

    private fun setupEvents() {
        btnBackReport.setOnClickListener { finish() }

        swipeRefreshReport.setOnRefreshListener { loadReport() }

        btnExportPdf.setOnClickListener { downloadReportFile("pdf") }
        btnExportExcel.setOnClickListener { downloadReportFile("excel") }

        tabReportOrders.setOnClickListener {
            activeViewMode = "orders"
            updateTabStyles()
            filterAndDisplayCurrentTab()
        }

        tabReportDaily.setOnClickListener {
            activeViewMode = "daily"
            updateTabStyles()
            filterAndDisplayCurrentTab()
        }

        tabReportProducts.setOnClickListener {
            activeViewMode = "products"
            updateTabStyles()
            filterAndDisplayCurrentTab()
        }

        tabReportCustomers.setOnClickListener {
            activeViewMode = "customers"
            updateTabStyles()
            filterAndDisplayCurrentTab()
        }

        chipReportAll.setOnClickListener { setFilter(null) }
        chipReportDelivered.setOnClickListener { setFilter("delivered") }
        chipReportTransit.setOnClickListener { setFilter("shipped") }
        chipReportReady.setOnClickListener { setFilter("ready_to_ship") }

        etSearchReport.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s?.toString()?.trim().orEmpty()
                filterAndDisplayCurrentTab()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateTabStyles() {
        val selectedBg = R.drawable.bg_chip_selected
        val unselectedBg = android.R.color.transparent
        val selectedText = Color.WHITE
        val unselectedText = Color.parseColor("#334155")

        tabReportOrders.setBackgroundResource(if (activeViewMode == "orders") selectedBg else unselectedBg)
        tabReportOrders.setTextColor(if (activeViewMode == "orders") selectedText else unselectedText)

        tabReportDaily.setBackgroundResource(if (activeViewMode == "daily") selectedBg else unselectedBg)
        tabReportDaily.setTextColor(if (activeViewMode == "daily") selectedText else unselectedText)

        tabReportProducts.setBackgroundResource(if (activeViewMode == "products") selectedBg else unselectedBg)
        tabReportProducts.setTextColor(if (activeViewMode == "products") selectedText else unselectedText)

        tabReportCustomers.setBackgroundResource(if (activeViewMode == "customers") selectedBg else unselectedBg)
        tabReportCustomers.setTextColor(if (activeViewMode == "customers") selectedText else unselectedText)
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

                                        cachedOrders = report.tasks?.data ?: emptyList()

                    cachedDaily = if (!report.by_daily.isNullOrEmpty()) {
                        report.by_daily
                    } else {
                        computeLocalDaily(cachedOrders)
                    }

                    cachedProducts = if (!report.by_products.isNullOrEmpty()) {
                        report.by_products
                    } else {
                        computeLocalProducts(cachedOrders)
                    }

                    cachedCustomers = if (!report.by_customers.isNullOrEmpty()) {
                        report.by_customers
                    } else {
                        computeLocalCustomers(cachedOrders)
                    }

                    filterAndDisplayCurrentTab()
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

    private fun filterAndDisplayCurrentTab() {
        val query = currentSearch.lowercase()

        when (activeViewMode) {
            "daily" -> {
                rvReportTasks.adapter = dailyAdapter
                val filtered = if (query.isEmpty()) {
                    cachedDaily
                } else {
                    cachedDaily.mapNotNull { day ->
                        val matchesDay = day.formatted_date.lowercase().contains(query) || day.day_name.lowercase().contains(query)
                        val matchingDest = day.destinations.filter { dest ->
                            dest.store_name.lowercase().contains(query) ||
                            dest.address.lowercase().contains(query) ||
                            dest.customer_name.lowercase().contains(query) ||
                            dest.order_code.lowercase().contains(query)
                        }
                        if (matchesDay) day
                        else if (matchingDest.isNotEmpty()) day.copy(destinations = matchingDest)
                        else null
                    }
                }
                dailyAdapter.setItems(filtered)
                layoutEmptyReport.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
            "products" -> {
                rvReportTasks.adapter = productAdapter
                val filtered = if (query.isEmpty()) {
                    cachedProducts
                } else {
                    cachedProducts.filter {
                        it.name.lowercase().contains(query) ||
                        it.sku.lowercase().contains(query) ||
                        it.stores.any { s -> s.lowercase().contains(query) }
                    }
                }
                productAdapter.setProducts(filtered)
                layoutEmptyReport.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
            "customers" -> {
                rvReportTasks.adapter = customerAdapter
                val filtered = if (query.isEmpty()) {
                    cachedCustomers
                } else {
                    cachedCustomers.filter {
                        it.store_name.lowercase().contains(query) ||
                        it.customer_name.lowercase().contains(query) ||
                        it.address.lowercase().contains(query) ||
                        it.phone.contains(query) ||
                        it.order_codes.any { c -> c.lowercase().contains(query) }
                    }
                }
                customerAdapter.setCustomers(filtered)
                layoutEmptyReport.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
            else -> {
                rvReportTasks.adapter = reportAdapter
                val filtered = if (query.isEmpty()) {
                    cachedOrders
                } else {
                    cachedOrders.filter {
                        val code = (it.calculation_code ?: it.id.toString()).lowercase()
                        val store = (it.store_name ?: "").lowercase()
                        val rec = (it.recipient_name ?: "").lowercase()
                        val addr = (it.shipping_address ?: "").lowercase()
                        code.contains(query) || store.contains(query) || rec.contains(query) || addr.contains(query)
                    }
                }
                reportAdapter.setOrders(filtered)
                layoutEmptyReport.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun computeLocalProducts(orders: List<Order>): List<ReportProductItem> {
        val map = mutableMapOf<Long, ReportProductItem>()
        for (order in orders) {
            val store = order.store_name ?: "Toko Reseller"
            val items = order.items ?: emptyList()
            for (item in items) {
                val pId = item.product_id
                val existing = map[pId]
                val pName = item.product?.name ?: "Produk Mayora"
                val pSku = item.product?.sku ?: "SKU-$pId"
                val newQty = (existing?.total_qty ?: 0) + item.quantity
                val newOrders = (existing?.orders_count ?: 0) + 1
                val storesList = (existing?.stores ?: emptyList()).toMutableList()
                if (!storesList.contains(store)) storesList.add(store)

                map[pId] = ReportProductItem(
                    product_id = pId,
                    name = pName,
                    sku = pSku,
                    total_qty = newQty,
                    orders_count = newOrders,
                    stores = storesList
                )
            }
        }
        return map.values.sortedByDescending { it.total_qty }
    }

    private fun computeLocalCustomers(orders: List<Order>): List<ReportCustomerItem> {
        val map = mutableMapOf<String, ReportCustomerItem>()
        for (order in orders) {
            val store = order.store_name ?: "Toko Reseller"
            val addr = order.shipping_address ?: "Alamat belum tercatat"
            val key = "$store-$addr"
            val owner = order.recipient_name ?: "-"
            val phone = order.recipient_phone ?: ""
            val code = order.calculation_code ?: "#${order.id}"

            val existing = map[key]
            val totalOrders = (existing?.total_orders ?: 0) + 1
            val totalQty = (existing?.total_qty ?: 0) + (if (order.total_qty > 0) order.total_qty else 1)
            val codeList = (existing?.order_codes ?: emptyList()).toMutableList()
            if (!codeList.contains(code)) codeList.add(code)

            val pMap = (existing?.products_summary ?: emptyMap()).toMutableMap()
            order.items?.forEach { itm ->
                val pName = itm.product?.name ?: "Produk Mayora"
                pMap[pName] = (pMap[pName] ?: 0) + itm.quantity
            }

            val status = if (order.shipping_status == "delivered") "delivered" else (order.shipping_status ?: "shipped")

            map[key] = ReportCustomerItem(
                store_name = store,
                customer_name = owner,
                address = addr,
                phone = phone,
                total_orders = totalOrders,
                total_qty = totalQty,
                order_codes = codeList,
                products_summary = pMap,
                status = status,
                delivered_at = order.delivered_at
            )
        }
        return map.values.sortedByDescending { it.total_qty }
    }

    private fun computeLocalDaily(orders: List<Order>): List<ReportDailyItem> {
        val map = mutableMapOf<String, MutableList<Order>>()
        for (order in orders) {
            val dateKey = (order.delivered_at ?: (order.shipped_at ?: (order.packed_at ?: "2026-11-01"))).take(10)
            map.getOrPut(dateKey) { mutableListOf() }.add(order)
        }

        return map.entries.map { (dateKey, dayOrders) ->
            val totalQty = dayOrders.sumOf { if (it.total_qty > 0) it.total_qty else 1 }
            val distinctStores = dayOrders.mapNotNull { it.store_name }.distinct().size.coerceAtLeast(1)
            val destinations = dayOrders.map { o ->
                val dtRaw = o.delivered_at ?: (o.shipped_at ?: (o.packed_at ?: "14:00"))
                val timeStr = if (dtRaw.contains("T")) dtRaw.substringAfter("T").take(5) else "14:00"
                ReportDestinationItem(
                    order_code = o.calculation_code ?: "ORD-${o.id}",
                    store_name = o.store_name ?: "Toko Reseller",
                    customer_name = o.recipient_name ?: "Pemilik Toko",
                    address = o.shipping_address ?: "Alamat belum tercatat",
                    phone = o.recipient_phone ?: "-",
                    qty = if (o.total_qty > 0) o.total_qty else 1,
                    weight_kg = if (o.package_weight > 0) o.package_weight else (o.shipment?.weight_kg ?: 25.0),
                    status = o.shipping_status ?: "shipped",
                    delivery_time = timeStr,
                    pod_receiver = o.recipient_name,
                    fleet_plate = o.shipment?.fleet_plate_number ?: "AG 9421 PQR"
                )
            }

            ReportDailyItem(
                date = dateKey,
                formatted_date = "Rute $dateKey",
                day_name = "Hari",
                total_orders = dayOrders.size,
                total_qty = totalQty,
                total_stores = distinctStores,
                destinations = destinations
            )
        }.sortedByDescending { it.date }
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
            .addHeader("Authorization", "Bearer ${sessionManager.getToken()}")
            .addHeader("Accept", "application/json")
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
                runOnUiThread { progressDialog.dismiss() }
                if (response.isSuccessful && response.body != null) {
                    try {
                        val ext = if (format == "pdf") "pdf" else "xlsx"
                        val fileName = "Laporan_Kurir_Mayora_${System.currentTimeMillis()}.$ext"
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val file = File(downloadsDir, fileName)

                        val bytes = response.body!!.bytes()
                        file.writeBytes(bytes)

                        runOnUiThread {
                            Toast.makeText(this@CourierReportActivity, "Dokumen $format berhasil diunduh!", Toast.LENGTH_SHORT).show()
                            openDownloadedFile(file, format)
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@CourierReportActivity, "Gagal menyimpan file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@CourierReportActivity, "Gagal mengunduh dari server: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun openDownloadedFile(file: File, format: String) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val mimeType = if (format == "pdf") "application/pdf" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "File tersimpan di folder Download: ${file.name}", Toast.LENGTH_LONG).show()
        }
    }
}
