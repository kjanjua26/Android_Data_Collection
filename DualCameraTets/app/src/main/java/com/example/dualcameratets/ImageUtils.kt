package com.example.dualcameratets


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.DngCreator
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.example.dualcameratets.MainActivity.Companion.BLUR_SCALE_FACTOR
import com.example.dualcameratets.MainActivity.Companion.DISPLAY_BITMAP_SCALE
import com.example.dualcameratets.MainActivity.Companion.Logd
import com.example.dualcameratets.MainActivity.Companion.twoLens
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@SuppressLint("NewApi")
class ImageAvailableListener(private val activity: MainActivity, internal var params: CameraParams) : ImageReader.OnImageAvailableListener {

    override fun onImageAvailable(reader: ImageReader) {

        Log.d(MainActivity.LOG_TAG, "ImageReader. Image is available, about to post.")
        val image: Image = reader.acquireNextImage()
        val format: Int = image.getFormat()

        //val img: Image = reader.acquireLatestImage()

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
//        Log.d(MainActivity.LOG_TAG, "ImageReader. Post has been set.")
    }
}

@SuppressLint("NewApi")
class ImageSaver internal constructor(private val activity: MainActivity, private val params: CameraParams, private val image: Image?, private val imageView: ImageView?, private val flip: Boolean, private val cameraParams: CameraParams) : Runnable {

