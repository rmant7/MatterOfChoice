package com.matterofchoice.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.matterofchoice.activity.GameActivity
import com.matterofchoice.databinding.CasesLayoutBinding

class CasesFragment : Fragment() {
    private lateinit var binding: CasesLayoutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = CasesLayoutBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()




        binding.apply {
            // Function to check if fields are filled and update button state
            fun updateButtonState() {
                generateBtn.isEnabled = subjectET.text.isNotEmpty() && ageET.text.isNotEmpty()
            }

            // Add text change listeners to the EditText fields
            subjectET.addTextChangedListener { updateButtonState() }
            ageET.addTextChangedListener { updateButtonState() }

            // Set initial button state
            updateButtonState()

            // Set up the button's click listener
            generateBtn.setOnClickListener {
                if (subjectET.text.isNotEmpty() && ageET.text.isNotEmpty()) {
                    editor.putString("userSubject", subjectET.text.toString())
                    editor.putString("userAge", ageET.text.toString())
                    editor.apply()
                }
                val i = Intent(requireContext(), GameActivity::class.java)
                startActivity(i)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        editor.putInt("userScore", 0)
        editor.apply()
        editor.putInt("totalScore", 0)
        editor.apply()

    }
}