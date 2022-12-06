package com.example.camera

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import com.example.camera.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

	private lateinit var viewBinding: ActivityMainBinding
	private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
	private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

	private var imageUri: Uri? = null
	private var bitmap: Bitmap? = null

	companion object {
		const val PERMISSION_REQ_CODE = 101
		const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-SS"
		val imagesList = MutableLiveData<ArrayList<Drawable>>()
		val multipleDrawableList = MutableLiveData<ArrayList<Drawable>>()
		var outputDirectory: File? = null

		val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		viewBinding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(viewBinding.root)

		// initialize launchers
		activityResultLauncher(this)

		// get directory where images is to save
		outputDirectory = getOutputDirectory()

		viewBinding.selectedImageRecV.layoutManager = GridLayoutManager(this, 2)
		viewBinding.multipleImageViewRecV.layoutManager = GridLayoutManager(this, 2)

		// observer for all images
		imagesList.observe(this) {
			if (it.isEmpty()) {
				viewBinding.counterText.visibility = View.GONE
				viewBinding.noImageText.visibility = View.VISIBLE
			} else {
				viewBinding.counterText.visibility = View.VISIBLE
				viewBinding.noImageText.visibility = View.GONE
				viewBinding.counterText.text = it.size.toString()
				viewBinding.selectedImageRecV.adapter = ImageAdapter(it, this)
			}
		}

		multipleDrawableList.observe(this) {
			viewBinding.multipleImageViewRecV.adapter = ImageAdapter(it, this)

		}

	}

	override fun onResume() {
		super.onResume()

		getBitmapFromOutputDirectory()

		if (allPermissionGranted()) {

			if (viewBinding.uploadImage.drawable == null) {
				viewBinding.uploadImage.setOnClickListener {
					cameraAndGallerySelectorDialog(this)
					viewBinding.uploadImage.isClickable = false
				}
			}

			viewBinding.uploadBtn.setOnClickListener {
				if (viewBinding.uploadImage.drawable != null) {
					viewBinding.uploadBtn.visibility = View.GONE
					viewBinding.clearAllText.visibility = View.GONE
					viewBinding.uploadImage.setImageDrawable(null)
					viewBinding.uploadImage.isClickable = true
					saveImageToFolder(bitmap!!)
				} else {
					viewBinding.uploadBtn.visibility = View.GONE
					viewBinding.uploadImage.isClickable = true
					multipleDrawableList.postValue(ArrayList())
					viewBinding.imageViewLayout.visibility = View.VISIBLE
					viewBinding.multipleImageViewRecV.visibility = View.GONE
					viewBinding.clearAllText.visibility = View.GONE
					saveImagesToFolder()
				}

				getBitmapFromOutputDirectory()
			}

			viewBinding.clearAllText.setOnClickListener {
				viewBinding.uploadImage.setImageDrawable(null)
				viewBinding.uploadImage.isClickable = true
				viewBinding.imageViewLayout.visibility = View.VISIBLE
				viewBinding.multipleImageViewRecV.visibility = View.GONE
				viewBinding.uploadBtn.visibility = View.GONE
				viewBinding.clearAllText.visibility = View.GONE
			}

			viewBinding.showSelectedImageBtn.setOnClickListener {

				val slideInBot = AnimationUtils.loadAnimation(this,
					androidx.appcompat.R.anim.abc_slide_in_bottom)
				slideInBot.duration = 500
				val fadeIn =
					AnimationUtils.loadAnimation(this, androidx.appcompat.R.anim.abc_fade_in)
				fadeIn.duration = 500

				viewBinding.selectedImagesLayout.startAnimation(fadeIn)
				viewBinding.showContent.startAnimation(slideInBot)

				viewBinding.selectedImagesLayout.visibility = View.VISIBLE
				viewBinding.uploadImage.isClickable = false
			}

			viewBinding.crossBtn.setOnClickListener {

				val slideOutBot = AnimationUtils.loadAnimation(this,
					androidx.appcompat.R.anim.abc_slide_out_bottom)
				slideOutBot.duration = 500
				val fadeOut =
					AnimationUtils.loadAnimation(this, androidx.appcompat.R.anim.abc_fade_out)
				fadeOut.duration = 500

				viewBinding.selectedImagesLayout.startAnimation(fadeOut)
				viewBinding.showContent.startAnimation(slideOutBot)

				viewBinding.selectedImagesLayout.visibility = View.GONE
				viewBinding.uploadImage.isClickable = true
			}

		} else {
			ActivityCompat.requestPermissions(this, REQUIRED_PERMISSION, PERMISSION_REQ_CODE)
		}
	}

	private fun saveImagesToFolder() {
		for (image in multipleDrawableList.value!!) {
			val bm = (image as BitmapDrawable).bitmap
			saveImageToFolder(bm)
		}
	}

	private fun getBitmapFromOutputDirectory() {
		MainScope().launch(IO) {
			val list = ArrayList<Drawable>()
			for (file in outputDirectory!!.listFiles()!!) {
				val drawable = Drawable.createFromPath(file.absolutePath)!!
				list.add(drawable)
			}
			imagesList.postValue(list)
		}
	}

	// get directory location
	private fun getOutputDirectory(): File {
		val path = Environment.getExternalStorageDirectory().toString() + "/DCIM/Kamal"
		val direct = File(path)

		if (!direct.exists()) {
			direct.mkdir()
		}

		return direct
	}

	// chooser dialog
	private fun cameraAndGallerySelectorDialog(context: Context) {
		val dialog = Dialog(context)
		dialog.setContentView(R.layout.selector_dialog)
		dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
		dialog.setCancelable(false)

		val takePhotoBtn = dialog.findViewById<TextView>(R.id.takePhotoBtn)
		val chooseBtn = dialog.findViewById<TextView>(R.id.chooseBtn)
		val crossBtn = dialog.findViewById<ImageView>(R.id.crossBtn)

		takePhotoBtn.setOnClickListener {
			dialog.dismiss()
			val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, saveImageToCache(context))
			takePhotoLauncher.launch(cameraIntent)
		}

		chooseBtn.setOnClickListener {
			dialog.dismiss()
			val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
			galleryIntent.type = "image/*"
			galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
			galleryLauncher.launch(galleryIntent)
		}

		crossBtn.setOnClickListener {
			dialog.dismiss()
			viewBinding.uploadImage.isClickable = true
		}

		dialog.show()
	}

	//activity result launchers
	private fun activityResultLauncher(context: Context) {

		takePhotoLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK) {
					imageUri = saveImageToCache(context)
					viewBinding.uploadImage.setImageURI(imageUri)
					viewBinding.uploadBtn.visibility = View.VISIBLE
					viewBinding.clearAllText.visibility = View.VISIBLE

					if (Build.VERSION.SDK_INT > 29) {
						val source = ImageDecoder.createSource(this.contentResolver, imageUri!!)
						bitmap = ImageDecoder.decodeBitmap(source)
					} else {
						//TODO
					}
				}
			}

		galleryLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK) {

					if (result.data?.clipData != null) {

						viewBinding.imageViewLayout.visibility = View.GONE
						viewBinding.multipleImageViewRecV.visibility = View.VISIBLE
						viewBinding.uploadBtn.visibility = View.VISIBLE
						viewBinding.clearAllText.visibility = View.VISIBLE

						val count = result.data?.clipData?.itemCount!!

						val listOfBitmap = ArrayList<Drawable>()

						for (i in 0 until count) {
							val list = ArrayList<Drawable>()
							val uri = result.data?.clipData?.getItemAt(i)?.uri!!

							if (Build.VERSION.SDK_INT > 29) {
								val source = ImageDecoder.createSource(this.contentResolver, uri)
								val bm = ImageDecoder.decodeBitmap(source)
								val dr = BitmapDrawable(resources, bm)
								list.add(dr)
							} else {
								//TODO
							}
							listOfBitmap.addAll(list)
						}
						multipleDrawableList.postValue(listOfBitmap)

					} else if (result.data != null) {
						viewBinding.uploadImage.visibility = View.VISIBLE
						viewBinding.uploadBtn.visibility = View.VISIBLE
						viewBinding.clearAllText.visibility = View.VISIBLE
						viewBinding.uploadImage.setImageURI(result.data!!.data)

						val uri = result.data!!.data!!

						if (Build.VERSION.SDK_INT > 29) {
							val source = ImageDecoder.createSource(this.contentResolver, uri)
							bitmap = ImageDecoder.decodeBitmap(source)
						} else {
							//TODO
						}

					}
				}
			}
	}

	private fun saveImageToCache(context: Context): Uri? {
		val path = Environment.getExternalStorageDirectory().toString() + "/DCIM/image"
		val direct = File(path)

		if (!direct.exists()) {
			direct.mkdir()
		}

		var uri: Uri? = null

		try {

			val image = File(direct, "data.jpg")
			uri = FileProvider.getUriForFile(context.applicationContext,
				"com.example.camera" + ".provider",
				image)

		} catch (e: Exception) {
			e.printStackTrace()
		}
		return uri
	}

	//save image to file
	private fun saveImageToFolder(bm: Bitmap) {
		val image = File(outputDirectory,
			SimpleDateFormat(FILE_NAME_FORMAT,
				Locale.getDefault()).format(System.currentTimeMillis()) + ".jpg")

		if (image.exists()) {
			image.delete()
		}

		try {
			val out = FileOutputStream(image)
			bm.compress(Bitmap.CompressFormat.JPEG, 100, out)
			out.flush()
			out.close()
		} catch (e: Exception) {
			e.printStackTrace()
		}

		Toast.makeText(this, "saved at: $image", Toast.LENGTH_SHORT).show()
	}

	// ask permission from user
	private fun allPermissionGranted() = REQUIRED_PERMISSION.all {
		ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray,
	) {
		if (requestCode != PERMISSION_REQ_CODE) {
			ActivityCompat.requestPermissions(this, REQUIRED_PERMISSION, PERMISSION_REQ_CODE)
			finish()
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}
}