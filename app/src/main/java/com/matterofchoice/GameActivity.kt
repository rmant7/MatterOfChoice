package com.matterofchoice

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.matterofchoice.model.Case
import com.matterofchoices.R
import com.matterofchoices.databinding.ActivityGameBinding


class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val myViewModel = ViewModelProvider(this)[ChatViewModel::class.java]


        lifecycleScope.launchWhenStarted {
            myViewModel.main()

        }

        myViewModel.imageLiveData.observe(this){ image ->
            binding.situationIV.setImageBitmap(image)

        }





        myViewModel.listContent.observe(this) { listContent ->

            if (listContent != null) {
                try {
                    listContent.let {
                        val gson = Gson()
                        val listType = object : TypeToken<List<Case>>() {}.type
                        val cases: List<Case> =
                            gson.fromJson(listContent.toString(), listType)

                        Log.v("CASESLIST", cases.toString())

                        binding.apply {
                            optionTxt.text = cases[0].case
                            option1TV.text = cases[0].options[0].option
                            option2TV.text = cases[0].options[1].option
                            option3TV.text = cases[0].options[2].option
                            option4TV.text = cases[0].options[3].option
                        }
                        myViewModel.generateAndDisplayImage(cases[0].case)

                    }
                } catch (e: Exception) {
                    Log.v("DISPLAYING", e.message.toString())
                }
            } else {
                Log.v("GAMEACTIVITY", "It's empty")
            }

        }
    }


}





