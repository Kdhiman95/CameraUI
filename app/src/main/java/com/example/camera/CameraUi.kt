package com.example.camera

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
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
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.camera.MainActivity.Companion.PERMISSION_REQ_CODE
import com.example.camera.MainActivity.Companion.REQUIRED_PERMISSION
import com.example.camera.databinding.ActivityCameraUiBinding
import com.example.camera.databinding.SelectedBottomSheetBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraUi : AppCompatActivity() {

	private lateinit var viewBinding: ActivityCameraUiBinding
	private lateinit var cameraExecutor: ExecutorService
	private lateinit var cameraProvider: ProcessCameraProvider
	private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
	private lateinit var selectedImagesBottomSheetBinding: SelectedBottomSheetBinding
	private lateinit var bottomSheetDialog: Dialog
	private lateinit var fileUtil: FileUtil

	private var flip: Boolean = true
	private var imageCapture: ImageCapture? = null
	private var outputDirectory: File? = null

	private val drawablesFromGallery = MutableLiveData<ArrayList<String>>()

	companion object {
		val selectedImagesList = MutableLiveData<ArrayList<String>>()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		viewBinding = ActivityCameraUiBinding.inflate(layoutInflater)
		setContentView(viewBinding.root)

		bottomSheetDialogInit()
		launcher(this)

		fileUtil = FileUtil()
		outputDirectory = fileUtil.getDir()
		cameraExecutor = Executors.newSingleThreadExecutor()

		viewBinding.imagesFromGalleryRecV.layoutManager =
			LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

		observer(this)
	}

	override fun onResume() {
		super.onResume()

		if (allPermissionGranted()) {
			startCamera(this)
			MainScope().launch {
				drawablesFromGallery.postValue(fileUtil.fetchImagesFromGallery(this@CameraUi))
				selectedImagesList.postValue(fileUtil.getImagesFromFile(outputDirectory!!))
			}

			viewBinding.showSelectedImageBtn.setOnClickListener {
				bottomSheetDialog.show()
			}

			viewBinding.takePhotoBtn.setOnClickListener {
				val takePhotoAnim = AnimationUtils.loadAnimation(this, R.anim.photo_click_animation)
				takePhotoAnim.duration = 500
				viewBinding.takePhotoBtn.startAnimation(takePhotoAnim)
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

	// observers for images
	private fun observer(cameraUi: CameraUi) {
		selectedImagesList.observe(cameraUi) {
			if (it.isEmpty()) {
				viewBinding.counterText.visibility = View.GONE
			} else {
				viewBinding.counterText.visibility = View.VISIBLE

				viewBinding.counterText.text = it.size.toString()
				selectedImagesBottomSheetBinding.dialogSelectedImageRecV.adapter =
					SelectedImageAdapter(it, cameraUi)
			}
		}

		drawablesFromGallery.observe(cameraUi) {
			viewBinding.imagesFromGalleryRecV.visibility = View.VISIBLE
			viewBinding.imagesFromGalleryRecV.adapter = ImageAdapter(it, cameraUi)
		}
	}

	// initialize bottom sheet dialog
	private fun bottomSheetDialogInit() {
		selectedImagesBottomSheetBinding = SelectedBottomSheetBinding.inflate(layoutInflater)
		bottomSheetDialog = Dialog(this)
		bottomSheetDialog.setContentView(selectedImagesBottomSheetBinding.root)

		selectedImagesBottomSheetBinding.dialogSelectedImageRecV.layoutManager =
			GridLayoutManager(this, 4)

		bottomSheetDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 1500)
		bottomSheetDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
		bottomSheetDialog.window?.setGravity(Gravity.BOTTOM)
	}

	// Start Camera
	private fun startCamera(context: Context) {
		val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
		cameraProviderFuture.addListener({
			cameraProvider = cameraProviderFuture.get()
			val preview = Preview.Builder().build().also { mPreview ->
				mPreview.setSurfaceProvider(viewBinding.cameraPreview.surfaceProvider)
			}
			imageCapture = ImageCapture.Builder().build()

			val cameraSelector =
				if (flip) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

			try {
				cameraProvider.unbindAll()
				cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
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

		val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

		imageCapture.takePicture(outputOption,
			ContextCompat.getMainExecutor(this),
			object : ImageCapture.OnImageSavedCallback {
				override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

					val savedUri = Uri.fromFile(photoFile)

					val ims: InputStream = context.contentResolver.openInputStream(savedUri)!!
					val bm = BitmapFactory.decodeStream(ims)
					fileUtil.saveImageToFolder(bm, outputDirectory!!)

					selectedImagesList.postValue(fileUtil.getImagesFromFile(outputDirectory!!))
				}

				override fun onError(exception: ImageCaptureException) {
					exception.printStackTrace()
				}
			})
	}

	// launchers
	private fun launcher(context: Context) {
		galleryLauncher =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK) {

					if (result.data?.clipData != null) {

						val count = result.data?.clipData?.itemCount!!

						for (i in 0 until count) {
							val uri = result.data?.clipData?.getItemAt(i)?.uri!!

							val ims: InputStream = context.contentResolver.openInputStream(uri)!!
							val bm = BitmapFactory.decodeStream(ims)
							fileUtil.saveImageToFolder(bm, outputDirectory!!)

						}

					} else if (result.data != null) {

						val uri = result.data!!.data!!
						val ims: InputStream = context.contentResolver.openInputStream(uri)!!
						val bm = BitmapFactory.decodeStream(ims)
						fileUtil.saveImageToFolder(bm, outputDirectory!!)

					}
					selectedImagesList.postValue(fileUtil.getImagesFromFile(outputDirectory!!))
				}
			}
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