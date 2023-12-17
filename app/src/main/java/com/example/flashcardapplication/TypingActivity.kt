package com.example.flashcardapplication

import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.databinding.DataBindingUtil
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.ActivityTypingBinding
import com.example.flashcardapplication.databinding.DialogFalseBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.min

@Suppress("DEPRECATION")
class TypingActivity : AppCompatActivity(), NetworkListener {
    private lateinit var binding: ActivityTypingBinding
    private val networkReceiver = NetworkReceiver(listener = this)
    private val dataSyncHelper = DataSyncHelper(
        firebaseDb = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance(),
        context = this
    )
    private var roomDb = RoomDb.getDatabase(this)
    private var textToSpeech: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_typing)

        val topicId = intent.getIntExtra("topicId", 0)
        val position = intent.getIntExtra("position", 1)
        if(position != -1) {
            val topic = roomDb.ApplicationDao()
                .getTopicWithTerminologies(topicId)
            binding.tvTerminology.text = topic.terminologies[position].terminology

            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "${position + 1}/${topic.terminologies.size}"

            val correctAnswer = topic.terminologies[position]

            textToSpeech = TextToSpeech(this) { status ->
                if (status != TextToSpeech.ERROR) {
                    textToSpeech?.language = Locale.US
                    textToSpeech?.setSpeechRate(0.8f)
                    textToSpeech?.speak(correctAnswer.terminology, TextToSpeech.QUEUE_ADD, null, null)
                }
            }

            val terminology = topic.terminologies as ArrayList

            binding.btnCheck.setOnClickListener {
                val answer = binding.etAnswer.text.toString()
                if (answer.lowercase().trim() == correctAnswer.definition.lowercase().trim()) {
                    val dialog = AlertDialog.Builder(this)
                        .setView(R.layout.dialog_true)
                        .setPositiveButton("Ok") { _, _ ->
                            val intent = Intent(this, TypingActivity::class.java)
                            intent.putExtra("topicId", topicId)
                            intent.putExtra("position", min(position + 1, terminology.size - 1))
                            startActivity(intent)
                            finish()
                        }.create()
                    dialog.show()
                } else {
                    val bindingDialog = DialogFalseBinding.inflate(layoutInflater)
                    bindingDialog.tvMeaning.text = terminology[position].terminology
                    bindingDialog.tvAnswer.text = correctAnswer.definition
                    bindingDialog.tvYourAnswer.text = binding.etAnswer.text.toString()

                    val dialog = AlertDialog.Builder(this)
                        .setView(bindingDialog.root)
                        .setPositiveButton("Ok") { _, _ ->
                            if (position + 1 > terminology.size - 1) {
                                val dialogComplete = AlertDialog.Builder(this)
                                    .setTitle("Chúc mừng")
                                    .setMessage("Bạn đã hoàn thành bài kiểm tra")
                                    .setPositiveButton("Ok") { _, _ ->
                                        finish()
                                    }.create()
                                dialogComplete.show()
                            } else {
                                val intent = Intent(this, TypingActivity::class.java)
                                intent.putExtra("topicId", topicId)
                                intent.putExtra("position", min(position + 1, terminology.size - 1))
                                startActivity(intent)
                                finish()
                            }
                        }.create()
                    dialog.show()
                }
            }
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
            if (!dataSyncHelper.getIsSyncDelete())
                dataSyncHelper.serverDelete()
            dataSyncHelper.syncData()
        }
    }

    override fun onNetworkUnavailable() {
        // do nothing
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkReceiver)
    }
}