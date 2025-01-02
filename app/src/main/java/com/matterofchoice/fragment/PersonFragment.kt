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

    override fun onResume() {
        super.onResume()

        val languages = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(requireContext(),R.layout.dropdown_item,languages)
        binding.autoTXt.setAdapter(adapter)

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        binding.button2.setOnClickListener {
            editor.putString("userLanguage", binding.autoTXt.text.toString())
            editor.apply()
            findNavController().navigate(R.id.action_personFragment_to_languagesFragment)
        }
    }
}