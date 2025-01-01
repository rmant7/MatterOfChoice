package com.matterofchoice.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.matterofchoice.R
import com.matterofchoice.databinding.ActivityResultBinding
import com.matterofchoice.model.Option

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

        val option = intent.getParcelableExtra<Option>("selected")
        val optimal = intent.getStringExtra("optimal")

        val optimalAnswer = intent.getParcelableExtra<Option>("optimalAnswer")

        binding.continueBtn.setOnClickListener {
            val i = Intent(this, GameActivity::class.java)
            startActivity(i)
            finish()
        }


        try {
            binding.apply {
                optimalTV.text = optimal

                optimalAnswerTV.text = "\"${optimalAnswer!!.option}\""

                textView12.text = "Health: "+option?.health
                textView13.text = "Wealth: "+option?.wealth
                textView14.text = "Relationships: "+option?.relationships
                textView15.text = "Happiness: "+option?.happiness

                textView16.text = "Knowledge: "+option?.knowledge
                textView17.text = "Karma: "+option?.karma
                textView18.text = "TimeManagement: "+option?.timeManagement
                textView19.text = "EnvironmentalImpact: "+option?.environmentalImpact

                textView20.text = "PersonalGrowth: "+option?.personalGrowth
                textView21.text = "SocialResponsibility "+option?.socialResponsibility

            }
        }catch (e:Exception){
            Log.v("MOSAAAAAA",e.message.toString())
        }

    }
}