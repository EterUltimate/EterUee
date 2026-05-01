package com.eterultimate.eteruee.utils

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap

import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

private const val TAG = "ContextUtil"

/**
 * Read clipboard data as text
 */
fun Context.readClipboardText(): String {
    val clipboardManager =
        getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = clipboardManager.primaryClip ?: return ""
    val item = clip.getItemAt(0) ?: return ""
    return item.text.toString()
}

/**
 * 鍙戣捣娣诲姞缇ゆ祦绋?
 *
 * @param key 鐢卞畼缃戠敓鎴愮殑key
 * @return 杩斿洖true琛ㄧず鍛艰捣鎵婹鎴愬姛锛岃繑鍥瀎alse琛ㄧず鍛艰捣澶辫触
 */
fun Context.joinQQGroup(key: String?): Boolean {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setData(("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$key").toUri())
    // 姝lag鍙牴鎹叿浣撲骇鍝侀渶瑕佽嚜瀹氫箟锛屽璁剧疆锛屽垯鍦ㄥ姞缇ょ晫闈㈡寜杩斿洖锛岃繑鍥炴墜Q涓荤晫闈紝涓嶈缃紝鎸夎繑鍥炰細杩斿洖鍒板懠璧蜂骇鍝佺晫闈?   //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
        return true
    } catch (e: java.lang.Exception) {
        // 鏈畨瑁呮墜Q鎴栧畨瑁呯殑鐗堟湰涓嶆敮鎸?
        return false
    }
}

/**
 * Write text into clipboard
 */
fun Context.writeClipboardText(text: String) {
    val clipboardManager =
        getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    runCatching {
        clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("text", text))
        Log.i(TAG, "writeClipboardText: $text")
    }.onFailure {
        Log.e(TAG, "writeClipboardText: $text", it)
        Toast.makeText(this, "Failed to write text into clipboard", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Open a url
 */
fun Context.openUrl(url: String) {
    Log.i(TAG, "openUrl: $url")
    runCatching {
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        intent.launchUrl(this, url.toUri())
    }.onFailure {
        it.printStackTrace()
        Toast.makeText(this, "Failed to open URL: $url", Toast.LENGTH_SHORT).show()
    }
}

fun Context.getActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun Context.getComponentActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun Context.exportImage(
    activity: Activity,
    bitmap: Bitmap,
    fileName: String = "RikkaHub_${System.currentTimeMillis()}.png"
) {
    // 妫€鏌ュ瓨鍌ㄦ潈闄愶紙Android 9鍙婁互涓嬮渶瑕侊級
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
            return
        }
    }

    // 淇濆瓨鍒扮浉鍐?
    var outputStream: OutputStream? = null
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10鍙婁互涓婁娇鐢∕ediaStore API
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                outputStream = contentResolver.openOutputStream(it)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream!!)
            }
        } else {
            // Android 9鍙婁互涓嬬洿鎺ュ啓鍏ユ枃浠?
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, fileName)
            outputStream = FileOutputStream(image)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

            // 閫氱煡鍥惧簱鏇存柊
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(image)
            sendBroadcast(mediaScanIntent)
        }
        Log.i(TAG, "Image saved successfully: $fileName")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to save image", e)
    } finally {
        outputStream?.close()
    }
}

fun Context.exportImageFile(
    activity: Activity,
    file: File,
    fileName: String = "RikkaHub_${System.currentTimeMillis()}.png"
) {
    // 妫€鏌ュ瓨鍌ㄦ潈闄愶紙Android 9鍙婁互涓嬮渶瑕侊級
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
            return
        }
    }

    // 淇濆瓨鍒扮浉鍐?
    var outputStream: OutputStream? = null
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10鍙婁互涓婁娇鐢∕ediaStore API
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                outputStream = contentResolver.openOutputStream(it)
                file.inputStream().copyTo(outputStream!!)
            }
        } else {
            // Android 9鍙婁互涓嬬洿鎺ュ啓鍏ユ枃浠?
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, fileName)
            file.copyTo(image, overwrite = true)

            // 閫氱煡鍥惧簱鏇存柊
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(image)
            sendBroadcast(mediaScanIntent)
        }
        Log.i(TAG, "Image file saved successfully: $fileName")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to save image file", e)
    } finally {
        outputStream?.close()
    }
}

