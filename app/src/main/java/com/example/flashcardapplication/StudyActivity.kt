package com.example.flashcardapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.ActivityStudyBinding
import com.example.flashcardapplication.models.Terminology
import com.example.flashcardapplication.models.TopicWithTerminologies
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.Locale

@Suppress("DEPRECATION")
class StudyActivity : AppCompatActivity(), NetworkListener {
    private val networkReceiver = NetworkReceiver(listener = this)
    private val dataSyncHelper = DataSyncHelper(
        firebaseDb = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance(),
        context = this
    )
    private lateinit var binding: ActivityStudyBinding
    private var roomDb = RoomDb.getDatabase(this)
    private var auth = FirebaseAuth.getInstance()
    private var idTopic: Int = 1

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_study)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chủ đề"

        val topicId = intent.getIntExtra("topicId", 0)
        idTopic = topicId
        val topic = roomDb.ApplicationDao()
            .getTopicWithTerminologies(topicId)

        // rcv_easy_flip_view
        binding.rcvEasyFlipView.adapter = FlashCardAdapter(this, topic.terminologies)
        binding.rcvEasyFlipView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        binding.tvTopicName.text = topic.topic.name
        Picasso.get().load(auth.currentUser?.photoUrl).into(binding.civAvatar)
        binding.tvAuthorName.text = auth.currentUser?.displayName
        binding.tvNumberLesson.text = topic.terminologies.size.toString() + " thuật ngữ"

        binding.tvFlashcard.movementMethod = LinkMovementMethod.getInstance()
        // handle with tvFlashcard
        binding.tvFlashcard.setOnClickListener {
            val intent = Intent(this, FlashCardActivity::class.java)
            intent.putExtra("topicId", topicId)
            startActivity(intent)
        }

        binding.tvQuiz.movementMethod = LinkMovementMethod.getInstance()
        // handle with tvQuiz
        binding.tvQuiz.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("topicId", topicId)
            intent.putExtra("position", if(topic.terminologies.isNotEmpty()) 0 else -1)
            startActivity(intent)
        }

        binding.tvTyping.movementMethod = LinkMovementMethod.getInstance()
        // handle with tvTyping
        binding.tvTyping.setOnClickListener {
            val intent = Intent(this, TypingActivity::class.java)
            intent.putExtra("topicId", topicId)
            intent.putExtra("position", if(topic.terminologies.isNotEmpty()) 0 else -1)
            startActivity(intent)
        }

        // rcvCard
        binding.rcvCard.adapter = CardAdapter(this, topic.terminologies)
        binding.rcvCard.layoutManager = LinearLayoutManager(this)

        // download to csv
        binding.btnDownload.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Bạn có muốn tải về chứ?")
                .setMessage("File sẽ được lưu trong thư mục Download")
                .setPositiveButton("Có") { _, _ ->
                    checkPermissionAnDownloadCSV(topic)
                }
                .setNegativeButton("Không") { _, _ -> }
                .create()
            dialog.show()
            checkPermissionAnDownloadCSV(topic)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun checkPermissionAnDownloadCSV(topic: TopicWithTerminologies) {
        val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, storagePermission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            GlobalScope.launch {
                downloadCSV(topic)
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(storagePermission), 100)
        }
    }

    private suspend fun downloadCSV(topic: TopicWithTerminologies) {
        withContext(Dispatchers.IO) {
            val content = generate(topic)
            try {
                val root =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(root, topic.topic.name + ".csv")

                val fileOutputStream = FileOutputStream(file)
                val outputStreamWriter = OutputStreamWriter(fileOutputStream)
                val bufferedWriter = BufferedWriter(outputStreamWriter)

                bufferedWriter.write(content)

                bufferedWriter.close()
                outputStreamWriter.close()
                fileOutputStream.close()

                runOnUiThread {
                    Toast.makeText(this@StudyActivity, "Tải về thành công", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@StudyActivity, "Tải về thất bại", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun generate(topic: TopicWithTerminologies): String {
        val sb = StringBuilder()
        sb.append("${topic.topic.name}\n")
        sb.append("Terminology,Definition\n")
        for (item in topic.terminologies) {
            sb.append(item.terminology)
            sb.append(",")
            sb.append(item.definition)
            sb.append("\n")
        }
        return sb.toString()
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
        // Do nothing
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkReceiver)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.folder_menu, menu)
        menu?.removeItem(R.id.i_add_folder)
        return super.onCreateOptionsMenu(menu)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.i_modify_folder -> {
                val intent = Intent(this, CreateLessonActivity::class.java)
                intent.putExtra("topicId", idTopic)
                startActivity(intent)
            }
            R.id.i_delete_folder -> {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Bạn có muốn xóa chủ đề này?")
                    .setMessage("Tất cả thuật ngữ trong chủ đề này sẽ bị xóa")
                    .setPositiveButton("Có") { _, _ ->
                        val topic = roomDb.ApplicationDao()
                            .getTopicWithTerminologies(idTopic)
                        GlobalScope.launch {
                            dataSyncHelper.localDelete(null, topic.topic)
                            dataSyncHelper.setIsSyncDelete(false)
                            dataSyncHelper.setIsSync(false)
                            finish()
                        }
                    }
                    .setNegativeButton("Không") { _, _ -> }
                    .create()
                dialog.show()
            }
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

class FlashCardAdapter(
    private val context: Context,
    private val data: List<Terminology>
) : RecyclerView.Adapter<FlashCardAdapter.ViewHolder>() {
    private var textToSpeech: TextToSpeech? = null
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFront = itemView.findViewById<TextView>(R.id.tv_front)!!
        val tvBack = itemView.findViewById<TextView>(R.id.tv_back)!!
        val btnVolume = itemView.findViewById<TextView>(R.id.btn_volume)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.easy_flip_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.tvFront.text = item.terminology
        holder.tvBack.text = item.definition

        holder.btnVolume.setOnClickListener {
            textToSpeech = TextToSpeech(context) { status ->
                if (status != TextToSpeech.ERROR) {
                    textToSpeech?.language = Locale.US
                    textToSpeech?.setSpeechRate(0.8f)
                    textToSpeech?.speak(item.terminology, TextToSpeech.QUEUE_ADD, null, null)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}

class CardAdapter(
    private val context: StudyActivity,
    private val data: List<Terminology>
) : RecyclerView.Adapter<CardAdapter.ViewHolder>() {
    private var textToSpeech: TextToSpeech? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTerminology = itemView.findViewById<TextView>(R.id.tv_terminology)!!
        val tvMeaning = itemView.findViewById<TextView>(R.id.tv_meaning)!!
        val btnVolume = itemView.findViewById<TextView>(R.id.btn_volume)!!
        val btnStar = itemView.findViewById<TextView>(R.id.btn_star)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.tvTerminology.text = item.terminology
        holder.tvMeaning.text = item.definition

        holder.btnVolume.setOnClickListener {
            textToSpeech = TextToSpeech(context) { status ->
                if (status != TextToSpeech.ERROR) {
                    textToSpeech?.language = Locale.US
                    textToSpeech?.setSpeechRate(0.8f)
                    textToSpeech?.speak(item.terminology, TextToSpeech.QUEUE_ADD, null, null)
                }
            }
        }

        holder.btnStar.setOnClickListener {
            // later
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}
