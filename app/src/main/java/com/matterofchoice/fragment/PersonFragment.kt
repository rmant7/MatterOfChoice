package com.matterofchoice.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.matterofchoice.R
import com.matterofchoice.databinding.PersonFragmentBinding

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


        val languages = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(requireContext(),R.layout.dropdown_item,languages)

        binding.autoTXt.setAdapter(adapter)




        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        binding.button2.setOnClickListener {
            editor.putString("userLanguage", binding.autoTXt.text.toString())
            editor.apply()
            findNavController().navigate(R.id.action_personFragment_to_languagesFragment)
        }


//
//            button2.setOnClickListener {
//
//                val selectedGenderPosition = spinner.selectedItemPosition
//                val selectedLanguagePosition = spinner2.selectedItemPosition
//
//                if (selectedGenderPosition == 0 || selectedLanguagePosition == 0) {
//                    // Show an error message if either spinner has the default item selected
//                    Toast.makeText(requireContext(), "Please select both gender and language.", Toast.LENGTH_SHORT).show()
//                } else {
//                    // Proceed with selected values
//                    val selectedGender = genders[selectedGenderPosition]
//                    val selectedLanguage = languages[selectedLanguagePosition]
//
//                    editor.putString("userGender", selectedGender)
//                    editor.putString("userLanguage", selectedLanguage)
//                    editor.putString("userAge",ageET.text.toString())
//                    editor.putString("userSubject",subjectET.text.toString())
//                    editor.apply()


    }
}