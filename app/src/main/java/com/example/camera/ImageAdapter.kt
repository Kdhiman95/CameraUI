package com.example.camera

import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.camera.CameraUi.Companion.selectedImagesList
import com.example.uploadimage.UploadImage
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.io.File

class ImageAdapter(
	private val list: ArrayList<String>,
	private val context: Context,
	private val uploadImage: UploadImage,
	private val outputDirectory: File,
) :
	RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

	inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val image: ImageView = view.findViewById(R.id.image)

		init {
//			val fileUtil = FileUtil()
//			val dir = fileUtil.getDir()

			view.setOnClickListener {
				val dialog = Dialog(context)
				dialog.setContentView(R.layout.dialog_img)
				dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
				dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT)

				val img = dialog.findViewById<ImageView>(R.id.image2)
				val saveBtn = dialog.findViewById<ExtendedFloatingActionButton>(R.id.dialogSaveBtn)
				val crossBtn = dialog.findViewById<ImageView>(R.id.dialogCrossBtn)

				Glide.with(context).load(list[adapterPosition]).into(img)

				saveBtn.setOnClickListener {
					saveBtn.visibility = View.GONE
					val bm = BitmapFactory.decodeFile(list[adapterPosition])
					uploadImage.getFileUtil().saveImageToFolder(bm, outputDirectory,
						MainActivity.FILE_NAME_FORMAT)
					selectedImagesList.postValue(uploadImage.getFileUtil()
						.getImagesFromFile(outputDirectory))

					Toast.makeText(context, "image selected", Toast.LENGTH_SHORT).show()
				}

				crossBtn.setOnClickListener {
					dialog.dismiss()
				}

				dialog.show()
			}
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.imageview, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		Glide.with(context).load(list[position]).into(holder.image)
	}

	override fun getItemCount(): Int = list.size

}
