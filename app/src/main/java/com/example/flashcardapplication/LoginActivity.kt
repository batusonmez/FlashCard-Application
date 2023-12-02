package com.example.flashcardapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("NAME_SHADOWING")
class LoginActivity : AppCompatActivity() {
    private var auth: FirebaseAuth? = null
    private var database: FirebaseFirestore? = null

    private var edEmail : TextInputEditText? = null
    private var edPassword: TextInputEditText? = null
    private var layoutEmail: TextInputLayout? = null
    private var layoutPassword: TextInputLayout? = null
    private var tvForgotPassword: TextView? = null
    private var btnLogin: Button? = null
    private var tvRegister: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        edEmail = findViewById(R.id.ed_email)
        edPassword = findViewById(R.id.ed_password)
        layoutEmail = findViewById(R.id.layout_email)
        layoutPassword = findViewById(R.id.layout_password)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        btnLogin = findViewById(R.id.btn_login)
        tvRegister = findViewById(R.id.tv_register)

        tvForgotPassword?.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        tvForgotPassword?.setOnClickListener {
            // create edittext
            val et = EditText(this)
            et.hint = "Enter your email address"
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginStart = 16 * resources.displayMetrics.density.toInt()
            params.marginEnd = 16 * resources.displayMetrics.density.toInt()
            et.layoutParams = params

            val builder = AlertDialog.Builder(this)
                .setMessage("Please enter your email address")
                .setView(et)
                .setPositiveButton("OK") { _, _ ->
                    // method to send new password via email
                    val email = et.text.toString()
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
                    } else {
                        auth?.sendPasswordResetEmail(email)
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "Email was sent", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Email not sent", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }.setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }

            builder.show()
        }

        tvRegister?.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        tvRegister?.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnLogin?.setOnClickListener {
            val email = edEmail?.text.toString()
            val password = edPassword?.text.toString()
            login(email, password)
        }
    }

    private fun login(email: String, password: String) {
        if (email.isEmpty() && layoutEmail?.isErrorEnabled == false) {
            layoutEmail?.error = "Please enter email address"
            layoutEmail?.isErrorEnabled = true
            return
        } else {
            layoutEmail?.isErrorEnabled = false
        }

        if (password.isEmpty() && layoutPassword?.isErrorEnabled == false) {
            layoutPassword?.error = "Please enter password"
            layoutPassword?.isErrorEnabled = true
            return
        } else {
            layoutPassword?.isErrorEnabled = false
        }

        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth?.currentUser
                    val uid = user?.uid
                    val email = user?.email
                    val name = user?.displayName
                    val photoUrl = user?.photoUrl

                    val userMap = hashMapOf(
                        "uid" to uid,
                        "email" to email,
                        "name" to name,
                        "photoUrl" to photoUrl
                    )

                    database?.collection("users")?.document(uid!!)?.set(userMap)

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    when (task.exception) {
                        is FirebaseAuthInvalidUserException -> {
                            layoutEmail?.error = "Email not found"
                            layoutEmail?.isErrorEnabled = true
                        }

                        is FirebaseAuthInvalidCredentialsException -> {
                            layoutPassword?.error = "Wrong password"
                            layoutPassword?.isErrorEnabled = true
                        }

                        else -> {
                            Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }
}