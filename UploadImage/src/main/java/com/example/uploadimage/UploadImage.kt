package com.example.uploadimage

class UploadImage {
	private val cameraUtil = CameraUtil()
	fun getCameraUtil(): CameraUtil {
		return cameraUtil
	}
	private val fileUtil = FileUtil()
	fun getFileUtil(): FileUtil {
		return fileUtil
	}
}