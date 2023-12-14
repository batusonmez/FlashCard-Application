package com.example.flashcardapplication.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val description: String,
    val status: String,
    val owner: String
){
    constructor(): this(0, "", "", "", "")
}

data class FolderWithTopics(
    @Embedded val folder: Folder,
    @Relation(
        parentColumn = "id",
        entity = Topic::class,
        entityColumn = "id",
        associateBy = Junction(
            value = TopicFolderCrossRef::class,
            parentColumn = "folderId",
            entityColumn = "topicId")
    )
    val topics: List<Topic>
)