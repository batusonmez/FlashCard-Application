package com.example.flashcardapplication.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapplication.R
import com.example.flashcardapplication.databinding.FragmentHomePageBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.io.Serializable

class HomePageFragment : Fragment() {
    private var auth : FirebaseAuth? = null
    private var database : FirebaseFirestore? = null

    private var dataCourse: ArrayList<Data>? = null
    private var dataFolder: ArrayList<Data>? = null

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
            Picasso.get().load(currentUser.photoUrl).into(binding.ivAvatar)
            binding.tvHelloUser.text = "Xin chào, ${currentUser.displayName}"
        }

        binding.edtSearch.setOnClickListener {
            // handle later
        }

        binding.tvAllCourse.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        // redirect to all course in layout library

        dataCourse = ArrayList()
        // to test
        dataCourse?.add(Data().apply {
            name = "Lập trình Android"
            numberLesson = 10
            avatar = Uri.parse("android.resource://com.example.flashcardapplication/drawable/avatar")
            nameAuthor = "Nguyễn Văn A"
        })
        dataCourse?.add(Data().apply {
            name = "Lập trình Android"
            numberLesson = 10
            avatar = Uri.parse("android.resource://com.example.flashcardapplication/drawable/avatar")
            nameAuthor = "Nguyễn Văn A"
        })

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


        dataFolder = ArrayList()
        // to test
        dataFolder?.add(Data().apply {
            name = "Lập trình Android"
            numberLesson = 10
            avatar = Uri.parse("android.resource://com.example.flashcardapplication/drawable/avatar")
            nameAuthor = "Nguyễn Văn A"
        })
        dataFolder?.add(Data().apply {
            name = "Lập trình Android"
            numberLesson = 10
            avatar = Uri.parse("android.resource://com.example.flashcardapplication/drawable/avatar")
            nameAuthor = "Nguyễn Văn A"
        })

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
}

class Data : Serializable {
    var name: String? = null
    var numberLesson: Int? = null
    var avatar: Uri? = null
    var nameAuthor: String? = null
}

class DataAdapter(
    private val context: Context?,
    private val data: ArrayList<Data>) :
    RecyclerView.Adapter<DataAdapter.DataViewHolder>() {
    class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
    }

    override fun getItemCount(): Int {
        return data.size
    }
}