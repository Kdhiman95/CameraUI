package com.example.camera

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ImageAdapter(private val list: ArrayList<Drawable>, private val context: Context) :
	RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

	inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val image: ImageView = view.findViewById(R.id.image)

		init {
			view.setOnClickListener {
				val dialog = Dialog(context)
				dialog.setContentView(R.layout.dialog_img)
				dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
				val img = dialog.findViewById<ImageView>(R.id.image2)
				img.setImageDrawable(list[list.size-adapterPosition-1])
				dialog.show()
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.imageview, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.image.setImageDrawable(list[list.size-position-1])
	}

	override fun getItemCount(): Int = list.size

}
