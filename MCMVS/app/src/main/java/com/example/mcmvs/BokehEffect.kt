package com.example.mcmvs

import android.graphics.*
import android.os.Build
import com.example.mcmvs.MainActivity.Companion.Logd
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer
import android.R.attr.path
import android.graphics.Bitmap
import android.hardware.SensorManager
import android.widget.Toast
import org.opencv.core.CvType.CV_64FC1
import org.opencv.core.CvType.CV_8UC1
import org.opencv.core.Mat
import org.opencv.calib3d.Calib3d.CALIB_ZERO_DISPARITY
import org.opencv.calib3d.Calib3d.stereoRectify
import org.opencv.calib3d.StereoMatcher
import org.opencv.calib3d.StereoSGBM
import org.opencv.core.*
import org.opencv.core.Core.*
import org.opencv.core.CvType.*
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.*
import org.opencv.android.Utils
import java.text.SimpleDateFormat
import java.util.*

/*
    Since the camera names are inverted, the distortion and intrinsic matrices are also inverted to match the actual camera outputs.
    Some weird multicam issues urges to preview the wide angle and not the normal, therefore inverted the names.
    Doesn't draw the preview if you invert the camera IDs in cameraUtils.kt
 */

// TODO: Stress the capture part of the code, maybe there is some callback lag here.
/*
    Some notes:

    1. use .recycle() for the garbage collector to collect the bitmaps, prolly should be used at the end of the code.
    2. Cannot clear heap of android, but prolly can free the memory after every capture.
    3. Rectification causes some lags.
    4. After about 3 mins of continuous capture, there is lag in the camera => Maybe the bitmaps are causing this?
 */

