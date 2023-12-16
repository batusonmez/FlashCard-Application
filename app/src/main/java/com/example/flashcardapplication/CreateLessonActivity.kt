package com.example.flashcardapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.MultiAutoCompleteTextView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.ActivityCreateLessonBinding
import com.example.flashcardapplication.models.Terminology
import com.example.flashcardapplication.models.Topic
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min

@Suppress("DEPRECATION", "NAME_SHADOWING")
class CreateLessonActivity : AppCompatActivity(), NetworkListener {
    private lateinit var binding: ActivityCreateLessonBinding
    private var data: ArrayList<Term>? = null
    private val networkReceiver = NetworkReceiver(listener = this)
    private val dataSyncHelper = DataSyncHelper(
        firebaseDb = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance(),
        context = this
    )

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_lesson)

        supportActionBar?.title = "Tạo học phần"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.tvScan.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        binding.tvScan.setOnClickListener {
            requestPermission.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        binding.tvDescription.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        binding.tvDescription.setOnClickListener {
            binding.tvDescription.visibility = View.GONE
            binding.llDescription.visibility = View.VISIBLE
        }

        // default to add 3 terminology
        data = ArrayList()
        for (i in 0..2) {
            data?.add(Term().apply {
                terminology = ""
                definition = ""
            })
        }
        data?.let {
            binding.rcvCreateLesson.adapter = CreateLessonAdapter(this, it, supportActionBar!!)
        }
        val layoutManager = LinearLayoutManager(this)
        binding.rcvCreateLesson.layoutManager = layoutManager

        binding.btnCreateLesson.setOnClickListener {
            data?.add(Term().apply {
                terminology = ""
                definition = ""
            })
            binding.rcvCreateLesson.adapter?.notifyItemInserted(data!!.size - 1)
            binding.rcvCreateLesson.adapter?.notifyItemRangeChanged(data!!.size - 1, data!!.size)
            binding.rcvCreateLesson.post {
                binding.rcvCreateLesson.smoothScrollToPosition(data!!.size - 1)
            }
            binding.tvDescription.visibility = View.VISIBLE
            binding.llDescription.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, intentFilter)
    }

    private var requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openFilePicker()
            }
        }
    private var filePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    if (isCSVFile(uri))
                        readCSVFile(uri)
                    else {
                        AlertDialog.Builder(this)
                            .setTitle("Lỗi")
                            .setMessage("File không đúng định dạng")
                            .setPositiveButton("OK") { _, _ -> }
                            .show()
                    }
                }
            }
        }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePicker.launch(intent)
    }

    private fun isCSVFile(uri: Uri): Boolean {
        return DocumentsContract.getDocumentId(uri).endsWith(".csv")
    }

    @SuppressLint("Recycle", "NotifyDataSetChanged")
    private fun readCSVFile(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        val bufferedReader = inputStream?.bufferedReader()
        data?.clear()

        val topic = bufferedReader?.readLine()
        binding.edtTopic.setText(topic)

        bufferedReader?.readLine()
        bufferedReader?.forEachLine {
            val line = it.split(",")
            data?.add(Term().apply {
                terminology = line[0]
                definition = line[1]
            })
        }
        bufferedReader?.close()
        binding.rcvCreateLesson.adapter = CreateLessonAdapter(this, data!!, supportActionBar!!)
        binding.rcvCreateLesson.adapter?.notifyItemRangeChanged(0, data!!.size)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.create_lesson_option, menu)
        return super.onCreateOptionsMenu(menu)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.i_save -> {
                val adapter = binding.rcvCreateLesson.adapter as CreateLessonAdapter
                val fullData = adapter.getTerminologyData()
                val database = RoomDb.getDatabase(this)
                val name: String = binding.edtTopic.text.toString().ifEmpty { "Nháp" }
                val description: String = binding.edtDescription.text.toString().ifEmpty { "" }
                val timestamp =
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val status = "public"
                val owner = FirebaseAuth.getInstance().currentUser?.email.toString()
                val topic = Topic(0, name, description, timestamp, status, owner)
                val id = database.ApplicationDao().insertTopic(topic).toInt()

                if (id > 0) {
                    val dataTerm = ArrayList<Terminology>()
                    for (item in fullData) {
                        dataTerm.add(Terminology(0, item.terminology!!, item.definition!!, id))
                    }
                    if (dataTerm.isNotEmpty()) {
                        database.ApplicationDao().insertAllTerminologies(dataTerm)
                    }
                }

                dataSyncHelper.setIsSync(false)

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("viewPager", 2)
                startActivity(intent)
                finish()
            }

            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNetworkAvailable() {
        GlobalScope.launch {
            if (!dataSyncHelper.getIsSyncDelete())
                dataSyncHelper.serverDelete()
            dataSyncHelper.syncData()
        }
    }

    override fun onNetworkUnavailable() {
        // nothing
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkReceiver)
    }
}

