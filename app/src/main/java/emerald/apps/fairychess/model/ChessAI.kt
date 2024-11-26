package emerald.apps.fairychess.model

import MovementHash
import ValueHash
import emerald.apps.fairychess.model.Movement.Companion.emptyMovement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.*


/**
 * Data class representing a move and its evaluated score.
 *
 * @property movement The chess move
 * @property value The evaluation score for the resulting position
 */
class MinimaxResult(val movement: Movement, val value: Int)

@ExperimentalUnsignedTypes
/**
 * An AI implementation for playing chess using the alpha-beta search algorithm.
 *
 * Key optimizations:
 * 1. Alpha-Beta pruning to reduce the number of nodes searched
 * 2. Transposition Table to cache and reuse previously evaluated positions
 * 3. Zobrist Hashing for efficient position identification
 *
 * Potential future optimizations:
 * - Move ordering (MVV/LVA, killer moves, history heuristic)
 * - Iterative deepening
 * - Quiescence search
 * - Null move pruning
 */
class ChessAI {
    //for Profiling
    var executionTimeMs = 0L
    var summedCloningTime = 0L

    //Settings
    private val recursionDepth = 4 //Maximum depth for the alpha-beta search

    //Profiling Fields
    var evaluatedPositions = 0 //Counter variable for number of positions evaluated
    var valueTableHits = 0 //Counter for successful transposition table lookups
    var movementTableHits = 0 //Counter for successful transposition table lookups

    var color: String  //Color this AI plays as ("white" or "black")
    var valueHash : ValueHash = ValueHash() //Zobrist hash generator for position identification
    var movementHash : MovementHash = MovementHash() //Zobrist hash generator for position identification

    /**
     * Static piece values for move ordering
     */
    private companion object {
        private val PIECE_VALUES = mapOf(
            "queen" to 900,
            "rook" to 500,
            "bishop" to 300,
            "knight" to 300,
            "pawn" to 100,
            "king" to 10000
        )
    }

    /**
     * Creates a new ChessAI instance.
     *
     * @param color The color this AI will play as ("white" or "black")
     */
    constructor(color: String) {
        this.color = color
    }

    /**
     * Returns the piece type to promote a pawn to.
     * Currently always returns "queen" as it's generally the strongest piece.
     * TODO: calculate best figure
     *
     * @return The name of the piece to promote to
     */
    fun getPromotion() : String {
        return "queen"
    }

    /**
     * Calculates the best move for the current position.
     *
     * @param bitboard The current chess position
     * @return The best move found within the search depth
     */
    suspend fun calcMove(bitboard: Bitboard) : Movement{
        evaluatedPositions = 0
        val startTime = System.currentTimeMillis()

        val result =  alphabeta(bitboard, recursionDepth, Int.MIN_VALUE, Int.MAX_VALUE).movement
        executionTimeMs = System.currentTimeMillis() - startTime

        return result
    }

    /**
     * Implements the alpha-beta pruning algorithm to find the best move.
     *
     * @param bitboard The current chess position
     * @param level Current search depth remaining
     * @param alpha Best score that maximizing player can guarantee
     * @param beta Best score that minimizing player can guarantee
     * @return The best move and its evaluation
     */
    suspend fun alphabeta(bitboard: Bitboard, level: Int, alpha: Int, beta: Int): MinimaxResult {
        // Base case
        if (level <= 0) {
            return MinimaxResult(emptyMovement(), getPointDifBW(bitboard))
        }

        val moves = bitboard.getAllPossibleMovesAsList(bitboard.moveColor)
        if (moves.isEmpty()) {
            return MinimaxResult(emptyMovement(), getPointDifBW(bitboard))
        }

        return if (bitboard.moveColor == "black") {
            findBestMoveForBlack(bitboard, moves, level, alpha, beta)
        } else {
            findBestMoveForWhite(bitboard, moves, level, alpha, beta)
        }
    }

    /**
     * Orders moves based on MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
     * Higher value indicates a better move to try first
     *
     * Prioritizes capturing high value pieces with lower value pieces
     * (For example, taking a queen with a pawn scores higher than taking a pawn with a queen)
     * Non-capturing moves are tried last
     */
    private fun getMoveScore(move: Movement, bitboard: Bitboard): Int {
        val targetPiece = bitboard.getPieceName(move.getTargetCoordinate())
        if (targetPiece.isEmpty()) return 0

        val attackingPiece = bitboard.getPieceName(move.getSourceCoordinate())
        val targetValue = PIECE_VALUES[targetPiece] ?: 0
        val attackerValue = PIECE_VALUES[attackingPiece] ?: 0

        // MVV-LVA formula: victim value * 10 - attacker value
        // This prioritizes capturing high value pieces with lower value pieces
        return targetValue * 10 - attackerValue
    }

    /**
     * Orders moves before searching
     */
    private fun orderMoves(moves: List<Movement>, bitboard: Bitboard): List<Movement> {
        return moves.sortedByDescending { move ->
            getMoveScore(move, bitboard)
        }
    }

