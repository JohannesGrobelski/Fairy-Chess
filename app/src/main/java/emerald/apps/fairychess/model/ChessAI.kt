package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.Movement.Companion.emptyMovement
import java.util.*
import kotlin.math.max
import kotlin.math.min

@ExperimentalUnsignedTypes
class ChessAI {
    //Settings
    private val algorithm = "alphabeta"
    private var recursionDepth = 4
    private var cntHashHits = 0
    private var cntHashFails = 0


    //DEBUG
    var cnt_movements = 0
    var transpositionTableHits = 0
    var transpositionTableFails = 0

    //fields
    var color: String

    //helper-fields
    lateinit var zobristHash : ZobristHash

    constructor(color: String) {
        this.color = color
    }

    constructor(color: String, recursionDepth : Int) {
        this.color = color
        this.recursionDepth = recursionDepth
    }

    //Fields for move ordering
    val transpositionTable = Hashtable<ULong, Int>() //map from zobristHash(Bitboard) -> value(Bitboard)

    fun calcMove(bitboard: Bitboard) : Movement{
        cnt_movements = 0
        zobristHash = ZobristHash(bitboard.figureMap.keys.toList())
        when (algorithm) {
            "alphabeta" -> {
                return alphabeta(bitboard, recursionDepth, Int.MIN_VALUE, Int.MAX_VALUE).movement
            }
        }
        return Movement(sourceRank = 0,sourceFile = 0,targetRank = 0,targetFile = 0)
    }

    fun getPromotion() : String {
        //TODO: calculate best figure
        return "queen"
    }


    class MinimaxResult(val movement: Movement, val value: Int)

    fun alphabeta(bitboard: Bitboard, level: Int, alpha:Int, beta:Int) : MinimaxResult{
        var newAlpha = alpha
        var newBeta = beta
        if(level <= 0){
            return MinimaxResult(emptyMovement(),getPointDifBW(bitboard))
        } else {
            val equalMoves = mutableListOf<Movement>()
            val allMovesList = bitboard.getAllPossibleMovesAsList(bitboard.moveColor)
            if(allMovesList.isNotEmpty()){
                var bestValue : Int
                if(bitboard.moveColor == "black"){
                    bestValue = Int.MIN_VALUE //find best move (max(getPointDifBW) for black)
                    for(move in allMovesList){
                        val copyBitboard = bitboard.clone()
                        bitboard.move(bitboard.moveColor,move); ++cnt_movements
                        val valuePosition = getValueOfPosition(bitboard, move, level, newAlpha, newBeta)
                        if(valuePosition > bestValue){
                            equalMoves.clear()
                            bestValue = valuePosition
                        }
                        if(valuePosition == bestValue)equalMoves.add(move)
                        bitboard.set(copyBitboard)
                        if(valuePosition >= newBeta)break //beta cutoff
                        newAlpha = max(newAlpha,valuePosition)
                    }
                } else {
                    bestValue = Int.MAX_VALUE //find best move (min(getPointDifBW) for white)
                    for(move in allMovesList){
                        val copyBitboard = bitboard.clone()
                        bitboard.move(bitboard.moveColor,move); ++cnt_movements
                        val valuePosition = getValueOfPosition(bitboard, move, level, newAlpha, newBeta)
                        if(valuePosition < bestValue){
                            equalMoves.clear()
                            bestValue = valuePosition
                        }
                        if(valuePosition == bestValue)equalMoves.add(move)
                        bitboard.set(copyBitboard)
                        if(valuePosition <= newAlpha)break //alpha cutoff
                        newBeta = min(newBeta,valuePosition)
                    }
                }
                return MinimaxResult(heuristic(equalMoves),bestValue)
            } else {
                return MinimaxResult(emptyMovement(),getPointDifBW(bitboard))
            }
        }
    }

    fun getValueOfPosition(bitboard: Bitboard, move: Movement, level: Int, _alpha:Int, _beta: Int) : Int{
        var valuePosition = 0
        //valuePosition = alphabeta(bitboard, level - 1, _alpha, _beta).value;
        val hash = zobristHash.generateHash(bitboard)
        valuePosition = 0
        if(transpositionTable.keys.contains(hash)){
            transpositionTable[hash]!!; ++transpositionTableHits
        } else {
            valuePosition =  alphabeta(bitboard, level - 1, _alpha, _beta).value; ++transpositionTableFails
            transpositionTable[hash] = valuePosition
        }
        return valuePosition
    }

    /** chooses a move from equal moves (point-wise) with the help of different heuristics */
    fun heuristic(equalMoves : List<Movement>) : Movement {
        val targetMovement = equalMoves[0]
        return targetMovement
    }

    fun getPointDifBW(bitboard: Bitboard) : Int{
        return bitboard.pointsBlack() - bitboard.pointsWhite()
    }

    class MovementValue(val movement: Movement?, val value: Int)
}