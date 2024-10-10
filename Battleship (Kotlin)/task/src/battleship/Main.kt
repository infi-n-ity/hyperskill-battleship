package battleship

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun main() {
    Battleship().start()
}

class Battleship {
    object Properties {
        object MapSize {
            const val LENGTH = 10
            const val WIDTH = 10
        }
        object CapitalLetter {
            const val START = 65
        }
    }

    private val players = mutableListOf(Player(), Player())

    fun start () {
        print("Player 1, place your ships on the game field")
        players.first().placeShips()

        changePlayer()

        print("Player 2, place your ships on the game field")
        players.last().placeShips()

        changePlayer()

        startGame()
    }

    private fun changePlayer() {
        println("Press Enter and pass the move to another player")
        println("...")
        println()
        readln()
        println()
    }

    private fun startGame() {
        while(true) {
            players.last().printHiddenMap()
            println("---------------------")
            players.first().printMap()
            print("Player 1, it's your turn:")
            players.last().takeShot()

            if (players.last().isLose()) {
                println("Player 1, You sank the last ship. You won. Congratulations!")
                return
            }
            changePlayer()

            players.first().printHiddenMap()
            println("---------------------")
            players.last().printMap()
            print("Player 2, it's your turn:")
            players.first().takeShot()

            if (players.first().isLose()) {
                println("Player 2, You sank the last ship. You won. Congratulations!")
                return
            }

            changePlayer()
        }
    }

    class Player {
        private val map = Map()
        private var hiddenMap = Map()

        fun placeShips() {
            map.print()
            map.placeShips()
        }

        fun printMap() {
            map.print()
        }

        fun printHiddenMap() {
            hiddenMap.print(withoutSpace = true)
        }

        fun takeShot() {
            val result = map.takeShot(hiddenMap)
            when (result) {
                ShotResult.HIT -> println("You hit a ship!")
                ShotResult.MISS -> println("You missed!")
                ShotResult.SANK -> println("You sank a ship!")
                else -> throw RuntimeException()
            }
        }

        fun isLose(): Boolean {
            return !map.hasShips()
        }
    }

    class Map {
        private val map = List(Properties.MapSize.LENGTH) { row ->
            MutableList(Properties.MapSize.WIDTH) { cell ->
                Cell(
                    row,
                    cell,
                    CellType.WATER,
                    false
                )
            }
        }
        private val ships = mutableListOf<Ship>()

        fun print(withoutSpace: Boolean = false) {
            println("  " + ((1 .. Properties.MapSize.WIDTH).joinToString( " ")))
            map.forEachIndexed { index, row ->
                println(
                    (Properties.CapitalLetter.START + index).toChar() + " " +
                            row.map { if (it.isHidden) '~' else it.cellType.char }.joinToString(" ")
                )
            }
            if (!withoutSpace) println()
        }

        fun placeShips() {
            ShipType.values().forEach { shipType ->
                do {
                    val coordinates = getValidCoordinates(shipType)
                    val result = placeShip(shipType, coordinates)
                    if (!result) print()
                } while (result)
            }
        }

        private fun getValidCoordinates(shipType: ShipType): Coordinaties {
            print("Enter the coordinates of the ${shipType.type} (${shipType.length} cells)")
            return getParsedCoordinates()
        }

        private fun getParsedCoordinates(): Coordinaties {
            val coordinates = readln().split(" ")
            println()
            val start = parseCoordinateToIndexes(coordinates.first())
            val finish = parseCoordinateToIndexes(coordinates.last())
            return Coordinaties(start, finish)
        }

        private fun parseCoordinateToIndexes(input: String): Point {
            val firstIndex = (input.first() - Properties.CapitalLetter.START.toChar())
            val secondIndex = input.substring(1).toInt() - 1
            return Point(firstIndex, secondIndex)
        }

