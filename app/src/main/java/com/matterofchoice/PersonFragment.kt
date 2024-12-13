package com.matterofchoice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.matterofchoices.databinding.PersonFragmentBinding

class PersonFragment: Fragment() {
    private lateinit var binding:PersonFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PersonFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val genders = listOf("Choose your gender","Male","Female")
        val languages = listOf("Choose your language","English","Arabic","Amharic","Bengali","Chinese Simplified","Dutch","French","German","Hebrew","Hindi","Italian","Japanese","Korean","Portuguese","Russian","Spanish","Turkish","Vietnamese")

        val adapter = ArrayAdapter(requireContext(),android.R.layout.simple_list_item_1,genders)
        val adapter2 = ArrayAdapter(requireContext(),android.R.layout.simple_list_item_1,languages)

        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        binding.apply {
            spinner.adapter = adapter
            spinner2.adapter = adapter2

            button2.setOnClickListener {

                val selectedGenderPosition = spinner.selectedItemPosition
                val selectedLanguagePosition = spinner2.selectedItemPosition

                if (selectedGenderPosition == 0 || selectedLanguagePosition == 0) {
                    // Show an error message if either spinner has the default item selected
                    Toast.makeText(requireContext(), "Please select both gender and language.", Toast.LENGTH_SHORT).show()
                } else {
                    // Proceed with selected values
                    val selectedGender = genders[selectedGenderPosition]
                    val selectedLanguage = languages[selectedLanguagePosition]

                    editor.putString("userGender", selectedGender)
                    editor.putString("userLanguage", selectedLanguage)
                    editor.putString("userAge",ageET.text.toString())
                    editor.apply()

                    val i = Intent(requireContext(),MainActivity::class.java)
                    startActivity(i)


                }
            }

        }

    }
}