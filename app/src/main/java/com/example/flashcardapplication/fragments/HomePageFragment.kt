package com.example.flashcardapplication.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapplication.FolderActivity
import com.example.flashcardapplication.R
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.FragmentHomePageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.Serializable

@Suppress("DEPRECATION")
class HomePageFragment : Fragment(), NetworkListener {
    private var auth : FirebaseAuth? = null
    private var database : FirebaseFirestore? = null
    private lateinit var dataSyncHelper: DataSyncHelper
    private val networkReceiver = NetworkReceiver(listener = this)

    private var dataCourse: ArrayList<Data>? = null
    private var dataFolder: ArrayList<Data>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        dataSyncHelper = DataSyncHelper(
            firebaseDb = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = context
        )
    }
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        val binding = FragmentHomePageBinding.inflate(inflater, container, false)
        val view = binding.root

        val currentUser = auth?.currentUser
        if(currentUser != null) {
            // delete this code later
            val storageRef = FirebaseStorage.getInstance().reference.child("avatar/images_1.png")
            storageRef.downloadUrl.addOnCompleteListener {task ->
                if (task.isSuccessful){
                    val downloadUri = task.result
                    currentUser.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(currentUser.email?.split("@")?.get(0))
                            .setPhotoUri(downloadUri)
                            .build())
                }
            }
            Picasso.get().load(currentUser.photoUrl).into(binding.ivAvatar)
            binding.tvHelloUser.text = "Xin chào, ${currentUser.displayName}"
        }

        binding.edtSearch.setOnClickListener {
            // handle later
        }

        binding.tvAllCourse.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        // redirect to all course in layout library

        val roomDb = context?.let { RoomDb.getDatabase(it) }
        val topics = roomDb?.ApplicationDao()
            ?.getAllTopics()
            ?.filter { it.owner == auth?.currentUser?.email }
            ?.take(5)
        dataCourse = ArrayList()
        if (topics != null) {
            if (topics.isNotEmpty()) {
                binding.llCourse.visibility = View.VISIBLE
                for(item in topics) {
                    val topicWithTerminologies = roomDb.ApplicationDao().getTopicWithTerminologies(item.id)
                    val data = Data().apply {
                        id = item.id
                        name = item.name
                        numberLesson = topicWithTerminologies.terminologies.size
                        avatar = auth?.currentUser?.photoUrl
                        nameAuthor = auth?.currentUser?.displayName
                        type = "topic"
                    }
                    dataCourse?.add(data)
                }
            }else{
                binding.llCourse.visibility = View.GONE
            }
        }

        dataCourse?.let {
            binding.rcvAllCourse.adapter = DataAdapter(context, it)
        }
        binding.rcvAllCourse.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            context,
            androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
            false
        )

        binding.tvAllFolder.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        // redirect to all folder in layout library


        val folders = roomDb?.ApplicationDao()
            ?.getFoldersWithTopics()
            ?.filter { it.folder.owner == auth?.currentUser?.email }
            ?.take(5)
        dataFolder = ArrayList()
        if (folders != null) {
            if (folders.isNotEmpty()) {
                binding.llFolder.visibility = View.VISIBLE
                for (item in folders) {
                    val data = Data().apply {
                        id = item.folder.id
                        name = item.folder.name
                        numberLesson = item.topics.size
                        avatar = auth?.currentUser?.photoUrl
                        nameAuthor = auth?.currentUser?.displayName
                        type = "folder"
                    }
                    dataFolder?.add(data)
                }
            }else{
                binding.llFolder.visibility = View.GONE
            }
        }

        dataFolder?.let {
            binding.rcvAllFolder.adapter = DataAdapter(context, it)
        }
        binding.rcvAllFolder.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            context,
            androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
            false
        )

        return view
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNetworkAvailable() {
        GlobalScope.launch {
            if (!dataSyncHelper.getIsSyncDelete()) {
                dataSyncHelper.serverDelete()
            }
            dataSyncHelper.syncData()
        }
    }

    override fun onNetworkUnavailable() {
        // nothing
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireActivity().registerReceiver(networkReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(networkReceiver)
    }
}

class Data : Serializable {
    var id : Int? = null
    var name: String? = null
    var numberLesson: Int? = null
    var avatar: Uri? = null
    var nameAuthor: String? = null
    var type: String? = null
    override fun toString(): String {
        return "Data(id=$id, name=$name, numberLesson=$numberLesson, avatar=$avatar, nameAuthor=$nameAuthor, type=$type)"
    }
}

class DataAdapter(
    private val context: Context?,
    private val data: ArrayList<Data>) :
    RecyclerView.Adapter<DataAdapter.DataViewHolder>() {
    class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val llItemCourse : View? = itemView.findViewById(R.id.ll_itemCourse)
        val tvNameCourse : TextView? = itemView.findViewById(R.id.tv_nameCourse)
        val tvNumberLesson : TextView? = itemView.findViewById(R.id.tv_numberLesson)
        val civAvatar : CircleImageView? = itemView.findViewById(R.id.civ_avatar)
        val tvNameAuthor : TextView? = itemView.findViewById(R.id.tv_nameAuthor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_course, parent, false)
        return DataViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        val item = data[position]
        holder.tvNameCourse?.text = item.name
        holder.tvNumberLesson?.text = item.numberLesson.toString() + " thuật ngữ"
        Picasso.get().load(item.avatar).into(holder.civAvatar)
        holder.tvNameAuthor?.text = item.nameAuthor

        if(item.type == "topic") {
            holder.llItemCourse?.setOnClickListener {
                // handle later
            }
        }else{
            holder.llItemCourse?.setOnClickListener {
                val intent = Intent(context, FolderActivity::class.java)
                intent.putExtra("folderId", item.id)
                context?.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}