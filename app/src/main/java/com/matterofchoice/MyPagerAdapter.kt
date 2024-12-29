package com.matterofchoice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.matterofchoice.databinding.SliderItemBinding
import com.matterofchoice.model.IntroSlide

class MyPagerAdapter(val items : List<IntroSlide>): RecyclerView.Adapter<MyPagerAdapter.MyPagerViewHolder> (){

    inner class MyPagerViewHolder(val binding:SliderItemBinding) : RecyclerView.ViewHolder(binding.root){

        fun bind(item : IntroSlide) {
            binding.apply {
                imageView4.setImageResource(item.image)
                textView85.text = item.title
                descriptionTV.text = item.description
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyPagerViewHolder {
        return MyPagerViewHolder(
            SliderItemBinding.inflate(LayoutInflater.from(parent.context),parent, false)
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MyPagerViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }
}