        private fun placeShip(shipType: ShipType, coordinaties: Coordinaties): Boolean {
            val (start, finish) = coordinaties
            if (isValidShipPlacement(start, finish, shipType.length)) {
                val maxX = max(start.x, finish.x)
                val minX = min(start.x, finish.x)
                val maxY = max(start.y, finish.y)
                val minY = min(start.y, finish.y)
                val shipCoordinates = mutableListOf<Point>()
                for (i in minX..maxX) {
                    for (j in minY..maxY) {
                        map[i][j].cellType = CellType.SHIP
                        shipCoordinates.add(Point(i, j))
                    }
                }
                ships.add(Ship(shipType.type, shipType.length, shipCoordinates))
                return false
            } else {
                println("Error! Wrong ship location! Try again:")
                return true
            }
        }

        private fun isValidShipPlacement(start: Point, finish: Point, length: Int): Boolean {
            val horizontal = start.x == finish.x
            val vertical = start.y == finish.y

            if (!(horizontal || vertical)) return false

            val shipLength = if (horizontal) abs(start.y - finish.y) + 1 else abs(start.x - finish.x) + 1
            if (shipLength != length) return false

            val minX = getPreviousCell(min(start.x, finish.x))
            val maxX = getNextCell(max(start.x, finish.x))
            val minY = getPreviousCell(min(start.y, finish.y))
            val maxY = getNextCell(max(start.y, finish.y))

            for (i in minX..maxX) {
                for (j in minY..maxY) {
                    if (map[i][j].cellType == CellType.SHIP) return false
                }
            }
            return true
        }

        private fun getNextCell(int: Int): Int {
            return if (int == 0 || int == 9) int else int + 1
        }

        private fun getPreviousCell(int: Int): Int {
            return if (int == 0 || int == 9) int else int - 1
        }

        fun takeShot(hiddenMap: Map): ShotResult {
            val point = getShotPoint()
            val cell = map[point.x][point.y]
            val hiddenCell = hiddenMap.map[point.x][point.y]
            return when {
                cell.cellType == CellType.SHIP || cell.cellType == CellType.HIT -> {
                    cell.isHidden = false
                    cell.cellType = CellType.HIT
                    hiddenCell.cellType = CellType.HIT
                    if (shipSank(point)) ShotResult.SANK else ShotResult.HIT
                }
                cell.cellType == CellType.WATER -> {
                    cell.isHidden = false
                    cell.cellType = CellType.MISS
                    hiddenCell.cellType = CellType.MISS
                    ShotResult.MISS
                }
                else -> ShotResult.INVALID
            }
        }

        private fun getShotPoint(): Point {
            val point = readln()
            println()
            return parseCoordinateToIndexes(point)
        }

        private fun shipSank(point: Point): Boolean {
            val ship = ships.firstOrNull {
                (it.coordinates.firstOrNull { ship -> ship.x == point.x && ship.y == point.y } != null)
            } ?: return false
            return ship.coordinates.all { map[it.x][it.y].cellType == CellType.HIT }
        }

        fun hasShips(): Boolean {
            return ships.any { it.coordinates.any { coord -> map[coord.x][coord.y].cellType == CellType.SHIP } }
        }
    }

    data class Coordinaties(val point1: Point, val point2: Point)

    class Point(val x: Int, val y: Int)

    enum class CellType(val char: Char) {
        WATER('~'),
        SHIP('O'),
        HIT('X'),
        MISS('M')
    }

    class Cell(val x: Int, val y: Int, var cellType: CellType, var isHidden: Boolean)

    class Ship(val name: String, val length: Int, val coordinates: List<Point>)

    enum class ShipType(val type: String, val length: Int) {
        AIRCRAFT_CARRIER("Aircraft Carrier", 5),
        BATTLESHIP("Battleship", 4),
        SUBMARINE("Submarine", 3),
        CRUISER("Cruiser", 3),
        DESTROYER("Destroyer", 2),
    }

    enum class ShotResult {
        HIT, MISS, SANK, INVALID
    }
}