// ChessAI.kt
package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.board.Movement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChessAI(private val color: String) {
    var executionTimeMs = 0L
    var evaluatedPositions = 0

    init {
        System.loadLibrary("stockfish")
    }

    // Native method declarations
    private external fun initializeEngine()
    private external fun setPosition(fen: String)
    private external fun makeMove(uciMove: String): String
    private external fun getLegalMoves(square: String): Array<String>
    private external fun getCurrentFen(): String
    private external fun getPiece(square: String): Pair<String, String>
    private external fun quit()
    private external fun getAIMove(colorStr: String): String

    fun getPromotion(): String = "queen"

    fun getMoveInfo(move: Movement): String {
        return "moveInfo:" +
                "\n• Native Fairy-Stockfish engine" +
                "\n• ${move.asString2(color)}" +
                "\n• Time: ${executionTimeMs}ms"
    }

    suspend fun calcMove(fenString: String): Movement {
        val startTime = System.currentTimeMillis()

        return withContext(Dispatchers.Default) {
            setPosition(fenString)
            val bestMoveStr = getAIMove(color)
            parseUCIMove(bestMoveStr)
        }.also {
            executionTimeMs = System.currentTimeMillis() - startTime
        }
    }


    private fun parseUCIMove(uciMove: String): Movement {
        val (piece, color) = getPiece(uciMove.substring(0, 2))
        val sourceFile = uciMove[0] - 'a'
        val sourceRank = uciMove[1] - '1'
        val targetFile = uciMove[2] - 'a'
        val targetRank = uciMove[3] - '1'

        return Movement(sourceRank, sourceFile, targetRank, targetFile)
    }

    private fun evaluatePosition(fen: String, isOurTurn: Boolean): Int {
        // Implement your position evaluation logic here
        // This is a placeholder implementation that always returns a positive score for the current player
        return if (isOurTurn) 100 else -100
    }

    protected fun finalize() {
        quit()
    }
}