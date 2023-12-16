package com.example.flashcardapplication

import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.ActivityFlashCardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("DEPRECATION", "UNREACHABLE_CODE")
class FlashCardActivity : AppCompatActivity(), NetworkListener {
    private lateinit var binding: ActivityFlashCardBinding
    private val networkReceiver = NetworkReceiver(listener = this)
    private val dataSyncHelper = DataSyncHelper(
        firebaseDb = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance(),
        context = this
    )
    private var roomDb = RoomDb.getDatabase(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_flash_card)

        val topicId = intent.getIntExtra("topicId", 0)
        val topic = roomDb.ApplicationDao()
            .getTopicWithTerminologies(topicId)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "1/${topic.terminologies.size}"

        topic.let {
            binding.ctvFlashCard.adapter = FlashCardAdapter(this, it.terminologies)
        }
        binding.ctvFlashCard.layoutManager = CardStackLayoutManager(this)
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
        return super.onOptionsItemSelected(item)
        if(item.itemId == android.R.id.home)
            finish()
    }
}
