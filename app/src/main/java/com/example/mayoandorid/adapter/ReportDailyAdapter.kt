package com.example.mayoandorid.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mayoandorid.R
import com.example.mayoandorid.model.ReportDailyItem
import com.example.mayoandorid.model.ReportDestinationItem

class ReportDailyAdapter(
    private val context: Context,
    private val onDestinationClick: ((ReportDestinationItem) -> Unit)? = null
) : RecyclerView.Adapter<ReportDailyAdapter.DailyViewHolder>() {

    private var items: List<ReportDailyItem> = emptyList()

    fun setItems(newItems: List<ReportDailyItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_daily, parent, false)
        return DailyViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class DailyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDailyFormattedDate: TextView = itemView.findViewById(R.id.tvDailyFormattedDate)
        private val tvDailySubStats: TextView = itemView.findViewById(R.id.tvDailySubStats)
        private val tvDailyBadge: TextView = itemView.findViewById(R.id.tvDailyBadge)
        private val layoutDestinationsContainer: LinearLayout = itemView.findViewById(R.id.layoutDestinationsContainer)

        fun bind(item: ReportDailyItem) {
            tvDailyFormattedDate.text = item.formatted_date.ifEmpty { item.date }
            tvDailySubStats.text = "${item.total_stores} Toko Tujuan · ${item.total_qty} Koli · ${item.total_orders} Pesanan"

            val isToday = item.date.isNotEmpty() && item.formatted_date.contains("Hari ini", ignoreCase = true)
            tvDailyBadge.text = if (isToday) "RUTE HARI INI" else "${item.total_stores} TOKO"

            layoutDestinationsContainer.removeAllViews()
            val inflater = LayoutInflater.from(context)

            item.destinations.forEachIndexed { index, dest ->
                val stopView = inflater.inflate(R.layout.item_report_daily_stop, layoutDestinationsContainer, false)
                val tvStopNumberTime: TextView = stopView.findViewById(R.id.tvStopNumberTime)
                val tvStopStatus: TextView = stopView.findViewById(R.id.tvStopStatus)
                val tvStopStoreName: TextView = stopView.findViewById(R.id.tvStopStoreName)
                val tvStopAddress: TextView = stopView.findViewById(R.id.tvStopAddress)
                val tvStopOrderInfo: TextView = stopView.findViewById(R.id.tvStopOrderInfo)
                val tvStopRecipient: TextView = stopView.findViewById(R.id.tvStopRecipient)

                val timeStr = dest.delivery_time.ifEmpty { "--:--" }
                tvStopNumberTime.text = "🕒 $timeStr · Stop ${index + 1}"
                tvStopStoreName.text = "🏪 ${dest.store_name}"
                tvStopAddress.text = "📍 ${dest.address}"
                tvStopOrderInfo.text = "📦 #${dest.order_code} · ${dest.qty} Koli (${dest.weight_kg} kg)"
                tvStopRecipient.text = "👤 ${dest.customer_name}"

                when (dest.status) {
                    "delivered" -> {
                        tvStopStatus.text = "TERKIRIM (POD)"
                        tvStopStatus.setTextColor(Color.parseColor("#065F46"))
                        tvStopStatus.setBackgroundResource(R.drawable.bg_badge_delivered)
                    }
                    "shipped" -> {
                        tvStopStatus.text = "DALAM ANTARAN"
                        tvStopStatus.setTextColor(Color.parseColor("#1E40AF"))
                        tvStopStatus.setBackgroundColor(Color.parseColor("#DBEAFE"))
                    }
                    else -> {
                        tvStopStatus.text = "SIAP DIANTAR"
                        tvStopStatus.setTextColor(Color.parseColor("#92400E"))
                        tvStopStatus.setBackgroundColor(Color.parseColor("#FEF3C7"))
                    }
                }

                stopView.setOnClickListener {
                    onDestinationClick?.invoke(dest)
                }

                layoutDestinationsContainer.addView(stopView)
            }
        }
    }
}

