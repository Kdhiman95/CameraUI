package com.example.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.camera.MainActivity.Companion.FILE_NAME_FORMAT
import com.example.camera.MainActivity.Companion.PERMISSION_REQ_CODE
import com.example.camera.MainActivity.Companion.REQUIRED_PERMISSION
import com.example.camera.MainActivity.Companion.imagesList
import com.example.camera.databinding.ActivityCameraUiBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraUi : AppCompatActivity() {

	private lateinit var viewBinding: ActivityCameraUiBinding
	private lateinit var cameraExecutor: ExecutorService
	private lateinit var cameraProvider: ProcessCameraProvider
	private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

	private var outputDirectory: File? = null
	private var flip: Boolean = true
	private var imageCapture: ImageCapture? = null


//	private val PERMISSION_REQ_CODE = 101
//	private val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-SS"
//	private val REQUIRED_PERMISSION =
//		arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)

//	private val imagesPath = ArrayList<String>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		viewBinding = ActivityCameraUiBinding.inflate(layoutInflater)
		setContentView(viewBinding.root)

		cameraExecutor = Executors.newSingleThreadExecutor()
		outputDirectory = getOutputDirectory()

		launcher()
		getDrawableFromOutputDirectory()

//		viewBinding.mukulImageRecV.layoutManager =
//			LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

		viewBinding.selectedImageRecV.layoutManager =
			LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
		imagesList.observe(this) {
			if (it.isEmpty()) {
				viewBinding.selectedImageRecV.visibility = View.GONE
			} else {
				viewBinding.selectedImageRecV.visibility = View.VISIBLE
				viewBinding.selectedImageRecV.adapter = ImageAdapter(it, this)
			}
		}
	}

	override fun onResume() {
		super.onResume()
		if (allPermissionGranted()) {
			startCamera(this)

			viewBinding.chooseFromGalleryBtn.setOnClickListener {
				val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
				galleryIntent.type = "image/*"
				galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
				galleryLauncher.launch(galleryIntent)
			}

			viewBinding.takePhotoBtn.setOnClickListener {
				takePhoto(this)
			}

			viewBinding.flipCameraBtn.setOnClickListener {
				flip = !flip
				startCamera(this)
			}

		} else {
			ActivityCompat.requestPermissions(this, REQUIRED_PERMISSION, PERMISSION_REQ_CODE)
		}
	}

	override fun onDestroy() {
		cameraExecutor.shutdown()
		super.onDestroy()
	}

	// Start Camera
	private fun startCamera(context: Context) {
		val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
		cameraProviderFuture.addListener({
			cameraProvider = cameraProviderFuture.get()
			val preview = Preview.Builder()
				.build().also { mPreview ->
					mPreview.setSurfaceProvider(
						viewBinding.cameraPreview.surfaceProvider
					)
				}
			imageCapture = ImageCapture.Builder().build()

			val cameraSelector =
				if (flip) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

			try {
				cameraProvider.unbindAll()
				cameraProvider.bindToLifecycle(
					this,
					cameraSelector,
					preview, imageCapture
				)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}, ContextCompat.getMainExecutor(context))
	}

	// take photo
	private fun takePhoto(context: Context) {

		val mediaDir = externalMediaDirs.firstOrNull()?.let { mFile ->
			File(mFile, resources.getString(R.string.app_name)).apply {
				mkdirs()
			}
		}
		val dir = if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir

		val imageCapture = imageCapture ?: return
		val photoFile = File(dir, "data.jpg")

		if (photoFile.exists()) {
			photoFile.delete()
		}

		Log.d("WWWWWW", "takePhoto: ${photoFile.absolutePath}")

		val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

		imageCapture.takePicture(
			outputOption,
			ContextCompat.getMainExecutor(this),
			object : ImageCapture.OnImageSavedCallback {
				override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

					val savedUri = Uri.fromFile(photoFile)

					if (Build.VERSION.SDK_INT > 29) {
						val source = ImageDecoder.createSource(context.contentResolver, savedUri)
						val bm = ImageDecoder.decodeBitmap(source)
						saveImageToFolder(bm)
					} else {
						//TODO
					}

					getDrawableFromOutputDirectory()
				}

				override fun onError(exception: ImageCaptureException) {
					Log.d("WWWWWW", "onError: ${exception.message}", exception)
				}
			}
		)
	}

	// fetchImages
//	fun fetchImages(context: Context){
//		val listOfImages = ArrayList<String>()
//
//		val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
//		val pro = arrayOf(MediaStore.MediaColumns.DATA,MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
//		val cursor = context.contentResolver.query(uri,pro,null,null,null)
//		val index = cursor?.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
//
//		if (cursor != null) {
//			while(cursor.moveToNext()){
//				val imagePath = cursor.getString(index!!)
//				listOfImages.add(imagePath)
//				Log.d("WWWWWW", "fetchImages: $imagePath")
//			}
//		}
//
//	}

	private fun launcher() {
		galleryLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK) {

					if (result.data?.clipData != null) {

						val count = result.data?.clipData?.itemCount!!

						for (i in 0 until count) {
							val uri = result.data?.clipData?.getItemAt(i)?.uri!!

							if (Build.VERSION.SDK_INT > 29) {
								val source = ImageDecoder.createSource(this.contentResolver, uri)
								val bm = ImageDecoder.decodeBitmap(source)
								saveImageToFolder(bm)
							} else {
								//TODO
							}
						}

					} else if (result.data != null) {

						val uri = result.data!!.data!!

						if (Build.VERSION.SDK_INT > 29) {
							val source = ImageDecoder.createSource(this.contentResolver, uri)
							val bm = ImageDecoder.decodeBitmap(source)
							saveImageToFolder(bm)
						} else {
							//TODO
						}
					}

					getDrawableFromOutputDirectory()

//					if (result.data?.clipData != null) {
//
//						val count = result.data?.clipData?.itemCount!!
//
//						val imagePathString = ArrayList<String>()
//
//						for (i in 0 until count) {
//							val uri = result.data?.clipData?.getItemAt(i)?.uri!!
//							imagePathString.add(uri.toString())
//						}
//						imagesPath.addAll(imagePathString)
//
//					} else if (result.data != null) {
//
//						val uri = result.data!!.data!!
//
//						imagesPath.add(uri.toString())
//
//					}
//
//					viewBinding.mukulImageRecV.adapter = MukulAdapter(imagesPath)

				}
			}
	}

	// get drawable from folder
	private fun getDrawableFromOutputDirectory() {
		MainScope().launch(Dispatchers.IO) {
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

	// save image to file
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
			ActivityCompat.requestPermissions(this,
				REQUIRED_PERMISSION,
				PERMISSION_REQ_CODE)
			finish()
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
	}
}