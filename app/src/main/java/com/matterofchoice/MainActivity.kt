package com.matterofchoice

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.matterofchoices.R
import com.matterofchoices.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val navController = findNavController(R.id.myFragment)
        binding.bottomNav.setupWithNavController(navController)
        val total = 2

//        val content = String(Files.readAllBytes(Paths.get("option_0.json")))
//
//        // Parse the JSON object
//        val jsonObject = JSONObject(content)
//
//        // Access the "option" value
//        val option = jsonObject.getString("option")
//        Log.v("ANSWER","Option: $option")
//
//        // Access the "cases" array
//        val casesArray: JSONArray = jsonObject.getJSONArray("cases")
//
//        // Iterate through the cases
//        for (i in 0 until casesArray.length()) {
//            val caseObject = casesArray.getJSONObject(i)
//            Log.v("ANSWER","Case: ${caseObject.getString("case")}")
//
//            // Access the "options" array
//            val optionsArray: JSONArray = caseObject.getJSONArray("options")
//            for (j in 0 until optionsArray.length()) {
//                val optionObject = optionsArray.getJSONObject(j)
//                Log.v("ANSWER","Option ${optionObject.getInt("number")}: ${optionObject.getString("option")}")
//                Log.v("ANSWER","Health: ${optionObject.getInt("health")}")
//                Log.v("ANSWER","Karma: ${optionObject.getInt("karma")}")
//            }
//
//            // Access the "optimal" value
//            val optimal = caseObject.getString("optimal")
//            Log.v("ANSWER","Optimal Option: $optimal")
//        }
       }
}