package com.example.camera

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SelectedImageAdapter(private val list: ArrayList<String>, private val context: Context) :
	RecyclerView.Adapter<SelectedImageAdapter.ViewHolder>() {
	inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val image: ImageView = view.findViewById(R.id.image)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view =
			LayoutInflater.from(parent.context).inflate(R.layout.selected_imageview, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		Glide.with(context).load(list[position]).into(holder.image)
	}

	override fun getItemCount(): Int = list.size
}