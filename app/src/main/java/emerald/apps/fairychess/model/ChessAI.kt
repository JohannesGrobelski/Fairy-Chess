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
     * Implements the alpha-beta pruning algorithm to find the best move.
     *
     * The algorithm works by:
     * 1. If at max depth, evaluate current position
     * 2. Get all legal moves
     * 3. For each move:
     *    - Make the move
     *    - Check transposition table for known evaluation
     *    - If not found, recursively evaluate position
     *    - Store evaluation in transposition table
     *    - Unmake the move
     *    - Update best move if better than current best
     *    - Prune if possible (alpha/beta cutoff)
     *
     * @param bitboard The current chess position
     * @param level Current search depth remaining
     * @param alpha Best score that maximizing player can guarantee
     * @param beta Best score that minimizing player can guarantee
     * @return The best move and its evaluation
     */
    fun alphabeta(bitboard: Bitboard, level: Int, alpha:Int, beta:Int) : MinimaxResult{
        var _alpha = alpha
        var _beta = beta
        if(level <= 0){
            return MinimaxResult(emptyMovement(),getPointDifBW(bitboard))
        } else {
            val allMovesList = bitboard.getAllPossibleMovesAsList(bitboard.moveColor)
            if(allMovesList.isNotEmpty()){
                var targetMove = emptyMovement()
                var bestValue : Int
                if(bitboard.moveColor == "black"){
                    //find best move (highest points for black) by going through all possible moves
                    bestValue = Int.MIN_VALUE
                    for(move in allMovesList){
                        val copyBitboard = bitboard.clone()
                        bitboard.move(bitboard.moveColor,move)
                        var valuePosition = 0
                        if(transpositionTable.contains(zobristHash.generateHash(bitboard))) {
                            ++transpositionTableHits
                            valuePosition = transpositionTable[zobristHash.generateHash(bitboard)]!!.value
                        } else {
                            valuePosition = alphabeta(bitboard, level - 1, _alpha, _beta).value
                            transpositionTable[zobristHash.generateHash(bitboard)] = MinimaxResult(move,valuePosition)
                        }
                        if(valuePosition > bestValue){
                            targetMove = move
                            bestValue = valuePosition
                        }
                        bitboard.set(copyBitboard)
                        //beta cutoff
                        if(valuePosition >= _beta)break
                        _alpha = max(_alpha,valuePosition)
                    }
                } else {
                    //find best move (highest points for white) by going through all possible moves
                    bestValue = Int.MAX_VALUE
                    for(i in allMovesList.indices){
                        val move = allMovesList[i]
                        val copyBitboard = bitboard.clone()
                        bitboard.move(bitboard.moveColor,move)
                        var valuePosition = 0
                        if(transpositionTable.contains(zobristHash.generateHash(bitboard))) {
                            ++transpositionTableHits
                            valuePosition = transpositionTable[zobristHash.generateHash(bitboard)]!!.value
                        } else {
                            valuePosition = alphabeta(bitboard, level - 1, _alpha, _beta).value
                            transpositionTable[zobristHash.generateHash(bitboard)] = MinimaxResult(move,valuePosition)
                        }
                        if(valuePosition < bestValue){
                            targetMove = move
                            bestValue = valuePosition
                        }
                        bitboard.set(copyBitboard)
                        //alpha cutoff
                        if(valuePosition <= _alpha)break
                        _beta = min(_beta,valuePosition)
                    }
                }
                return MinimaxResult(targetMove,bestValue)
            } else {
                return MinimaxResult(emptyMovement(),getPointDifBW(bitboard))
            }
        }
    }

    private fun getPointDifBW(bitboard: Bitboard) : Int{
        ++movementCounter
        return bitboard.pointsBlack() - bitboard.pointsWhite()
    }

}