import kotlin.concurrent.thread

fun main() {
/*    val goodStart = intArrayOf(
        5, 5, 5, 4, 4, 4,
        0,

        4, 4, 0, 5, 0, 6,
        2,
    )


    repeat(6) {
        thread {
            val position = Position(goodStart.copyOf())
            position.genMoves()
            position.sortMoves()
            position.makeMove(position.moves[it])
            val result = alphaBeta(position, -30_000, 30_000, 0, 14, 0)
            println("$it $result")
        }
    }*/


    game(
        Position(

        )
    )
}

fun game(position: Position) {
    // ==
   // playerMove(position)
    skipPlayerMove(position)
    // ==

    while (!position.isEndOfGame()) {
        position.firstMoves = emptyList()
        showSwappedPosition(position)
        val eval = alphaBeta(position, -30_000, 30_000, 0, 12, 0)
        println("Evaluation: $eval")
        println("Move: ${position.firstMoves.joinToString(", ") { ((it and EIGHT_BIT_MASK) + 1).toString() }}")
        println(DIVIDER)
        position.firstMoves.forEach(position::makeMove)
        position.swap()
        if (position.isEndOfGame()) {
            position.swap()
            break
        }
        playerMove(position)
    }

    position.swap()
    println(position)
    println("Game is ended")
}

fun showSwappedPosition(position: Position) {
    position.swap()
    println(position)
    position.swap()
}

fun skipPlayerMove(position: Position) {
    println(position)
    println("Skipped move")
    println(DIVIDER)
    position.swap()
}

fun playerMove(position: Position) {
    do {
        val move = requestPlayerToMove(position)
        val cont = position.makeMove(move)
    } while (!position.isEndOfGame() && cont)
    position.swap()
}

fun requestPlayerToMove(position: Position): Int {
    position.genMoves()
    println(position)
    println("Your turn")
    var turn: Int? = null
    while (turn == null || turn !in 1..6 || position.moves[turn - 1] == -1) {
        turn = read().toIntOrNull()
    }
    println(DIVIDER)
    return position.moves[turn - 1]
}

fun read(): String {
    return readln()
}

const val DIVIDER = "============================================================"
const val EIGHT_BIT_MASK = 0b1111_1111
const val COUNT = 6 + 6 + 2

val standard = IntArray(COUNT) {
    if (it == 6 || it == 13) 0 else 4
}

class Position(var array: IntArray = standard) {

    var reversed = IntArray(COUNT) {
        array[opposite[it]]
    }

    val cache = IntArray(COUNT)

    var lastMove: Int = 0
    var firstMoves: List<Int> = emptyList()
    var bestMove = 0

    val moves = IntArray(6)
    val movesScores = IntArray(6)

    val renderMoves get() = moves.joinToString(", ") { (it and EIGHT_BIT_MASK).toString() }

    fun makeMove(move: Int): Boolean {
        var count = (move shr 8) and EIGHT_BIT_MASK
        var i = move and EIGHT_BIT_MASK
        array[i] = 0
        while (count-- > 0) {
            if (++i == 13) i = 0
            array[i]++
        }
        if (i == 6) {
            lastMove = move
            return true
        } else {
            if (i > -1 && i < 6 && array[i] == 1) {
                val o = opposite[i]
                val stolen = array[o]
                if (stolen > 0) {
                    array[i] = 0
                    array[o] = 0
                    array[6] += stolen + 1
                    lastMove = move or (stolen shl 16)
                } else {
                    lastMove = move
                }
            } else {
                lastMove = move
            }
            return false
        }
    }

    fun unmakeMove(move: Int) {
        var count = (move shr 8) and EIGHT_BIT_MASK
        var i = move and EIGHT_BIT_MASK
        val firstIndex = i
        val firstCount = count
        while (count-- > 0) {
            if (++i == 13) i = 0
            array[i]--
        }
        if (array[i] == -1) {
            array[i] = 0
            val stolen = move shr 16
            array[opposite[i]] += stolen
            array[6] -= stolen + 1
        }
        array[firstIndex] += firstCount
    }

    fun swap() {
        for (i in 0..13) {
            reversed[reversedPos[i]] = array[i]
        }
        val tmp = array
        array = reversed
        reversed = tmp
    }

    fun isEndOfGame(): Boolean {
        if (array[6] > 24 || array[13] > 24) return true
        kotlin.run {
            for (i in 0..5) {
                if (array[i] != 0) return@run
            }
            return true
        }
        for (i in 7..12) {
            if (array[i] != 0) return false
        }
        return true
    }

    fun endGameEvaluation(): Int {
        var first = 0
        var second = 0
        for (i in 0..6) {
            first += array[i]
        }
        for (i in 7..13) {
            second += array[i]
        }
        return first.compareTo(second) * 30_000
    }

