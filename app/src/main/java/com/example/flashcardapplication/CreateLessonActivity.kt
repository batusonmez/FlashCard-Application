package com.example.flashcardapplication

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapplication.databinding.ActivityCreateLessonBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.Serializable
import kotlin.math.min

@Suppress("NAME_SHADOWING")
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
            binding.rcvCreateLesson.adapter = CreateLessonAdapter(this, it, supportActionBar!!)
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

                // to test data
                val dataTest: ArrayList<Terminology>?
                dataTest = (binding.rcvCreateLesson.adapter as CreateLessonAdapter).getTerminologyData()

                for (item in dataTest) {
                    Log.e("TAG", "onCreate: " + item.terminology + ", " + item.definition)
                }
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
    private val data: ArrayList<Terminology>,
    private val supportActionBar: androidx.appcompat.app.ActionBar) :
    RecyclerView.Adapter<CreateLessonAdapter.ViewHolder>() {
    private var translatorToVN = false
    private var translator: Translator? = null
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnVolume: Button? = view.findViewById(R.id.btn_volume)
        val btnStar: Button? = view.findViewById(R.id.btn_star)
        val btnDelete: Button? = view.findViewById(R.id.btn_delete)
        val actvTerminology: AutoCompleteTextView? = view.findViewById(R.id.actv_terminology)
        val edtDefinition: EditText? = view.findViewById(R.id.edt_definition)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_create_lesson, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.actvTerminology?.setText(item.terminology)
        holder.edtDefinition?.setText(item.definition)

        val vocabularies = readVocabularyFromRawFile(R.raw.words_alpha, context)
        val adapter =
            LimitedArrayAdapter(context, android.R.layout.simple_list_item_1, vocabularies)
        holder.actvTerminology?.setAdapter(adapter)

        downloadModel()
        holder.actvTerminology?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                CoroutineScope(Dispatchers.Main).launch {
                    if (translatorToVN) {
                        val translatedText = withContext(Dispatchers.IO) {
                            translator?.translate(s.toString())?.await()
                        }
                        item.definition = translatedText
                        holder.edtDefinition?.setText(translatedText)
                    }
                    item.terminology = s.toString()
                }
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

        holder.actvTerminology?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                supportActionBar.title = (position + 1).toString() + "/" + data.size
            }
        }
        holder.edtDefinition?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                supportActionBar.title = (position + 1).toString() + "/" + data.size
            }
        }

        holder.btnDelete?.setOnClickListener {
            if(data.size > 2){
                data.removeAt(position)
                notifyDataSetChanged()
                if (position == data.size) {
                    supportActionBar.title = data.size.toString() + "/" + data.size
                }
            }
        }

        var textToSpeech: TextToSpeech? = null
        holder.btnVolume?.setOnClickListener {
            textToSpeech = TextToSpeech(context) { status ->
                if (status != TextToSpeech.ERROR) {
                    textToSpeech?.language = java.util.Locale.US
                    textToSpeech?.setSpeechRate(0.8f)
                    textToSpeech?.speak(item.terminology, TextToSpeech.QUEUE_ADD, null, null)
                }
            }
        }

        holder.btnStar?.setOnClickListener {
            // handle to add terminology to favorite
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    private fun readVocabularyFromRawFile(
        rawResourseId: Int,
        context: Context
    ): ArrayList<String> {
        val vocabularies = ArrayList<String>()
        context.resources.openRawResource(rawResourseId).bufferedReader().useLines { lines ->
            vocabularies.addAll(lines)
        }
        return vocabularies
    }
    fun getTerminologyData(): ArrayList<Terminology> {
        return data.filter {
            it.terminology?.isNotEmpty() == true && it.definition?.isNotEmpty() == true }
                as ArrayList<Terminology>
    }

    private fun downloadModel(){
        translator = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.VIETNAMESE)
            .build().let {
                Translation.getClient(it)
            }
        translator?.downloadModelIfNeeded(DownloadConditions.Builder().build())
            ?.addOnSuccessListener {
                translatorToVN = true
            }
            ?.addOnFailureListener {
                translatorToVN = false
            }
    }
}

class LimitedArrayAdapter(
    context: Context,
    resource: Int,
    data: ArrayList<String>,
) :
    ArrayAdapter<String>(context, resource, data) {
    override fun getCount(): Int {
        return min(super.getCount(), 5)
    }
}