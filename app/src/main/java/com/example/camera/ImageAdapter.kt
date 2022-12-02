package com.example.camera

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class ImageAdapter(private val list: ArrayList<File>, private val context: Context) :
	RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

	inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val image: ImageView = view.findViewById(R.id.image)

		init {
			view.setOnClickListener {
				val dialog = Dialog(context)
				dialog.setContentView(R.layout.dialog_img)
				dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
				val img = dialog.findViewById<ImageView>(R.id.image2)
				img.setImageURI(Uri.parse(list[adapterPosition].toString()))
				dialog.show()
			}
			view.setOnLongClickListener {
				list[adapterPosition].delete()
				list.remove(list[adapterPosition])
				notifyItemRemoved(adapterPosition)
				true
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.imageview, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.image.setImageURI(Uri.parse(list[position].toString()))
	}

	override fun getItemCount(): Int = list.size

}