fun DoBokeh(activity: MainActivity, twoLens: TwoLensCoordinator) {
    //Temporary Bitmap for flipping and rotation operations, to ensure correct memory clean-up
    //var tempBitmap: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    activity.sensorManager!!.registerListener(activity.sensorReader, activity.gyroSensor, SensorManager.SENSOR_DELAY_NORMAL)
    activity.sensorManager!!.registerListener(activity.sensorReader, activity.accSensor, SensorManager.SENSOR_DELAY_NORMAL)

    //We need both shots to be done and both images in order to proceed
    //if (!twoLens.normalShotDone || !twoLens.wideShotDone || (null == twoLens.normalImage)
    //    || (null == twoLens.wideImage))
    //    return tempBitmap //Return empty bitmap

    //tempBitmap.recycle()

    Logd("Normal image timestamp: " + twoLens.normalImage?.timestamp)
    Logd("Wide image timestamp: " + twoLens.wideImage?.timestamp)

    val wideBuffer: ByteBuffer? = twoLens.wideImage!!.planes[0].buffer
    val wideBytes = ByteArray(wideBuffer!!.remaining())
    wideBuffer.get(wideBytes)

    val normalBuffer: ByteBuffer? = twoLens.normalImage!!.planes[0].buffer
    // Timestamp => twoLens.normalImage!!.timestamp
    val normalBytes = ByteArray(normalBuffer!!.remaining())
    normalBuffer.get(normalBytes)

    //val wideMat: Mat = Mat(twoLens.wideImage!!.height, twoLens.wideImage!!.width, CV_8UC1)
    //val tempWideBitmap = BitmapFactory.decodeByteArray(wideBytes, 0, wideBytes.size, null)
    //Utils.bitmapToMat(tempWideBitmap, wideMat)

    //val normalMat: Mat = Mat(twoLens.normalImage!!.height, twoLens.normalImage!!.width, CV_8UC1)
    //val tempNormalBitmap = BitmapFactory.decodeByteArray(normalBytes, 0, normalBytes.size, null)
    //Utils.bitmapToMat(tempNormalBitmap, normalMat)
    //Logd("Sensor: " + activity.gyroData)

    //MainActivity.counter += 1
    //save(tempWideBitmap, "NormalShot")
    //save(tempNormalBitmap, "WideShot")
    save(wideBytes, "NormalShot")
    save(normalBytes, "WideShot")
    save(activity, activity.gyroData, activity.accData, (twoLens.normalImage!!.timestamp/1e9).toString(), (twoLens.wideImage!!.timestamp/1e9).toString())


    // Do some clearing here to save some memory
    normalBuffer.clear()
    wideBuffer.clear()


    /*MainActivity.wideBitmaps.put(MainActivity.counter.toString(), tempWideBitmap)
    MainActivity.normalBitmaps.put(MainActivity.counter.toString(), tempNormalBitmap)

    var finalNormalMat: Mat = Mat(normalMat.rows(), normalMat.cols(), CV_8UC1)
    Imgproc.cvtColor(normalMat, finalNormalMat, Imgproc.COLOR_BGR2GRAY)

    var finalWideMat: Mat = Mat(wideMat.rows(), wideMat.cols(), CV_8UC1)
    Imgproc.cvtColor(wideMat, finalWideMat, Imgproc.COLOR_BGR2GRAY)

    val camMatrixNormal: Mat = Mat(3, 3, CV_64FC1)
    val normalIntrinsic = doubleArrayOf(2194.74126636864, 0.0, 1521.31199299266,
        0.0, 2197.96511039655, 1589.35743671811,
        0.0, 0.0, 1.0)
    setMat(camMatrixNormal, 3, 3, normalIntrinsic)

    val camMatrixWide: Mat = Mat(3, 3, CV_64FC1)
    val wideIntrinsic = doubleArrayOf(2241.54939636141, 0.0, 1515.75552775772,
        0.0, 2244.61921892845, 1601.02584222253,
        0.0, 0.0, 1.0)
    setMat(camMatrixWide, 3, 3, wideIntrinsic)

    var distCoeffNormal: Mat = Mat(5, 1, CV_64FC1)
    val normalDist = doubleArrayOf(0.00970718088539291,	0.0302182814054848,	-0.0196189363907549, 0.00137731709482448,	1.91652329368791e-05)
    setMat(distCoeffNormal, 5, 1, normalDist)

    var distCoeffWide: Mat = Mat(5, 1, CV_64FC1)
    val wideDist = doubleArrayOf(-0.0386022410437852,	0.0356860602389576,	-0.0206818230155171, -0.000730111144107342,	-9.30878336147479e-06)
    setMat(distCoeffWide, 5, 1, wideDist)

    Logd("Cam Matrix K1 Check: "
            + camMatrixNormal[0, 0].get(0) + ", "
            + camMatrixNormal[0, 1].get(0) + ", "
            + camMatrixNormal[0, 2].get(0) + ", "
            + camMatrixNormal[1, 0].get(0) + ", "
            + camMatrixNormal[1, 1].get(0) + ", "
            + camMatrixNormal[1, 2].get(0) + ", "
            + camMatrixNormal[2, 0].get(0) + ", "
            + camMatrixNormal[2, 1].get(0) + ", "
            + camMatrixNormal[2, 2].get(0)
    )

    Logd("Cam Matrix K2 Check: "
            + camMatrixWide[0, 0].get(0) + ", "
            + camMatrixWide[0, 1].get(0) + ", "
            + camMatrixWide[0, 2].get(0) + ", "
            + camMatrixWide[1, 0].get(0) + ", "
            + camMatrixWide[1, 1].get(0) + ", "
            + camMatrixWide[1, 2].get(0) + ", "
            + camMatrixWide[2, 0].get(0) + ", "
            + camMatrixWide[2, 1].get(0) + ", "
            + camMatrixWide[2, 2].get(0)
    )
    val combinedR: Mat = Mat(3, 3, CV_64FC1)
    val combinedT: Mat = Mat(3, 1, CV_64FC1)

    val rotationMatrix = doubleArrayOf(0.999997520763306,	0.00193782468335406,	-0.00109695156599813,
        -0.00194423396541784,	0.999980868338488,	-0.00587221519451645,
        0.00108555125594218,	0.00587433336639810,	0.999982156733795)
    setMat(combinedR, 3, 3, rotationMatrix)

    val translationMatrix = doubleArrayOf(-0.0679691746773442, -10.3504444250521,	1.13315247620887)
    setMat(combinedT, 3, 1, translationMatrix)


    //Stereo rectify
    val R1: Mat = Mat(3, 3, CV_64FC1)
    val R2: Mat = Mat(3, 3, CV_64FC1)
    val P1: Mat = Mat(3, 4, CV_64FC1)
    val P2: Mat = Mat(3, 4, CV_64FC1)
    val Q: Mat = Mat(4, 4, CV_64FC1)

    val roi1: Rect = Rect()
    val roi2: Rect = Rect()

    stereoRectify( camMatrixNormal, distCoeffNormal, camMatrixWide, distCoeffWide,
        finalNormalMat.size(), combinedR, combinedT, R1, R2, P1, P2, Q,
        CALIB_ZERO_DISPARITY, 0.0, Size(), roi1, roi2)


    Logd("R1: " + R1[0,0].get(0) + ", " + R1[0,1].get(0) + ", " + R1[0,2].get(0) + ", " + R1[1,0].get(0) + ", " + R1[1,1].get(0) + ", " + R1[1,2].get(0) + ", " + R1[2,0].get(0) + ", " + R1[2,1].get(0) + ", " + R1[2,2].get(0))
    Logd("R2: " + R2[0,0].get(0) + ", " + R2[0,1].get(0) + ", " + R2[0,2].get(0) + ", " + R2[1,0].get(0) + ", " + R2[1,1].get(0) + ", " + R2[1,2].get(0) + ", " + R2[2,0].get(0) + ", " + R2[2,1].get(0) + ", " + R2[2,2].get(0))
    Logd("P1: " + P1[0,0].get(0) + ", " + P1[0,1].get(0) + ", " + P1[0,2].get(0) + ", " + P1[0,3].get(0) + ", " + P1[1,0].get(0) + ", " + P1[1,1].get(0) + ", " + P1[1,2].get(0) + ", " + P1[1,3].get(0) + ", "
            + P1[2,0].get(0) + ", " + P1[2,1].get(0) + ", " + P1[2,2].get(0) + ", " + P1[2,3].get(0))
    Logd("P2: " + P2[0,0].get(0) + ", " + P2[0,1].get(0) + ", " + P2[0,2].get(0) + ", " + P2[0,3].get(0) + ", " + P2[1,0].get(0) + ", " + P2[1,1].get(0) + ", " + P2[1,2].get(0) + ", " + P2[1,3].get(0) + ", "
            + P2[2,0].get(0) + ", " + P2[2,1].get(0) + ", " + P2[2,2].get(0) + ", " + P2[2,3].get(0))
    Logd("roi1: " + roi1.x+","+roi1.y+","+roi1.width+","+roi1.height)
    Logd("roi2: " + roi2.x+","+roi2.y+","+roi2.width+","+roi2.height)

    val mapNormal1: Mat = Mat()
    val mapNormal2: Mat = Mat()
    val mapWide1: Mat = Mat()
    val mapWide2: Mat = Mat()

    /*
        Did the changings here
     */
    initUndistortRectifyMap(camMatrixNormal, distCoeffNormal, R1, P1, finalNormalMat.size(), CV_32F, mapNormal1, mapNormal2)
    initUndistortRectifyMap(camMatrixWide, distCoeffWide, R2, P2, finalWideMat.size(), CV_32F, mapWide1, mapWide2)

    //initUndistortRectifyMap(camMatrixNormal, distCoeffNormal, R1, P1, finalWideMat.size(), CV_32F, mapWide1, mapWide2)
    //initUndistortRectifyMap(camMatrixWide, distCoeffWide, R2, P2, finalNormalMat.size(), CV_32F, mapNormal1, mapNormal2)

    val rectifiedNormalMat: Mat = Mat()
    val rectifiedWideMat: Mat = Mat()

    val rectifiedNormalMatColor: Mat = Mat()
    val rectifiedWideMatColor: Mat = Mat()

    remap(finalNormalMat, rectifiedNormalMat, mapNormal1, mapNormal2, INTER_LINEAR)
    remap(finalWideMat, rectifiedWideMat, mapWide1, mapWide2, INTER_LINEAR)

    remap(normalMat, rectifiedNormalMatColor, mapNormal1, mapNormal2, INTER_LINEAR)
    remap(wideMat, rectifiedWideMatColor, mapWide1, mapWide2, INTER_LINEAR)

    val daiMat = Mat()
    absdiff(rectifiedNormalMat,rectifiedWideMat,daiMat)

    Logd( "Now saving rectified photos to disk.")
    val rectifiedNormalBitmap: Bitmap = Bitmap.createBitmap(rectifiedNormalMat.cols(), rectifiedNormalMat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(rectifiedNormalMat, rectifiedNormalBitmap)
    val rectifiedWideBitmap: Bitmap = Bitmap.createBitmap(rectifiedWideMat.cols(), rectifiedWideMat.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(rectifiedWideMat, rectifiedWideBitmap)

    val rectifiedNormalBitmapColor: Bitmap = Bitmap.createBitmap(rectifiedNormalMatColor.cols(), rectifiedNormalMatColor.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(rectifiedNormalMatColor, rectifiedNormalBitmapColor)
    val rectifiedWideBitmapColor: Bitmap = Bitmap.createBitmap(rectifiedWideMatColor.cols(), rectifiedWideMatColor.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(rectifiedWideMatColor, rectifiedWideBitmapColor)

    val rectifiedNormalBitmapColorFinal = horizontalFlip(rotateBitmap(rectifiedNormalBitmapColor, 180f))
    val rectifiedWideBitmapColorFinal = horizontalFlip(rotateBitmap(rectifiedWideBitmapColor, 180f))

    //val rectifiedNormalBitmapColorResize = Bitmap.createScaledBitmap(rectifiedNormalBitmapColorFinal, 280, 280, false)
    //val rectifiedWideBitmapColorResize = Bitmap.createScaledBitmap(rectifiedWideBitmapColorFinal, 280, 280, false)

    //val rectifiedNormalBitmapColorResize = Bitmap.createScaledBitmap(rectifiedNormalBitmapColor, tempNormalBitmap.width, tempNormalBitmap.height, false)
    //val rectifiedWideBitmapColorResize = Bitmap.createScaledBitmap(rectifiedWideBitmapColor, tempWideBitmap.width, tempWideBitmap.height, false)

    save(rectifiedWideBitmapColor, "NormalShotR")
    save(rectifiedNormalBitmapColor, "WideShotR")

/*

//    if (PrefHelper.getIntermediate(activity)) {
//        activity.runOnUiThread {
//            activity.imageIntermediate1.setImageBitmap(tempNormalBitmap)
//            activity.imageIntermediate2.setImageBitmap(tempWideBitmap)
//        }
//    }

    //if (PrefHelper.getCalibrationMode(activity)) {
    //    return tempNormalBitmap
   // }

   //if (PrefHelper.getSaveIntermediate(activity)) {
    //    WriteFile(activity, tempWideBitmap,"WideShot",false, dirTimeStamp)
     //   WriteFile(activity, tempNormalBitmap, "NormalShot",false, dirTimeStamp)
   // }

//    activity.runOnUiThread {
//        activity.captureFinished()
//    }
//
//    return tempNormalBitmap

    //Convert the Mats to 1-channel greyscale so we can compute depth maps
    //var finalNormalMat: Mat = Mat(normalMat.rows(), normalMat.cols(), CV_8UC1)
    //Imgproc.cvtColor(normalMat, finalNormalMat, Imgproc.COLOR_BGR2GRAY)

    //var finalWideMat: Mat = Mat(wideMat.rows(), wideMat.cols(), CV_8UC1)
    //Imgproc.cvtColor(wideMat, finalWideMat, Imgproc.COLOR_BGR2GRAY)

    //Get camera matrices
    //If we are >= 28, rectify images to get a good depth map.
    if (Build.VERSION.SDK_INT >= 28) {

        val camMatrixNormal: Mat = Mat(3, 3, CV_64FC1)
//        setMat(camMatrixNormal, 3, 3, cameraMatrixFromCalibration(twoLens.normalParams.intrinsicCalibration))

//        val normalIntrinsic = doubleArrayOf(2602.83361988417, 0.0, 1514.37913414489,
//            0.0, 2606.20113153243, 1574.34632335443,
//            0.0,  0.0, 1.0)

        val normalIntrinsic = doubleArrayOf(2241.54939636141, 0.0, 1515.75552775772,
            0.0, 2244.61921892845, 1601.02584222253,
            0.0, 0.0, 1.0)
        setMat(camMatrixNormal, 3, 3, normalIntrinsic)

        val camMatrixWide: Mat = Mat(3, 3, CV_64FC1)
//        setMat(camMatrixWide, 3, 3, cameraMatrixFromCalibration(twoLens.wideParams.intrinsicCalibration))



//        val wideIntrinsic = doubleArrayOf(2548.26291085944, 0.0, 1518.74465212339,
//            0.0, 2551.38435738305, 1559.03369002338,
//            0.0, 0.0, 1.0)
        val wideIntrinsic = doubleArrayOf(2194.74126636864, 0.0, 1521.31199299266,
            0.0, 2197.96511039655, 1589.35743671811,
            0.0, 0.0, 1.0)
        setMat(camMatrixWide, 3, 3, wideIntrinsic)

        var distCoeffNormal: Mat = Mat(5, 1, CV_64FC1)
//        setMat(distCoeffNormal, 5, 1, cameraDistortionFromCalibration(twoLens.normalParams.lensDistortion))

//        val normalDist = doubleArrayOf(-0.0516873222187034,	0.0622539345632542,	-0.0444891872754358, -0.000498353209195074,	3.08876718191467e-05)
        val normalDist = doubleArrayOf(-0.0386022410437852,	0.0356860602389576,	-0.0206818230155171, -0.000730111144107342,	-9.30878336147479e-06)
        setMat(distCoeffNormal, 5, 1, normalDist)

        var distCoeffWide: Mat = Mat(5, 1, CV_64FC1)
//        setMat(distCoeffWide, 5, 1, cameraDistortionFromCalibration(twoLens.wideParams.lensDistortion))
//        val wideDist = doubleArrayOf(0.0132168412951877, 0.0519522241383888, -0.0376508479748742, 0.00131540518070837, -4.32891999998737e-05)
        val wideDist = doubleArrayOf(0.00970718088539291,	0.0302182814054848,	-0.0196189363907549, 0.00137731709482448,	1.91652329368791e-05)
        setMat(distCoeffWide, 5, 1, wideDist)


        Logd("Cam Matrix K1 Check: "
                + camMatrixNormal[0, 0].get(0) + ", "
                + camMatrixNormal[0, 1].get(0) + ", "
                + camMatrixNormal[0, 2].get(0) + ", "
                + camMatrixNormal[1, 0].get(0) + ", "
                + camMatrixNormal[1, 1].get(0) + ", "
                + camMatrixNormal[1, 2].get(0) + ", "
                + camMatrixNormal[2, 0].get(0) + ", "
                + camMatrixNormal[2, 1].get(0) + ", "
                + camMatrixNormal[2, 2].get(0)
        )

        Logd("Cam Matrix K2 Check: "
                + camMatrixWide[0, 0].get(0) + ", "
                + camMatrixWide[0, 1].get(0) + ", "
                + camMatrixWide[0, 2].get(0) + ", "
                + camMatrixWide[1, 0].get(0) + ", "
                + camMatrixWide[1, 1].get(0) + ", "
                + camMatrixWide[1, 2].get(0) + ", "
                + camMatrixWide[2, 0].get(0) + ", "
                + camMatrixWide[2, 1].get(0) + ", "
                + camMatrixWide[2, 2].get(0)
        )

//        val poseRotationNormal: Mat = Mat(3, 3, CV_64FC1)
//        setMat(poseRotationNormal, 3, 3, rotationMatrixFromQuaternion(twoLens.normalParams.poseRotation))
//        val poseRotationWide: Mat = Mat(3, 3, CV_64FC1)
//        setMat(poseRotationWide, 3, 3, rotationMatrixFromQuaternion(twoLens.wideParams.poseRotation))
//
//        val poseTranslationNormal: Mat = Mat(3, 1, CV_64FC1)
//        setMat(poseTranslationNormal, 3, 1, floatArraytoDoubleArray(twoLens.normalParams.poseTranslation))
//        val poseTranslationWide: Mat = Mat(3, 1, CV_64FC1)
//        setMat(poseTranslationWide, 3, 1, floatArraytoDoubleArray(twoLens.wideParams.poseTranslation))

        val combinedR: Mat = Mat(3, 3, CV_64FC1)
        val combinedT: Mat = Mat(3, 1, CV_64FC1)

//        multiply(poseTranslationNormal, poseRotationNormal, combinedT, -1.0)
//        multiply(poseRotationNormal, poseTranslationNormal, combinedT)
//        multiply(poseRotationWide, poseTranslationWide, combinedT2, -1.0)
//        t[i] = -1.0 * np.dot(r[i], t[i])

        //To get T1 -> T2 we need to translate using -1 * innerproduct(R1 * T1) for each row. So:
        // T[0] = -1 * innerProduct(row0(R1) * T1)
        // T[1] = -1 * innerProduct(row1(R1) * T1)
        // T[2] = -1 * innerProduct(row2(R1) * T1)
//        combinedT.put(0,0, -1.0 * poseRotationNormal.colRange(0, 1).dot(poseTranslationNormal))
//        combinedT.put(1,0, -1.0 * poseRotationNormal.colRange(1, 2).dot(poseTranslationNormal))
//        combinedT.put(2,0, -1.0 * poseRotationNormal.colRange(2, 3).dot(poseTranslationNormal))
//        combinedT.put(2,0, -1.0 * poseRotationNormal.colRange(2, 3).dot(poseTranslationNormal))

        //To get our combined R, inverse poseRotationWide and multiply
//        Core.gemm(poseRotationWide.inv(DECOMP_SVD), poseRotationNormal, 1.0, Mat(), 0.0, combinedR)

        // NOTE todo For future if implementing for back cams
        //    if props['android.lens.facing']:
        //        print 'lens facing BACK'
        //        chart_distance *= -1  # API spec defines +z i pointing out from screen

/*
        Logd("Final Combined Rotation Matrix: "
                + combinedR[0, 0].get(0) + ", "
                + combinedR[0, 1].get(0) + ", "
                + combinedR[0, 2].get(0) + ", "
                + combinedR[1, 0].get(0) + ", "
                + combinedR[1, 1].get(0) + ", "
                + combinedR[1, 2].get(0) + ", "
                + combinedR[2, 0].get(0) + ", "
                + combinedR[2, 1].get(0) + ", "
                + combinedR[2, 2].get(0)
        )

        Logd("Final Combined Translation Matrix: "
                + combinedT[0, 0].get(0) + ", "
                + combinedT[1, 0].get(0) + ", "
                + combinedT[2, 0].get(0)
        )
*/

//        //       normal as img2, wide as img1
//        val rotationMatrix= doubleArrayOf(0.999997078544648, 0.00193674188656933, -0.00144635162886871,
//            -0.00194639504553176, 0.999975641513022, -0.00670282977162702,
//            0.00143333474675411, 0.00670562536125364, 0.999976489793644)
//        setMat(combinedR, 3, 3, rotationMatrix)
//        setMat(combinedT, 3, 3, rotationMatrix)
//
//        val tmpT = Mat(3, 1, CV_64FC1)
//        val translationMatrix= doubleArrayOf(0.0869105156370570, 10.3464503615669, -1.25290003267295)
//        setMat(tmpT, 3, 1, translationMatrix)



//       normal as img1, wide as img2
        val rotationMatrix= doubleArrayOf(0.999997520763306,	0.00193782468335406,	-0.00109695156599813,
            -0.00194423396541784,	0.999980868338488,	-0.00587221519451645,
            0.00108555125594218,	0.00587433336639810,	0.999982156733795)
        setMat(combinedR, 3, 3, rotationMatrix)

        val translationMatrix= doubleArrayOf(-0.0679691746773442, -10.3504444250521,	1.13315247620887)
        setMat(combinedT, 3, 1, translationMatrix)


        //Stereo rectify
        val R1: Mat = Mat(3, 3, CV_64FC1)
        val R2: Mat = Mat(3, 3, CV_64FC1)
        val P1: Mat = Mat(3, 4, CV_64FC1)
        val P2: Mat = Mat(3, 4, CV_64FC1)
        val Q: Mat = Mat(4, 4, CV_64FC1)

        val roi1: Rect = Rect()
        val roi2: Rect = Rect()

        stereoRectify( camMatrixNormal, distCoeffNormal,camMatrixWide, distCoeffWide,
            finalNormalMat.size(), combinedR, combinedT, R1, R2, P1, P2, Q,
            CALIB_ZERO_DISPARITY, 0.0, Size(), roi1, roi2)


        Logd("R1: " + R1[0,0].get(0) + ", " + R1[0,1].get(0) + ", " + R1[0,2].get(0) + ", " + R1[1,0].get(0) + ", " + R1[1,1].get(0) + ", " + R1[1,2].get(0) + ", " + R1[2,0].get(0) + ", " + R1[2,1].get(0) + ", " + R1[2,2].get(0))
        Logd("R2: " + R2[0,0].get(0) + ", " + R2[0,1].get(0) + ", " + R2[0,2].get(0) + ", " + R2[1,0].get(0) + ", " + R2[1,1].get(0) + ", " + R2[1,2].get(0) + ", " + R2[2,0].get(0) + ", " + R2[2,1].get(0) + ", " + R2[2,2].get(0))
        Logd("P1: " + P1[0,0].get(0) + ", " + P1[0,1].get(0) + ", " + P1[0,2].get(0) + ", " + P1[0,3].get(0) + ", " + P1[1,0].get(0) + ", " + P1[1,1].get(0) + ", " + P1[1,2].get(0) + ", " + P1[1,3].get(0) + ", "
                + P1[2,0].get(0) + ", " + P1[2,1].get(0) + ", " + P1[2,2].get(0) + ", " + P1[2,3].get(0))
        Logd("P2: " + P2[0,0].get(0) + ", " + P2[0,1].get(0) + ", " + P2[0,2].get(0) + ", " + P2[0,3].get(0) + ", " + P2[1,0].get(0) + ", " + P2[1,1].get(0) + ", " + P2[1,2].get(0) + ", " + P2[1,3].get(0) + ", "
                + P2[2,0].get(0) + ", " + P2[2,1].get(0) + ", " + P2[2,2].get(0) + ", " + P2[2,3].get(0))
        Logd("roi1: " + roi1.x+","+roi1.y+","+roi1.width+","+roi1.height)
        Logd("roi2: " + roi2.x+","+roi2.y+","+roi2.width+","+roi2.height)



        val mapNormal1: Mat = Mat()
        val mapNormal2: Mat = Mat()
        val mapWide1: Mat = Mat()
        val mapWide2: Mat = Mat()

        initUndistortRectifyMap(camMatrixNormal, distCoeffNormal, R1, P1, finalNormalMat.size(), CV_32F, mapNormal1, mapNormal2);
        initUndistortRectifyMap(camMatrixWide, distCoeffWide, R2, P2, finalWideMat.size(), CV_32F, mapWide1, mapWide2);

        val rectifiedNormalMat: Mat = Mat()
        val rectifiedWideMat: Mat = Mat()

        val rectifiedNormalMatColor: Mat = Mat()
        val rectifiedWideMatColor: Mat = Mat()

        remap(finalNormalMat, rectifiedNormalMat, mapNormal1, mapNormal2, INTER_LINEAR);
        remap(finalWideMat, rectifiedWideMat, mapWide1, mapWide2, INTER_LINEAR);

        remap(normalMat, rectifiedNormalMatColor, mapNormal1, mapNormal2, INTER_LINEAR);
        remap(wideMat, rectifiedWideMatColor, mapWide1, mapWide2, INTER_LINEAR);

        val daiMat = Mat()
        absdiff(rectifiedNormalMat,rectifiedWideMat,daiMat)

        Logd( "Now saving rectified photos to disk.")
        val rectifiedNormalBitmap: Bitmap = Bitmap.createBitmap(rectifiedNormalMat.cols(), rectifiedNormalMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rectifiedNormalMat, rectifiedNormalBitmap)
        val rectifiedWideBitmap: Bitmap = Bitmap.createBitmap(rectifiedWideMat.cols(), rectifiedWideMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rectifiedWideMat, rectifiedWideBitmap)

        val rectifiedNormalBitmapColor: Bitmap = Bitmap.createBitmap(rectifiedNormalMatColor.cols(), rectifiedNormalMatColor.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rectifiedNormalMatColor, rectifiedNormalBitmapColor)
        val rectifiedWideBitmapColor: Bitmap = Bitmap.createBitmap(rectifiedWideMatColor.cols(), rectifiedWideMatColor.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rectifiedWideMatColor, rectifiedWideBitmapColor)

        val rectifiedNormalBitmapColorFinal = horizontalFlip(rotateBitmap(rectifiedNormalBitmapColor, 180f))
        val rectifiedWideBitmapColorFinal = horizontalFlip(rotateBitmap(rectifiedWideBitmapColor, 180f))

        val daiBitmap: Bitmap = Bitmap.createBitmap(daiMat.cols(), daiMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(daiMat, daiBitmap)
        val daiBitmapFinal = horizontalFlip(rotateBitmap(daiBitmap, 180f))

        val rectifiedNormalBitmapColorResize = Bitmap.createScaledBitmap(rectifiedNormalBitmapColorFinal, 280, 280, false)
        val rectifiedWideBitmapColorResize = Bitmap.createScaledBitmap(rectifiedWideBitmapColorFinal, 280, 280, false)
        val daiBitmapResize = Bitmap.createScaledBitmap(daiBitmapFinal, 280, 280, false)

        if (PrefHelper.getSaveIntermediate(activity)) {
            WriteFile(activity, rectifiedNormalBitmapColorResize, "RectifiedNormalShot", false, dirTimeStamp)
            WriteFile(activity, rectifiedWideBitmapColorResize,"RectifiedWideShot", false, dirTimeStamp)
            WriteFile(activity, daiBitmapResize,"DifferentialAngleImage", false, dirTimeStamp)
        }


        finalNormalMat = rectifiedNormalMat
        finalWideMat = rectifiedWideMat
    }

    val sgbmWinSize = PrefHelper.getWindowSize(activity)
    val sgbmBlockSize = sgbmWinSize
    val sgbmMinDisparity = 0
    val sgbmNumDisparities = PrefHelper.getNumDisparities(activity)
    val sgbmP1 = PrefHelper.getP1(activity)
    val sgbmP2 = PrefHelper.getP1(activity)
    val sgbmDispMaxDiff = -1
    val sgbmPreFilterCap = PrefHelper.getPrefilter(activity)
    val sgbmUniquenessRatio = 0
    val sgbmSpeckleSize = PrefHelper.getSpecklesize(activity)
    val sgbmSpeckleRange = PrefHelper.getSpecklerange(activity)
    val sgbmMode = StereoSGBM.MODE_HH4
//    val sgbmMode = StereoSGBM.MODE_SGBM


    val resizedNormalMat: Mat = Mat()
    val resizedWideMat: Mat = Mat()

    val depthMapScaleFactor = 0.5f

    //Scale down so we at least have a chance of not burning through the heap
    resize(finalNormalMat, resizedNormalMat, Size((finalNormalMat.width() * depthMapScaleFactor).toDouble(), (finalNormalMat.height() * depthMapScaleFactor).toDouble()))
    resize(finalWideMat, resizedWideMat, Size((finalWideMat.width()  * depthMapScaleFactor).toDouble(), (finalWideMat.height()  * depthMapScaleFactor).toDouble()))

    val rotatedNormalMat: Mat = Mat()
    val rotatedWideMat: Mat = Mat()

    rotate(resizedNormalMat, rotatedNormalMat, Core.ROTATE_90_CLOCKWISE)
    rotate(resizedWideMat, rotatedWideMat, Core.ROTATE_90_CLOCKWISE)

    val disparityMat: Mat = Mat(rotatedNormalMat.rows(), rotatedNormalMat.cols(), CV_8UC1)
    val disparityMat2: Mat = Mat(rotatedNormalMat.rows(), rotatedNormalMat.cols(), CV_8UC1)

    val stereoBM: StereoSGBM = StereoSGBM.create(sgbmMinDisparity, sgbmNumDisparities, sgbmBlockSize,
        sgbmP1, sgbmP2, sgbmDispMaxDiff, sgbmPreFilterCap, sgbmUniquenessRatio, sgbmSpeckleSize,
        sgbmSpeckleRange, sgbmMode)
//    val stereoBM2: StereoSGBM = StereoSGBM.create(sgbmMinDisparity, sgbmNumDisparities, sgbmBlockSize,
//            sgbmP1, sgbmP2, sgbmDispMaxDiff, sgbmPreFilterCap, sgbmUniquenessRatio, sgbmSpeckleSize,
//            sgbmSpeckleRange, sgbmMode)

    val stereoMatcher: StereoMatcher = createRightMatcher(stereoBM)

//    val stereoBM: StereoBM = StereoBM.create()
//    val stereoBM2: StereoBM = StereoBM.create()

    if (PrefHelper.getInvertFilter(activity)) {
        stereoBM.compute(rotatedNormalMat, rotatedWideMat, disparityMat2)
        stereoMatcher.compute(rotatedWideMat, rotatedNormalMat, disparityMat)
    } else {

        stereoBM.compute(rotatedNormalMat, rotatedWideMat, disparityMat)
        stereoMatcher.compute(rotatedWideMat, rotatedNormalMat, disparityMat2)
    }

    val normalizedDisparityMat1: Mat = Mat()
    val normalizedDisparityMat2: Mat = Mat()

    normalize(disparityMat, normalizedDisparityMat1, 0.0, 255.0, NORM_MINMAX, CV_8U)
    normalize(disparityMat2, normalizedDisparityMat2, 0.0, 255.0, NORM_MINMAX, CV_8U)

    val disparityMatConverted1: Mat = Mat()
    val disparityMatConverted2: Mat = Mat()

    val disparityBitmap: Bitmap = Bitmap.createBitmap(disparityMat.cols(), disparityMat.rows(), Bitmap.Config.ARGB_8888)
    val disparityBitmap2: Bitmap = Bitmap.createBitmap(disparityMat2.cols(), disparityMat2.rows(), Bitmap.Config.ARGB_8888)

    normalizedDisparityMat1.convertTo(disparityMatConverted1, CV_8UC1, 1.0 );
    normalizedDisparityMat2.convertTo(disparityMatConverted2, CV_8UC1, 1.0);
    Utils.matToBitmap(disparityMatConverted1, disparityBitmap)
    Utils.matToBitmap(disparityMatConverted2, disparityBitmap2)
    val disparityBitmap2Final = horizontalFlip(rotateBitmap(disparityBitmap2, 90f))
    val disparityBitmap2Resize = Bitmap.createScaledBitmap(disparityBitmap2Final, 280, 280, false)

    if (PrefHelper.getIntermediate(activity)) {
        activity.runOnUiThread {
//            activity.imageIntermediate1.setImageBitmap(rotateBitmap(disparityBitmap,getRequiredBitmapRotation(activity, true)))
            activity.imageIntermediate4.setImageBitmap(disparityBitmap2Final)
        }
    }
    if (PrefHelper.getSaveIntermediate(activity)) {
//        WriteFile(activity, rotateBitmap(disparityBitmap,180f), "DisparityMap",false, dirTimeStamp)
        WriteFile(activity, disparityBitmap2Resize, "DisparityMap2",false, dirTimeStamp)
    }


    val disparityMatFiltered: Mat = Mat(rotatedNormalMat.rows(), rotatedNormalMat.cols(), CV_8UC1)
    val disparityWLSFilter = createDisparityWLSFilter(stereoBM)
    disparityWLSFilter.lambda = PrefHelper.getLambda(activity)
    disparityWLSFilter.sigmaColor = PrefHelper.getSigma(activity)
    disparityWLSFilter.filter(disparityMat, rotatedNormalMat, disparityMatFiltered, disparityMat2, Rect(0, 0, disparityMatConverted1.cols(), disparityMatConverted1.rows()), rotatedWideMat)

    var disparityMapFilteredNormalized: Mat = Mat(disparityMatFiltered.rows(), disparityMatFiltered.cols(), CV_8UC1)
    disparityMapFilteredNormalized = disparityMatFiltered
    normalize(disparityMatFiltered, disparityMapFilteredNormalized, 0.0, 255.0, NORM_MINMAX, CV_8U)

    val disparityMatFilteredConverted: Mat = Mat(disparityMapFilteredNormalized.rows(), disparityMapFilteredNormalized.cols(), CV_8UC1)
    disparityMapFilteredNormalized.convertTo(disparityMatFilteredConverted, CV_8UC1)

    val disparityBitmapFiltered: Bitmap = Bitmap.createBitmap(disparityMatFilteredConverted.cols(), disparityMatFilteredConverted.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(disparityMatFilteredConverted, disparityBitmapFiltered)
    val disparityBitmapFilteredFinal = horizontalFlip(rotateBitmap(disparityBitmapFiltered, 90f))
    val disparityBitmapFilteredResize = Bitmap.createScaledBitmap(disparityBitmapFilteredFinal, 280, 280, false)

    if (PrefHelper.getSaveIntermediate(activity)) {
        WriteFile(activity, disparityBitmapFilteredResize, "DisparityMapFilteredNormalized", false, dirTimeStamp)
    }
//    if (PrefHelper.getIntermediate(activity)) {
//        activity.runOnUiThread {
//            activity.imageIntermediate3.setImageBitmap(disparityBitmapFilteredFinal)
//        }
//    }

//
//    val normalizedMaskBitmap = Bitmap.createBitmap(disparityMatFilteredConverted.cols(), disparityMatFilteredConverted.rows(), Bitmap.Config.ARGB_8888)
//    Utils.matToBitmap(disparityMatFilteredConverted, normalizedMaskBitmap)
//    var hardNormalizedMaskBitmap = hardNormalizeDepthMap(activity, normalizedMaskBitmap)
//
//    if (twoLens.normalParams.hasFace) {
//        Logd("DoBokeh: Masking in face...")
//        //Let's protect the face of the foreground using face detect
//
//        //Let's take a transparent bitmap same as normal
//        //Then paste rect
//        //Then rotate and flip.
//        //Then past on other bitmap...
//        val clearBitmap = Bitmap.createBitmap(tempNormalBitmap.width, tempNormalBitmap.height, Bitmap.Config.ARGB_8888)
//        val clearCanvas = Canvas(clearBitmap)
//        val clearPaint = Paint()
//        clearPaint.setColor(Color.TRANSPARENT)
//        clearCanvas.drawRect(0f, 0f, clearBitmap.width.toFloat(), clearBitmap.height.toFloat(), clearPaint)
//
//        val faceRect = android.graphics.Rect(twoLens.normalParams.faceBounds)
//        val faceMask = Bitmap.createBitmap(faceRect.width(), faceRect.height(), Bitmap.Config.ARGB_8888)
//        val faceCanvas = Canvas(faceMask)
//        val facePaint = Paint()
//        facePaint.setColor(Color.WHITE)
//        faceCanvas.drawRect(0f, 0f, faceRect.width().toFloat(), faceRect.height().toFloat(), facePaint)
//
//        val protectFaceMask = pasteBitmap(activity, clearBitmap, faceMask, faceRect)
//        val protectFaceMaskScaled = scaleBitmap(protectFaceMask, depthMapScaleFactor)
//        val protectFaceMaskRotated = rotateBitmap(protectFaceMaskScaled, 90f)
//        val protectFaceMaskFlipped = horizontalFlip(protectFaceMaskRotated)
//
//        hardNormalizedMaskBitmap = pasteBitmap(activity, hardNormalizedMaskBitmap, protectFaceMaskFlipped)
//    }
//
//    if (PrefHelper.getIntermediate(activity)) {
//        //Lay it on a black background
//        val black = Bitmap.createBitmap(hardNormalizedMaskBitmap.width, hardNormalizedMaskBitmap.height, Bitmap.Config.ARGB_8888)
//        val blackCanvas = Canvas(black)
//        val paint = Paint()
//        paint.setColor(Color.BLACK)
//        blackCanvas.drawRect(0f, 0f, hardNormalizedMaskBitmap.width.toFloat(), hardNormalizedMaskBitmap.height.toFloat(), paint)
//        tempBitmap = rotateBitmap(hardNormalizedMaskBitmap,getRequiredBitmapRotation(activity, true))
//        activity.runOnUiThread {
//            activity.imageIntermediate4.setImageBitmap(pasteBitmap(activity, black, tempBitmap))
//            tempBitmap.recycle()
//        }
//
//    }
//    if (PrefHelper.getSaveIntermediate(activity)) {
//        WriteFile(activity, rotateBitmap(hardNormalizedMaskBitmap,180f), "HardMask")
//    }

//    val smallNormalBitmap = scaleBitmap(tempNormalBitmap, depthMapScaleFactor)
//    var rotatedSmallNormalBitmap = rotateAndFlipBitmap(smallNormalBitmap, 90f)
//    val nicelyMaskedColour = applyMask(activity, rotatedSmallNormalBitmap, hardNormalizedMaskBitmap)

//    if (PrefHelper.getSaveIntermediate(activity)) {
//        WriteFile(activity, nicelyMaskedColour, "NicelyMaskedColour", 100, true)
//    }

//    if (PrefHelper.getIntermediate(activity)) {
//        activity.runOnUiThread {
//            activity.imageIntermediate2.setImageBitmap(rotateBitmap(nicelyMaskedColour,getRequiredBitmapRotation(activity, true)))
//        }
//    }
//
//    var backgroundBitmap = Bitmap.createBitmap(rotatedSmallNormalBitmap)
//
//    if (PrefHelper.getSepia(activity))
//        backgroundBitmap = sepiaFilter(activity, rotatedSmallNormalBitmap)
//    else
//        backgroundBitmap = monoFilter(rotatedSmallNormalBitmap)
//
//    val blurredBackgroundBitmap = CVBlur(backgroundBitmap)

//    if (PrefHelper.getSaveIntermediate(activity)) {
//        WriteFile(activity, blurredBackgroundBitmap, "Background")
//    }

//    val finalImage = pasteBitmap(activity, blurredBackgroundBitmap, nicelyMaskedColour, android.graphics.Rect(0, 0, blurredBackgroundBitmap.width, blurredBackgroundBitmap.height))
//
//    if (PrefHelper.getSaveIntermediate(activity)) {
//        WriteFile(activity, finalImage, "FinalImage")
//    }
    activity.runOnUiThread {
        activity.captureFinished()
    }
    return disparityBitmapFilteredFinal
*/
    return tempWideBitmap*/
    //return tempWideBitmap
}

fun floatArraytoDoubleArray(fArray: FloatArray) : DoubleArray {
    val dArray: DoubleArray = DoubleArray(fArray.size)

    var output = ""
    for ((index, float) in fArray.withIndex()) {
        dArray.set(index, float.toDouble())
        output += "" + float.toDouble() + ", "
    }

    return dArray
}

//From https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics#LENS_POSE_ROTATION
//For (x,y,x,w)
//R = [ 1 - 2y^2 - 2z^2,       2xy - 2zw,       2xz + 2yw,
//2xy + 2zw, 1 - 2x^2 - 2z^2,       2yz - 2xw,
//2xz - 2yw,       2yz + 2xw, 1 - 2x^2 - 2y^2 ]
fun rotationMatrixFromQuaternion(quatFloat: FloatArray) : DoubleArray {
    val quat: DoubleArray = floatArraytoDoubleArray(quatFloat)
    val rotationMatrix: DoubleArray = DoubleArray(9)

    val x: Int = 0
    val y: Int = 1
    val z: Int = 2
    val w: Int = 3

    //Row 1
    rotationMatrix[0] = 1 - (2 * quat[y] * quat[y]) - (2 * quat[z] * quat[z])
    rotationMatrix[1] = (2 * quat[x] * quat[y]) - (2 * quat[z] * quat[w])
    rotationMatrix[2] = (2 * quat[x] * quat[z]) + (2 * quat[y] * quat[w])

    //Row 2
    rotationMatrix[3] = (2 * quat[x] * quat[y]) + (2 * quat[z] * quat[w])
    rotationMatrix[4] = 1 - (2 * quat[x] * quat[x]) - (2 * quat[z] * quat[z])
    rotationMatrix[5] = (2 * quat[y] * quat[z]) - (2 * quat[x] * quat[w])

    //Row 3
    rotationMatrix[6] = (2 * quat[x] * quat[z]) - (2 * quat[y] * quat[w])
    rotationMatrix[7] = (2 * quat[y] * quat[z]) + (2 * quat[x] * quat[w])
    rotationMatrix[8] = 1 - (2 * quat[x] * quat[x]) - (2 * quat[y] * quat[y])

    //Print
    Logd("Final Rotation Matrix: "
            + rotationMatrix[0] + ", "
            + rotationMatrix[1] + ", "
            + rotationMatrix[2] + ", "
            + rotationMatrix[3] + ", "
            + rotationMatrix[4] + ", "
            + rotationMatrix[5] + ", "
            + rotationMatrix[6] + ", "
            + rotationMatrix[7] + ", "
            + rotationMatrix[8]
    )


    return rotationMatrix
}

//https://developer.android.com/reference/android/hardware/camera2/CameraCharacteristics#LENS_INTRINSIC_CALIBRATION
//[f_x, f_y, c_x, c_y, s]
//K = [ f_x,   s, c_x,
//0, f_y, c_y,
//0    0,   1 ]
fun cameraMatrixFromCalibration(calibrationFloat: FloatArray) : DoubleArray {
    val cal: DoubleArray = floatArraytoDoubleArray(calibrationFloat)
    val cameraMatrix: DoubleArray = DoubleArray(9)

    val f_x: Int = 0
    val f_y: Int = 1
    val c_x: Int = 2
    val c_y: Int = 3
    val s: Int = 4

    //Row 1
    cameraMatrix[0] = cal[f_x]
    cameraMatrix[1] = cal[s]
    cameraMatrix[2] = cal[c_x]

    //Row 2
    cameraMatrix[3] = 0.0
    cameraMatrix[4] = cal[f_y]
    cameraMatrix[5] = cal[c_y]

    //Row 3
    cameraMatrix[6] = 0.0
    cameraMatrix[7] = 0.0
    cameraMatrix[8] = 1.0

    //Print
    Logd("Final Cam Matrix: "
            + cameraMatrix[0] + ", "
            + cameraMatrix[1] + ", "
            + cameraMatrix[2] + ", "
            + cameraMatrix[3] + ", "
            + cameraMatrix[4] + ", "
            + cameraMatrix[5] + ", "
            + cameraMatrix[6] + ", "
            + cameraMatrix[7] + ", "
            + cameraMatrix[8]
    )

//    printArray(cameraMatrix)

    return cameraMatrix
}

//The android intrinsic values are swizzled from what OpenCV needs. Output indexs should be: 0,1,3,4,2
fun cameraDistortionFromCalibration(calibrationFloat: FloatArray) : DoubleArray {
    val cal: DoubleArray = floatArraytoDoubleArray(calibrationFloat)
    val cameraDistortion: DoubleArray = DoubleArray(5)

    cameraDistortion[0] = cal[0]
    cameraDistortion[1] = cal[1]
    cameraDistortion[2] = cal[3]
    cameraDistortion[3] = cal[4]
    cameraDistortion[4] = cal[2]

    //Print
    Logd("Final Distortion Matrix: "
            + cameraDistortion[0] + ", "
            + cameraDistortion[1] + ", "
            + cameraDistortion[2] + ", "
            + cameraDistortion[3] + ", "
            + cameraDistortion[4]
    )


    return cameraDistortion
}

fun setMat(mat: Mat, rows: Int, cols: Int, vals: DoubleArray) {
    mat.put(0,0, *vals)
/*
    for (row in 0..rows-1) {
        for (col in 0..cols-1) {
            //For some reason, Mat allocation fails for the 5th element sometimes...
            var temp: DoubleArray? = mat[row, col]
            if (null == temp) {
                Logd("Weird, at " + row + "and " + col + " and array is null.")
                continue
            }
            Logd("Checking mat at r: " + row + " and col: " + col + " : " + mat.get(row, col)[0])
        }
    }
*/
}

fun printArray(doubleArray: DoubleArray) {
    Logd("Checking double array Start")
    for (double in doubleArray)
        Logd("element: " + double)
    Logd("Checking double array End")
}