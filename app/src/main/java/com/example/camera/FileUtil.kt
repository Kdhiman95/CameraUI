package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FileUtil {
	fun getDir(): File {
		val path = Environment.getExternalStorageDirectory().toString() + "/DCIM/Kamal"
		val direct = File(path)
		if (!direct.exists()) {
			direct.mkdir()
		}
		return direct
	}

	fun getImagesFromFile(dir: File): ArrayList<String> {
		val list = ArrayList<String>()
		for (file in dir.listFiles()!!) {
			list.add(file.absolutePath)
		}
		return list
	}

	fun saveImageToFolder(bm: Bitmap, dir: File) {

		val image = File(dir,
			SimpleDateFormat(MainActivity.FILE_NAME_FORMAT,
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
	}

	fun fetchImagesFromGallery(context: Context): ArrayList<String> {
		val list = ArrayList<String>()

		val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
		val pro =
			arrayOf(MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
		val orderBy = MediaStore.Images.Media.DATE_TAKEN

		val cursor = context.contentResolver.query(uri, pro, null, null, "$orderBy DESC")
		val index = cursor?.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

		if (cursor != null) {
			while (cursor.moveToNext()) {
				val imagePath = cursor.getString(index!!)
				list.add(imagePath)
			}
		}
		cursor?.close()
		return list
	}
}