package com.example.flashcardapplication.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Terminology(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val terminology: String,
    val definition: String,
    val topicId: Int
) {
    constructor() : this(0, "", "", 0)
}