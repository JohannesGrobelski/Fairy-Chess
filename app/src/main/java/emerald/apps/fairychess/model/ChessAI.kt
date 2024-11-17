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
    private val recursionDepth = 2 //Maximum depth for the alpha-beta search

    //Fields
    var movementCounter = 0 //Counter variable for number of positions evaluated
    var transpositionTableHits = 0 //Counter for successful transposition table lookups

    var color: String  //Color this AI plays as ("white" or "black")
    lateinit var zobristHash : ZobristHash //Zobrist hash generator for position identification

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

        for (move in moves) {
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

        for (move in moves) {
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

    /**
     * Assigns scores to moves for better move ordering:
     * - Capturing a higher value piece with a lower value piece scores higher
     * - Queen captures score highest
     * - Pawn captures score lowest
     *
     * @param moves List of possible moves
     * @param bitboard Current board position
     * @return Sorted list of moves, best moves first
     */
    private fun orderMoves(moves: List<Movement>, bitboard: Bitboard): List<Movement> {
        // Create pairs of move and score for sorting
        val scoredMoves = moves.map { move ->
            val targetPiece = bitboard.getPieceName(move.getTargetCoordinate())
            val attackingPiece = bitboard.getPieceName(move.getSourceCoordinate())

            // Calculate move score
            val score = when {
                // Capturing moves
                targetPiece.isNotEmpty() -> {
                    getPieceValue(targetPiece) - (getPieceValue(attackingPiece) / 10)
                }
                // Non-capturing moves score 0
                else -> 0
            }

            move to score
        }

        // Return moves sorted by score descending
        return scoredMoves.sortedByDescending { it.second }.map { it.first }
    }

    /**
     * Returns the relative value of each piece type for move ordering
     */
    private fun getPieceValue(piece: String): Int = when (piece) {
        "queen" -> 900
        "rook" -> 500
        "bishop", "knight" -> 300
        "pawn" -> 100
        else -> 0
    }

    private fun getPointDifBW(bitboard: Bitboard) : Int{
        ++movementCounter
        return bitboard.pointsBlack() - bitboard.pointsWhite()
    }

}