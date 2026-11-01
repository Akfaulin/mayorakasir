package com.example.mayoandorid.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mayoandorid.R
import com.example.mayoandorid.api.ApiClient
import com.example.mayoandorid.model.LoginRequest
import com.example.mayoandorid.model.LoginResponse
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

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, CourierMainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val loginRoot: View = findViewById(R.id.loginRoot)
        ViewCompat.setOnApplyWindowInsetsListener(loginRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            loginRoot.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)

        // Secret long click on Mayora logo to adjust API Server URL during development/testing
        findViewById<View>(R.id.cardMayoraLogo)?.setOnLongClickListener {
            showServerConfigDialog()
            true
        }

        btnLogin.setOnClickListener { performLogin() }
    }


    private fun performLogin() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString()?.trim().orEmpty()

        if (email.isEmpty()) {
            etEmail.error = "Email wajib diisi"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password wajib diisi"
            etPassword.requestFocus()
            return
        }

        btnLogin.isEnabled = false
        progressBar.visibility = View.VISIBLE

        val api = ApiClient.getService(this)
        api.login(LoginRequest(email, password)).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                btnLogin.isEnabled = true
                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val loginRes = response.body()!!
                    val user = loginRes.user
                    if (user != null) {
                        val role = user.role?.lowercase().orEmpty()
                        if (role != "kurir" && role != "courier" && role != "admin") {
                            Toast.makeText(this@LoginActivity, "Akses ditolak: Akun ini bukan bertugas sebagai Kurir.", Toast.LENGTH_LONG).show()
                            return
                        }

                        sessionManager.saveAuth(
                            loginRes.token ?: "",
                            user.id,
                            user.name ?: "",
                            user.email ?: "",
                            user.role ?: ""
                        )

                        Toast.makeText(this@LoginActivity, "Selamat datang, ${user.name}!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, CourierMainActivity::class.java))
                        finish()
                    }
                } else {
                    var err = "Login gagal. Periksa kembali email dan password."
                    if (response.code() == 401) {
                        err = "Email atau kata sandi tidak cocok."
                    }
                    Toast.makeText(this@LoginActivity, err, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                btnLogin.isEnabled = true
                progressBar.visibility = View.GONE
                Toast.makeText(this@LoginActivity, "Koneksi ke server gagal: ${t.message}\nPeriksa pengaturan Server URL API.", Toast.LENGTH_LONG).show()
            }
        })
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

        btnPresetWifi.setOnClickListener { etServerUrl.setText("http://192.168.0.105:8000/api/") }
        btnPresetUsb.setOnClickListener { etServerUrl.setText("http://127.0.0.1:8000/api/") }
        btnPresetEmulator.setOnClickListener { etServerUrl.setText("http://10.0.2.2:8000/api/") }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val newUrl = etServerUrl.text.toString().trim()
            if (newUrl.isNotEmpty()) {
                sessionManager.setBaseUrl(newUrl)
                ApiClient.reset()
                Toast.makeText(this@LoginActivity, "URL Server diperbarui", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}