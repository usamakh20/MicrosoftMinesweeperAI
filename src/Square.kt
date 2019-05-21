import java.awt.Point

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class Square internal constructor() {
    var image: Mat? = null
        internal set
    lateinit var size: Point
    internal var colGap: Double = 0.toDouble()
    internal var rowGap: Double = 0.toDouble()
    private val lowerRange = arrayOfNulls<Scalar>(8)
    private val upperRange = arrayOfNulls<Scalar>(8)
    private val flagL = Scalar(20.0, 170.0, 230.0)
    private val flagH = Scalar(30.0, 200.0, 255.0)

    init {
        lowerRange[Game.Num1.toInt()] = Scalar(90.0, 50.0, 50.0)
        lowerRange[Game.Num2.toInt()] = Scalar(35.0, 150.0, 140.0)
        lowerRange[Game.Num3.toInt()] = Scalar(140.0, 190.0, 190.0)
        lowerRange[Game.Num4.toInt()] = Scalar(110.0, 50.0, 50.0)
        lowerRange[Game.Num5.toInt()] = Scalar(0.0, 110.0, 130.0)
        lowerRange[Game.Num6.toInt()] = Scalar(55.0, 50.0, 120.0)
        lowerRange[Game.Num7.toInt()] = Scalar(140.0, 50.0, 50.0)
        lowerRange[Game.Num8.toInt()] = Scalar(5.0, 50.0, 50.0)

        upperRange[Game.Num1.toInt()] = Scalar(95.0, 225.0, 225.0)
        upperRange[Game.Num2.toInt()] = Scalar(45.0, 210.0, 180.0)
        upperRange[Game.Num3.toInt()] = Scalar(170.0, 255.0, 255.0)
        upperRange[Game.Num4.toInt()] = Scalar(130.0, 230.0, 230.0)
        upperRange[Game.Num5.toInt()] = Scalar(5.0, 210.0, 210.0)
        upperRange[Game.Num6.toInt()] = Scalar(70.0, 255.0, 160.0)
        upperRange[Game.Num7.toInt()] = Scalar(160.0, 255.0, 255.0)
        upperRange[Game.Num8.toInt()] = Scalar(15.0, 255.0, 255.0)

    }


    private fun checkBlack(m: Mat): Boolean {
        for (i in 0 until m.cols()) {
            for (j in 0 until m.rows()) {
                if (m.get(j, i)[0].toInt() != 0)
                    return true
            }
        }
        return false
    }

    internal fun identify(): Byte {
        val hsv = Mat()
        val threshold = Mat()
        Imgproc.cvtColor(image!!, hsv, Imgproc.COLOR_BGR2HSV)
        for (i in Game.Num1..Game.Num8) {
            Core.inRange(hsv, lowerRange[i], upperRange[i], threshold)
            if (checkBlack(threshold)) {
                if (i == Game.Num1.toInt()) {
                    Imgproc.cvtColor(image!!, hsv, Imgproc.COLOR_BGR2HSV)
                    Core.inRange(hsv, Scalar(0.0, 0.0, 250.0), Scalar(3.0, 10.0, 255.0), threshold)
                    //Imgcodecs.imwrite("thr.png", threshold);
                    if (checkBlack(threshold))
                        return Game.Num1
                }
                if (i + 1 == 5) {
                    Imgproc.cvtColor(image!!, hsv, Imgproc.COLOR_BGR2HSV)
                    Core.inRange(hsv, flagL, flagH, threshold)
                    if (checkBlack(threshold))
                        return Game.Flag
                }
                return i.toByte()
            }
        }
        Imgproc.cvtColor(image!!, hsv, Imgproc.COLOR_BGR2HSV)
        Core.inRange(hsv, Scalar(100.0, 150.0, 230.0), Scalar(110.0, 180.0, 255.0), threshold)
        return if (checkBlack(threshold))
            Game.UnOpen
        else
            Game.Open // open // empty
    }
}
