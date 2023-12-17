package com.example.flashcardapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.ActivityQuizBinding
import com.example.flashcardapplication.databinding.DialogFalseBinding
import com.example.flashcardapplication.models.Terminology
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.min

@Suppress("DEPRECATION")
class QuizActivity : AppCompatActivity(), NetworkListener {
    private lateinit var binding: ActivityQuizBinding
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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quiz)

        val topicId = intent.getIntExtra("topicId", 0)
        val position = intent.getIntExtra("position", 1)
        if (position != -1) {
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
                    textToSpeech?.speak(
                        correctAnswer.terminology,
                        TextToSpeech.QUEUE_ADD,
                        null,
                        null
                    )
                }
            }

            val listShuffle = roomDb.ApplicationDao()
                .getTopicWithTerminologies(topicId)
                .terminologies as ArrayList<Terminology>
            listShuffle.remove(correctAnswer)
            while (listShuffle.size < 3) {
                listShuffle.addAll(topic.terminologies)
            }
            listShuffle.shuffle()

            when ((1..4).random()) {
                1 -> {
                    binding.tvMeaning1.text = correctAnswer.definition
                    binding.tvMeaning2.text = listShuffle[0].definition
                    binding.tvMeaning3.text = listShuffle[1].definition
                    binding.tvMeaning4.text = listShuffle[2].definition
                }

                2 -> {
                    binding.tvMeaning1.text = listShuffle[0].definition
                    binding.tvMeaning2.text = correctAnswer.definition
                    binding.tvMeaning3.text = listShuffle[1].definition
                    binding.tvMeaning4.text = listShuffle[2].definition
                }

                3 -> {
                    binding.tvMeaning1.text = listShuffle[0].definition
                    binding.tvMeaning2.text = listShuffle[1].definition
                    binding.tvMeaning3.text = correctAnswer.definition
                    binding.tvMeaning4.text = listShuffle[2].definition
                }

                4 -> {
                    binding.tvMeaning1.text = listShuffle[0].definition
                    binding.tvMeaning2.text = listShuffle[1].definition
                    binding.tvMeaning3.text = listShuffle[2].definition
                    binding.tvMeaning4.text = correctAnswer.definition
                }
            }

            binding.tvMeaning1.setOnClickListener {
                checkAnswerAndHover(
                    binding.tvMeaning1,
                    correctAnswer.definition,
                    topic.terminologies as ArrayList,
                    position,
                    topicId
                )
            }
            binding.tvMeaning2.setOnClickListener {
                checkAnswerAndHover(
                    binding.tvMeaning2,
                    correctAnswer.definition,
                    topic.terminologies as ArrayList,
                    position,
                    topicId
                )
            }
            binding.tvMeaning3.setOnClickListener {
                checkAnswerAndHover(
                    binding.tvMeaning3,
                    correctAnswer.definition,
                    topic.terminologies as ArrayList,
                    position,
                    topicId
                )
            }
            binding.tvMeaning4.setOnClickListener {
                checkAnswerAndHover(
                    binding.tvMeaning4,
                    correctAnswer.definition,
                    topic.terminologies as ArrayList,
                    position,
                    topicId
                )
            }

            binding.btnVolume.setOnClickListener{
                textToSpeech = TextToSpeech(this) { status ->
                    if (status != TextToSpeech.ERROR) {
                        textToSpeech?.language = Locale.US
                        textToSpeech?.setSpeechRate(0.8f)
                        textToSpeech?.speak(
                            correctAnswer.terminology,
                            TextToSpeech.QUEUE_ADD,
                            null,
                            null
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun checkAnswerAndHover(
        textview: TextView, correctAnswer: String,
        terminology: ArrayList<Terminology>, position: Int,
        topicId: Int
    ) {
        textview.background = getDrawable(R.drawable.background_quiz_hover)
        val isTrue: Boolean = checkAnswer(textview.text.toString(), correctAnswer)

        if (isTrue) {
            val dialog = AlertDialog.Builder(this)
                .setView(R.layout.dialog_true)
                .setPositiveButton("Ok") { _, _ ->
                    val intent = Intent(this, QuizActivity::class.java)
                    intent.putExtra("topicId", topicId)
                    intent.putExtra("position", min(position + 1, terminology.size - 1))
                    startActivity(intent)
                    finish()
                }.create()
            dialog.show()
        } else {
            val bindingDialog = DialogFalseBinding.inflate(layoutInflater)
            bindingDialog.tvMeaning.text = terminology[position].terminology
            bindingDialog.tvAnswer.text = correctAnswer
            bindingDialog.tvYourAnswer.text = textview.text.toString()

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
                        val intent = Intent(this, QuizActivity::class.java)
                        intent.putExtra("topicId", topicId)
                        intent.putExtra("position", min(position + 1, terminology.size - 1))
                        startActivity(intent)
                        finish()
                    }
                }.create()
            dialog.show()
        }
    }

    private fun checkAnswer(answer: String, correctAnswer: String): Boolean {
        if (answer.lowercase().trim() == correctAnswer.lowercase().trim())
            return true
        return false
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}