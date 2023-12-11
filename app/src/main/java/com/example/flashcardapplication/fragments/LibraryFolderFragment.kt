package com.example.flashcardapplication.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapplication.R
import com.example.flashcardapplication.databinding.FragmentLibraryFolderBinding
import com.squareup.picasso.Picasso

class LibraryFolderFragment : Fragment() {
    private var data: ArrayList<Data>? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentLibraryFolderBinding.inflate(inflater, container, false)

        data = ArrayList()
        // to test
        data?.add(Data().apply {
            name = "Lập trình Android"
            numberLesson = 10
            avatar = Uri.parse("android.resource://com.example.flashcardapplication/drawable/avatar")
            nameAuthor = "Nguyễn Văn A"
        })
        data?.add(Data().apply {
            name = "Lập trình Android"
            numberLesson = 10
            avatar = Uri.parse("android.resource://com.example.flashcardapplication/drawable/avatar")
            nameAuthor = "Nguyễn Văn A"
        })

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
        var tvName: TextView? = null
        var tvNumberLesson: TextView? = null
        var tvNameAuthor: TextView? = null
        var imgAvatar: de.hdodenhof.circleimageview.CircleImageView? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_folder, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var item = data[position]
        holder.tvName?.text = item.name
        holder.tvNumberLesson?.text = item.numberLesson.toString() + "học phần"
        holder.tvNameAuthor?.text = item.nameAuthor
        Picasso.get().load(item.avatar).into(holder.imgAvatar)
    }

}
