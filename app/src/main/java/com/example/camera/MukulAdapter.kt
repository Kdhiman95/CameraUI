package com.example.camera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView

class MukulAdapter(private val imagesPath: ArrayList<String>) : RecyclerView.Adapter<MukulAdapter.ViewHolder>() {

	inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
		val image: ImageView = view.findViewById(R.id.image)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.imageview, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.image.setImageURI(imagesPath[position].toUri())
	}

	override fun getItemCount(): Int = imagesPath.size
}
