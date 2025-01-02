package com.matterofchoice.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.matterofchoice.R
import com.matterofchoice.databinding.ActivityGameBinding
import com.matterofchoice.model.Case
import com.matterofchoice.viewmodel.ChatViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private var selectedOption: String? = null
    private var isError: Boolean = false
    private var isFirstPress = true


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

        val editor = sharedPreferences.edit()


        // Retrieve saved values
        val userGender = sharedPreferences.getString("userGender", "Not specified")
        val userLanguage = sharedPreferences.getString("userLanguage", "Not specified")
        val userAge = sharedPreferences.getString("userAge", "Not specified")
        val userSubject = sharedPreferences.getString("userSubject", "Not specified")


        initiateMethod(myViewModel, userGender!!, userLanguage!!, userAge!!, userSubject!!)


        myViewModel.errorLiveData.observe(this) { error ->
            if (!error.isNullOrEmpty()) {
                binding.apply {
                    isError = true
                    animationView.visibility = View.GONE
                    errorTV.visibility = View.VISIBLE
                    errorTV.text = error
                }
            }
        }

        lifecycleScope.launch {
            while (!isError) {
                binding.errorTV.text = "Generating situations based on your subject..."
                delay(5000)
                if (isError) break

                binding.errorTV.text = "Analyzing..."
                delay(3000)
                if (isError) break

                binding.errorTV.text = "Almost done..."
                delay(3000)
                if (isError) break

                binding.errorTV.text = "Done"
                break
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

                            situationIV.visibility = View.VISIBLE

                            CoroutineScope(Dispatchers.Main).launch {
                                anim(cases[0].case, optionTxt)

                                // Sequentially add and animate RadioButtons
                                for (option in cases[0].options) {
                                    val radioBtn = RadioButton(applicationContext)
                                    radioBtn.typeface = ResourcesCompat.getFont(
                                        applicationContext,
                                        R.font.merriweather_sans
                                    )
                                    radioBtn.setPadding(4, 4, 4, 8)
                                    radioGroup.addView(radioBtn)
                                    // Wait for the animation to complete before moving to the next
                                    anim(option.option, radioBtn)
                                }
                                button3.visibility = View.VISIBLE
                            }

                            fun updateButtonState() {
                                button3.isEnabled = !selectedOption.isNullOrEmpty()
                            }

                            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                                val selectedRadioButton = findViewById<RadioButton>(checkedId)
                                selectedOption = selectedRadioButton?.text?.toString() ?: ""
                                updateButtonState()
                            }
                        }
                        binding.button3.setOnClickListener {
                            if (isFirstPress) {
                                val correctAnswer =
                                    cases[0].options.firstOrNull { it.number.toString() == cases[0].optimal }
                                correctAnswer?.number
                                for (i in 0 until binding.radioGroup.childCount) {
                                    val view = binding.radioGroup.getChildAt(i)
                                    if (view is RadioButton) {
                                        try {
                                            if (view.text == correctAnswer?.option) {
                                                view.setTextColor(
                                                    resources.getColor(
                                                        R.color.correctColor,
                                                        theme
                                                    )
                                                )
                                            } else {
                                                view.setTextColor(
                                                    resources.getColor(
                                                        R.color.wrongColor,
                                                        theme
                                                    )
                                                )
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                applicationContext,
                                                e.message.toString(),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                                binding.seeResultBtn.visibility = View.VISIBLE
                                isFirstPress = false
                            } else {
                                val i =
                                    Intent(this@GameActivity, GameActivity::class.java)
                                startActivity(i)
                                finish()
                            }
                        }

                        binding.seeResultBtn.setOnClickListener {
                            if (!selectedOption.isNullOrEmpty()) {
                                val i =
                                    Intent(this@GameActivity, ResultActivity::class.java)


                                val userChoice =
                                    cases[0].options.find { it.option == selectedOption }
                                var userScore = 0

                                userChoice!!.apply {
                                    userScore += health + wealth + relationships + happiness + knowledge + karma + timeManagement +
                                            environmentalImpact + personalGrowth + socialResponsibility
                                }
                                editor.putInt("userScore", userScore)
                                editor.apply()

                                var totalScore = 0

                                val optimalOption =
                                    cases[0].options.find { it.number == cases[0].optimal.toInt() }

                                optimalOption!!.apply {
                                    totalScore += health + wealth + relationships + happiness + knowledge + karma + timeManagement +
                                            environmentalImpact + personalGrowth + socialResponsibility
                                }
                                editor.putInt("totalScore", totalScore)
                                editor.apply()


                                startActivity(i)
                                finish()
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

    private fun initiateMethod(
        myViewModel: ChatViewModel,
        userGender: String,
        userLanguage: String,
        userAge: String,
        userSubject: String
    ) {
        lifecycleScope.launchWhenStarted {
            myViewModel.getUserInfo(userGender, userLanguage, userAge, userSubject)
            myViewModel.main()

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





