package com.example.flashcardapplication

import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.databinding.ActivitySettingBinding
import com.example.flashcardapplication.databinding.CustomPasswordViewBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class SettingActivity : AppCompatActivity(), NetworkListener {
    private var auth : FirebaseAuth? = null
    private var binding : ActivitySettingBinding? = null
    private var dataSyncHelper = DataSyncHelper(
        firebaseDb = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance(),
        context = this
    )
    private val networkReceiver = NetworkReceiver(listener = this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Cài đặt"

        binding?.tvChangePassword?.movementMethod = LinkMovementMethod.getInstance()
        binding?.tvChangePassword?.setOnClickListener {
            val bindingDialog = CustomPasswordViewBinding.inflate(layoutInflater)
            val dialog = AlertDialog.Builder(this)
                .setView(bindingDialog.root)
                .setPositiveButton("OK") { _, _ ->
                    val password = bindingDialog.edtPassword.text.toString()
                    val newPassword = bindingDialog.edtNewPassword.text.toString()
                    val confirmPassword = bindingDialog.edtConfirmPassword.text.toString()
                    if(password.isNotEmpty()){
                        val user = auth?.currentUser

                        val credential = EmailAuthProvider.getCredential(
                            FirebaseAuth.getInstance().currentUser?.email.toString(),
                            password
                        )

                        user?.reauthenticate(credential)?.addOnCompleteListener {
                            if (it.isSuccessful) {
                                if (newPassword == confirmPassword) {
                                    user.updatePassword(newPassword).addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            val builder = AlertDialog.Builder(this)
                                            builder.setTitle("Thành công")
                                            builder.setMessage("Mật khẩu đã được đặt lại")
                                            builder.setPositiveButton("OK") { _, _ -> }
                                            builder.show()
                                        } else {
                                            val builder = AlertDialog.Builder(this)
                                            builder.setTitle("Lỗi")
                                            builder.setMessage("Không thể đặt lại mật khẩu")
                                            builder.setPositiveButton("OK") { _, _ -> }
                                            builder.show()
                                        }
                                    }
                                } else {
                                    val builder = AlertDialog.Builder(this)
                                    builder.setTitle("Không thể đặt lại mật khẩu")
                                    builder.setMessage("Mật khẩu mới không khớp với mật khẩu xác nhận")
                                    builder.setPositiveButton("OK") { _, _ -> }
                                    builder.show()
                                }
                            } else {
                                val builder = AlertDialog.Builder(this)
                                builder.setTitle("Lỗi")
                                builder.setMessage("Mật khẩu hiện tại không đúng")
                                builder.setPositiveButton("OK") { _, _ -> }
                                builder.show()
                            }
                        }
                    }else{
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Lỗi")
                        builder.setMessage("Mật khẩu hiện tại không được để trống")
                        builder.setPositiveButton("OK") { _, _ -> }
                        builder.show()
                    }
                }.create()
            dialog.show()
        }

        binding?.tvLogout?.movementMethod = LinkMovementMethod.getInstance()
        binding?.tvLogout?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, intentFilter)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNetworkAvailable() {
        GlobalScope.launch {
            if(!dataSyncHelper.getIsSyncDelete())
                dataSyncHelper.serverDelete()
            dataSyncHelper.syncData()
        }
    }

    override fun onNetworkUnavailable() {
        // Do nothing
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkReceiver)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}