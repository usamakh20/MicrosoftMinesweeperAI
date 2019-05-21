import java.awt.AWTException
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage


internal class MyRobot @Throws(AWTException::class)
constructor() {

    private val robot: Robot
    var delay: Int = 0  // Milliseconds
    private var lastMouseLocation: Point? = null


    init {
        delay = 40
        lastMouseLocation = null
        robot = Robot()
        robot.isAutoWaitForIdle = true
    }

    fun getScreenshot(rect: Rectangle): BufferedImage {
        return robot.createScreenCapture(rect)
    }


    fun click(x: Int, y: Int, button: Int): Boolean {
        if (lastMouseLocation != null && MouseInfo.getPointerInfo().location != lastMouseLocation) {
            System.err.println("Mouse moved")
            lastMouseLocation = null
            return false
        }
        do {
            robot.mouseMove(x, y)
        } while (MouseInfo.getPointerInfo().location != Point(x, y))
        robot.mousePress(button)
        robot.mouseRelease(button)
        robot.delay(delay)
        lastMouseLocation = Point(x, y)
        return true
    }

    fun clearMouseLocation() {
        lastMouseLocation = null
    }
}
