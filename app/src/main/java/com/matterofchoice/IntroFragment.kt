package com.matterofchoice

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.matterofchoice.databinding.IntroFragmentBinding
import com.matterofchoice.model.IntroSlide

class IntroFragment : Fragment() {
    private lateinit var binding: IntroFragmentBinding
    private lateinit var myAdapter: MyPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = IntroFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


//        findNavController().navigate(R.id.action_introFragment_to_personFragment)

        val myList = listOf(
            IntroSlide(requireContext().getString(R.string.greeting),requireContext().getString(R.string.intro),R.drawable.d2),
            IntroSlide(requireContext().getString(R.string.greeting2),"this is a test and I think it will work2",R.drawable.d1),
            IntroSlide(requireContext().getString(R.string.greeting3),"this is a test and I think it will work3",R.drawable.welcome_image)
        )


         myAdapter = MyPagerAdapter(myList)
        binding.myViewPager.adapter = myAdapter
        setDots(requireContext().applicationContext)
        setCurrentDot(0)
        binding.myViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setCurrentDot(position)
            }
        })

        val viewPager = binding.myViewPager

        binding.nextBtn.setOnClickListener {
            if (viewPager.currentItem + 1 < myAdapter.itemCount){
                viewPager.currentItem += 1
            }else{
                findNavController().navigate(R.id.action_introFragment_to_personFragment)
            }
        }
        binding.skipBtn.setOnClickListener {
            findNavController().navigate(R.id.action_introFragment_to_personFragment)
        }
    }
    private fun setDots(context: Context){
        val dots = arrayOfNulls<ImageView>(myAdapter.itemCount)
        val layoutParams: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
        layoutParams.setMargins(8,0,8,0)
        for (i in dots.indices){
            dots[i] = ImageView(context)
            dots[i].apply {
                this?.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.dot_active))
                this?.layoutParams = layoutParams
            }
            binding.dotsContainer.addView(dots[i])
        }
    }

    private fun setCurrentDot(index:Int){
        val childCount = binding.dotsContainer.childCount
        for (i in 0 until childCount){
            val imageView = binding.dotsContainer[i] as ImageView
            if (i == index){
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.dot_active
                    )
                )
            }else{
                imageView.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.dot_inactive
                    )
                )
            }
        }
    }

}