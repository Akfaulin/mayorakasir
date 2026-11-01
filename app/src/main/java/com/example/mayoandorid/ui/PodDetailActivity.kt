package com.example.mayoandorid.ui

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.example.mayoandorid.R
import com.example.mayoandorid.model.Order
import com.example.mayoandorid.util.ImageUtils

class PodDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pod_detail)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val detailRoot: View = findViewById(R.id.detailRoot)
        val detailHeaderBar: View = findViewById(R.id.detailHeaderBar)
        val detailScrollView: View = findViewById(R.id.detailScrollView)
        val initialTopPadding = detailHeaderBar.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(detailRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            detailHeaderBar.updatePadding(top = systemBars.top + initialTopPadding)
            detailScrollView.updatePadding(bottom = systemBars.bottom)
            insets
        }

        val order = intent.getSerializableExtra("order") as? Order
        if (order == null) {
            finish()
            return
        }

        val btnBack: ImageButton = findViewById(R.id.btnBackDetail)
        val tvDetailTitle: TextView = findViewById(R.id.tvDetailTitle)
        val tvDetailRecipient: TextView = findViewById(R.id.tvDetailRecipient)
        val tvDetailDeliveredAt: TextView = findViewById(R.id.tvDetailDeliveredAt)
        val tvDetailNotes: TextView = findViewById(R.id.tvDetailNotes)
        val ivDetailPhoto: ImageView = findViewById(R.id.ivDetailPhoto)
        val ivDetailSignature: ImageView = findViewById(R.id.ivDetailSignature)

        btnBack.setOnClickListener { finish() }

        val code = order.calculation_code ?: order.id.toString()
        tvDetailTitle.text = "Bukti POD #$code"

        val shipment = order.shipment
        if (shipment != null) {
            val recipient = shipment.pod_recipient_name ?: (order.recipient_name ?: "-")
            tvDetailRecipient.text = recipient

            val deliveredTime = shipment.delivered_at ?: (order.delivered_at ?: "Terkonfirmasi Selesai")
            tvDetailDeliveredAt.text = deliveredTime

            val notes = shipment.pod_notes ?: "Tidak ada catatan khusus."
            tvDetailNotes.text = notes

            // Display Photo
            if (!shipment.pod_photo.isNullOrEmpty()) {
                val bmp = ImageUtils.base64ToBitmap(shipment.pod_photo)
                if (bmp != null) {
                    ivDetailPhoto.setImageBitmap(bmp)
                }
            }

            // Display Signature
            if (!shipment.pod_signature.isNullOrEmpty()) {
                val sigBmp = ImageUtils.base64ToBitmap(shipment.pod_signature)
                if (sigBmp != null) {
                    ivDetailSignature.setImageBitmap(sigBmp)
                }
            }
        }
    }
}