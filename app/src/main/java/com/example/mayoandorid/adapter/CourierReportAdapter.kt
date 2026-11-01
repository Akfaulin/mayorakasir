package com.example.mayoandorid.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mayoandorid.R
import com.example.mayoandorid.model.Order
import com.example.mayoandorid.ui.PodDetailActivity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class CourierReportAdapter(
    private val context: Context
) : RecyclerView.Adapter<CourierReportAdapter.ReportViewHolder>() {

    private var reportOrders: List<Order> = emptyList()

    fun setOrders(orders: List<Order>?) {
        this.reportOrders = orders ?: emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_courier_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(reportOrders[position])
    }

    override fun getItemCount(): Int = reportOrders.size

    inner class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderCode: TextView = itemView.findViewById(R.id.tvOrderCode)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        private val tvStoreName: TextView = itemView.findViewById(R.id.tvStoreName)
        private val tvRecipient: TextView = itemView.findViewById(R.id.tvRecipient)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvTotalQty: TextView = itemView.findViewById(R.id.tvTotalQty)
        private val tvItemsBreakdown: TextView = itemView.findViewById(R.id.tvItemsBreakdown)
        private val layoutIssueAlert: View = itemView.findViewById(R.id.layoutIssueAlert)
        private val tvIssueText: TextView = itemView.findViewById(R.id.tvIssueText)
        private val layoutPodFooter: View = itemView.findViewById(R.id.layoutPodFooter)
        private val tvPodReceiver: TextView = itemView.findViewById(R.id.tvPodReceiver)
        private val tvPodTime: TextView = itemView.findViewById(R.id.tvPodTime)
        private val btnViewPodDetail: MaterialButton = itemView.findViewById(R.id.btnViewPodDetail)

        fun bind(order: Order) {
            val code = order.tracking_number ?: order.calculation_code ?: "DO-${order.id}"
            tvOrderCode.text = "#$code"

            // Format date time
            val rawDate = order.delivered_at ?: order.shipped_at ?: order.picked_at
            if (!rawDate.isNullOrEmpty()) {
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("id", "ID"))
                    val parsed = inputFormat.parse(rawDate.substring(0, 19))
                    tvDateTime.text = if (parsed != null) "${outputFormat.format(parsed)} WIB" else rawDate.take(16)
                } catch (e: Exception) {
                    tvDateTime.text = rawDate.replace("T", " ").take(16)
                }
            } else {
                tvDateTime.text = "Tercatat di sistem"
            }

            tvStoreName.text = order.store_name ?: "Toko Reseller Mayora"
            val recName = order.recipient_name ?: "-"
            val recPhone = order.recipient_phone ?: ""
            tvRecipient.text = "👤 Penerima: $recName" + if (recPhone.isNotEmpty()) " ($recPhone)" else ""
            tvAddress.text = order.shipping_address ?: "Alamat belum tercatat"

            val qty = if (order.total_qty > 0) order.total_qty else (order.items?.sumOf { it.quantity } ?: 1)
            tvTotalQty.text = "$qty Karton (Ctn)"

            // Rincian produk yang diantar
            val items = order.items
            if (!items.isNullOrEmpty()) {
                val breakdownText = items.joinToString("\n") { item ->
                    val pName = item.product?.name ?: "Produk Mayora"
                    "• $pName (${item.quantity} ctn)"
                }
                tvItemsBreakdown.text = breakdownText
            } else {
                tvItemsBreakdown.text = "• Muatan Campuran Produk Mayora ($qty ctn)"
            }

            // Status Badge
            val status = order.shipping_status ?: "pending"
            when (status) {
                "delivered" -> {
                    tvStatusBadge.text = "Terkirim (POD Sah)"
                    tvStatusBadge.setTextColor(Color.parseColor("#059669"))
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_emerald)

                    layoutPodFooter.visibility = View.VISIBLE
                    val receivedBy = order.shipment?.pod_recipient_name ?: order.recipient_name ?: "Penerima Toko"
                    tvPodReceiver.text = "✓ Diterima oleh: $receivedBy"
                    tvPodTime.text = "Bukti foto & tanda tangan digital terverifikasi"

                    btnViewPodDetail.visibility = View.VISIBLE
                    btnViewPodDetail.setOnClickListener {
                        val intent = Intent(context, PodDetailActivity::class.java).apply {
                            putExtra("order", order)
                        }
                        context.startActivity(intent)
                    }
                }
                "shipped" -> {
                    tvStatusBadge.text = "Dalam Antaran"
                    tvStatusBadge.setTextColor(Color.parseColor("#1E40AF"))
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_blue)
                    layoutPodFooter.visibility = View.GONE
                }
                else -> {
                    tvStatusBadge.text = "Siap Berangkat"
                    tvStatusBadge.setTextColor(Color.parseColor("#92400E"))
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_amber)
                    layoutPodFooter.visibility = View.GONE
                }
            }

            // Issue alert
            val issueType = order.shipment?.issue_type
            val issueNotes = order.shipment?.issue_notes
            if (!issueType.isNullOrEmpty() || !issueNotes.isNullOrEmpty()) {
                layoutIssueAlert.visibility = View.VISIBLE
                val note = issueNotes ?: issueType ?: ""
                tvIssueText.text = "⚠️ Kendala: $note"
            } else {
                layoutIssueAlert.visibility = View.GONE
            }
        }
    }
}
