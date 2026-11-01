package com.example.mayoandorid.model

import java.io.Serializable

data class User(
    val id: Long = 0,
    val name: String = "",
    val email: String = "",
    val role: String = "kurir",
    val phone: String? = null
) : Serializable

data class Fleet(
    val id: Long = 0,
    val plate_number: String = "",
    val model_name: String = "",
    val capacity_kg: Int = 1000,
    val status: String = "available",
    val notes: String? = null
) : Serializable

data class Product(
    val id: Long = 0,
    val name: String = "",
    val sku: String = "",
    val price: Double = 0.0,
    val weight_per_unit: Double = 0.0
) : Serializable

data class OrderItem(
    val id: Long = 0,
    val order_id: Long = 0,
    val product_id: Long = 0,
    val quantity: Int = 0,
    val price: Double = 0.0,
    val subtotal: Double = 0.0,
    val product: Product? = null
) : Serializable

data class Shipment(
    val id: Long = 0,
    val order_id: Long = 0,
    val courier_id: Long? = null,
    val courier_name: String? = null,
    val courier_phone: String? = null,
    val courier: String? = null,
    val fleet_plate_number: String? = null,
    val fleet_model: String? = null,
    val service_type: String? = null,
    val tracking_number: String? = null,
    val status: String = "ready",
    val weight_kg: Double = 0.0,
    val picked_up_at: String? = null,
    val shipped_at: String? = null,
    val delivered_at: String? = null,
    val pod_photo: String? = null,
    val pod_signature: String? = null,
    val pod_recipient_name: String? = null,
    val pod_notes: String? = null,
    val issue_type: String? = null,
    val issue_notes: String? = null,
    val issue_reported_at: String? = null
) : Serializable

data class Order(
    val id: Long = 0,
    val calculation_code: String? = null,
    val store_name: String? = null,
    val recipient_name: String? = null,
    val recipient_phone: String? = null,
    val shipping_address: String? = null,
    val status: String = "confirmed",
    val payment_status: String = "paid",
    val shipping_status: String = "packed",
    val courier: String? = null,
    val tracking_number: String? = null,
    val package_weight: Double = 0.0,
    val total_qty: Int = 0,
    val subtotal: Double = 0.0,
    val total: Double = 0.0,
    val notes: String? = null,
    val picked_at: String? = null,
    val packed_at: String? = null,
    val shipped_at: String? = null,
    val delivered_at: String? = null,
    val items: List<OrderItem>? = null,
    val shipment: Shipment? = null
) : Serializable

data class CourierSummary(
    val ready_to_deliver: Int = 0,
    val ready_to_pick: Int = 0,
    val in_transit: Int = 0,
    val delivered_today: Int = 0,
    val total_delivered: Int = 0,
    val reported_issues: Int = 0
) : Serializable

data class CourierDashboardResponse(
    val summary: CourierSummary? = null,
    val active_fleet: Fleet? = null,
    val priority_tasks: List<Order>? = null
) : Serializable

data class TaskResponse(
    val data: List<Order>? = null,
    val current_page: Int = 1,
    val last_page: Int = 1,
    val total: Int = 0
) : Serializable

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String? = null,
    val token: String? = null,
    val user: User? = null
)

data class PickupRequest(
    val fleet_plate_number: String
)

data class BulkPickupRequest(
    val order_ids: List<Long>,
    val fleet_plate_number: String
)

data class BulkPodRequest(
    val order_ids: List<Long>,
    val recipient_name: String,
    val pod_photo: String?,
    val pod_signature: String?,
    val notes: String?
)

data class PodRequest(
    val recipient_name: String,
    val pod_photo: String?,
    val pod_signature: String?,
    val notes: String?
)

data class IssueRequest(
    val issue_type: String,
    val issue_notes: String
)

data class ApiResponse(
    val message: String? = null,
    val order: Order? = null
)

data class CourierReportSummary(
    val total_tasks: Int = 0,
    val delivered_count: Int = 0,
    val in_transit_count: Int = 0,
    val issues_count: Int = 0,
    val total_qty: Int = 0
) : Serializable

data class ReportProductItem(
    val product_id: Long = 0,
    val name: String = "",
    val sku: String = "",
    val total_qty: Int = 0,
    val orders_count: Int = 0,
    val stores: List<String> = emptyList()
) : Serializable

data class ReportCustomerItem(
    val store_name: String = "",
    val customer_name: String = "",
    val address: String = "",
    val phone: String = "",
    val total_orders: Int = 0,
    val total_qty: Int = 0,
    val order_codes: List<String> = emptyList(),
    val products_summary: Map<String, Int> = emptyMap(),
    val status: String = "delivered",
    val delivered_at: String? = null
) : Serializable

data class ReportDailyItem(
    val date: String = "",
    val formatted_date: String = "",
    val day_name: String = "",
    val total_orders: Int = 0,
    val total_qty: Int = 0,
    val total_stores: Int = 0,
    val destinations: List<ReportDestinationItem> = emptyList()
) : Serializable

data class ReportDestinationItem(
    val order_code: String = "",
    val store_name: String = "",
    val customer_name: String = "",
    val address: String = "",
    val phone: String = "",
    val qty: Int = 0,
    val weight_kg: Double = 0.0,
    val status: String = "shipped",
    val delivery_time: String = "",
    val pod_receiver: String? = null,
    val fleet_plate: String? = null
) : Serializable

data class CourierReportResponse(
    val user: User? = null,
    val fleet: Fleet? = null,
    val summary: CourierReportSummary? = null,
    val tasks: TaskResponse? = null,
    val by_daily: List<ReportDailyItem>? = null,
    val by_products: List<ReportProductItem>? = null,
    val by_customers: List<ReportCustomerItem>? = null
) : Serializable