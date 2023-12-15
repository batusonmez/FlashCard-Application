package com.example.flashcardapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

@Suppress("NAME_SHADOWING")
class SignUpActivity : AppCompatActivity() {
    private var auth: FirebaseAuth? = null
    private var database: FirebaseFirestore? = null

    private var edEmail : TextInputEditText? = null
    private var edPassword: TextInputEditText? = null
    private var edRePassword: TextInputEditText? = null
    private var layoutEmail: TextInputLayout? = null
    private var layoutPassword: TextInputLayout? = null
    private var layoutRePassword: TextInputLayout? = null
    private var btnRegister: Button? = null
    private var tvLogin: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        edEmail = findViewById(R.id.ed_email)
        edPassword = findViewById(R.id.ed_password)
        edRePassword = findViewById(R.id.ed_re_password)
        layoutEmail = findViewById(R.id.layout_email)
        layoutPassword = findViewById(R.id.layout_password)
        layoutRePassword = findViewById(R.id.layout_re_password)
        btnRegister = findViewById(R.id.btn_register)
        tvLogin = findViewById(R.id.tv_login)

        tvLogin?.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        tvLogin?.setOnClickListener {
            val intent = android.content.Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnRegister?.setOnClickListener {
            val email = edEmail?.text.toString()
            val password = edPassword?.text.toString()
            val rePassword = edRePassword?.text.toString()
            register(email, password, rePassword)
        }
    }

    private fun register(email: String, password: String, rePassword: String) {
        if(validateEmail(email)
            && validatePassword(password)
            && validateRePassword(password, rePassword)) {
            auth?.createUserWithEmailAndPassword(email, password)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth?.currentUser
                        val storageRef = FirebaseStorage.getInstance().reference.child("avatar/images_1.png")

                        storageRef.downloadUrl.addOnCompleteListener {task ->
                            if (task.isSuccessful){
                                val downloadUri = task.result
                                user?.updateProfile(UserProfileChangeRequest.Builder()
                                    .setDisplayName(user.email?.split("@")?.get(0))
                                    .setPhotoUri(downloadUri)
                                    .build())
                            }
                        }

                        val intent = android.content.Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        when (task.exception) {
                            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> {
                                layoutPassword?.error = "Password is too weak"
                            }
                            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                                layoutEmail?.error = "Invalid email"
                            }
                            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> {
                                layoutEmail?.error = "Email already registered"
                            }
                            else -> {
                                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
        }
    }

    private fun validateEmail(email: String): Boolean {
            return if (email.isEmpty()) {
                layoutEmail?.error = "Email cannot be empty"
                false
            } else {
                layoutEmail?.error = null
                true
            }
    }
    private fun validatePassword(password: String): Boolean {
            return if (password.length < 6) {
                layoutPassword?.error = "Password is too short"
                false
            } else {
                layoutPassword?.error = null
                true
            }
    }
    private fun validateRePassword(password: String, rePassword: String): Boolean {
            return if (password != rePassword) {
                layoutRePassword?.error = "Password does not match"
                false
            } else {
                layoutRePassword?.error = null
                true
            }
    }
}