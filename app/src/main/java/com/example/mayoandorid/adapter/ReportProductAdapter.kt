package com.example.mayoandorid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mayoandorid.R
import com.example.mayoandorid.model.ReportProductItem

class ReportProductAdapter : RecyclerView.Adapter<ReportProductAdapter.ProductViewHolder>() {

    private var productList: List<ReportProductItem> = emptyList()

    fun setProducts(products: List<ReportProductItem>?) {
        this.productList = products ?: emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvProductSku: TextView = itemView.findViewById(R.id.tvProductSku)
        private val tvTotalQtyBadge: TextView = itemView.findViewById(R.id.tvTotalQtyBadge)
        private val tvOrdersCount: TextView = itemView.findViewById(R.id.tvOrdersCount)
        private val tvProductStores: TextView = itemView.findViewById(R.id.tvProductStores)

        fun bind(item: ReportProductItem) {
            tvProductName.text = item.name
            tvProductSku.text = "SKU: ${item.sku}"
            tvTotalQtyBadge.text = "${item.total_qty} ctn"
            tvOrdersCount.text = "📋 Termasuk dalam ${item.orders_count} Pesanan"

            if (item.stores.isNotEmpty()) {
                tvProductStores.visibility = View.VISIBLE
                tvProductStores.text = "🏪 Toko Tujuan: " + item.stores.joinToString(", ")
            } else {
                tvProductStores.visibility = View.GONE
            }
        }
    }
}