    fun genMoves() {
        for (i in 0..5) {
            val count = array[i]
            if (count == 0) {
                moves[i] = -1
                movesScores[i] = Int.MIN_VALUE
            } else {
                moves[i] = i or (count shl 8)
                movesScores[i] = -count
            }
        }
    }

    fun sortMoves() {
        var needIteration = true
        while (needIteration) {
            needIteration = false
            for (i in 1 until movesScores.size) {
                if (movesScores[i] > movesScores[i - 1]) {
                    var tmp = movesScores[i]
                    movesScores[i] = movesScores[i - 1]
                    movesScores[i - 1] = tmp
                    tmp = moves[i]
                    moves[i] = moves[i - 1]
                    moves[i - 1] = tmp
                    needIteration = true
                }
            }
        }
    }

    private val cacheArrays = Array(128) { IntArray(6) }
    private var count = 0

    fun copyMovesArray(original: IntArray): IntArray {
        val result = cacheArrays[count++]
        System.arraycopy(original, 0, result, 0, 6)
        return result
    }

    fun freeMovesArray() {
        count--
    }

    override fun toString(): String {
        return buildString {
            val firstRow = array.toList().subList(7, 13).reversed()
                .joinToString(separator = " | ", prefix = "    | ", postfix = " |    ") {
                    if (it > 9) it.toString() else "0$it"
                }
            append(firstRow)
            append('\n')
            append(" ").append(if (array[13] > 9) array[13].toString() else "0${array[13]}").append(" |")
            append(" ".repeat(firstRow.length - 10))
            append("| ").append(if (array[6] > 9) array[6].toString() else "0${array[6]}").append(" ")
            append('\n')
            append(
                array.toList().subList(0, 6).joinToString(separator = " | ", prefix = "    | ", postfix = " |    ") {
                    if (it > 9) it.toString() else "0$it"
                }
            )
        }
    }
}

fun alphaBeta(position: Position, alpha: Int, beta: Int, depth: Int, maxDepth: Int, d: Int): Int {
    if (position.isEndOfGame()) {
        var evaluation = position.endGameEvaluation()
        if (evaluation < 0) {
            evaluation += depth
        } else if (evaluation > 0) {
            evaluation -= depth
        }
        return evaluation
    }
    if (depth >= maxDepth) {
        return evaluation(position)
    }

    position.genMoves()
    position.sortMoves()
    require(position.moves[0] != -1)

    val moves = position.copyMovesArray(position.moves)
    var i = 0
    var bestResult = Int.MIN_VALUE
    var alpha = alpha
    var bestMove = 0
    var firstMoves: List<Int>? = null
    do {
        var move = moves[i]
        if (move == -1) break
//        val before = position.array.copyOf()
        val swapNotNeeded = position.makeMove(move)
        /*  require(position.array.any { it >= 0 }){
              "asd"
          }*/
        move = position.lastMove
        var result: Int
        if (swapNotNeeded) {
            result = alphaBeta(position, alpha, beta, depth, maxDepth, d + 1)
        } else {
            position.swap()
            result = -alphaBeta(position, -beta, -alpha, depth + 1, maxDepth, d + 1)
            position.swap()
        }
        position.unmakeMove(move)
        /*  require(before.toList() == position.array.toList()) {
              "asd"
          }*/
        if (result > bestResult) {
            bestMove = move
            if (depth == 0) {
                firstMoves = position.firstMoves
            }
            bestResult = result
        }
        if (bestResult > alpha) {
            alpha = bestResult
        }
        if (alpha >= beta) {
            break
        }
    } while (++i < 6)
    position.freeMovesArray()
    position.bestMove = bestMove
    if (depth == 0) {
        position.firstMoves = listOf(bestMove, *(firstMoves!!.toTypedArray()))
    } else {
        position.firstMoves = emptyList()
    }

    return alpha
}



val opposite = intArrayOf(12, 11, 10, 9, 8, 7, 13, 5, 4, 3, 2, 1, 0, 6)
val reversedPos = intArrayOf(7, 8, 9, 10, 11, 12, 13, 0, 1, 2, 3, 4, 5, 6)
val oppositeEmptyScore = intArrayOf(
    0, 2, 4, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16, 16, 16, 16, 17, 17, 17, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18,
)

fun evaluation(position: Position): Int {
    val reversed = position.cache
    val array = position.array
    for (i in 0..13) {
        reversed[reversedPos[i]] = array[i]
    }
    return firstGuyEvaluation(array) - firstGuyEvaluation(reversed)
}

fun firstGuyEvaluation(array: IntArray): Int {
    var score = array[6] * 100
    score += array[0] * (100 - 4 * 1)
    score += array[1] * (100 - 4 * 2)
    score += array[2] * (100 - 4 * 3)
    score += array[3] * (100 - 4 * 4)
    score += array[4] * (100 - 4 * 5)
    score += array[5] * (100 - 4 * 6)

    for (i in 0..5) {
        if (array[i] == 0) {
            score += oppositeEmptyScore[array[opposite[i]]]
        }
    }

    return score
}
