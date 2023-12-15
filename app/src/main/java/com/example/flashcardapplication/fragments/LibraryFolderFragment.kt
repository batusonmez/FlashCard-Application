package com.example.flashcardapplication.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapplication.R
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.FragmentLibraryFolderBinding
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso

class LibraryFolderFragment : Fragment() {
    private var data: ArrayList<Data>? = null
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
                    name = folder.folder.name
                    numberLesson = folder.topics.size
                    avatar = FirebaseAuth.getInstance().currentUser?.photoUrl
                    nameAuthor = FirebaseAuth.getInstance().currentUser?.displayName
                })
            }
        }

        binding.rcvLibraryFolder.adapter = LibraryFolderAdapter(requireContext(), data!!)
        binding.rcvLibraryFolder.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        return binding.root
    }
}

class LibraryFolderAdapter(
    private var context: Context,
    private var data: ArrayList<Data>) :
    RecyclerView.Adapter<LibraryFolderAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
    }

}
