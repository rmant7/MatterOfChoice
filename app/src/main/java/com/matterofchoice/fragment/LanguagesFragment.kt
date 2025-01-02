package com.matterofchoice.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.matterofchoice.R
import com.matterofchoice.databinding.LanguagesFragmentBinding

class LanguagesFragment: Fragment() {
    private lateinit var binding: LanguagesFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = LanguagesFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        val genders = resources.getStringArray(R.array.genders)
        val arrayAdapter = ArrayAdapter(requireContext(),R.layout.dropdown_item,genders)
        binding.genderTxt.setAdapter(arrayAdapter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()


        binding.apply {
            button2.setOnClickListener {
                editor.putString("userGender", genderTxt.text.toString())
                editor.apply()
                findNavController().navigate(R.id.action_languagesFragment_to_casesFragment)
            }
        }
    }
}