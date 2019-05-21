import java.awt.AWTException
import java.awt.Point

//The solver class which solves the game

internal class Solver @Throws(AWTException::class)
constructor() {

    val msMinesweeper: Game = Game()

    var isPlaying: Boolean
        get() = msMinesweeper.play
        set(state) {
            msMinesweeper.play=state
            msMinesweeper.gamebot.clearMouseLocation()
        }

    var speed: Int
        get() = 100 - msMinesweeper.gamebot.delay
        set(speed) {
            msMinesweeper.gamebot.delay = 100 - speed
        }


    fun initialize(): Boolean {
        msMinesweeper.initialize()
        if (msMinesweeper.columns or msMinesweeper.rows == 0) {
            println("GAME NOT DETECTED!!!!")
            return false
        }
        return true
    }


    fun solveGame(): Boolean {
        msMinesweeper.readAll()
        msMinesweeper.printDifference()
        if (msMinesweeper.isNewGame())
            msMinesweeper.randomClick()

        while (isPlaying) {
            msMinesweeper.readAll()
            if (msMinesweeper.isGameSolved())
            // If game is solved
                return true
            var changed = false
            // Try to solve as much as possible without the expensive screen reread

            while ((solveSingles() || solvePairs()) && isPlaying)
                changed = true

            if (!changed) {
                println("I am Taking A Guess")
                msMinesweeper.randomClick()
                if (msMinesweeper.isGameOver())
                // if game is ended
                    return false
            }
        }
        return false
    }


    private fun solveSingles(): Boolean {
        var changed = false
        var thisPoint: Point
        var iter: ListIterator<Point>
        var noOfMines: Int
        // Access cells all numbered cells 1-8
        for (Mine in Game.Num1..Game.Num8) {
            iter = msMinesweeper.getListIterator(Mine)
            while (iter.hasNext()) {
                thisPoint = iter.next()
                val x = thisPoint.x
                val y = thisPoint.y
                noOfMines = Mine + 1
                val flags = countNeighboring(x, y, Game.Flag)
                val unopened = countNeighboring(x, y, Game.UnOpen)
                if (flags > noOfMines) {
                    msMinesweeper.changeNeighbourFlagsAt(x, y, Game.UnOpen)
                    System.err.println("Warning!! Wrong Flags are Marked around: " + (x + 1) + "," + (y + 1))
                    changed = true
                }

                // If all mines are flagged open remaining cells
                if (flags == noOfMines && unopened >= 1) {
                    msMinesweeper.openNeighboursat(x, y)
                    changed = true
                } else if (unopened >= 1 && flags + unopened == noOfMines) {
                    msMinesweeper.changeNeighbourFlagsAt(x, y, Game.Flag)
                    changed = true
                }// If everything except mines are opened then Flag mines
            }
        }
        return changed
    }

    private fun solvePairs(): Boolean {
        var changed = false
        var temp: Point
        var thisPoint: Point
        var iter: ListIterator<Point>
        var uncomIter: ListIterator<Point>
        var noOfMines: Int
        // For each cell (x, y) with a number on it
        for (Mines in Game.Num1..Game.Num8) {
            iter = msMinesweeper.getListIterator(Mines)
            while (iter.hasNext()) {
                thisPoint = iter.next()
                val x = thisPoint.x
                val y = thisPoint.y
                noOfMines = Mines + 1
                noOfMines -= countNeighboring(x, y, Game.Flag)

                // For each neighbor (nx, ny) with a number on it
                for (ny in y - 1..y + 1) {
                    for (nx in x - 1..x + 1) {
                        if (nx == x && ny == y || !msMinesweeper.isInBounds(nx, ny))
                            continue

                        var neighMines = msMinesweeper.getGridCell(nx, ny).toInt()
                        if (neighMines < 1 || neighMines > 8)
                            continue
                        neighMines -= countNeighboring(nx, ny, Game.Flag)

                        // Check if ALL unopened neighbors of (x, y) are
                        // neighbors of (nx, ny)
                        val uncommon = msMinesweeper.matchAllNeighbours(x, y, nx, ny) ?: continue

                        // Open all cells unique to the neighbor
                        if (neighMines == noOfMines) {
                            uncomIter = uncommon.listIterator()
                            while (uncomIter
                                    .hasNext()
                            ) {
                                temp = uncomIter.next()
                                msMinesweeper.openCell(temp.x, temp.y)
                                changed = true
                            }
                        } else if (neighMines - noOfMines == countNeighboring(
                                nx,
                                ny, Game.UnOpen
                            ) - countNeighboring(x, y, Game.UnOpen)
                        ) {
                            uncomIter = uncommon.listIterator()
                            while (uncomIter
                                    .hasNext()
                            ) {
                                temp = uncomIter.next()
                                msMinesweeper.changeCellFlag(temp.x, temp.y, Game.Flag)
                                changed = true
                            }
                        }// Flag all cells unique to the neighbor
                    }
                }
            }
        }
        return changed
    }

    private fun countNeighboring(x: Int, y: Int, value: Byte): Int {
        var count = 0
        for (yy in y - 1..y + 1) {
            for (xx in x - 1..x + 1) {
                if (msMinesweeper.getGridCell(xx, yy) == value)
                    count++
            }
        }
        return count
    }
}
