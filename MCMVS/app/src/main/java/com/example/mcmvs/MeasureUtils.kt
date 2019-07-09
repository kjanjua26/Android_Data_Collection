package com.example.mcmvs

import android.util.Size
import com.example.mcmvs.MainActivity.Companion.Logd
import java.util.*

/**
 * Compares two `Size`s based on their areas.
 */
internal class CompareSizesByArea : Comparator<Size> {
    override fun compare(lhs: Size, rhs: Size): Int {
        // We cast here to ensure the multiplications won't overflow
        return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
    }
}

/**
 * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
 * width and height are at least as large as the respective requested values.
 * @param choices The list of sizes that the camera supports for the intended output class
 * @param width The minimum desired width
 * @param height The minimum desired height
 * @return The optimal `Size`, or an arbitrary one if none were big enough
 */
internal fun chooseBigEnoughSize(choices: Array<Size>, width: Int, height: Int): Size {
    // Collect the supported resolutions that are at least as big as the preview Surface
    val bigEnough = ArrayList<Size>()
    for (option in choices) {
        if (option.width >= width && option.height >= height) {
            bigEnough.add(option)
        }
    }
    // Pick the smallest of those, assuming we found any
    if (bigEnough.size > 0) {
        return Collections.min(bigEnough, CompareSizesByArea())
    } else {
        Logd("Couldn't find any suitable preview size")
        return choices[0]
    }
}
