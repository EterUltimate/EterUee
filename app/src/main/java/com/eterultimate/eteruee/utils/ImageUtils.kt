@file:Suppress("unused")

package com.eterultimate.eteruee.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.png.PngChunkType
import com.drew.metadata.png.PngDirectory
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * 鍥剧墖澶勭悊宸ュ叿绫?
 * 鎻愪緵鍥剧墖鍘嬬缉銆佹棆杞慨姝ｃ€佷簩缁寸爜瑙ｆ瀽绛夊姛鑳?
 */
object ImageUtils {

    /**
     * 浼樺寲鐨勫浘鐗囧姞杞芥柟娉曪紝閬垮厤OOM
     * 1. 鍏堣幏鍙栧浘鐗囧昂瀵?
     * 2. 璁＄畻鍚堥€傜殑閲囨牱鐜?
     * 3. 鍔犺浇鍘嬬缉鍚庣殑鍥剧墖
     * 4. 澶勭悊鍥剧墖鏃嬭浆
     *
     * @param context Android涓婁笅鏂?
     * @param uri 鍥剧墖URI
     * @param maxSize 鏈€澶у昂瀵搁檺鍒讹紝榛樿1024px
     * @return 鍘嬬缉鍚庣殑Bitmap锛屽け璐ヨ繑鍥瀗ull
     */
    fun loadOptimizedBitmap(
        context: Context,
        uri: Uri,
        maxSize: Int = 1024
    ): Bitmap? {
        return runCatching {
            // 绗竴姝ワ細鑾峰彇鍥剧墖鐨勫師濮嬪昂瀵革紝涓嶅姞杞藉埌鍐呭瓨
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            // 璁＄畻鍚堥€傜殑閲囨牱鐜?
            val sampleSize = calculateInSampleSize(options, maxSize, maxSize)

            // 绗簩姝ワ細浣跨敤閲囨牱鐜囧姞杞藉帇缂╁悗鐨勫浘鐗?
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // 浣跨敤RGB_565鍑忓皯鍐呭瓨鍗犵敤
            }

            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, loadOptions)
            }

            // 绗笁姝ワ細澶勭悊鍥剧墖鏃嬭浆锛堝鏋滈渶瑕侊級
            bitmap?.let { correctImageOrientation(context, uri, it) }
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    /**
     * 璁＄畻鍚堥€傜殑閲囨牱鐜?
     *
     * @param options BitmapFactory.Options鍖呭惈鍘熷鍥剧墖灏哄淇℃伅
     * @param reqWidth 鐩爣瀹藉害
     * @param reqHeight 鐩爣楂樺害
     * @return 閲囨牱鐜囷紙2鐨勫箓锛?
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // 璁＄畻鏈€澶х殑inSampleSize鍊硷紝璇ュ€兼槸2鐨勫箓锛屽苟涓斾繚鎸侀珮搴﹀拰瀹藉害閮藉ぇ浜庤姹傜殑楂樺害鍜屽搴?
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 淇鍥剧墖鏃嬭浆
     * 鏍规嵁EXIF淇℃伅鑷姩鏃嬭浆鍥剧墖鍒版纭柟鍚?
     *
     * @param context Android涓婁笅鏂?
     * @param uri 鍥剧墖URI
     * @param bitmap 鍘熷bitmap
     * @return 鏃嬭浆鍚庣殑bitmap
     */
    fun correctImageOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()

            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap // 涓嶉渶瑕佹棆杞?
            }

            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotatedBitmap != bitmap) {
                bitmap.recycle() // 鍥炴敹鍘熷bitmap
            }
            rotatedBitmap
        }.onFailure {
            it.printStackTrace()
        }.getOrDefault(bitmap)
    }

    /**
     * 浠庡浘鐗囦腑瑙ｆ瀽浜岀淮鐮?
     *
     * @param bitmap 瑕佽В鏋愮殑鍥剧墖
     * @return 浜岀淮鐮佸唴瀹癸紝瑙ｆ瀽澶辫触杩斿洖null
     */
    fun decodeQRCodeFromBitmap(bitmap: Bitmap): String? {
        return runCatching {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)

            result.text
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    /**
     * 浠嶶RI鍔犺浇鍥剧墖骞惰В鏋愪簩缁寸爜锛堢粍鍚堟柟娉曪級
     *
     * @param context Android涓婁笅鏂?
     * @param uri 鍥剧墖URI
     * @param maxSize 鏈€澶у昂瀵搁檺鍒讹紝榛樿1024px
     * @return 浜岀淮鐮佸唴瀹癸紝瑙ｆ瀽澶辫触杩斿洖null
     */
    fun decodeQRCodeFromUri(
        context: Context,
        uri: Uri,
        maxSize: Int = 1024
    ): String? {
        val bitmap = loadOptimizedBitmap(context, uri, maxSize) ?: return null
        return try {
            decodeQRCodeFromBitmap(bitmap)
        } finally {
            bitmap.recycle() // 纭繚閲婃斁鍐呭瓨
        }
    }

    /**
     * 瀹夊叏鍦板洖鏀禕itmap鍐呭瓨
     *
     * @param bitmap 瑕佸洖鏀剁殑bitmap
     */
    fun recycleBitmapSafely(bitmap: Bitmap?) {
        bitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }

    /**
     * 鑾峰彇鍥剧墖鐨勫熀鏈俊鎭紙涓嶅姞杞藉埌鍐呭瓨锛?
     *
     * @param context Android涓婁笅鏂?
     * @param uri 鍥剧墖URI
     * @return ImageInfo鍖呭惈瀹藉害銆侀珮搴︺€丮IME绫诲瀷绛変俊鎭?
     */
    fun getImageInfo(context: Context, uri: Uri): ImageInfo? {
        return runCatching {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            if (options.outWidth > 0 && options.outHeight > 0) {
                ImageInfo(
                    width = options.outWidth,
                    height = options.outHeight,
                    mimeType = options.outMimeType
                )
            } else null
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    /**
     * 鑾峰彇閰掗瑙掕壊鍗′腑鐨勮鑹插厓鏁版嵁锛堝鏋滃瓨鍦級
     *
     * @param context Android涓婁笅鏂?
     * @param uri 鍥剧墖URI
     * @return Result<String> 鍖呭惈瑙掕壊鍏冩暟鎹殑Result瀵硅薄
     */
    fun getTavernCharacterMeta(context: Context, uri: Uri): Result<String> = runCatching {
        val metadata = context.contentResolver.openInputStream(uri)?.use { ImageMetadataReader.readMetadata(it) }
        if (metadata == null) error("Metadata is null, please check if the image is a character card")
        if (!metadata.containsDirectoryOfType(PngDirectory::class.java)) error("No PNG directory found, please check if the image is a character card")

        val pngDirectory = metadata.getDirectoriesOfType(PngDirectory::class.java)
            .firstOrNull { directory ->
                directory.pngChunkType == PngChunkType.tEXt
                    && directory.getString(PngDirectory.TAG_TEXTUAL_DATA).startsWith("[chara:")
            } ?: error("No tEXt chunk found, please check if the image is a character card")

        val value = pngDirectory.getString(PngDirectory.TAG_TEXTUAL_DATA)

        val regex = Regex("""\[chara:\s*(.+?)]""")
        return Result.success(regex.find(value)?.groupValues?.get(1) ?: error("No character data found"))
    }

    data class ImageInfo(
        val width: Int,
        val height: Int,
        val mimeType: String?
    )
}

