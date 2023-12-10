package com.example.flashcardapplication

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapplication.databinding.ActivityCreateLessonBinding
import java.io.Serializable

class CreateLessonActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateLessonBinding
    private var data: ArrayList<Terminology>? = null
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_lesson)

        supportActionBar?.title = "Tạo học phần"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val topic = binding.edtTopic.text.toString()

        binding.tvScan.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        // handle to scan file excel

        binding.tvDescription.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        // handle to open dialog to input description

        // default to add 2 terminology
        data = ArrayList()
        data?.add(Terminology().apply {
            terminology = ""
            definition = ""
        })
        data?.add(Terminology().apply {
            terminology = ""
            definition = ""
        })

        data?.let {
            binding.rcvCreateLesson.adapter = CreateLessonAdapter(this, it)
        }
        binding.rcvCreateLesson.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this)

        binding.btnCreateLesson.setOnClickListener {
            data?.add(Terminology().apply {
                terminology = ""
                definition = ""
            })
            binding.rcvCreateLesson.adapter?.notifyDataSetChanged()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.create_lesson_option, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.i_save -> {
                val adapter = binding.rcvCreateLesson.adapter as CreateLessonAdapter
                val data = adapter.getTerminologyData()
                // handle to save data to database
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

class Terminology: Serializable {
    var terminology: String? = null
    var definition: String? = null
}

class CreateLessonAdapter(
    private val context: Context,
    private val data: ArrayList<Terminology>) :
    RecyclerView.Adapter<CreateLessonAdapter.ViewHolder>() {

    fun getTerminologyData(): ArrayList<Terminology> {
        return data
    }
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val edtTerminology: EditText? = view.findViewById(R.id.edt_terminology)
        val edtDefinition: EditText? = view.findViewById(R.id.edt_definition)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_create_lesson, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.edtTerminology?.setText(item.terminology)
        holder.edtDefinition?.setText(item.definition)

        holder.edtTerminology?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                item.terminology = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // do nothing
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // do nothing
            }
        })
        holder.edtDefinition?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                item.definition = s.toString()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // do nothing
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // do nothing
            }
        })
    }

    override fun getItemCount(): Int {
        return data.size
    }
}