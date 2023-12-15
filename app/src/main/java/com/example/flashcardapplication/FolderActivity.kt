package com.example.flashcardapplication

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.ActivityFolderBinding
import com.example.flashcardapplication.fragments.Data
import com.example.flashcardapplication.fragments.DataAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class FolderActivity : AppCompatActivity(), NetworkListener {
    private lateinit var binding: ActivityFolderBinding
    private var auth : FirebaseAuth? = null
    private var data : ArrayList<Data>? = null
    private val networkReceiver = NetworkReceiver(listener = this)
    private val dataSyncHelper = DataSyncHelper(
        firebaseDb = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance(),
        context = this
    )
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_folder)
        auth = FirebaseAuth.getInstance()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Thư mục"

        val folderId = intent.getIntExtra("folderId", 0)
        Log.e("TAG", "onCreate: $folderId")
        val roomDb = RoomDb.getDatabase(this)
        val folder = roomDb.ApplicationDao().getFolderById(folderId)
        val folderWithTopic = roomDb.ApplicationDao()
            .getFoldersWithTopics()
            .filter { it.folder.id == folderId }[0]

        binding.tvNumberLesson.text = folderWithTopic.topics.size.toString() + " học phần"
        Picasso.get().load(auth?.currentUser?.photoUrl).into(binding.civAvatar)
        binding.tvNameAuthor.text = auth?.currentUser?.displayName
        binding.tvFolderName.text = folder.name

        data = ArrayList()
        if (folderWithTopic.topics.isNotEmpty()) {
            binding.layoutEmptyFolder.visibility = android.view.View.GONE
            binding.rcvLesson.visibility = android.view.View.VISIBLE
            for (topic in folderWithTopic.topics) {
                val term = roomDb.ApplicationDao()
                    .getTopicWithTerminologies(topicId = topic.id)
                val topicView = Data().apply {
                    name = topic.name
                    numberLesson = term.terminologies.size
                    avatar = auth?.currentUser?.photoUrl
                    nameAuthor = topic.owner
                }
                data?.add(topicView)
            }
        }else{
            binding.layoutEmptyFolder.visibility = android.view.View.VISIBLE
            binding.rcvLesson.visibility = android.view.View.GONE
        }

        data?.let {
            binding.rcvLesson.adapter = DataAdapter(this, it)
        }
        binding.rcvLesson.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, intentFilter)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNetworkAvailable() {
        GlobalScope.launch {
            dataSyncHelper.syncData()
        }
    }

    override fun onNetworkUnavailable() {
        // nothing
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkReceiver)
    }
}