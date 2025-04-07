package com.vehiclesafetyguardian.lane

import android.graphics.Bitmap
import android.graphics.Color
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class LaneDetector {
    fun detectLanes(inputBitmap: Bitmap): Bitmap {
        // Convert Bitmap to Mat
        val src = Mat(inputBitmap.height, inputBitmap.width, CvType.CV_8UC4)
        val dst = Mat()
        val gray = Mat()
        val edges = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(inputBitmap, src)

        // Convert to grayscale
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)

        // Apply Gaussian blur
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        // Canny edge detection
        Imgproc.Canny(gray, edges, 50.0, 150.0)

        // Hough transform for lane detection
        val lines = Mat()
        Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI / 180, 100, 100.0, 10.0)

        // Draw detected lines on the original image
        for (i in 0 until lines.rows()) {
            val line = lines.row(i)
            val x1 = line.get(0, 0)[0].toInt()
            val y1 = line.get(0, 1)[0].toInt()
            val x2 = line.get(0, 2)[0].toInt()
            val y2 = line.get(0, 3)[0].toInt()
            Imgproc.line(src, org.opencv.core.Point(x1.toDouble(), y1.toDouble()), org.opencv.core.Point(x2.toDouble(), y2.toDouble()), Scalar(0.0, 255.0, 0.0), 2)
        }

        // Convert Mat back to Bitmap
        val outputBitmap = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(src, outputBitmap)

        // Release resources
        src.release()
        gray.release()
        edges.release()
        lines.release()

        return outputBitmap
    }
}