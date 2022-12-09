package com.example.uploadimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.InputStream
import java.util.ArrayList

class CameraUtil {

	private lateinit var cameraProvider: ProcessCameraProvider

	private var imageCapture: ImageCapture? = null

	fun startCamera(
		activity: AppCompatActivity,
		cameraView: PreviewView,
		cameraFlip: Boolean,
	) {
		val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
		cameraProviderFuture.addListener({
			cameraProvider = cameraProviderFuture.get()
			val preview = Preview.Builder().build().also { mPreview ->
				mPreview.setSurfaceProvider(cameraView.surfaceProvider)
			}
			imageCapture = ImageCapture.Builder().build()

			val cameraSelector =
				if (cameraFlip) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

			try {
				cameraProvider.unbindAll()
				cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}, ContextCompat.getMainExecutor(activity))
	}

	fun takePhoto(
		activity: AppCompatActivity,
		fileUtil: FileUtil,
		outputDirectory: File,
		FILE_NAME_FORMAT: String,
	): ArrayList<String> {

		val mediaDir = activity.externalMediaDirs.firstOrNull()?.let { mFile ->
			File(mFile, "Data").apply {
				mkdirs()
			}
		}
		val dir = if (mediaDir != null && mediaDir.exists()) mediaDir else activity.filesDir

		val imageCapture = imageCapture
		val photoFile = File(dir, "data.jpg")

		if (photoFile.exists()) {
			photoFile.delete()
		}

		val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()


		imageCapture?.takePicture(outputOption,
			ContextCompat.getMainExecutor(activity),
			object : ImageCapture.OnImageSavedCallback {
				override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
					val savedUri = Uri.fromFile(photoFile)
					val ims: InputStream = activity.contentResolver.openInputStream(savedUri)!!
					val bm = BitmapFactory.decodeStream(ims)
					fileUtil.saveImageToFolder(bm!!, outputDirectory, FILE_NAME_FORMAT)
					Log.d("WWWWWW", "onImageSaved: $bm")
				}

				override fun onError(exception: ImageCaptureException) {
					exception.printStackTrace()
				}
			})

		return fileUtil.getImagesFromFile(outputDirectory)
	}
}