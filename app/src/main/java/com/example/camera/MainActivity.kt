package com.example.camera

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import com.example.camera.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

	private lateinit var viewBinding: ActivityMainBinding
	private lateinit var takePhotoLauncher: ActivityResultLauncher<Intent>
	private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

	private val imagesList = MutableLiveData<ArrayList<Drawable>>()
	private val images = ArrayList<Drawable>()

	companion object {
		const val PERMISSION_REQ_CODE = 101

		val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		viewBinding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(viewBinding.root)

		activityResultLauncher()

		viewBinding.selectedImageRecV.layoutManager =
			GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false)

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
					images.add(viewBinding.uploadImageView.drawable)
					imagesList.postValue(images)
					viewBinding.uploadImageView.setImageDrawable(null)
				}
			}

		} else {
			ActivityCompat.requestPermissions(this, REQUIRED_PERMISSION, PERMISSION_REQ_CODE)
		}
	}

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

	private fun activityResultLauncher() {
		takePhotoLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK) {
					val bitmap = (result.data!!.extras!!.get("data")) as Bitmap
					viewBinding.uploadImageView.setImageBitmap(bitmap)
				}
			}

		galleryLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK) {
					viewBinding.uploadImageView.setImageURI(result.data!!.data)
				}
			}
	}

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