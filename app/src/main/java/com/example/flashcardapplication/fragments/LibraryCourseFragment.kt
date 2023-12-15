package com.example.flashcardapplication.fragments

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
import com.example.flashcardapplication.databinding.FragmentLibraryCourseBinding
import com.example.flashcardapplication.models.Topic
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class LibraryCourseFragment : Fragment() {
    private var auth : FirebaseAuth? = null
    private var data: ArrayList<LibraryCourse>? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = FirebaseAuth.getInstance()
        val binding = FragmentLibraryCourseBinding.inflate(inflater, container, false)

        data = ArrayList()
        val roomDb = context?.let { RoomDb.getDatabase(it) }
        val topic = roomDb?.ApplicationDao()
            ?.getAllTopics()
            ?.filter { it.owner == FirebaseAuth.getInstance().currentUser?.email }
            ?.sortedByDescending { it.dateAsTimestamp() }

        val filterTopic = filterTopicByMonth(topic, roomDb!!)
        data?.addAll(filterTopic)

        binding.rcvLibraryCourse.adapter = LibraryCourseAdapter(requireContext(), data!!)
        binding.rcvLibraryCourse.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        return binding.root
    }

    private fun Topic.dateAsTimestamp(): Long {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = dateFormat.parse(timestamp)
        return date?.time ?: 0L
    }

    private fun filterTopicByMonth(topics: List<Topic>?, roomDb: RoomDb): ArrayList<LibraryCourse> {
        val filter = ArrayList<LibraryCourse>()
        var libraryCourse = LibraryCourse(null, ArrayList())
        for(item in topics!!){
            val split = item.timestamp.split("/")
            val time = "Tháng " + split[1] + " " + split[2]
            val topicWithTerminologies = roomDb.ApplicationDao().getTopicWithTerminologies(item.id)
            val data = Data().apply {
                name = item.name
                numberLesson = topicWithTerminologies.terminologies.size
                avatar = auth?.currentUser?.photoUrl
                nameAuthor = auth?.currentUser?.displayName
            }
            if (libraryCourse.time == null){
                libraryCourse.time = time
                libraryCourse.data?.add(data)
            }
            else {
                if (libraryCourse.time == time){
                    libraryCourse.data?.add(data)
                }
                else {
                    filter.add(libraryCourse)
                    libraryCourse = LibraryCourse(null, ArrayList())
                    libraryCourse.time = time
                    libraryCourse.data?.add(data)
                }
            }
        }
        filter.add(libraryCourse)
        return filter
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
