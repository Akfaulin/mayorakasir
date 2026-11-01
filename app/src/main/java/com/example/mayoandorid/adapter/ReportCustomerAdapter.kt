package com.example.mayoandorid.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.mayoandorid.R
import com.example.mayoandorid.model.ReportCustomerItem
import com.google.android.material.button.MaterialButton

class ReportCustomerAdapter(
    private val context: Context
) : RecyclerView.Adapter<ReportCustomerAdapter.CustomerViewHolder>() {

    private var customerList: List<ReportCustomerItem> = emptyList()

    fun setCustomers(customers: List<ReportCustomerItem>?) {
        this.customerList = customers ?: emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(customerList[position])
    }

    override fun getItemCount(): Int = customerList.size

    inner class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustStoreName: TextView = itemView.findViewById(R.id.tvCustStoreName)
        private val tvCustStatusBadge: TextView = itemView.findViewById(R.id.tvCustStatusBadge)
        private val tvCustOwner: TextView = itemView.findViewById(R.id.tvCustOwner)
        private val btnCallCustomer: MaterialButton = itemView.findViewById(R.id.btnCallCustomer)
        private val tvCustAddress: TextView = itemView.findViewById(R.id.tvCustAddress)
        private val tvCustTotalOrders: TextView = itemView.findViewById(R.id.tvCustTotalOrders)
        private val tvCustTotalQty: TextView = itemView.findViewById(R.id.tvCustTotalQty)
        private val tvCustProductsSummary: TextView = itemView.findViewById(R.id.tvCustProductsSummary)

        fun bind(item: ReportCustomerItem) {
            tvCustStoreName.text = item.store_name
            tvCustOwner.text = "👤 Pemilik: ${item.customer_name}"
            tvCustAddress.text = item.address
            tvCustTotalQty.text = "${item.total_qty} ctn"

            val orderCodeSummary = if (item.order_codes.isNotEmpty()) {
                "📋 ${item.total_orders} Pesanan (" + item.order_codes.joinToString(", ") + ")"
            } else {
                "📋 ${item.total_orders} Pesanan"
            }
            tvCustTotalOrders.text = orderCodeSummary

            if (item.products_summary.isNotEmpty()) {
                val pText = item.products_summary.entries.joinToString("\n") { "• ${it.key} (${it.value} ctn)" }
                tvCustProductsSummary.visibility = View.VISIBLE
                tvCustProductsSummary.text = pText
            } else {
                tvCustProductsSummary.visibility = View.GONE
            }

            if (item.phone.isNotEmpty() && item.phone != "-") {
                btnCallCustomer.visibility = View.VISIBLE
                btnCallCustomer.setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.phone}"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Tidak dapat membuka dialer: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                btnCallCustomer.visibility = View.GONE
            }

            when (item.status) {
                "delivered" -> {
                    tvCustStatusBadge.text = "Terkirim (POD)"
                    tvCustStatusBadge.setTextColor(Color.parseColor("#059669"))
                    tvCustStatusBadge.setBackgroundColor(Color.parseColor("#ECFDF5"))
                }
                "shipped" -> {
                    tvCustStatusBadge.text = "Dalam Antaran"
                    tvCustStatusBadge.setTextColor(Color.parseColor("#1E40AF"))
                    tvCustStatusBadge.setBackgroundColor(Color.parseColor("#DBEAFE"))
                }
                else -> {
                    tvCustStatusBadge.text = "Siap Diantar"
                    tvCustStatusBadge.setTextColor(Color.parseColor("#92400E"))
                    tvCustStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"))
                }
            }
        }
    }
}

