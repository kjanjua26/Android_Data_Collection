package com.example.mcmvs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.camera2.CameraMetadata
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.example.mcmvs.MainActivity.Companion.BLUR_SCALE_FACTOR
import com.example.mcmvs.MainActivity.Companion.DISPLAY_BITMAP_SCALE
import com.example.mcmvs.MainActivity.Companion.Logd
import com.example.mcmvs.MainActivity.Companion.twoLens
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt


class ImageAvailableListener(private val activity: MainActivity, internal var params: CameraParams) : ImageReader.OnImageAvailableListener {

    override fun onImageAvailable(reader: ImageReader) {
        Log.d(MainActivity.LOG_TAG, "ImageReader. Image is available, about to post.")
        val image: Image = reader.acquireNextImage()

        //It might be that we received the image first and we're still waiting for the face calculations
        if (MainActivity.twoLens.isTwoLensShot) {
            Logd("Image Received, dual lens shot.")

            if (MainActivity.wideAngleId == params.id) {
                twoLens.wideImage = image

            } else if (MainActivity.normalLensId == params.id) {
                twoLens.normalImage = image
            }

            if (twoLens.wideShotDone && twoLens.normalShotDone
                && null != twoLens.wideImage
                && null != twoLens.normalImage) {

                val finalBitmap: Bitmap = DoBokeh(activity, twoLens)
                setCapturedPhoto(activity, params.capturedPhoto, finalBitmap)

                twoLens.normalImage?.close()
                twoLens.wideImage?.close()
            }

        }
        Log.d(MainActivity.LOG_TAG, "ImageReader. Post has been set.")
    }
}

fun save(bytes: Bitmap, tempName: String) {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val dataDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DataCollection")
    if (!dataDir.exists()) {
        dataDir.mkdir()
    }
    val fileName = tempName + "_IMG_$timeStamp.jpg"
    val fileDir = File(dataDir.path + File.separator + fileName)
    try {
        val fileOutputStream = FileOutputStream(fileDir)
        bytes.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        fileOutputStream.close()
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun save(activity: MainActivity, gyroData: String, accData: String){
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val dataDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DataCollection")
    if (!dataDir.exists()) {
        dataDir.mkdir()
    }
    val fileName = "data" + timeStamp + ".csv"
    val fileDir = File(dataDir.path + File.separator + fileName)
    activity.bufferedWriter = BufferedWriter(FileWriter(fileDir))
    activity.bufferedWriter!!.write(gyroData)
    activity.bufferedWriter!!.write(accData)
    activity.bufferedWriter!!.close()
}

fun setCapturedPhoto(activity: Activity, imageView: ImageView?, bitmap: Bitmap) {
    activity.runOnUiThread { imageView?.setImageBitmap(bitmap) }
}


fun WriteFile(activity: MainActivity, bitmap: Bitmap, name: String, quality: Int = 100, writePNG: Boolean = false, dirTimeStamp: String = "") {
    val PHOTOS_DIR: String = "BasicBokeh/"+dirTimeStamp

    var jpgFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        File.separatorChar + PHOTOS_DIR + File.separatorChar +
                name + ".jpg")

    if (writePNG)
        jpgFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            File.separatorChar + PHOTOS_DIR + File.separatorChar +
                    name + ".png")

    val photosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), PHOTOS_DIR)

    if (!photosDir.exists()) {
        val createSuccess = photosDir.mkdir()
        if (!createSuccess) {
            Toast.makeText(activity, "DCIM/" + PHOTOS_DIR + " creation failed.", Toast.LENGTH_SHORT).show()
            Logd("Photo storage directory DCIM/" + PHOTOS_DIR + " creation failed!!")
        } else {
            Logd("Photo storage directory DCIM/" + PHOTOS_DIR + " did not exist. Created.")
        }
    }

    var output: FileOutputStream? = null
    try {
        output = FileOutputStream(jpgFile)

        if (writePNG)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        else
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)

    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        if (null != output) {
            try {
                output.close()

                //File is written, let media scanner know
                val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                scannerIntent.data = Uri.fromFile(jpgFile)
                activity.sendBroadcast(scannerIntent)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    Logd("WriteFile: Completed.")
}

fun getFileHandle (activity: MainActivity, name: String, withTimestamp: Boolean, dirTimeStamp: String) : File {
//    val PHOTOS_DIR: String = "BasicBokeh"

    val PHOTOS_DIR: String = "BasicBokeh/"+ dirTimeStamp
    var filePath = File.separatorChar + PHOTOS_DIR + File.separatorChar + name

    if (withTimestamp)
        filePath += "-" + generateTimestamp()


    filePath += ".jpg"

    val jpgFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filePath)

    val photosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), PHOTOS_DIR)

    if (!photosDir.exists()) {
        val createSuccess = photosDir.mkdir()
        if (!createSuccess) {
            Toast.makeText(activity, "DCIM/" + PHOTOS_DIR + " creation failed.", Toast.LENGTH_SHORT).show()
            Logd("Photo storage directory DCIM/" + PHOTOS_DIR + " creation failed!!")
        } else {
            Logd("Photo storage directory DCIM/" + PHOTOS_DIR + " did not exist. Created.")
        }
    }

    return jpgFile
}

