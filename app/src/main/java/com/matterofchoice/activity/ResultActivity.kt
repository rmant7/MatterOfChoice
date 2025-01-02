package com.matterofchoice.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.matterofchoice.R
import com.matterofchoice.databinding.ActivityResultBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)


        val userScore = sharedPreferences.getInt("userScore",0)
        val totalScore = sharedPreferences.getInt("totalScore",0)

        val calculatedRating = (userScore.toFloat() / totalScore.toFloat()) * 5

        binding.ratingBar.animate()
            .setDuration(500)
            .rotationY(360f)
            .withEndAction {
                binding.ratingBar.rating = calculatedRating
            }
            .start()
        lifecycleScope.launch {
            anim(userScore.toString(),binding.userScoreTV)
        }




        binding.continueBtn.setOnClickListener {
            val i = Intent(this, WelcomeActivity::class.java)
            startActivity(i)
            finish()
        }

    }

    private suspend fun anim(textAnim: String, tV: TextView) {
        val stringBuilder = StringBuilder()
        withContext(Dispatchers.IO) {
            for (letter in textAnim) {
                stringBuilder.append(letter)
                delay(50)
                withContext(Dispatchers.Main) {
                    tV.text = stringBuilder.toString()
                }
            }
        }
    }
}