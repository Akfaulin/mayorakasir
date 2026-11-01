package com.example.mayoandorid.api

import com.example.mayoandorid.model.*
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("courier/dashboard")
    fun getDashboard(): Call<CourierDashboardResponse>

    @GET("courier/tasks")
    fun getTasks(
        @Query("filter") filter: String?,
        @Query("search") search: String?
    ): Call<TaskResponse>

    @GET("courier/tasks/{id}")
    fun getTaskDetail(@Path("id") id: Long): Call<Order>

    @POST("courier/tasks/pickup-selected")
    fun pickupSelectedTasks(
        @Body request: BulkPickupRequest
    ): Call<ApiResponse>

    @POST("courier/tasks/{id}/pickup")
    fun pickupTask(
        @Path("id") id: Long,
        @Body request: PickupRequest
    ): Call<ApiResponse>

    @POST("courier/tasks/{id}/start")
    fun startDelivery(@Path("id") id: Long): Call<ApiResponse>

    @POST("courier/tasks/pod-selected")
    fun submitBulkPod(
        @Body request: BulkPodRequest
    ): Call<ApiResponse>

    @POST("courier/tasks/{id}/pod")
    fun submitPod(
        @Path("id") id: Long,
        @Body request: PodRequest
    ): Call<ApiResponse>

    @POST("courier/tasks/{id}/issue")
    fun reportIssue(
        @Path("id") id: Long,
        @Body request: IssueRequest
    ): Call<ApiResponse>

    @GET("courier/fleets")
    fun getFleets(): Call<List<Fleet>>

    @GET("courier/reports")
    fun getCourierReport(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = 1
    ): Call<CourierReportResponse>
}