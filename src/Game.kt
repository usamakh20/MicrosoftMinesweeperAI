
import java.awt.event.InputEvent
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.AWTException
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.util.*

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.core.Scalar
import org.opencv.core.Range
import org.opencv.core.Core
import kotlin.collections.ArrayList

class Game @Throws(AWTException::class)
constructor() {
    private var fieldSize: Rectangle? = null // The Position And Size of the Field
    private var isDailyChallenge: Boolean = false
    private var mineField: Mat? = null
    internal var columns: Int = 0
        private set // The Dimensions of the Field
    internal var rows: Int = 0
        private set

    internal var gamebot: MyRobot
    internal var play: Boolean = false

    // We only want lists of Num1 to Flag
    private val listsOfCells: Array<LinkedList<Point>>

    private var current: Square? = null

    private var gridStates: Array<ByteArray>? = null

    init {
        isDailyChallenge = false
        gamebot = MyRobot()
        random = Random()
        rows = 0
        columns = rows
        listsOfCells = Array(Flag.toInt()){ LinkedList<Point>()}
    }


    internal fun isInBounds(x: Int, y: Int): Boolean {
        return x in 0 until columns && y >= 0 && y < rows
    }

    internal fun isNewGame(): Boolean {
        return listsOfCells[Num1].isEmpty()
    }

    internal fun printDifference() {
        val m1 = getScrnShot(fieldSize)
        val p: Point
        val rand = random.nextInt(listsOfCells[UnOpen].size)
        p = listsOfCells[UnOpen][rand]
        // Right Click at x,y
        clickCell(p.x, p.y, InputEvent.BUTTON3_MASK)
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        val m2 = getScrnShot(fieldSize)

        // Two right clicks returns that cell to normal
        // Right Click at x,y
        clickCell(p.x, p.y, InputEvent.BUTTON3_MASK)
        clickCell(p.x, p.y, InputEvent.BUTTON3_MASK)

        val m3 = Mat()
        Core.absdiff(m1, m2, m3)

        val start = org.opencv.core.Point(m3.cols().toDouble(), m3.rows().toDouble())
        val end = org.opencv.core.Point(0.0, 0.0)
        for (y in 0 until m3.rows()) {
            for (x in 0 until m3.cols()) {
                for (d in m3.get(y, x)) {
                    if (d != 0.0) {
                        start.x = Math.min(x.toDouble(), start.x)
                        start.y = Math.min(y.toDouble(), start.y)
                        end.x = Math.max(x.toDouble(), end.x)
                        end.y = Math.max(y.toDouble(), end.y)
                    }
                }
            }
        }
        val diff = Rect(start, end)
        println("Difference between actual and detected width is: " + (diff.width - current!!.size.y))
        println("Difference between actual and detected height is: " + (diff.height - current!!.size.x))
    }

    internal fun initialize() {
        val thresh = 100
        val fullScreen = Rectangle(
            Toolkit.getDefaultToolkit()
                .screenSize
        )
        val scrnShot = getScrnShot(fullScreen)
        val squares = ArrayList<MatOfPoint>()
        val contours = ArrayList<MatOfPoint>()
        val gray = Mat()
        val cannyOutput = Mat()
        var startx = scrnShot.cols()
        var endx = 0
        var starty = scrnShot.rows()
        var endy = 0
        var sqrWidth = 0
        var sqrHeight = 0
        val heights = ArrayList<Int>()
        val widths = ArrayList<Int>()

        // Imgcodecs.imwrite("Scrnshot.png", ScrnShot);
        // System.out.println(contours);
        // Convert image to gray and blur it
        Imgproc.cvtColor(scrnShot, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.blur(gray, gray, Size(3.0, 3.0))

        // applying canny
        Imgproc.Canny(gray, cannyOutput, thresh.toDouble(), (thresh * 3).toDouble(), 3, false)
        // Imgcodecs.imwrite("p2.png", canny_output);

        // find contours and store them all as a list
        Imgproc.findContours(
            cannyOutput, contours, Mat(),
            Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE
        )


        // test each contour
        for (i in contours.indices) {

            // approximate contour with accuracy proportional
            // to the contour perimeter
            val contour2f = MatOfPoint2f(
                *contours[i]
                    .toArray()
            )
            val approx2f = MatOfPoint2f()
            Imgproc.approxPolyDP(
                contour2f, approx2f,
                Imgproc.arcLength(contour2f, true) * 0.02, true
            )
            // square contours should have 4 vertices after approximation
            // relatively large area (to filter out noisy contours)
            // and be convex.
            // Note: absolute value of an area is used because
            // area may be positive or negative - in accordance with the
            // contour orientation

            if (approx2f.rows() == 4
                && Math.abs(Imgproc.contourArea(approx2f)) > 380
                && Imgproc.isContourConvex(
                    MatOfPoint(
                        *approx2f
                            .toArray()
                    )
                )
            ) {
                // For a square length and width are approximately equal
                // Calculating ength and width of shape
                val width = Math.abs(approx2f.get(2, 0)[0] - approx2f.get(0, 0)[0]).toInt()
                val height = Math.abs(approx2f.get(2, 0)[1] - approx2f.get(0, 0)[1]).toInt()
                if (Math.abs(height - width) > 10) {
                    isDailyChallenge = true
                    continue
                }
                var maxCosine = 0.0
                for (j in 2..4) {
                    // find the maximum cosine of the angle between joint edges
                    val cosine = Math.abs(
                        angle(
                            approx2f.get(j % 4, 0),
                            approx2f.get(j - 2, 0), approx2f.get(j - 1, 0)
                        )
                    )
                    maxCosine = Math.max(maxCosine, cosine)
                }
                // if cosines of all angles are small
                // (all angles are ~90 degree) then write quandrange
                // vertices to resultant sequence
                if (maxCosine < 0.3) {
                    squares.add(MatOfPoint(*approx2f.toArray()))
                    // Calculating the  size of  the  grid
                    startx = Math.min(approx2f.get(0, 0)[0], startx.toDouble()).toInt()
                    endx = Math.max(approx2f.get(3, 0)[0], endx.toDouble()).toInt()
                    starty = Math.min(approx2f.get(0, 0)[1], starty.toDouble()).toInt()
                    endy = Math.max(approx2f.get(1, 0)[1], endy.toDouble()).toInt()

                    // approximating size of a single square
                    sqrHeight = Math.max(height, sqrHeight)
                    sqrWidth = Math.max(width, sqrWidth)
                    heights.add(height)
                    widths.add(width)
                }
            }
        }

        if (squares.isEmpty())
            return
        val rangeX = Range(startx, endx)
        val rangeY = Range(starty, endy)
        mineField = scrnShot.submat(starty, endy, startx, endx)
        fieldSize = Rectangle(startx, starty, endx - startx, endy - starty)

        var cleaned = Mat.zeros(cannyOutput.size(), CvType.CV_8U)
        // Draw contours
        Imgproc.drawContours(cleaned, squares, -1, Scalar(255.0, 255.0, 255.0), Imgproc.FILLED,Imgproc.LINE_AA)

        cleaned = cleaned.submat(rangeY, rangeX)
        Imgproc.threshold(cleaned, cleaned, 10.0, 255.0, Imgproc.THRESH_BINARY)
        Imgcodecs.imwrite("cleaned.png", cleaned);
        var cGap = 0.0
        var rGap = 0.0
        var count = 0
        var noOfGaps = 0.0
        var ignore = true

        val data = ByteArray(1)
        // Calculate Column Gap
        for (j in -5..5) {
            for (i in 0 until cleaned.cols()) {
                cleaned.get(sqrHeight.toInt() / 2 + j, i, data)
                if (ignore) {
                    if (data[0].toInt() == 0)
                        ignore = false
                    else
                        continue
                }
                if (data[0].toInt() == 0) {
                    count++
                } else {
                    if (count < sqrWidth / 2) { // Detect a gap
                        cGap += count
                        noOfGaps++
                    }
                    count = 0
                    ignore = true
                }
            }
        }
        if (noOfGaps == 0.0)
            return
        cGap /= noOfGaps

        noOfGaps = 0.0
        count = 0
        ignore = true

        // Calculate Row Gap
        for (j in -5..5) {
            for (i in 0 until cleaned.rows()) {
                cleaned.get(i, sqrWidth.toInt() / 2 + j, data)
                if (ignore) {
                    if (data[0].toInt() == 0)
                        ignore = false
                    else
                        continue
                }
                if (data[0].toInt() == 0) {
                    count++
                } else {
                    if (count < sqrHeight / 2) {
                        rGap += count
                        noOfGaps++
                    }

                    count = 0
                    ignore = true
                }
            }
        }
        if (noOfGaps == 0.0)
            return
        rGap /= noOfGaps

        val sqrSide = Math.min(sqrHeight,sqrWidth)
        columns = (fieldSize!!.width / (sqrWidth + cGap)).toInt()
        rows = (fieldSize!!.height / (sqrHeight + rGap)).toInt()

//        when {
//            Math.abs(columns - 30) < 2 -> columns = 30
//            Math.abs(columns - 28) < 2 -> columns = 28
//            Math.abs(columns - 18) < 2 -> columns = 18
//        }
//        if (Math.abs(rows - 24) < 2)
//            rows = 24
//        if (Math.abs(columns - 16) < 2)
//            columns = 16
//        if (Math.abs(rows - 16) < 2)
//            rows = 16
//        if (Math.abs(columns - 9) < 2)
//            columns = 9

        current = Square()
        current!!.size = Point(sqrHeight.toInt(), sqrWidth.toInt())
        current!!.colGap = (rangeX.size() - sqrSide * columns).toDouble() / columns
        current!!.rowGap = (rangeY.size() - sqrSide * rows).toDouble() / rows

        gridStates = Array(rows) { ByteArray(columns) }
        Imgcodecs.imwrite("Detected Field " + columns + "x" + rows + ".png", getScrnShot(fieldSize))
    }

    internal fun readAll() {
        var num: Byte
        mineField = getScrnShot(fieldSize)
        for (i in Num1 until Flag) {
            listsOfCells[i].clear()
        }

        for (y in 0 until rows) {
            for (x in 0 until columns) {
                num = readCell(x, y)
                gridStates!![y][x] = num
                if (num < Flag)
                    listsOfCells[num].add(Point(x, y))
            }
        }
    }


    //This Function has problems
    fun update() {
        var num: Byte
        var x: Int
        var y: Int
        var listnum = UnOpen.toInt()
        var iter: ListIterator<Point>
        var thisPoint: Point
        mineField = getScrnShot(fieldSize)

        val old: LinkedList<Point>
        old = LinkedList(listsOfCells[listnum])
        while (listnum < Flag) {
            iter = old.listIterator()
            while (iter.hasNext()) {
                thisPoint = iter.next()
                x = thisPoint.x
                y = thisPoint.y
                num = readCell(x, y)
                if (gridStates!![y][x] == num)
                    continue
                changeCell(x, y, num)
            }
            listnum++
            old
                .clear()
        }
        y = 0
        while (y < rows) {
            x = 0
            while (x < columns) {
                printGrid(x, y)
                x++
            }
            println()
            y++
        }
        println()
        println()
        println()
    }

    // For Debugging Purposes Only
    private fun printGrid(x: Int, y: Int) {
        when (val num = gridStates!![y][x]) {
            Num1 -> print("1 ")
            Num2 -> print("2 ")
            Num3 -> print("3 ")
            Num4 -> print("4 ")
            Num5 -> print("5 ")
            Num6 -> print("6 ")
            Num7 -> print("7 ")
            Num8 -> print("8 ")
            UnOpen -> print("O ")
            Open -> print(". ")
            Flag -> print("X ")
            else -> print("$num ")
        }
    }

    private fun readCell(x: Int, y: Int): Byte {
        val startY = y.toDouble() / rows * fieldSize!!.height + current!!.rowGap / 4
        val startX = x.toDouble() / columns * fieldSize!!.width + current!!.colGap / 4
        val crop = Rect(
            startX.toInt(), startY.toInt(), current!!.size.x,
            current!!.size.y
        )

        current!!.image = Mat(mineField!!, crop)
        // Imgcodecs.imwrite("current.png", current.getImage());
        return current!!.identify()

    }

    private fun getScrnShot(screenRect: Rectangle?): Mat {
        try {
            Thread.sleep(600)   //This is at it's least do not change
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        val bi = BufferedImage(screenRect!!.width, screenRect.height, BufferedImage.TYPE_3BYTE_BGR)
        bi.graphics.drawImage(gamebot.getScreenshot(screenRect), 0, 0, null)

        val mat = Mat(bi.height, bi.width, CvType.CV_8UC3)
        mat.put(0, 0, (bi.raster.dataBuffer as DataBufferByte).data)
        return mat
    }

    private fun clickCell(x: Int, y: Int, button: Int) {
        if (!isInBounds(x, y))
            throw IllegalArgumentException()

        val curr = dimToScreen(x, y)
        if (!gamebot.click(curr.x, curr.y, button))
            play = false
    }

    internal fun openCell(x: Int, y: Int) {
        // left click at x,y
        clickCell(x, y, InputEvent.BUTTON1_MASK)
        changeCell(x, y, Open_Unknown)
    }

    /*
    Flag or Un Flag cell
     */
    internal fun changeCellFlag(x: Int, y: Int, newStatus: Byte) {
        // Right click at x,y
        if (newStatus == UnOpen)
        //if Unflag cell then click 1 time extra
            clickCell(x, y, InputEvent.BUTTON3_MASK)

        clickCell(x, y, InputEvent.BUTTON3_MASK)
        changeCell(x, y, newStatus)
    }

    internal fun openNeighboursat(x: Int, y: Int) {
        // Middle click to open neighbors
        clickCell(x, y, InputEvent.BUTTON2_MASK)
        for (yy in y - 1..y + 1)
            for (xx in x - 1..x + 1)
                if (getGridCell(xx, yy) == UnOpen)
                    changeCell(xx, yy, Open_Unknown)
    }

    /*
    Flag or Un Flag neighbouring cells
     */
    internal fun changeNeighbourFlagsAt(x: Int, y: Int, flagStatus: Byte) {
        val statusCheck = UnOpen * Flag / flagStatus //If Cell is to be flagged check for unopen and vice versa
        for (yy in y - 1..y + 1)
            for (xx in x - 1..x + 1)
                if (getGridCell(xx, yy).toInt() == statusCheck)
                    changeCellFlag(xx, yy, flagStatus) // Right click to flag the cell
    }

    internal fun getListIterator(`val`: Int): ListIterator<Point> {
        return listsOfCells[`val`].listIterator()
    }

    internal fun getGridCell(x: Int, y: Int): Byte {
        return if (isInBounds(x, y))
            gridStates!![y][x]
        else
            13
    }

    private fun changeCell(x: Int, y: Int, `val`: Byte) {
        val prevVal = gridStates!![y][x]
        val temp = Point(x, y)
        if (isInBounds(x, y)) {
            if (prevVal < Flag)
            // There are only 10 Lists
                if (!listsOfCells[prevVal].remove(temp)) {
                    // remove the x,y from current list
                    throw RuntimeException(
                        "The Lists are not in sync with Matrix"
                    )
                }
            if (`val` < Flag)
                listsOfCells[`val`].add(temp) // add x,y to new changed list

            gridStates!![y][x] = `val`
        }
    }

    internal fun isGameSolved(): Boolean {
        // If there are no squares
        // left to open the game is
        // solved
        Imgcodecs.imwrite("Current view " + columns + "x" + rows + ".png", mineField!!)
        return listsOfCells[UnOpen].isEmpty()
    }

    // Game Over Logic: If Right click on a cell flags that cell then the game
    // is running
    // Else the Game is OVER
    internal fun isGameOver(): Boolean {
        Imgcodecs.imwrite("Current view " + columns + "x" + rows + ".png", mineField!!)
        val p: Point
        val rand = random.nextInt(listsOfCells[UnOpen].size)
        p = listsOfCells[UnOpen][rand]
        // Right Click at x,y
        clickCell(p.x, p.y, InputEvent.BUTTON3_MASK)
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        mineField = getScrnShot(fieldSize)
        if (readCell(p.x, p.y) != Flag)
            return true

        // Two right clicks returns that cell to normal
        // Right Click at x,y
        clickCell(p.x, p.y, InputEvent.BUTTON3_MASK)
        clickCell(p.x, p.y, InputEvent.BUTTON3_MASK)

        return false
    }

    internal fun matchAllNeighbours(x: Int, y: Int, nx: Int, ny: Int): LinkedList<Point>? {
        val uncommon = LinkedList<Point>()
        val temp = Point()
        for (yy in ny - 1..ny + 1) {
            for (xx in nx - 1..nx + 1) {
                if (getGridCell(xx, yy) == UnOpen) {
                    temp.x = xx
                    temp.y = yy
                    uncommon.add(temp)
                }
            }
        }

        for (yy in y - 1..y + 1) {
            for (xx in x - 1..x + 1) {
                temp.x = xx
                temp.y = yy
                if (getGridCell(xx, yy) == UnOpen && !isNeighbor(xx, yy, nx, ny))
                    return null
                else
                    uncommon.remove(temp)
            }
        }

        return uncommon
    }

    internal fun randomClick() {
        val p: Point
        // Choose a random unopened cell to click
        val rand = random.nextInt(listsOfCells[UnOpen].size)
        p = listsOfCells[UnOpen][rand]
        openCell(p.x, p.y)
    }

    private fun dimToScreen(x: Int, y: Int): Point {
        // Convert From grid dimensions to screen coordinates
        val curr = Point()
        curr.x =
            (fieldSize!!.x.toDouble() + x.toDouble() / columns * fieldSize!!.width + (current!!.size.x / 2).toDouble()).toInt()
        curr.y =
            (fieldSize!!.y.toDouble() + y.toDouble() / rows * fieldSize!!.height + (current!!.size.y / 2).toDouble()).toInt()
        return curr
    }


    private fun isNeighbor(x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        return Math.max(Math.abs(x0 - x1), Math.abs(y0 - y1)) <= 1
    }

    companion object {
        private var random = Random()

        // Associating Cells with Numbers
        internal const val Num1: Byte = 0
        internal const val Num2: Byte = 1
        internal const val Num3: Byte = 2
        internal const val Num4: Byte = 3
        internal const val Num5: Byte = 4
        internal const val Num6: Byte = 5
        internal const val Num7: Byte = 6
        internal const val Num8: Byte = 7
        internal const val UnOpen: Byte = 8
        private const val Open_Unknown: Byte = 9
        internal const val Flag: Byte = 10
        internal const val Question: Byte = 11
        internal const val Open: Byte = 12

        private fun angle(ds: DoubleArray, ds2: DoubleArray, ds3: DoubleArray): Double {
            val dx1 = ds[0] - ds3[0]
            val dy1 = ds[1] - ds3[1]
            val dx2 = ds2[0] - ds3[0]
            val dy2 = ds2[1] - ds3[1]
            return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10)
        }
    }
    private operator fun <T> Array<T>.get(index: Byte): T {
        return this[index.toInt()]
    }
}


// Improvements we can store only numbers that are next to unopened blocks
// Instead of exception when wrong flags are marked i can unflag the cells
// update logic
// Moeed's logic: boxes open sequentially so check how many boxes have
// opened
// Have to check open_Unknown cells