    private suspend fun findBestMoveForBlack(
        bitboard: Bitboard,
        moves: List<Movement>,
        level: Int,
        alpha: Int,
        beta: Int
    ): MinimaxResult = coroutineScope {
        // Check cache first
        movementHash.getMove(bitboard)?.let {
            ++movementTableHits
            MinimaxResult(it, evaluateMove(bitboard, it, level, alpha, beta))
        }

        var bestValue = Int.MIN_VALUE
        var bestMove = emptyMovement()
        var alphaCurrent = alpha

        val orderedMoves = orderMoves(moves, bitboard)

        if (level == recursionDepth) {
            // Parallel evaluation for level 4
            val results = orderedMoves.map { move ->
                async(Dispatchers.Default) {
                    // Create a copy of bitboard for each thread
                    val bitboardCopy = bitboard.clone()
                    val value = evaluateMove(bitboardCopy, move, level, alphaCurrent, beta)
                    Pair(move, value)
                }
            }

            // Await all results and find the best one
            for (deferred in results) {
                val (move, value) = deferred.await()

                if (value > bestValue) {
                    bestValue = value
                    bestMove = move
                }

                if (value >= beta) {
                    results.forEach { it.cancel() }
                    break
                }
                alphaCurrent = maxOf(alphaCurrent, value)
            }
        } else {
            // Original sequential logic
            for (move in orderedMoves) {
                val value = evaluateMove(bitboard, move, level, alphaCurrent, beta)

                if (value > bestValue) {
                    bestValue = value
                    bestMove = move
                }

                if (value >= beta) break
                alphaCurrent = maxOf(alphaCurrent, value)
            }
        }
        movementHash.putMove(bitboard, bestMove)
        MinimaxResult(bestMove, bestValue)
    }

    /**
     * Finds the best move for white (minimizing player)
     */
    private suspend fun findBestMoveForWhite(
        bitboard: Bitboard,
        moves: List<Movement>,
        level: Int,
        alpha: Int,
        beta: Int
    ): MinimaxResult = coroutineScope {
        // Check cache first
        movementHash.getMove(bitboard)?.let {
            ++movementTableHits
            MinimaxResult(it, evaluateMove(bitboard, it, level, alpha, beta))
        }

        var bestValue = Int.MAX_VALUE
        var bestMove = emptyMovement()
        var betaCurrent = beta

        val orderedMoves = orderMoves(moves, bitboard)

        if (level == recursionDepth) {
            // Parallel evaluation for level 4
            val results = orderedMoves.map { move ->
                async(Dispatchers.Default) {
                    // Create a copy of bitboard for each thread
                    val bitboardCopy = bitboard.clone()
                    val value = evaluateMove(bitboardCopy, move, level, alpha, betaCurrent)
                    Pair(move, value)
                }
            }

            // Await all results and find the best one
            for (deferred in results) {
                val (move, value) = deferred.await()

                if (value < bestValue) {
                    bestValue = value
                    bestMove = move
                }

                if (value <= alpha) {
                    results.forEach { it.cancel() }
                    break
                }
                betaCurrent = minOf(betaCurrent, value)
            }
        } else {
            // Original sequential logic
            for (move in orderedMoves) {
                val value = evaluateMove(bitboard, move, level, alpha, betaCurrent)

                if (value < bestValue) {
                    bestValue = value
                    bestMove = move
                }

                if (value <= alpha) break
                betaCurrent = minOf(betaCurrent, value)
            }
        }

        MinimaxResult(bestMove, bestValue)
    }

    /**
     * Evaluates a single move by either looking it up in the transposition table
     * or calculating it recursively
     */
    private suspend fun evaluateMove(
        bitboard: Bitboard,
        move: Movement,
        level: Int,
        alpha: Int,
        beta: Int
    ): Int {
        val copyBitboard = bitboard.clone()
        bitboard.move(bitboard.moveColor, move)

        val value = getPositionValue(bitboard, move, level, alpha, beta)

        bitboard.set(copyBitboard)
        return value
    }

    /**
     * Gets the value of a position either from the transposition table
     * or by calculating it
     */
    private suspend fun getPositionValue(
        bitboard: Bitboard,
        move: Movement,
        level: Int,
        alpha: Int,
        beta: Int
    ): Int {
        //TODO: hash doubles the time without any hits - for now disable it

        val hashValue = valueHash.getFromCache(bitboard)

        if (hashValue != null) {
            ++valueTableHits
            return hashValue
        }

        val value = alphabeta(bitboard, level - 1, alpha, beta).value
        valueHash.putInCache(bitboard, value)
        return value
    }

    private fun getPointDifBW(bitboard: Bitboard) : Int{
        ++evaluatedPositions
        return bitboard.pointsBlack() - bitboard.pointsWhite()
    }

    /**
     * Gets formatted move information and statistics
     */
    fun getMoveInfo(move: Movement): String {
        return "moveInfo:\n- ${move.asString2(color)}\n• Time: ${executionTimeMs}ms\n• evaluated Positions: $evaluatedPositions"
    }

    /**
     * Gets formatted move information and statistics
     */
    fun getHashInfo(move: Movement): String {
        return "Hash Info:"+
                "\n• valueTableHits: $valueTableHits\n• valueTableSize: ${valueHash.getKeysize()}" +
                "\n• movementTableHits: ${movementTableHits}\n• movementTableSize: ${movementHash.getKeysize()}"
    }

}