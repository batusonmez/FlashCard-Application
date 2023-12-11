package com.example.flashcardapplication.fragments

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
import com.example.flashcardapplication.databinding.FragmentLibraryCourseBinding

class LibraryCourseFragment : Fragment() {
    private var data: ArrayList<LibraryCourse>? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentLibraryCourseBinding.inflate(inflater, container, false)

        data = ArrayList()
        // to test
        data?.add(LibraryCourse().apply {
            time = "Tháng 12 2022"
            data = ArrayList<Data>().apply {
                add(Data().apply {
                    name = "Lập trình Android"
                    numberLesson = 10
                    avatar = Uri.parse("android.resource://com.example.flashcardapplication/drawable/avatar")
                    nameAuthor = "Nguyễn Văn A"
                })
            }
        })
        data?.add(LibraryCourse().apply {
            time = "Tháng 1 2023"
            data = ArrayList<Data>().apply {
                add(Data().apply {
                    name = "Lập trình Android"
                    numberLesson = 10
                    avatar = Uri.parse("android.resource://com.example.flashcardapplication/drawable/avatar")
                    nameAuthor = "Nguyễn Văn A"
                })
                add(Data().apply {
                    name = "Lập trình Android"
                    numberLesson = 10
                    avatar = Uri.parse("android.resource://com.example.flashcardapplication/drawable/avatar")
                    nameAuthor = "Nguyễn Văn A"
                })
                add(Data().apply {
                    name = "Lập trình Android"
                    numberLesson = 10
                    avatar = Uri.parse("android.resource://com.example.flashcardapplication/drawable/avatar")
                    nameAuthor = "Nguyễn Văn A"
                })
            }
        })

        binding.rcvLibraryCourse.adapter = LibraryCourseAdapter(requireContext(), data!!)
        binding.rcvLibraryCourse.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        return binding.root
    }

}

class LibraryCourse(
    var time: String? = null,
    var data: ArrayList<Data>? = null,
)

class LibraryCourseAdapter(
    private val context: Context,
    private val data: ArrayList<LibraryCourse>
) : RecyclerView.Adapter<LibraryCourseAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val time : TextView? = itemView.findViewById(R.id.tv_time)
        var rcvLibraryCourse : RecyclerView? = itemView.findViewById(R.id.rcv_course)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_library_course, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.time?.text = item.time

        val itemRcv = DataAdapter(context, item.data!!)
        holder.rcvLibraryCourse?.adapter = itemRcv
        holder.rcvLibraryCourse?.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(context)
    }

}
