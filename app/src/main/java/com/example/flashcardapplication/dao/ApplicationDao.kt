package com.example.flashcardapplication.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.flashcardapplication.models.Folder
import com.example.flashcardapplication.models.FolderWithTopics
import com.example.flashcardapplication.models.Terminology
import com.example.flashcardapplication.models.Topic
import com.example.flashcardapplication.models.TopicFolderCrossRef
import com.example.flashcardapplication.models.TopicWithFolders
import com.example.flashcardapplication.models.TopicWithTerminologies

@Dao
interface ApplicationDao {
    @Transaction
    @Query("SELECT * FROM terminology")
    fun getAllTerminologies(): List<Terminology>

    @Transaction
    @Query("SELECT * FROM terminology WHERE id = :id")
    fun getTerminologyById(id: Int): Terminology

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTerminology(terminology: Terminology)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllTerminologies(terminologies: List<Terminology>) {
        terminologies.forEach {
            insertTerminology(it)
        }
    }

    @Transaction
    @Update
    fun updateTerminology(terminology: Terminology)

    @Transaction
    @Delete
    fun deleteTerminology(terminology: Terminology)

    @Transaction
    @Query("DELETE FROM terminology")
    fun deleteAllTerminologies()

    @Transaction
    @Query("SELECT * FROM topic")
    fun getAllTopics(): List<Topic>

    @Transaction
    @Query("SELECT * FROM topic WHERE id = :id")
    fun getTopicById(id: Int): Topic

    @Transaction
    @Query("SELECT * FROM topic WHERE owner = :owner")
    fun getTopicsByOwner(owner: String): List<Topic>

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTopic(topic: Topic) : Long

    @Transaction
    @Update
    fun updateTopic(topic: Topic)

    @Transaction
    @Delete
    fun deleteTopic(topic: Topic)

    @Transaction
    @Query("DELETE FROM topic")
    fun deleteAllTopics()

    @Transaction
    @Query("SELECT * FROM folder")
    fun getAllFolders(): List<Folder>

    @Transaction
    @Query("SELECT * FROM folder WHERE id = :id")
    fun getFolderById(id: Int): Folder

    @Transaction
    @Query("SELECT * FROM folder WHERE owner = :owner")
    fun getFoldersByOwner(owner: String): List<Folder>

    @Transaction
    @Insert
    fun insertFolder(folder: Folder)

    @Transaction
    @Update
    fun updateFolder(folder: Folder)

    @Transaction
    @Delete
    fun deleteFolder(folder: Folder)

    @Transaction
    @Query("DELETE FROM folder")
    fun deleteAllFolders()

    @Transaction
    @Query("SELECT * FROM topicfoldercrossref")
    fun getAllTopicFolderCrossRef(): List<TopicFolderCrossRef>

    @Transaction
    @Insert
    fun insertTopicFolderCrossRef(crossRef: TopicFolderCrossRef)

    @Transaction
    @Delete
    fun deleteTopicFolderCrossRef(crossRef: TopicFolderCrossRef)

    @Transaction
    @Query("DELETE FROM topicfoldercrossref WHERE topicId = :topicId")
    fun deleteTopicFolderCrossRefByTopicId(topicId: Int)

    @Transaction
    @Query("DELETE FROM topicfoldercrossref WHERE folderId = :folderId")
    fun deleteTopicFolderCrossRefByFolderId(folderId: Int)

    @Transaction
    @Query("DELETE FROM topicfoldercrossref")
    fun deleteAllTopicFolderCrossRef()

    @Transaction
    @Query("SELECT * FROM topic")
    fun getTopicsWithFolders(): List<TopicWithFolders>

    @Transaction
    @Query("SELECT * FROM folder")
    fun getFoldersWithTopics(): List<FolderWithTopics>

    @Transaction
    @Query("SELECT * FROM topic WHERE id = :topicId")
    fun getTopicWithTerminologies(topicId: Int): TopicWithTerminologies
}
