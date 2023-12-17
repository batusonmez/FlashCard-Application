package com.example.flashcardapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flashcardapplication.database.DataSyncHelper
import com.example.flashcardapplication.database.NetworkListener
import com.example.flashcardapplication.database.NetworkReceiver
import com.example.flashcardapplication.database.RoomDb
import com.example.flashcardapplication.databinding.ActivityFolderBinding
import com.example.flashcardapplication.databinding.CustomViewDialogBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("DEPRECATION", "NAME_SHADOWING")
class FolderActivity : AppCompatActivity(), NetworkListener {
    private lateinit var binding: ActivityFolderBinding
    private var auth : FirebaseAuth? = null
    private val networkReceiver = NetworkReceiver(listener = this)
    private val dataSyncHelper = DataSyncHelper(
        firebaseDb = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance(),
        context = this
    )
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_folder)
        auth = FirebaseAuth.getInstance()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Thư mục"

        val folderId = intent.getIntExtra("folderId", 0)
        val roomDb = RoomDb.getDatabase(this)
        val folder = roomDb.ApplicationDao().getFolderById(folderId)
        val folderWithTopic = roomDb.ApplicationDao()
            .getFoldersWithTopics()
            .filter { it.folder.id == folderId }[0]

        binding.tvNumberLesson.text = folderWithTopic.topics.size.toString() + " học phần"
        Picasso.get().load(auth?.currentUser?.photoUrl).into(binding.civAvatar)
        binding.tvNameAuthor.text = auth?.currentUser?.displayName
        binding.tvFolderName.text = folder.name

        if (folderWithTopic.topics.isNotEmpty()) {
            binding.layoutEmptyFolder.visibility = android.view.View.GONE
            binding.rcvLesson.visibility = android.view.View.VISIBLE
        }else{
            binding.layoutEmptyFolder.visibility = android.view.View.VISIBLE
            binding.rcvLesson.visibility = android.view.View.GONE
        }

        binding.btnAddLesson.setOnClickListener {
            val intent = Intent(this, AddReferencesActivity::class.java)
            intent.putExtra("folderId", folderId)
            startActivity(intent)
        }

        binding.rcvLesson.adapter = AddReferencesAdapter(this, folderWithTopic.topics, roomDb)
        binding.rcvLesson.layoutManager = LinearLayoutManager(this)
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, intentFilter)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onResume() {
        super.onResume()
        GlobalScope.launch {
            if(!dataSyncHelper.getIsSyncDelete())
                dataSyncHelper.serverDelete()
            dataSyncHelper.syncData()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNetworkAvailable() {
        GlobalScope.launch {
            if(!dataSyncHelper.getIsSyncDelete())
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.folder_menu, menu)
        return true
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val bindingDialog = CustomViewDialogBinding.inflate(layoutInflater)
        val roomDb = RoomDb.getDatabase(this)
        val folderId = intent.getIntExtra("folderId", 0)
        val folder = roomDb.ApplicationDao().getFolderById(folderId)
        bindingDialog.edtName.setText(folder.name)
        bindingDialog.edtDescription.setText(folder.description)
        return when (item.itemId) {
            R.id.i_add_folder -> {
                val intent = Intent(this, AddReferencesActivity::class.java)
                intent.putExtra("folderId", folderId)
                startActivity(intent)
                true
            }
            R.id.i_modify_folder -> {
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Sửa thư mục")
                    .setView(bindingDialog.root)
                    .setPositiveButton("OK") { dialog, _ ->
                        val name = bindingDialog.edtName.text.toString()
                        if (name.isNotEmpty()) {
                            val description = bindingDialog.edtDescription.text.toString()
                            folder.name = name
                            folder.description = description
                            roomDb.ApplicationDao().updateFolder(folder)
                            binding.tvFolderName.text = name
                            dataSyncHelper.setIsSync(false)
                            dialog.dismiss()
                        }
                    }
                    .setNegativeButton("Hủy") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.show()
                true
            }
            R.id.i_delete_folder -> {
                val topicSelected= (binding.rcvLesson.adapter as AddReferencesAdapter).getDataClick()
                val roomDb = RoomDb.getDatabase(this)
                val folderId = intent.getIntExtra("folderId", 0)
                val folder = roomDb.ApplicationDao().getFolderById(folderId)

                if(topicSelected.isEmpty()){
                    val dialog = AlertDialog.Builder(this)
                        .setTitle("Xóa thư mục")
                        .setMessage("Bạn có chắc chắn muốn xóa thư mục này không?")
                        .setPositiveButton("OK") { dialog, _ ->
                            dataSyncHelper.localDelete(folder, null)
                            dataSyncHelper.setIsSync(false)
                            dataSyncHelper.setIsSyncDelete(false)
                            dialog.dismiss()

                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("viewPager", 2)
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("Hủy") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                    dialog.show()
                }else {
                    val dialog = AlertDialog.Builder(this)
                        .setTitle("Xóa học phần")
                        .setMessage("Bạn có chắc chắn muốn tất cả học phần đã chọn?")
                        .setPositiveButton("OK") { dialog, _ ->
                            for (item in topicSelected) {
                                dataSyncHelper.localDelete(folder, item)
                            }
                            dataSyncHelper.setIsSync(false)
                            dataSyncHelper.setIsSyncDelete(false)
                            GlobalScope.launch {
                                if(!dataSyncHelper.getIsSyncDelete())
                                    dataSyncHelper.serverDelete()
                                dataSyncHelper.syncData()
                            }
                            dialog.dismiss()

                            val intent = Intent(this, FolderActivity::class.java)
                            intent.putExtra("folderId", folderId)
                            startActivity(intent)
                            finish()
                        }
                        .setNegativeButton("Hủy") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                    dialog.show()
                }
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}