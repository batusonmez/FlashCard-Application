package com.example.flashcardapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.flashcardapplication.databinding.ActivityCreateLessonBinding

class CreateLessonActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateLessonBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_lesson)

    }
}