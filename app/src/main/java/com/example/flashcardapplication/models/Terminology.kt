package com.example.flashcardapplication.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Terminology(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    var terminology: String,
    var definition: String,
    val topicId: Int
) {
    constructor() : this(0, "", "", 0)
}