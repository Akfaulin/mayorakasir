package com.example.mayoandorid.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.mayoandorid.R
import com.example.mayoandorid.model.Order
import com.google.android.material.button.MaterialButton

class TaskAdapter(
    private val context: Context,
    private val listener: OnTaskActionListener
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    interface OnTaskActionListener {
        fun onPickup(order: Order)
        fun onSubmitPod(order: Order)
        fun onReportIssue(order: Order)
        fun onViewPod(order: Order)
        fun onSelectionChanged(selectedCount: Int, totalSelectableCount: Int, isPodMode: Boolean)
    }

    private var orderList: List<Order> = emptyList()
    private val selectedOrderIds = mutableSetOf<Long>()
    private var activeFilter: String = "all"

    fun setFilterMode(filter: String) {
        this.activeFilter = filter
    }

    fun setOrders(orders: List<Order>?) {
        this.orderList = orders ?: emptyList()
        val validIds = (getReadyToPickOrders() + getInTransitOrders()).map { it.id }.toSet()
        selectedOrderIds.retainAll(validIds)
        notifyDataSetChanged()
        notifySelectionChanged()
    }

    fun getReadyToPickOrders(): List<Order> {
        return orderList.filter {
            val status = it.shipping_status ?: "pending"
            status != "delivered" && status != "shipped"
        }
    }

    fun getInTransitOrders(): List<Order> {
        return orderList.filter {
            val status = it.shipping_status ?: "pending"
            status == "shipped"
        }
    }

    fun isPodSelection(): Boolean {
        if (selectedOrderIds.isEmpty()) return activeFilter == "in_transit"
        val firstSelected = orderList.firstOrNull { selectedOrderIds.contains(it.id) }
        return firstSelected?.shipping_status == "shipped"
    }

    fun getSelectedOrderIds(): List<Long> {
        return selectedOrderIds.toList()
    }

    fun getSelectedOrders(): List<Order> {
        return orderList.filter { selectedOrderIds.contains(it.id) }
    }

    fun selectAllReadyOrders(select: Boolean) {
        selectAll(select, false)
    }

    fun selectAllTransitOrders(select: Boolean) {
        selectAll(select, true)
    }

    fun selectAll(select: Boolean, forPod: Boolean) {
        val targetOrders = if (forPod) getInTransitOrders() else getReadyToPickOrders()
        if (select) {
            selectedOrderIds.clear()
            selectedOrderIds.addAll(targetOrders.map { it.id })
        } else {
            selectedOrderIds.clear()
        }
        notifyDataSetChanged()
        notifySelectionChanged()
    }

    fun clearSelection() {
        selectedOrderIds.clear()
        notifyDataSetChanged()
        notifySelectionChanged()
    }

    private fun notifySelectionChanged() {
        val isPod = isPodSelection()
        val totalSelectable = if (isPod) getInTransitOrders().size else getReadyToPickOrders().size
        listener.onSelectionChanged(selectedOrderIds.size, totalSelectable, isPod)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_courier_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(orderList[position])
    }

    override fun getItemCount(): Int = orderList.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardTaskRoot: CardView? = itemView.findViewById(R.id.cardTaskRoot)
        private val cbSelectOrder: CheckBox = itemView.findViewById(R.id.cbSelectOrder)
        private val tvOrderCode: TextView = itemView.findViewById(R.id.tvOrderCode)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        private val tvStoreName: TextView = itemView.findViewById(R.id.tvStoreName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvRecipient: TextView = itemView.findViewById(R.id.tvRecipient)
        private val tvPackageInfo: TextView = itemView.findViewById(R.id.tvPackageInfo)
        private val tvFleetInfo: TextView = itemView.findViewById(R.id.tvFleetInfo)
        private val tvIssueText: TextView = itemView.findViewById(R.id.tvIssueText)
        private val layoutIssueAlert: View = itemView.findViewById(R.id.layoutIssueAlert)

        private val btnCallPhone: MaterialButton = itemView.findViewById(R.id.btnCallPhone)
        private val btnPickup: MaterialButton = itemView.findViewById(R.id.btnPickup)
        private val btnSubmitPod: MaterialButton = itemView.findViewById(R.id.btnSubmitPod)
        private val btnReportIssue: MaterialButton = itemView.findViewById(R.id.btnReportIssue)
        private val btnViewPod: MaterialButton = itemView.findViewById(R.id.btnViewPod)

        fun bind(order: Order) {
            val code = order.calculation_code ?: "ORD-${order.id}"
            tvOrderCode.text = "#$code"
            tvStoreName.text = order.store_name ?: "Toko Reseller"
            tvAddress.text = order.shipping_address ?: "Alamat belum tercatat"

            val recName = order.recipient_name ?: "-"
            val recPhone = order.recipient_phone ?: ""
            tvRecipient.text = "👤 $recName" + if (recPhone.isNotEmpty()) " ($recPhone)" else ""

            btnCallPhone.setOnClickListener {
                if (recPhone.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$recPhone"))
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Nomor telepon tidak tersedia", Toast.LENGTH_SHORT).show()
                }
            }

            val weight = if (order.package_weight > 0) order.package_weight else (order.shipment?.weight_kg ?: 25.0)
            val qty = if (order.total_qty > 0) order.total_qty else 1
            tvPackageInfo.text = "$qty Koli · $weight kg"

            val plate = order.shipment?.fleet_plate_number ?: "AG 9421 PQR"
            tvFleetInfo.text = plate

            val shippingStatus = order.shipping_status ?: "pending"
            val hasIssue = !order.shipment?.issue_type.isNullOrEmpty()

            if (hasIssue) {
                layoutIssueAlert.visibility = View.VISIBLE
                tvIssueText.text = "⚠️ Kendala: ${order.shipment?.issue_notes}"
            } else {
                layoutIssueAlert.visibility = View.GONE
            }

            btnPickup.visibility = View.GONE
            btnSubmitPod.visibility = View.GONE
            btnReportIssue.visibility = View.GONE
            btnViewPod.visibility = View.GONE

            val isReadyToPick = shippingStatus != "delivered" && shippingStatus != "shipped"
            val isInTransit = shippingStatus == "shipped"

            val hasSelection = selectedOrderIds.isNotEmpty()
            val selectionIsPod = isPodSelection()

            val canSelect = when {
                !hasSelection -> isReadyToPick || isInTransit
                selectionIsPod -> isInTransit
                else -> isReadyToPick
            }

            if (canSelect) {
                cbSelectOrder.visibility = View.VISIBLE
                cbSelectOrder.setOnCheckedChangeListener(null)
                val isSelected = selectedOrderIds.contains(order.id)
                cbSelectOrder.isChecked = isSelected

                if (isSelected) {
                    cardTaskRoot?.setCardBackgroundColor(Color.parseColor("#FFF5F5"))
                } else {
                    cardTaskRoot?.setCardBackgroundColor(Color.WHITE)
                }

                cbSelectOrder.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedOrderIds.add(order.id)
                    } else {
                        selectedOrderIds.remove(order.id)
                    }
                    notifyDataSetChanged()
                    notifySelectionChanged()
                }

                itemView.setOnClickListener {
                    cbSelectOrder.toggle()
                }
            } else {
                cbSelectOrder.visibility = View.GONE
                cardTaskRoot?.setCardBackgroundColor(Color.WHITE)
                itemView.setOnClickListener(null)
            }

            when (shippingStatus) {
                "delivered" -> {
                    tvStatusBadge.text = "Selesai (POD)"
                    tvStatusBadge.setTextColor(Color.parseColor("#059669"))
                    tvStatusBadge.setBackgroundColor(Color.parseColor("#ECFDF5"))
                    btnViewPod.visibility = View.VISIBLE
                    btnViewPod.setOnClickListener { listener.onViewPod(order) }
                }
                "shipped" -> {
                    tvStatusBadge.text = if (hasIssue) "Kendala Pengantaran" else "Dalam Antaran"
                    tvStatusBadge.setTextColor(if (hasIssue) Color.parseColor("#9F1239") else Color.parseColor("#1E40AF"))
                    tvStatusBadge.setBackgroundColor(if (hasIssue) Color.parseColor("#FFE4E6") else Color.parseColor("#DBEAFE"))

                    btnSubmitPod.visibility = View.VISIBLE
                    btnReportIssue.visibility = View.VISIBLE

                    btnSubmitPod.setOnClickListener { listener.onSubmitPod(order) }
                    btnReportIssue.setOnClickListener { listener.onReportIssue(order) }
                }
                else -> {
                    tvStatusBadge.text = "Siap Diambil"
                    tvStatusBadge.setTextColor(Color.parseColor("#92400E"))
                    tvStatusBadge.setBackgroundColor(Color.parseColor("#FEF3C7"))

                    btnPickup.visibility = View.VISIBLE
                    btnPickup.setOnClickListener { listener.onPickup(order) }
                }
            }
        }
    }
}