    override fun run() {
        // Orientation
        val rotation = activity.getWindowManager().getDefaultDisplay().getRotation()
        val capturedImageRotation = getOrientation(params, rotation).toFloat()

        Logd( "ImageSaver. ImageSaver is running.")

        if (null == image)
            return

        val file = File(Environment.getExternalStorageDirectory(), MainActivity.SAVE_FILE)



        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        save(bytes, "SINGLE_SHOT")
        //val wasFaceDetected = true
        /*
        val wasFaceDetected: Boolean =
            CameraMetadata.STATISTICS_FACE_DETECT_MODE_OFF != cameraParams.bestFaceDetectionMode
                && cameraParams.hasFace
                && (cameraParams.faceBounds.left + cameraParams.faceBounds.right +
                    cameraParams.faceBounds.bottom + cameraParams.faceBounds.top != 0)
        */

        //1. Single lens: cut out head, paste it on blurred/sepia'd background
        //val backgroundImageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        //val foregroundImageBitmap = backgroundImageBitmap.copy(backgroundImageBitmap.config, true)

        /*
        if (PrefHelper.getIntermediate(activity)) {
            activity.runOnUiThread {
                activity.imageIntermediate1.setImageBitmap(horizontalFlip(rotateBitmap(backgroundImageBitmap, capturedImageRotation)))
            }
        }*/

        //Foreground
        //var croppedForeground = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        //croppedForeground = cropBitmap(activity, foregroundImageBitmap, cameraParams.expandedFaceBounds)
        /*
            Save both shots.
            TODO: REMEMBER => Interested in CroppedHead and FeatheredHead only. => ONLY FOR SINGLE SHOT.
            TODO: Remove the face restriction.
         */
        //save(croppedForeground, "CroppedHead")

        /*if (PrefHelper.getSaveIntermediate(activity)) {
            save(croppedForeground, "CroppedHead")
            WriteFile(activity, croppedForeground,"CroppedHead", 100, true)
        }*/

        //val scaledForeground = scaleBitmap(croppedForeground, MainActivity.BLUR_SCALE_FACTOR)

        /*if (PrefHelper.getIntermediate(activity)) {
            activity.runOnUiThread {
                activity.imageIntermediate2.setImageBitmap(horizontalFlip(rotateBitmap(scaledForeground, capturedImageRotation)))
            }
        }*/

        //al featheredForeground = featherBitmap(activity, scaledForeground, 0.20f)
        //save(featheredForeground , "FeatheredHead")

        /*if (PrefHelper.getSaveIntermediate(activity)) {
            save(featheredForeground , "FeatheredHead")
            WriteFile(activity, featheredForeground,"FeatheredHead", 100, true)
        }*/

        /*if (PrefHelper.getIntermediate(activity)) {
            activity.runOnUiThread {
                activity.imageIntermediate3.setImageBitmap(horizontalFlip(rotateBitmap(featheredForeground, capturedImageRotation)))
            }
        }*/

        //val scaledBackground = scaleBitmap(backgroundImageBitmap, MainActivity.BLUR_SCALE_FACTOR)

        //var sepiaBackground = Bitmap.createBitmap(scaledBackground)
        /*if (PrefHelper.getSepia(activity))
            sepiaBackground = sepiaFilter(activity, scaledBackground)
        else
            sepiaBackground = monoFilter(scaledBackground)
        */
//        val blurredBackground = gaussianBlur(activity, sepiaBackground, MainActivity.GAUSSIAN_BLUR_RADIUS)
        //val blurredBackground = CVBlur(sepiaBackground)

        /*if (PrefHelper.getSaveIntermediate(activity)) {
            WriteFile(activity, blurredBackground,"BlurredSepiaBackground")
        }*/

        /*if (PrefHelper.getIntermediate(activity)) {
            activity.runOnUiThread {
                activity.imageIntermediate4.setImageBitmap(horizontalFlip(rotateBitmap(blurredBackground, capturedImageRotation)))
            }
        }*/
        /*
            Saving the image regardless.
            TODO: Check THIS for face errors.
         */
        //val rotatedImageBitmap = rotateBitmap(featheredForeground, capturedImageRotation.toFloat())

        //var finalBitmap = rotatedImageBitmap

        //If front facing camera, flip the bitmap
        //if (cameraParams.isFront)
        //    finalBitmap = horizontalFlip(finalBitmap)

        //Set the image view to be the final
        //setCapturedPhoto(activity, imageView, rotatedImageBitmap)
        /*
        if (wasFaceDetected) {
            //val pasteRect = Rect(cameraParams.expandedFaceBounds)
            //pasteRect.top = (pasteRect.top.toFloat() * BLUR_SCALE_FACTOR).roundToInt()
            //pasteRect.left = (pasteRect.left.toFloat() * BLUR_SCALE_FACTOR).roundToInt()
            //val combinedBitmap = pasteBitmap(activity, blurredBackground, featheredForeground, pasteRect)
            val rotatedImageBitmap = rotateBitmap(featheredForeground, capturedImageRotation.toFloat())

            var finalBitmap = rotatedImageBitmap

            //If front facing camera, flip the bitmap
            if (cameraParams.isFront)
                finalBitmap = horizontalFlip(finalBitmap)

            //Set the image view to be the final
            setCapturedPhoto(activity, imageView, finalBitmap)

        } else {
            Logd("No face detected.")
            val rotatedImageBitmap = rotateBitmap(featheredForeground, capturedImageRotation)
            var finalBitmap = rotatedImageBitmap

            //If front facing camera, flip the bitmap
            if (cameraParams.isFront)
                finalBitmap = horizontalFlip(finalBitmap)

            //Set the image view to be the final
            setCapturedPhoto(activity, imageView, finalBitmap)

            //Save to disk

        }
        */
    }

}
fun saveRAW(){
    //val dngCreater: DngCreator = DngCreator(cameraChars, )
}
fun save(bytes: ByteArray, tempName: String) {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val dataDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TwoCameraImages")
    if (!dataDir.exists()) {
        dataDir.mkdir()
    }
    val fileName = tempName + "_IMG_$timeStamp.jpg"
    val fileDir = File(dataDir.path + File.separator + fileName)
    try {
        val fileOutputStream = FileOutputStream(fileDir)
        //bytes.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
        fileOutputStream.write(bytes)
        fileOutputStream.close()
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun save(bytes: Bitmap, tempName: String) {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val dataDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TwoCameraImages")
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

//fun drawBox(activity: Activity, cameraParams: CameraParams, bitmap: Bitmap): Bitmap {
//   return drawBox(activity, bitmap, cameraParams.expandedFaceBounds)
//}

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

fun WriteFile(activity: MainActivity, bitmap: Bitmap, name: String, quality: Int = 100, writePNG: Boolean = false) {
    val PHOTOS_DIR: String = "BasicBokeh"

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

fun getFileHandle (activity: MainActivity, name: String, withTimestamp: Boolean) : File {
    val PHOTOS_DIR: String = "BasicBokeh"

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

fun WriteFile(activity: MainActivity, bitmap: Bitmap, name: String, withTimestamp: Boolean = false) {

    val jpgFile = getFileHandle(activity, name, withTimestamp)

    var output: FileOutputStream? = null
    try {
        output = FileOutputStream(jpgFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 91, output)
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

fun WriteFile(activity: MainActivity, bytes: ByteArray, name: String, withTimestamp: Boolean = false) {

    val jpgFile = getFileHandle(activity, name, withTimestamp)

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