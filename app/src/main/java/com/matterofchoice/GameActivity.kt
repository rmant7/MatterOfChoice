package com.matterofchoice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.matterofchoice.databinding.ActivityGameBinding
import com.matterofchoice.model.Case


class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private var selectedOption: String? = null


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
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

        // Retrieve saved values
        val userGender = sharedPreferences.getString("userGender", "Not specified")
        val userLanguage = sharedPreferences.getString("userLanguage", "Not specified")
        val userAge = sharedPreferences.getString("userAge", "Not specified")
        val userSubject = sharedPreferences.getString("userSubject", "Not specified")


        initiateMethod(myViewModel, userGender!!,userLanguage!!,userAge!!,userSubject!!)

        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioButton = findViewById<RadioButton>(checkedId)
            selectedOption = selectedRadioButton.text.toString()
        }
        myViewModel.errorLiveData.observe(this){ error ->
            if (!error.isNullOrEmpty()){
                binding.apply {
                    animationView.visibility = View.GONE
                    errorTV.visibility = View.VISIBLE
                    errorTV.text = error
                }

            }

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
                            binding.constraintLoad.visibility = View.GONE

                            button3.visibility = View.VISIBLE
                            situationIV.visibility = View.VISIBLE

                            optionTxt.text = cases[0].case
                            option1TV.text = cases[0].options[0].option
                            option2TV.text = cases[0].options[1].option
                            option3TV.text = cases[0].options[2].option
                            option4TV.text = cases[0].options[3].option


                            button3.setOnClickListener {
                                if (selectedOption != null) {
                                    val i = Intent(this@GameActivity, ResultActivity::class.java)
                                    i.putExtra(
                                        "selected",
                                        cases[0].options.find { it.option == selectedOption })

                                    i.putExtra("optimal", cases[0].optimal)

                                    val optimalOption =
                                        cases[0].options.find { it.number == cases[0].optimal.toInt() }

                                    i.putExtra("optimalAnswer", optimalOption)
                                    startActivity(i)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.v("DISPLAYING", e.message.toString())
                }
            } else {
                Log.v("GAMEACTIVITY", "It's empty")
            }
        }
    }

    private fun initiateMethod(myViewModel:ChatViewModel,userGender:String, userLanguage:String,userAge:String, userSubject: String){
        lifecycleScope.launchWhenStarted {
            myViewModel.getUserInfo(userGender, userLanguage, userAge, userSubject)
            myViewModel.main()

        }
    }

}





