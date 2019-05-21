import java.awt.EventQueue
import javax.swing.JFrame
import javax.swing.JSlider
import javax.swing.JLabel
import javax.swing.ImageIcon
import java.awt.Color
import javax.swing.JTextPane
import java.awt.AWTException
import java.awt.Font
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import org.opencv.core.Core
import java.awt.Component
import java.awt.Toolkit

object Interface {

    private var frame: JFrame? = null
    private var play_stop: JLabel? = null
    private var slider: JSlider? = null
    private var Status: JTextPane? = null
    private var Value: JTextPane? = null
    private var AI: Solver? = null

    /**
     * Launch the application.
     *
     * throws InterruptedException
     */
    @Throws(InterruptedException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        var i = 0
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
        try {
            AI = Solver()
        } catch (e: AWTException) {
            e.printStackTrace()
        }

//        initialize()
//        EventQueue.invokeLater { frame!!.isVisible = true }


        AI!!.initialize()
        AI!!.msMinesweeper.readAll()
        AI!!.msMinesweeper.printDifference()
        // Play until win
//        while (true) {
//            Thread.sleep(100)
//            if (AI!!.isPlaying) {
//                play_stop!!.icon = ImageIcon("Stop.png")
//                Status!!.text = "AI RUNNING"
//                Status!!.foreground = Color.BLUE
//                frame!!.state = Frame.ICONIFIED
//                if (AI!!.initialize()) {
//                    println("GAME: " + i++)
//                    if (AI!!.solveGame())
//                        println("GAME WON!!!!")
//                    else
//                        System.err.println("GAME OVER!!!!")
//                }
//                play_stop!!.icon = ImageIcon("Play.png")
//                Status!!.text = "AI STOPPED"
//                Status!!.foreground = Color.RED
//                AI!!.isPlaying = false
//                frame!!.state = Frame.NORMAL
//            }
//        }
    }


    /**
     * initialize the contents of the frame.
     */

    private fun initialize() {
        frame = JFrame()
        frame!!.iconImage = Toolkit
            .getDefaultToolkit()
            .getImage(
                Interface::class.java
                    .getResource("/com/sun/java/swing/plaf/windows/icons/Computer.gif")
            )
        frame!!.setBounds(100, 100, 223, 234)
        frame!!.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame!!.contentPane.layout = null

        slider = JSlider()
        slider!!.value = AI!!.speed
        slider!!.addChangeListener {
            Value!!.text = slider!!.value.toString()
            AI!!.speed = slider!!.value
        }
        slider!!.setBounds(28, 147, 150, 26)
        frame!!.contentPane.add(slider)

        play_stop = JLabel("")
        play_stop!!.alignmentX = Component.CENTER_ALIGNMENT
        play_stop!!.setBounds(90, 48, 26, 44)
        play_stop!!.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(arg0: MouseEvent?) {
                AI!!.isPlaying = !AI!!.isPlaying
            }
        })
        play_stop!!.icon = ImageIcon("Play.png")
        frame!!.contentPane.add(play_stop)

        val txtSpeed = JTextPane()
        txtSpeed.setBounds(32, 103, 79, 33)
        txtSpeed.isEditable = false
        txtSpeed.isOpaque = false
        txtSpeed.font = Font("Cambria", Font.BOLD, 20)
        txtSpeed.preferredSize = Dimension(14, 20)
        txtSpeed.text = "SPEED :"
        frame!!.contentPane.add(txtSpeed)

        Status = JTextPane()
        Status!!.setBounds(44, 11, 118, 26)
        Status!!.isEditable = false
        Status!!.foreground = Color.RED
        Status!!.font = Font("Microsoft Tai Le", Font.BOLD, 18)
        Status!!.isOpaque = false
        Status!!.text = "AI STOPPED"
        frame!!.contentPane.add(Status)

        Value = JTextPane()
        Value!!.isEditable = false
        Value!!.setBounds(125, 103, 47, 33)
        Value!!.text = slider!!.value.toString()
        Value!!.font = Font("Cambria", Font.BOLD, 20)
        frame!!.contentPane.add(Value)
    }
}
