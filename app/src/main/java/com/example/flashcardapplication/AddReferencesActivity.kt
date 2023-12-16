package com.example.flashcardapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.ActivityAddReferencesBinding
import com.example.flashcardapplication.fragments.Data
import com.example.flashcardapplication.models.Topic
import com.example.flashcardapplication.models.TopicFolderCrossRef
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress("DEPRECATION")
class AddReferencesActivity : AppCompatActivity(), NetworkListener {
    private lateinit var binding: ActivityAddReferencesBinding
    private var auth = FirebaseAuth.getInstance()
    private var data: ArrayList<Data>? = null
    private val dataSyncHelper = DataSyncHelper(
        firebaseDb = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance(),
        context = this
    )
    private val networkReceiver = NetworkReceiver(listener = this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_references)

        auth = FirebaseAuth.getInstance()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Thêm học phần"

        var folderId = intent.getIntExtra("folderId", 0)

        data = ArrayList()
        val  roomDb = RoomDb.getDatabase(this)
        val topics = roomDb.ApplicationDao()
            .getAllTopics()
            .filter { it.owner ==  auth.currentUser?.email }
            .sortedByDescending { it.dateAsTimestamp() }

        binding.rcvAddReferences.adapter = AddReferencesAdapter(this, topics, roomDb)

        binding.rcvAddReferences.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
    }

    private fun Topic.dateAsTimestamp(): Long {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.parse(timestamp)
        return date?.time ?: 0L
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, intentFilter)
    }
    @OptIn(DelicateCoroutinesApi::class)
    override fun onNetworkAvailable() {
        if(!dataSyncHelper.getIsSyncDelete()){
            GlobalScope.launch {
                dataSyncHelper.serverDelete()
            }
        }
        dataSyncHelper.setIsSync(false)
        GlobalScope.launch {
            dataSyncHelper.syncData()
        }
    }

    override fun onNetworkUnavailable() {
        // nothing
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.create_lesson_option, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.i_save -> {
                val adapter = binding.rcvAddReferences.adapter as AddReferencesAdapter
                val dataClick = adapter.getDataClick()
                val roomDb = RoomDb.getDatabase(this)
                val folderId = intent.getIntExtra("folderId", 0)
                Log.e("TAG", "onOptionsItemSelected1: ", )
                if(dataClick.isNotEmpty()){
                    Log.e("TAG", "onOptionsItemSelected: ", )
                    dataClick.forEach {
                        val topicId = it.id
                        roomDb.ApplicationDao().insertTopicFolderCrossRef(TopicFolderCrossRef(topicId, folderId))
                    }
                    dataSyncHelper.setIsSync(false)
                }
                val intent = Intent(this, FolderActivity::class.java)
                intent.putExtra("folderId", folderId)
                startActivity(intent)
                finish()
            }
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkReceiver)
    }
}

class AddReferencesAdapter(
    private var context: Context,
    private var data: List<Topic>,
    private var roomDb: RoomDb) :
    RecyclerView.Adapter<AddReferencesAdapter.ViewHolder>() {
    private var dataClick = ArrayList<Topic>()
    fun getDataClick(): ArrayList<Topic>{
        return dataClick
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var llCourse = itemView.findViewById<LinearLayout>(R.id.ll_itemCourse)!!
        var tvName = itemView.findViewById<TextView>(R.id.tv_nameCourse)!!
        var tvNumberLesson = itemView.findViewById<TextView>(R.id.tv_numberLesson)!!
        var civAvatar = itemView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.civ_avatar)!!
        var tvNameAuthor = itemView.findViewById<TextView>(R.id.tv_nameAuthor)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).
                inflate(R.layout.item_course, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        val topicWithTerminologies = roomDb.ApplicationDao().getTopicWithTerminologies(item.id)
        holder.tvName.text = item.name
        holder.tvNumberLesson.text = topicWithTerminologies.terminologies.size.toString() + " học phần"
        Picasso.get().load(FirebaseAuth.getInstance().currentUser?.photoUrl).into(holder.civAvatar)
        holder.tvNameAuthor.text = FirebaseAuth.getInstance().currentUser?.displayName

        holder.llCourse.setOnClickListener {
            if(dataClick.contains(item)) {
                dataClick.remove(item)
                holder.llCourse.setBackgroundResource(R.drawable.background_class)
            }
            else {
                dataClick.add(item)
                holder.llCourse.background = context.getDrawable(R.drawable.background_class_click)
            }
        }
    }
}
