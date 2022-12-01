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
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import com.example.camera.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

	private lateinit var viewBinding: ActivityMainBinding
	private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
	private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
	private lateinit var outputDirectory: File

	private val imagesList = MutableLiveData<Array<File>>()

	private var bitmap: Bitmap? = null

	companion object {
		const val PERMISSION_REQ_CODE = 101
		const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-SS"

		val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		viewBinding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(viewBinding.root)

		activityResultLauncher()
		outputDirectory = getOutputDirectory()

		viewBinding.selectedImageRecV.layoutManager =
			GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)

		imagesList.postValue(outputDirectory.listFiles())

		imagesList.observe(this) {
			viewBinding.selectedImageRecV.adapter = ImageAdapter(it, this)
		}

	}

	override fun onResume() {
		super.onResume()
		if (allPermissionGranted()) {
			viewBinding.uploadImageBtn.setOnClickListener {

				if (viewBinding.uploadImageView.drawable == null) {
					cameraAndGallerySelectorDialog(this)
				} else {
					saveImageToFolder(bitmap!!)
					bitmap = null
					imagesList.postValue(outputDirectory.listFiles())
					viewBinding.uploadImageView.setImageDrawable(null)
				}
			}

		} else {
			ActivityCompat.requestPermissions(this, REQUIRED_PERMISSION, PERMISSION_REQ_CODE)
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

		val takePhotoBtn = dialog.findViewById<TextView>(R.id.takePhotoBtn)
		val chooseBtn = dialog.findViewById<TextView>(R.id.chooseBtn)

		takePhotoBtn.setOnClickListener {
			dialog.dismiss()
			val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
			takePhotoLauncher.launch(cameraIntent)
		}

		chooseBtn.setOnClickListener {
			dialog.dismiss()
			val galleryIntent = Intent(Intent.ACTION_PICK)
			galleryIntent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
			galleryLauncher.launch(galleryIntent)
		}

		dialog.show()
	}

	//activity result launchers
	private fun activityResultLauncher() {

		takePhotoLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK) {
					val bm = result.data?.extras?.get("data") as Bitmap
					bitmap = bm
					viewBinding.uploadImageView.setImageBitmap(bm)

				}
			}

		galleryLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK) {
					viewBinding.uploadImageView.setImageURI(result.data!!.data)

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
		} catch (e: java.lang.Exception) {
			e.printStackTrace()
		}

		Toast.makeText(this, "$image", Toast.LENGTH_SHORT).show()
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