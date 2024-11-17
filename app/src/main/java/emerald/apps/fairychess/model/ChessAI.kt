package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.Movement.Companion.emptyMovement
import java.util.*
import kotlin.math.max
import kotlin.math.min


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
    //Settings
    private val recursionDepth = 4 //Maximum depth for the alpha-beta search

    //Fields
    var movementCounter = 0 //Counter variable for number of positions evaluated
    var transpositionTableHits = 0 //Counter for successful transposition table lookups

    var color: String  //Color this AI plays as ("white" or "black")
    lateinit var zobristHash : ZobristHash //Zobrist hash generator for position identification

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
     * Cache of previously evaluated positions.
     * Key: Zobrist hash of position
     * Value: Best move and evaluation for that position
     */
    val transpositionTable = Hashtable<ULong, MinimaxResult>()

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
    fun calcMove(bitboard: Bitboard) : Movement{
        movementCounter = 0
        zobristHash = ZobristHash(bitboard.figureMap.keys.toList())
        return alphabeta(bitboard, recursionDepth, Int.MIN_VALUE, Int.MAX_VALUE).movement
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
    fun alphabeta(bitboard: Bitboard, level: Int, alpha: Int, beta: Int): MinimaxResult {
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

    /**
     * Finds the best move for black (maximizing player)
     */
    private fun findBestMoveForBlack(
        bitboard: Bitboard,
        moves: List<Movement>,
        level: Int,
        alpha: Int,
        beta: Int
    ): MinimaxResult {
        var bestValue = Int.MIN_VALUE
        var bestMove = emptyMovement()
        var alphaCurrent = alpha

        val orderedMoves = orderMoves(moves, bitboard)

        for (move in orderedMoves) {
            val value = evaluateMove(bitboard, move, level, alphaCurrent, beta)

            if (value > bestValue) {
                bestValue = value
                bestMove = move
            }

            if (value >= beta) break  // Beta cutoff
            alphaCurrent = maxOf(alphaCurrent, value)
        }

        return MinimaxResult(bestMove, bestValue)
    }

    /**
     * Finds the best move for white (minimizing player)
     */
    private fun findBestMoveForWhite(
        bitboard: Bitboard,
        moves: List<Movement>,
        level: Int,
        alpha: Int,
        beta: Int
    ): MinimaxResult {
        var bestValue = Int.MAX_VALUE
        var bestMove = emptyMovement()
        var betaCurrent = beta

        val orderedMoves = orderMoves(moves, bitboard)

        for (move in orderedMoves) {
            val value = evaluateMove(bitboard, move, level, alpha, betaCurrent)

            if (value < bestValue) {
                bestValue = value
                bestMove = move
            }

            if (value <= alpha) break  // Alpha cutoff
            betaCurrent = minOf(betaCurrent, value)
        }

        return MinimaxResult(bestMove, bestValue)
    }

    /**
     * Evaluates a single move by either looking it up in the transposition table
     * or calculating it recursively
     */
    private fun evaluateMove(
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
    private fun getPositionValue(
        bitboard: Bitboard,
        move: Movement,
        level: Int,
        alpha: Int,
        beta: Int
    ): Int {
        val hash = zobristHash.generateHash(bitboard)

        if (transpositionTable.contains(hash)) {
            transpositionTableHits++
            return transpositionTable[hash]!!.value
        }

        val value = alphabeta(bitboard, level - 1, alpha, beta).value
        transpositionTable[hash] = MinimaxResult(move, value)
        return value
    }

    private fun getPointDifBW(bitboard: Bitboard) : Int{
        ++movementCounter
        return bitboard.pointsBlack() - bitboard.pointsWhite()
    }

}