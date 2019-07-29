package com.example.mcmvs

import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.AsyncTask
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.widget.ImageView
import com.example.mcmvs.CameraStateCallback
import com.example.mcmvs.FocusCaptureSessionCallback
import com.example.mcmvs.FocusCaptureSessionCallback.Companion.STATE_UNINITIALIZED
import com.example.mcmvs.MainActivity.Companion.NO_APERTURE
import java.util.*
import java.util.concurrent.Executor
import kotlin.collections.HashMap

class CameraParams {
    internal var id: String? = null
    internal var state: Int = STATE_UNINITIALIZED
    internal var isFront: Boolean = false
    internal var hasFlash: Boolean = false
    internal var hasMulti: Boolean = false
    internal var hasManualControl: Boolean = false
    internal var isOpen: Boolean = false
    internal var canSync: Boolean = false
    internal var characteristics: CameraCharacteristics? = null

    internal var backgroundThread: HandlerThread? = null
    internal var backgroundHandler: Handler? = null
    internal var backgroundExecutor: Executor = AsyncTask.THREAD_POOL_EXECUTOR

    //internal var shutter: ImageView? = null
    //internal var capturedPhoto: ImageView? = null
    internal var imageReader: ImageReader? = null
    internal var previewTextureView: AutoFitTextureView? = null

    internal var physicalCameras: Set<String> = HashSet<String>()
    internal var focalLengths: FloatArray = FloatArray(0)
    internal var apertures: FloatArray = FloatArray(0)
    //internal var distortionModes: IntArray = IntArray(0)
    internal var smallestFocalLength: Float = MainActivity.INVALID_FOCAL_LENGTH
    internal var minDeltaFromNormal: Float = MainActivity.INVALID_FOCAL_LENGTH
    internal var minFocusDistance: Float = MainActivity.FIXED_FOCUS_DISTANCE
    internal var largestAperture: Float = NO_APERTURE
    internal var effects: IntArray = IntArray(0)
    internal var hasSepia: Boolean = false
    internal var hasMono: Boolean = false
    internal var hasAF: Boolean = false

    internal var isLogicalBackedByPhysical: Boolean = false

    //internal var bestFaceDetectionMode: Int = 0

    //Bokeh calculations
    internal var lensDistortion: FloatArray = FloatArray(0)
    internal var intrinsicCalibration: FloatArray = FloatArray(0)
    internal var poseRotation: FloatArray = FloatArray(0)
    internal var poseTranslation: FloatArray = FloatArray(0)
    internal var hasDepth: Boolean = false

    internal var previewBuilder: CaptureRequest.Builder? = null
    internal var captureBuilder: CaptureRequest.Builder? = null

    internal var captureSession: CameraCaptureSession? = null
    internal var cameraCallback: CameraStateCallback? = null
    //internal var textureListener: TextureListener? = null
    internal var captureCallback: FocusCaptureSessionCallback? = null
    internal var imageAvailableListener: ImageAvailableListener? = null

    //Camera2 min/max sizeaa
    internal var minSize: Size = Size(0, 0)
    internal var maxSize: Size = Size(0, 0)

    internal var device: CameraDevice? = null

    internal var hasFace: Boolean = false
    internal var faceBounds: Rect = Rect(0,0,0,0)
    internal var expandedFaceBounds: Rect = Rect(0,0,0,0)
    //internal var grabCutBounds: Rect = Rect(0,0,0,0)

    // TODO: Make two hash maps <String, Bitmap>.

}