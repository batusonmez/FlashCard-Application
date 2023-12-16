package com.example.flashcardapplication.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapplication.FolderActivity
import com.example.flashcardapplication.R
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.FragmentLibraryFolderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class LibraryFolderFragment : Fragment(), NetworkListener {
    private var data: ArrayList<Data>? = null
    private lateinit var dataSyncHelper: DataSyncHelper
    private val networkReceiver = NetworkReceiver(listener = this)
    override fun onAttach(context: Context) {
        super.onAttach(context)
        dataSyncHelper = DataSyncHelper(
            firebaseDb = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            context = context
        )
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLibraryFolderBinding.inflate(inflater, container, false)

        data = ArrayList()
        val roomDb = context?.let { RoomDb.getDatabase(it) }
        val folders = roomDb?.ApplicationDao()
            ?.getAllFolders()
            ?.filter { it.owner == FirebaseAuth.getInstance().currentUser?.email }
        for (item in folders!!){
            val folderWithTopics = roomDb.ApplicationDao()
                .getFoldersWithTopics()
                .filter { it.folder.id == item.id }

            for (folder in folderWithTopics){
                data?.add(Data().apply {
                    id = folder.folder.id
                    name = folder.folder.name
                    numberLesson = folder.topics.size
                    avatar = FirebaseAuth.getInstance().currentUser?.photoUrl
                    nameAuthor = FirebaseAuth.getInstance().currentUser?.displayName
                    type = "folder"
                })
            }
        }

        binding.rcvLibraryFolder.adapter = LibraryFolderAdapter(requireContext(), data!!)
        binding.rcvLibraryFolder.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        requireContext().registerReceiver(networkReceiver, intentFilter)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNetworkAvailable() {
        if(!dataSyncHelper.getIsSyncDelete()){
            GlobalScope.launch {
                dataSyncHelper.serverDelete()
            }
        }
        GlobalScope.launch {
            dataSyncHelper.syncData()
        }
    }

    override fun onNetworkUnavailable() {
        // nothing
    }
}

class LibraryFolderAdapter(
    private var context: Context,
    private var data: ArrayList<Data>) :
    RecyclerView.Adapter<LibraryFolderAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var llItemFolder = itemView.findViewById<LinearLayout>(R.id.ll_item_folder)!!
        var tvTopicFolder = itemView.findViewById<TextView>(R.id.tv_topic_folder)!!
        var tvNumberLesson = itemView.findViewById<TextView>(R.id.tv_number_lesson)!!
        var civAvatar = itemView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.civ_avatar)!!
        var tvNameAuthor = itemView.findViewById<TextView>(R.id.tv_name_author)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).
                inflate(R.layout.item_folder, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.tvTopicFolder.text = item.name
        holder.tvNumberLesson.text = item.numberLesson.toString() + " học phần"
        Picasso.get().load(item.avatar).into(holder.civAvatar)
        holder.tvNameAuthor.text = item.nameAuthor.toString()

        holder.llItemFolder.setOnClickListener{
            val intent = Intent(context, FolderActivity::class.java)
            intent.putExtra("folderId", item.id)
            context.startActivity(intent)
        }
    }

}