class Term(
    var terminology: String? = null,
    var definition: String? = null
) : Serializable {
    override fun toString(): String {
        return "Terminology(terminology=$terminology, definition=$definition)"
    }
}

class CreateLessonAdapter(
    private val context: Context,
    private val data: ArrayList<Term>,
    private val supportActionBar: androidx.appcompat.app.ActionBar
) :
    RecyclerView.Adapter<CreateLessonAdapter.ViewHolder>() {
    private var translatorToVN = false
    private var translator: Translator? = null
    private var textToSpeech: TextToSpeech? = null
    private val vocabularies = readVocabularyFromRawFile(R.raw.words_alpha, context)
    private val adapter =
        LimitedArrayAdapter(context, android.R.layout.simple_list_item_1, vocabularies)

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView? = view.findViewById(R.id.tv_number)
        val btnVolume: Button? = view.findViewById(R.id.btn_volume)
        val btnStar: Button? = view.findViewById(R.id.btn_star)
        val btnDelete: Button? = view.findViewById(R.id.btn_delete)
        val mactvTerminology: MultiAutoCompleteTextView? = view.findViewById(R.id.mactv_terminology)
        val edtDefinition: EditText? = view.findViewById(R.id.edt_definition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_create_lesson, parent, false)
        downloadModel()
        return ViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.tvNumber?.text = (position + 1).toString()
        holder.mactvTerminology?.setText(item.terminology)
        holder.edtDefinition?.setText(item.definition)

        holder.mactvTerminology?.setTokenizer(SpaceTokenizer())
        holder.mactvTerminology?.setAdapter(adapter)

        holder.mactvTerminology?.setOnItemClickListener { _, _, _, _ ->
            CoroutineScope(Dispatchers.Main).launch {
                if (translatorToVN) {
                    val translatedText = withContext(Dispatchers.IO) {
                        translator?.translate(holder.mactvTerminology.text.toString())?.await()
                    }
                    item.definition = translatedText
                    holder.edtDefinition?.setText(translatedText)
                }
                item.terminology = holder.mactvTerminology.text.toString()
            }
        }

        holder.mactvTerminology?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                CoroutineScope(Dispatchers.Main).launch {
                    if (translatorToVN) {
                        val translatedText = withContext(Dispatchers.IO) {
                            translator?.translate(s.toString())?.await()
                        }
                        item.definition = translatedText
                        holder.edtDefinition?.setText(translatedText)
                    }
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

        holder.mactvTerminology?.setOnFocusChangeListener { _, hasFocus ->
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
            if (data.size > 2) {
                data.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, data.size)
                if (position == data.size) {
                    supportActionBar.title = data.size.toString() + "/" + data.size
                }
            }
        }

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

    fun getTerminologyData(): ArrayList<Term> {
        return data.filter {
            it.terminology?.isNotEmpty() == true && it.definition?.isNotEmpty() == true
        }
                as ArrayList<Term>
    }

    private fun downloadModel() {
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

class SpaceTokenizer : MultiAutoCompleteTextView.Tokenizer {
    override fun findTokenStart(text: CharSequence, cursor: Int): Int {
        var i = cursor
        while (i > 0 && text[i - 1] != ' ') {
            i--
        }
        while (i < cursor && text[i] == ' ') {
            i++
        }
        return i
    }

    override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
        var i = cursor
        val len = text.length
        while (i < len) {
            if (text[i] == ' ') {
                return i
            } else {
                i++
            }
        }
        return len
    }

    override fun terminateToken(text: CharSequence): CharSequence {
        var i = text.length
        while (i > 0 && text[i - 1] == ' ') {
            i--
        }
        return if (i > 0 && text[i - 1] == ' ') {
            text
        } else {
            if (text is Spanned) {
                val sp = SpannableString("$text ")
                TextUtils.copySpansFrom(
                    text, 0, text.length,
                    Any::class.java, sp, 0
                )
                sp
            } else {
                "$text "
            }
        }
    }
}