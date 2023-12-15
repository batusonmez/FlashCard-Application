package com.example.flashcardapplication.database

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import com.example.flashcardapplication.models.Folder
import com.example.flashcardapplication.models.Terminology
import com.example.flashcardapplication.models.TopicFolderCrossRef
import com.example.flashcardapplication.models.Topic
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class DataSyncHelper(
    private val firebaseDb: FirebaseFirestore,
    private val auth: FirebaseAuth,
    context: Context
){
    private var ref : ArrayList<TopicFolderCrossRef> = ArrayList()
    private var terminology : ArrayList<Terminology> = ArrayList()
    private var folderDelete: ArrayList<Folder> = ArrayList()
    private var topicDelete: ArrayList<Topic> = ArrayList()
    var database:RoomDb = RoomDb.getDatabase(context)
    private var isSync : Boolean = false
    private var isSyncDelete : Boolean = true
    fun getIsSync() : Boolean{
        return isSync
    }
    fun setIsSync(isSync : Boolean){
        this.isSync = isSync
    }
    fun getIsSyncDelete() : Boolean{
        return isSyncDelete
    }
    fun setIsSyncDelete(isSyncDelete : Boolean){
        this.isSyncDelete = isSyncDelete
    }

    suspend fun syncData() {
        if(!isSync){
            syncUp()
            syncDown()
            setIsSync(true)
        }
    }

    @SuppressLint("RestrictedApi")
    private suspend fun syncUp() {
        // get data from local db
        val folders = database.ApplicationDao()
            .getFoldersByOwner(auth.currentUser?.email.toString())

        val topics = database.ApplicationDao()
            .getTopicsByOwner(auth.currentUser?.email.toString())

        val refs = database.ApplicationDao()
            .getAllTopicFolderCrossRef()
            .filter { it.topicId in topics.map { topic -> topic.id }
                    && it.folderId in folders.map { folder -> folder.id } }

        val terminologies = database.ApplicationDao()
            .getAllTerminologies()
            .filter { it.topicId in topics.map { topic -> topic.id } }

        // update data to firebase
        for (folder in folders) {
            auth.currentUser?.uid?.let {
                firebaseDb.collection("data")
                    .document(it)
                    .collection("folders")
                    .document(folder.id.toString())
                    .set(folder)
                    .await()
            }
        }
        for (topic in topics) {
            auth.currentUser?.uid?.let {
                firebaseDb.collection("data")
                    .document(it)
                    .collection("topics")
                    .document(topic.id.toString())
                    .set(topic)
                    .await()
            }
        }

        val refCollection = firebaseDb.collection("data")
            .document(auth.currentUser?.uid.toString())
            .collection("topic_folder_refs")
        for (ref in refs) {
            auth.currentUser?.uid?.let {
                refCollection.whereEqualTo("topicId", ref.topicId)
                    .whereEqualTo("folderId", ref.folderId)
                    .get().addOnSuccessListener { querySnapshot ->
                        if (querySnapshot.isEmpty) {
                            refCollection.add(ref)
                        }
                    }
                    .await()
            }
        }
        for (terminology in terminologies) {
            auth.currentUser?.uid?.let {
                firebaseDb.collection("data")
                    .document(it)
                    .collection("terminologies")
                    .document(terminology.id.toString())
                    .set(terminology)
                    .await()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private suspend fun syncDown() {
        database.ApplicationDao().deleteAllTopicFolderCrossRef()
        database.ApplicationDao().deleteAllTerminologies()
        database.ApplicationDao().deleteAllTopics()
        database.ApplicationDao().deleteAllFolders()


        // get data from firebase
        val terminologyTask = auth.currentUser?.uid?.let {
            firebaseDb.collection("data")
                .document(it)
                .collection("terminologies")
                .get()
                .await()
        }
        val refTask = auth.currentUser?.uid?.let {
            firebaseDb.collection("data")
                .document(it)
                .collection("topic_folder_refs")
                .get()
                .await()
        }
        val topicTask = auth.currentUser?.uid?.let {
            firebaseDb.collection("data")
                .document(it)
                .collection("topics")
                .get()
                .await()
        }
        val folderTask = auth.currentUser?.uid?.let {
            firebaseDb.collection("data")
                .document(it)
                .collection("folders")
                .get()
                .await()
        }

        if (terminologyTask != null) {
            for (document in terminologyTask.documents) {
                val terminology = document.toObject(Terminology::class.java)
                if (terminology != null) {
                    database.ApplicationDao().insertTerminology(terminology)
                }
            }
        }
        if (refTask != null) {
            for (document in refTask.documents) {
                val ref = document.toObject(TopicFolderCrossRef::class.java)
                if (ref != null) {
                    database.ApplicationDao().insertTopicFolderCrossRef(ref)
                }
            }
        }
        if(topicTask != null){
            for (document in topicTask.documents) {
                val topic = document.toObject(Topic::class.java)
                if (topic != null) {
                    database.ApplicationDao().insertTopic(topic)
                }
            }
        }
        if(folderTask != null){
            for (document in folderTask.documents) {
                val folder = document.toObject(Folder::class.java)
                if (folder != null) {
                    database.ApplicationDao().insertFolder(folder)
                }
            }
        }
    }

    // run when network isn't available
    fun localDelete(folder: Folder?, topic: Topic?) {
        setIsSyncDelete(false)
        if(folder != null && topic != null){
            ref.addAll(database.ApplicationDao().getAllTopicFolderCrossRef()
                .filter { it.topicId == topic.id && it.folderId == folder.id })
            database.ApplicationDao().deleteTopicFolderCrossRef(
                TopicFolderCrossRef(topic.id, folder.id)
            )
            return
        }
        if (folder != null) {
            ref.addAll(database.ApplicationDao().getAllTopicFolderCrossRef()
                .filter { it.folderId == folder.id })
            for (r in ref){
                database.ApplicationDao().deleteTopicFolderCrossRef(r)
            }
            folderDelete.add(folder)
            database.ApplicationDao().deleteFolder(folder)
            return
        }
        if (topic != null) {
            ref.addAll(database.ApplicationDao().getAllTopicFolderCrossRef()
                .filter { it.topicId == topic.id })
            for (reference in ref){
                database.ApplicationDao().deleteTopicFolderCrossRef(reference)
            }

            terminology.addAll(database.ApplicationDao().getAllTerminologies()
                .filter { it.topicId == topic.id })
            for (term in terminology){
                database.ApplicationDao().deleteTerminology(term)
            }

            topicDelete.add(topic)
            database.ApplicationDao().deleteTopic(topic)
            return
        }
    }
    // sync data when from offline to online
    suspend fun serverDelete() {
        val refCollection = firebaseDb.collection("data")
            .document(auth.currentUser?.uid.toString())
            .collection("topic_folder_refs")

        for(term in terminology){
            auth.currentUser?.uid?.let {
                firebaseDb.collection("data")
                    .document(it)
                    .collection("terminologies")
                    .document(term.id.toString())
                    .delete()
                    .addOnSuccessListener {
                        terminology.remove(term)
                    }.await()
            }
        }

        for (reference in ref) {
            refCollection.whereEqualTo("topicId", reference.topicId)
                .whereEqualTo("folderId", reference.folderId)
                .get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.delete()
                    }
                }.await()
        }

        for (folder in folderDelete) {
            refCollection.whereEqualTo("folderId", folder.id)
                .get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.delete()
                    }
                }.await()
        }

        for (topic in topicDelete) {
            refCollection.whereEqualTo("topicId", topic.id)
                .get().addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.delete()
                    }
                }.await()
        }

        setIsSyncDelete(true)
    }
}

@Suppress("DEPRECATION")
class NetworkReceiver(
    private var listener: NetworkListener
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            val connectivityManager =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected) {
                listener.onNetworkAvailable()
            } else {
                listener.onNetworkUnavailable()
            }
        }
    }
}

interface NetworkListener {
    fun onNetworkAvailable()
    fun onNetworkUnavailable()
}