fun WriteFile(activity: MainActivity, bitmap: Bitmap, name: String, withTimestamp: Boolean = false, dirTimeStamp: String = "") {

    val jpgFile = getFileHandle(activity, name, withTimestamp, dirTimeStamp)

    var output: FileOutputStream? = null
    try {
        output = FileOutputStream(jpgFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        if (null != output) {
            try {
                output.close()

                //File is written, let media scanner know
                val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                scannerIntent.data = Uri.fromFile(jpgFile)
                activity.sendBroadcast(scannerIntent)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    Logd("WriteFile: Completed.")
}

fun WriteFile(activity: MainActivity, bytes: ByteArray, name: String, withTimestamp: Boolean = false, dirTimeStamp: String = "") {

    val jpgFile = getFileHandle(activity, name, withTimestamp, dirTimeStamp)

    var output: FileOutputStream? = null
    try {
        output = FileOutputStream(jpgFile)
        output.write(bytes)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        if (null != output) {
            try {
                output.close()

                //File is written, let media scanner know
                val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                scannerIntent.data = Uri.fromFile(jpgFile)
                activity.sendBroadcast(scannerIntent)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    Logd("WriteFile: Completed.")
}

fun generateTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
    return sdf.format(Date())
}

fun generateTimestamp2Sec(): String {
    val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
    return sdf.format(Date())
}

fun horizontalFlip(bitmap: Bitmap) : Bitmap {
    val matrix = Matrix()
    matrix.preScale(-1.0f, 1.0f)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun rotateBitmap(original: Bitmap, degrees: Float): Bitmap {
    //If no rotation, no-op
    if (0f == degrees)
        return original

    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
}

/*class ImageSaver internal constructor(private val activity: MainActivity, private val params: CameraParams, private val image: Image?, private val imageView: ImageView?, private val flip: Boolean, private val cameraParams: CameraParams) : Runnable {

    override fun run() {
        // Orientation
        val rotation = activity.getWindowManager().getDefaultDisplay().getRotation()
        val capturedImageRotation = getOrientation(params, rotation).toFloat()

        Logd( "ImageSaver. ImageSaver is running.")

        if (null == image)
            return

//        val file = File(Environment.getExternalStorageDirectory(), MainActivity.SAVE_FILE)

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)



//        val wasFaceDetected: Boolean =
//            CameraMetadata.STATISTICS_FACE_DETECT_MODE_OFF != cameraParams.bestFaceDetectionMode
//                && cameraParams.hasFace
//                && (cameraParams.faceBounds.left + cameraParams.faceBounds.right +
//                    cameraParams.faceBounds.bottom + cameraParams.faceBounds.top != 0)

        //1. Single lens: cut out head, paste it on blurred/sepia'd background
        val backgroundImageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        val foregroundImageBitmap = backgroundImageBitmap.copy(backgroundImageBitmap.config, true)

        //Foreground
        var croppedForeground = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        if (PrefHelper.getGrabCut(activity)) {

            MainActivity.Logd("Image callback Grabcut Bounds: bottom: " + cameraParams.grabCutBounds.bottom + " left: " + cameraParams.grabCutBounds.left + " right: " + cameraParams.grabCutBounds.right + " top: " + cameraParams.grabCutBounds.top)

            croppedForeground = doGrabCut(activity, foregroundImageBitmap, cameraParams.grabCutBounds)

            //Set the image view to be the final
            setCapturedPhoto(activity, imageView, rotateAndFlipBitmap(croppedForeground, -90f))

            activity.runOnUiThread {
                activity.captureFinished()
            }

            return
        } else {
            croppedForeground = cropBitmap(activity, foregroundImageBitmap, cameraParams.expandedFaceBounds)
        }

        if (PrefHelper.getSaveIntermediate(activity)) {
            WriteFile(activity, croppedForeground,"CroppedHead")
        }

        val scaledForeground = scaleBitmap(croppedForeground, MainActivity.BLUR_SCALE_FACTOR)

        if (PrefHelper.getIntermediate(activity)) {
            activity.runOnUiThread {
                activity.imageIntermediate2.setImageBitmap(horizontalFlip(rotateBitmap(scaledForeground, capturedImageRotation)))
            }
        }

        val featheredForeground = featherBitmap(activity, scaledForeground, 0.20f)

        if (PrefHelper.getSaveIntermediate(activity)) {
            WriteFile(activity, featheredForeground,"FeatheredHead", 100, true)
        }

        if (PrefHelper.getIntermediate(activity)) {
            activity.runOnUiThread {
                activity.imageIntermediate3.setImageBitmap(horizontalFlip(rotateBitmap(featheredForeground, capturedImageRotation)))
            }
        }

        val scaledBackground = scaleBitmap(backgroundImageBitmap, MainActivity.BLUR_SCALE_FACTOR)

        var sepiaBackground = Bitmap.createBitmap(scaledBackground)
        if (PrefHelper.getSepia(activity))
            sepiaBackground = sepiaFilter(activity, scaledBackground)
        else
            sepiaBackground = monoFilter(scaledBackground)

//        val blurredBackground = gaussianBlur(activity, sepiaBackground, MainActivity.GAUSSIAN_BLUR_RADIUS)
        val blurredBackground = CVBlur(sepiaBackground)

        if (PrefHelper.getSaveIntermediate(activity)) {
            WriteFile(activity, blurredBackground,"BlurredSepiaBackground")
        }

        if (PrefHelper.getIntermediate(activity)) {
            activity.runOnUiThread {
                activity.imageIntermediate4.setImageBitmap(horizontalFlip(rotateBitmap(blurredBackground, capturedImageRotation)))
            }
        }

//        if (wasFaceDetected) {
//            val pasteRect = Rect(cameraParams.expandedFaceBounds)
//            pasteRect.top = (pasteRect.top.toFloat() * BLUR_SCALE_FACTOR).roundToInt()
//            pasteRect.left = (pasteRect.left.toFloat() * BLUR_SCALE_FACTOR).roundToInt()
//            val combinedBitmap = pasteBitmap(activity, blurredBackground, featheredForeground, pasteRect)
//            val rotatedImageBitmap = rotateBitmap(combinedBitmap, capturedImageRotation.toFloat())
//
//            var finalBitmap = rotatedImageBitmap
//
//            //If front facing camera, flip the bitmap
//            if (cameraParams.isFront)
//                finalBitmap = horizontalFlip(finalBitmap)
//
//            //Set the image view to be the final
//            setCapturedPhoto(activity, imageView, finalBitmap)
//
//            //Save to disk
//            if (PrefHelper.getSaveIntermediate(activity)) {
//                WriteFile(activity, finalBitmap,"FloatingHeadShot", 100, true)
//            }

//        } else {
//            Logd("No face detected.")
//            val rotatedImageBitmap = rotateBitmap(blurredBackground, capturedImageRotation)
//            var finalBitmap = rotatedImageBitmap
//
//            //If front facing camera, flip the bitmap
//            if (cameraParams.isFront)
//                finalBitmap = horizontalFlip(finalBitmap)
//
//            //Set the image view to be the final
//            setCapturedPhoto(activity, imageView, finalBitmap)
//
//            //Save to disk
//            if (PrefHelper.getSaveIntermediate(activity)) {
//                WriteFile(activity, finalBitmap,"BackgroundShot")
//            }
//        }

        activity.runOnUiThread {
            activity.captureFinished()
        }
    }

}

fun rotateFlipScaleBitmap(original: Bitmap, degrees: Float) : Bitmap {
    val rotated: Bitmap = rotateBitmap(original, degrees)
    val flipped: Bitmap = horizontalFlip(rotated)
    val scaled: Bitmap = scaleBitmap(flipped, DISPLAY_BITMAP_SCALE)

    //We don't want to accidentally recycle the original bitmap
    if (original != rotated)
        rotated.recycle()

    if (original != flipped)
        flipped.recycle()

    return scaled
}

fun save(bytes: Bitmap, tempName: String) {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val dataDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TwoCameraImagesNew")
    if (!dataDir.exists()) {
        dataDir.mkdir()
    }
    val fileName = tempName + "_IMG_$timeStamp.jpg"
    val fileDir = File(dataDir.path + File.separator + fileName)
    try {
        val fileOutputStream = FileOutputStream(fileDir)
        bytes.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        //fileOutputStream.write(bytes)
        fileOutputStream.close()
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

//Convenience method that recycles unneeded temp bitmap
fun rotateAndFlipBitmap(original: Bitmap, degrees: Float): Bitmap {
    val rotated: Bitmap = rotateBitmap(original, degrees)
    val flipped: Bitmap = horizontalFlip(rotated)

    //We don't want to accidentally recycle the original bitmap
    if (original != rotated)
        rotated.recycle()

    return  flipped
}

fun rotateBitmap(original: Bitmap, degrees: Float): Bitmap {
    //If no rotation, no-op
    if (0f == degrees)
        return original

    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
}

fun setCapturedPhoto(activity: Activity, imageView: ImageView?, bitmap: Bitmap) {
    activity.runOnUiThread { imageView?.setImageBitmap(bitmap) }
}

fun scaleBitmap(bitmap: Bitmap, scaleFactor: Float): Bitmap {
    //If no scale, no-op
    if (1f == scaleFactor)
        return bitmap


    val scaledWidth = Math.round(bitmap.width * scaleFactor)
    val scaledHeight = Math.round(bitmap.height * scaleFactor)

    return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
}

fun gaussianBlur(activity: Activity, bitmap: Bitmap, blurRadius: Float): Bitmap {
    val rs = RenderScript.create(activity)
    val allocation = Allocation.createFromBitmap(rs, bitmap)
    val allocationType = allocation.type
    val blurredAllocation = Allocation.createTyped(rs, allocationType)
    val blurredBitmap = bitmap

    val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
    script.setRadius(blurRadius)
    script.setInput(allocation)

    //Do the blur
    script.forEach(blurredAllocation)
    blurredAllocation.copyTo(blurredBitmap)

    allocation.destroy()
    blurredAllocation.destroy()
    script.destroy()
    allocationType.destroy()
    rs.destroy()
    return blurredBitmap
}

fun sepiaFilter(activity: Activity, bitmap: Bitmap): Bitmap {
    val rs = RenderScript.create(activity)
    val allocation = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE,
        Allocation.USAGE_SCRIPT)
    val allocationType = allocation.type
    val sepiaAllocation = Allocation.createTyped(rs, allocationType)
    val sepiaBitmap = bitmap
    val script = ScriptC_sepia(rs)

    script.set_allocationIn(allocation)
    script.set_allocationOut(sepiaAllocation);
    script.set_script(script);
    script.invoke_filter();
    sepiaAllocation.copyTo(sepiaBitmap);

    return sepiaBitmap
}

fun drawBox(activity: Activity, cameraParams: CameraParams, bitmap: Bitmap): Bitmap {
    return drawBox(activity, bitmap, cameraParams.expandedFaceBounds)
}

fun drawBox(activity: Activity, bitmap: Bitmap, rect: Rect): Bitmap {
    val bitmapBoxed = bitmap.copy(Bitmap.Config.ARGB_8888, true);
    val canvas = Canvas(bitmapBoxed)
    val paint = Paint()
    paint.setColor(Color.GREEN)
    canvas.drawBitmap(bitmap, 0f, 0f, null)
    canvas.drawRect(rect, paint)
    return bitmapBoxed
}

fun monoFilter(bitmap: Bitmap) : Bitmap {
    val mono: Bitmap = Bitmap.createBitmap(bitmap)
    val canvas: Canvas = Canvas(mono)
    val matrix: ColorMatrix = ColorMatrix()
    matrix.setSaturation(0f)

    val paint: Paint = Paint()
    paint.setColorFilter(ColorMatrixColorFilter(matrix))
    canvas.drawBitmap(bitmap, 0f, 0f, paint)

    return mono
}

fun horizontalFlip(bitmap: Bitmap) : Bitmap {
    val matrix = Matrix()
    matrix.preScale(-1.0f, 1.0f)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun cropMat(mat: Mat, cropFactor: Float) : Mat {
    val cropRect: Rect = Rect(0, 0, mat.cols(), mat.rows())
    Logd("In cropMat.  Left: " + cropRect.left + " Right: " + cropRect.right + " Top: " + cropRect.top + " Bottom: " + cropRect.bottom)
    cropRect.inset(mat.cols() - (cropFactor * mat.cols()).toInt(), mat.rows() - (cropFactor * mat.rows()).toInt())

    Logd("In cropMat after inset.  Left: " + cropRect.left + " Right: " + cropRect.right + " Top: " + cropRect.top + " Bottom: " + cropRect.bottom)

    val cvRect: org.opencv.core.Rect = org.opencv.core.Rect(cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
    val croppedMat: Mat = Mat(mat, cvRect);

    return croppedMat
}

fun cropBitmap(activity: Activity, bitmap: Bitmap, cropFactor: Float) : Bitmap {
    val cropRect: Rect = Rect(0, 0, bitmap.width, bitmap.height)
    cropRect.inset((cropFactor * bitmap.width).toInt(), (cropFactor * bitmap.height).toInt())
    return cropBitmap(activity, bitmap, cropRect)
}

fun cropBitmap(activity: Activity, bitmap: Bitmap, rect: Rect) : Bitmap {
    if (!(rect.left < rect.right && rect.top < rect.bottom)) {
        Logd("In cropBitmap. Rect bounds incorrect, skipping crop. Left: " + rect.left + " Right: " + rect.right + " Top: " + rect.top + " Bottom: " + rect.bottom)
        return bitmap
    }
    Logd("In cropBitmap. Rect bounds Left: " + rect.left + " Right: " + rect.right + " Top: " + rect.top + " Bottom: " + rect.bottom)

    val croppedBitmap = Bitmap.createBitmap(rect.right - rect.left, rect.bottom - rect.top, Bitmap.Config.ARGB_8888)
    Canvas(croppedBitmap).drawBitmap(bitmap, 0f - rect.left, 0f - rect.top, null)

    return croppedBitmap
}

fun pasteBitmap(activity: Activity, background: Bitmap, foreground: Bitmap) : Bitmap {
    val rect: Rect = Rect(0, 0, background.width, background.height)
    return pasteBitmap(activity, background, foreground, rect)
}

fun pasteBitmap(activity: Activity, background: Bitmap, foreground: Bitmap, rect: Rect) : Bitmap {
    val combinedBitmap = Bitmap.createBitmap(background.width, background.height, background.config)
    val canvas = Canvas(combinedBitmap)
    canvas.drawBitmap(background, Matrix(), null)
    canvas.drawBitmap(foreground, rect.left.toFloat(), rect.top.toFloat(), null)
    return combinedBitmap
}

fun featherBitmap(activity: Activity, bitmap: Bitmap, borderSize: Float = 0.1f) : Bitmap {
    val featheredBitmap = Bitmap.createBitmap(bitmap)

    val canvas = Canvas(featheredBitmap)
    val framePaint = Paint()
    for (i in 0..3) {
        setFramePaint(framePaint, i, featheredBitmap.width.toFloat(), featheredBitmap.height.toFloat(), borderSize)
        canvas.drawPaint(framePaint)
    }

    return featheredBitmap
}


//From https://stackoverflow.com/questions/14172085/draw-transparent-gradient-with-alpha-transparency-from-0-to-1
private fun setFramePaint(p: Paint, side: Int, width: Float, height: Float, borderSize: Float = 0.1f) {

    p.shader = null
    p.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

    //use the smaller image size to calculate the actual border size
    val bSize = if (width > height) height * borderSize else width * borderSize

    var g1x = 0f
    var g1y = 0f
    var g2x = 0f
    var g2y = 0f
    var c1 = 0
    var c2 = 0

    if (side == 0) {
        //left
        g1x = 0f
        g1y = height / 2
        g2x = bSize
        g2y = height / 2
        c1 = Color.TRANSPARENT
        c2 = Color.BLACK

    } else if (side == 1) {
        //top
        g1x = width / 2
        g1y = 0f
        g2x = width / 2
        g2y = bSize
        c1 = Color.TRANSPARENT
        c2 = Color.BLACK


    } else if (side == 2) {
        //right
        g1x = width
        g1y = height / 2
        g2x = width - bSize
        g2y = height / 2
        c1 = Color.TRANSPARENT
        c2 = Color.BLACK


    } else if (side == 3) {
        //bottom
        g1x = width / 2
        g1y = height
        g2x = width / 2
        g2y = height - bSize
        c1 = Color.TRANSPARENT
        c2 = Color.BLACK
    }

    p.shader = LinearGradient(g1x, g1y, g2x, g2y, c1, c2, Shader.TileMode.CLAMP)
}

class OpenCVLoaderCallback(val context: Context) : BaseLoaderCallback(context) {
    override fun onManagerConnected(status: Int) {
        when (status) {
            LoaderCallbackInterface.SUCCESS -> {
                Logd("OpenCV loaded successfully")
            }
            else -> {
                super.onManagerConnected(status);
            }
        }
    }
}

fun WriteFile(activity: MainActivity, bitmap: Bitmap, name: String, quality: Int = 100, writePNG: Boolean = false, dirTimeStamp: String = "") {
    val PHOTOS_DIR: String = "BasicBokeh/"+dirTimeStamp

    var jpgFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
        File.separatorChar + PHOTOS_DIR + File.separatorChar +
                name + ".jpg")

    if (writePNG)
        jpgFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            File.separatorChar + PHOTOS_DIR + File.separatorChar +
                    name + ".png")

    val photosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), PHOTOS_DIR)

    if (!photosDir.exists()) {
        val createSuccess = photosDir.mkdir()
        if (!createSuccess) {
            Toast.makeText(activity, "DCIM/" + PHOTOS_DIR + " creation failed.", Toast.LENGTH_SHORT).show()
            Logd("Photo storage directory DCIM/" + PHOTOS_DIR + " creation failed!!")
        } else {
            Logd("Photo storage directory DCIM/" + PHOTOS_DIR + " did not exist. Created.")
        }
    }

    var output: FileOutputStream? = null
    try {
        output = FileOutputStream(jpgFile)

        if (writePNG)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        else
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)

    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        if (null != output) {
            try {
                output.close()

                //File is written, let media scanner know
                val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                scannerIntent.data = Uri.fromFile(jpgFile)
                activity.sendBroadcast(scannerIntent)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    Logd("WriteFile: Completed.")
}

fun getFileHandle (activity: MainActivity, name: String, withTimestamp: Boolean, dirTimeStamp: String) : File {
//    val PHOTOS_DIR: String = "BasicBokeh"

    val PHOTOS_DIR: String = "BasicBokeh/"+ dirTimeStamp
    var filePath = File.separatorChar + PHOTOS_DIR + File.separatorChar + name

    if (withTimestamp)
        filePath += "-" + generateTimestamp()


    filePath += ".jpg"

    val jpgFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filePath)

    val photosDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), PHOTOS_DIR)

    if (!photosDir.exists()) {
        val createSuccess = photosDir.mkdir()
        if (!createSuccess) {
            Toast.makeText(activity, "DCIM/" + PHOTOS_DIR + " creation failed.", Toast.LENGTH_SHORT).show()
            Logd("Photo storage directory DCIM/" + PHOTOS_DIR + " creation failed!!")
        } else {
            Logd("Photo storage directory DCIM/" + PHOTOS_DIR + " did not exist. Created.")
        }
    }

    return jpgFile
}

fun WriteFile(activity: MainActivity, bitmap: Bitmap, name: String, withTimestamp: Boolean = false, dirTimeStamp: String = "") {

    val jpgFile = getFileHandle(activity, name, withTimestamp, dirTimeStamp)

    var output: FileOutputStream? = null
    try {
        output = FileOutputStream(jpgFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        if (null != output) {
            try {
                output.close()

                //File is written, let media scanner know
                val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                scannerIntent.data = Uri.fromFile(jpgFile)
                activity.sendBroadcast(scannerIntent)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    Logd("WriteFile: Completed.")
}

fun WriteFile(activity: MainActivity, bytes: ByteArray, name: String, withTimestamp: Boolean = false, dirTimeStamp: String = "") {

    val jpgFile = getFileHandle(activity, name, withTimestamp, dirTimeStamp)

    var output: FileOutputStream? = null
    try {
        output = FileOutputStream(jpgFile)
        output.write(bytes)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        if (null != output) {
            try {
                output.close()

                //File is written, let media scanner know
                val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                scannerIntent.data = Uri.fromFile(jpgFile)
                activity.sendBroadcast(scannerIntent)

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    Logd("WriteFile: Completed.")
}

fun generateTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
    return sdf.format(Date())
}

fun generateTimestamp2Sec(): String {
    val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
    return sdf.format(Date())
}

fun applyMask(activity: MainActivity, image: Bitmap, mask: Bitmap) : Bitmap {
    val maskedImage = Bitmap.createBitmap(image.width, image.height, image.config)
    val canvas = Canvas(maskedImage)

    val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    maskPaint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_IN))

    canvas.drawBitmap(image, 0.0f, 0.0f, Paint())
    canvas.drawBitmap(mask, 0.0f, 0.0f, maskPaint)

    WriteFile(activity, maskedImage, "MaskedFull", 100, true)

    return maskedImage
}

fun hardNormalizeDepthMap(activity: MainActivity, inputBitmap: Bitmap) : Bitmap {
    return hardNormalizeDepthMap(activity, inputBitmap, PrefHelper.getForegroundCutoff(activity), 100.0)
}

//Normalize depthmap to be mostly white or black, with steep curve at cutoff between 0 - 255
fun hardNormalizeDepthMap(activity: Activity, inputBitmap: Bitmap, cutoff: Double, blurSize: Double = 100.0) : Bitmap {
    val inputMat: Mat = Mat()
    Utils.bitmapToMat(inputBitmap, inputMat)
    val inputMat1C = Mat()
//    inputMat.convertTo(inputMat1C, CV_8UC1) //Make sure the bit depth is right
    Imgproc.cvtColor(inputMat, inputMat1C, CV_8UC1) //Make sure the channels are right

    val outputMat = hardNormalizeDepthMap(inputMat, cutoff, blurSize)
    var outputBitmap: Bitmap = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(outputMat, outputBitmap)

    outputBitmap = blackToTransparent(outputBitmap)
    outputBitmap = CVBlur(outputBitmap)
//    outputBitmap = gaussianBlur(activity, outputBitmap, 25f)
//    outputBitmap = gaussianBlur(activity, outputBitmap, 25f)

    return outputBitmap
}

//Normalize depthmap to be mostly white or black, with steep curve at cutoff between 0 - 255
fun hardNormalizeDepthMap(inputMat: Mat, cutoff: Double = 80.0, blurSize: Double = 100.0) : Mat {
    val normalizedMat = Mat()

    //Make sure we're in the right format
    inputMat.convertTo(normalizedMat, CV_8UC1)

    //Hard threshold everything
    threshold(normalizedMat, normalizedMat, cutoff, 255.0, THRESH_BINARY);

    return normalizedMat
}

fun blackToTransparent(bitmap: Bitmap, cutoff: Int = 50) : Bitmap {
    val pixels: IntArray = IntArray(bitmap.height * bitmap.width)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    for (i in 0 until pixels.size) {
        if (Color.red(pixels[i]) <= cutoff && Color.blue(pixels[i]) <= cutoff && Color.green(pixels[i]) <= cutoff) {
            pixels[i] = pixels[i] and 0x00ffffff
        }
    }

    bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return  bitmap
}

fun CVBlur(bitmap: Bitmap, radius: Int = 71) : Bitmap {
    val inMat: Mat = Mat()
    val outMat: Mat = Mat()

    var blurRadius = radius

    //Only odd numbers for blur radius
    if (0 == radius % 2) blurRadius++

    Utils.bitmapToMat(bitmap, inMat)
    GaussianBlur(inMat, outMat, Size(blurRadius.toDouble(), blurRadius.toDouble()), 0.0, 0.0)

    val outputBitmap: Bitmap = Bitmap.createBitmap(outMat.width(), outMat.height(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(outMat, outputBitmap)

    return outputBitmap
}

fun doGrabCut(activity: MainActivity, bitmap: Bitmap, rect: Rect) : Bitmap {

    val imageMat: Mat = Mat()
    val foregroundMat: Mat = Mat()
    val backgroundMat: Mat = Mat()
    val fgModel: Mat = Mat()
    val bgModel: Mat = Mat()
    val cutMask: Mat = Mat()

    val scaleFactor: Float = 0.25f

    val scaledBitmap = scaleBitmap(bitmap, scaleFactor)

    val cvRect: org.opencv.core.Rect = org.opencv.core.Rect((rect.left * scaleFactor).toInt(), (rect.top * scaleFactor).toInt(),
        (rect.width() * scaleFactor).toInt(), (rect.height() * scaleFactor).toInt())
    Utils.bitmapToMat(scaledBitmap, imageMat)
    val imageMat3: Mat = Mat()
    Imgproc.cvtColor(imageMat, imageMat3, COLOR_BGRA2BGR)

    Logd("About to do Grabcut. Images size is: " + imageMat3.cols() + "x" + imageMat3.rows() + ". Bounds are: " + cvRect.x + ", " + cvRect.y + ", " + (cvRect.width + cvRect.x) + ", " + (cvRect.height + cvRect.y))

    Imgproc.grabCut(imageMat3, cutMask, cvRect, bgModel, fgModel, 2, Imgproc.GC_INIT_WITH_RECT)

    val normalizedCutMask: Mat = Mat()
    Core.normalize(cutMask, normalizedCutMask, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)

    val normalizedCutMaskConverted: Mat = Mat()
    normalizedCutMask.convertTo(normalizedCutMaskConverted, CV_8UC1, 1.0);

    val normalizedBitmap: Bitmap = Bitmap.createBitmap(normalizedCutMaskConverted.cols(), normalizedCutMaskConverted.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(normalizedCutMaskConverted, normalizedBitmap)

    val foregroundMask = hardNormalizeDepthMap(activity, normalizedBitmap, 254.0)

    val scaledMask = scaleBitmap(foregroundMask, 1f / scaleFactor)

    if (PrefHelper.getIntermediate(activity)) {
        //Lay it on a black background
        val black = Bitmap.createBitmap(scaledMask.height, scaledMask.width, Bitmap.Config.ARGB_8888)
        val blackCanvas = Canvas(black)
        val paint = Paint()
        paint.setColor(Color.BLACK)
        blackCanvas.drawRect(0f, 0f, scaledMask.height.toFloat(), scaledMask.width.toFloat(), paint)
        val tempBitmap = rotateAndFlipBitmap(scaledMask,getRequiredBitmapRotation(activity))
        activity.runOnUiThread {
            activity.imageIntermediate2.setImageBitmap(pasteBitmap(activity, black, tempBitmap))
        }
    }

    val nicelyMasked = applyMask(activity, bitmap, scaledMask)

    if (PrefHelper.getIntermediate(activity)) {
        activity.runOnUiThread {
            activity.imageIntermediate3.setImageBitmap(rotateAndFlipBitmap(nicelyMasked,getRequiredBitmapRotation(activity)))
        }
    }

    var backgroundBitmap = bitmap

    if (PrefHelper.getSepia(activity))
        backgroundBitmap = sepiaFilter(activity, bitmap)
    else
        backgroundBitmap = monoFilter(bitmap)

    val blurredBackgroundBitmap = CVBlur(backgroundBitmap)

    if (PrefHelper.getIntermediate(activity)) {
        activity.runOnUiThread {
            activity.imageIntermediate4.setImageBitmap(rotateAndFlipBitmap(blurredBackgroundBitmap,getRequiredBitmapRotation(activity)))
        }
    }


    val finalImage = pasteBitmap(activity, blurredBackgroundBitmap, nicelyMasked, android.graphics.Rect(0, 0, blurredBackgroundBitmap.width, blurredBackgroundBitmap.height))


    return finalImage
}

fun faceBoundsToGrabCutBounds(activity: MainActivity, faceRect: Rect, imageWidth: Int, imageHeight: Int) : Rect {
    val faceWidth = (faceRect.right - faceRect.left) / 2
    val grabCutRect = Rect(faceRect)

    grabCutRect.right = faceRect.right
    grabCutRect.left = 0
    grabCutRect.top = faceRect.top - faceWidth
    grabCutRect.bottom = faceRect.bottom + faceWidth


    //Make sure we don't overshoot
    if (grabCutRect.left < 0) grabCutRect.left = 0
    if (grabCutRect.top < 0) grabCutRect.top = 0
    if (grabCutRect.right > imageWidth)
        grabCutRect.right = imageWidth
    if (grabCutRect.bottom > imageHeight)
        grabCutRect.bottom = imageHeight

    return grabCutRect
}

fun rotateRect(rect: Rect, rotation: Float, imageCenterX: Int, imageCenterY: Int, horizontalFlip: Boolean = false) {
    Logd("RotateRect Pre: left: " + rect.left + ", top: " + rect.top + ", right: " + rect.right + ", bottom: " + rect.bottom)

    val rectF: RectF = RectF(rect)
    val matrix: Matrix = Matrix();
    matrix.setRotate(rotation, imageCenterX.toFloat(), imageCenterY.toFloat());

    if (horizontalFlip)
        matrix.preScale(1.0f, -1f, imageCenterX.toFloat(), imageCenterY.toFloat())

    matrix.mapRect(rectF)
    rect.left = rectF.left.roundToInt()
    rect.top = rectF.top.roundToInt()
    rect.right = rectF.right.roundToInt()
    rect.bottom = rectF.bottom.roundToInt()

    Logd("RotateRect Post: left: " + rect.left + ", top: " + rect.top + ", right: " + rect.right + ", bottom: " + rect.bottom)
}*/