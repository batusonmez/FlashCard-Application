package com.example.flashcardapplication.models

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["topicId", "folderId"])
data class TopicFolderCrossRef(
    @ColumnInfo(index = true)
    val topicId: Int,
    @ColumnInfo(index = true)
    val folderId: Int
){
    constructor() : this(0, 0)
}
