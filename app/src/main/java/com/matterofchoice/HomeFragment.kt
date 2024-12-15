package com.matterofchoice

import CustomAdapter
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.matterofchoices.databinding.HomeFragmentBinding

class HomeFragment : Fragment() {
    private lateinit var binding: HomeFragmentBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var myAdapter: CustomAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = HomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        myAdapter = CustomAdapter()
        viewModel = ViewModelProvider(this)[ChatViewModel::class.java]

//        myAdapter.onClick = {
//            val mine = it.message
//            binding.apply {
//                linearLayout.visibility = View.VISIBLE
//                editPromptTV.text = mine
//                prompt.setText(mine)
//                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                prompt.requestFocus()
//                imm.showSoftInput(prompt, InputMethodManager.SHOW_IMPLICIT)
//            }
//
//        }

        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

        val gender = sharedPreferences.getString("userGender", "Not Selected")
        val language = sharedPreferences.getString("userLanguage", "Not Selected")
        val age = sharedPreferences.getString("userAge", "Not Selected")
        val subject = sharedPreferences.getString("userSubject", "Not Selected")


        lifecycleScope.launchWhenStarted {
            viewModel.sendMessage()
            viewModel.getUserInfo(gender!!,language!!,age!!,subject!!)
            viewModel.messageList.collect { messages ->
                if (messages.isNotEmpty()) binding.chatIntro.visibility = View.GONE
                myAdapter.submitList(messages.drop(0).reversed())
                binding.recycler.layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,true)
                binding.recycler.adapter = myAdapter
            }
        }

    }


}