package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.Movement.Companion.emptyMovement
import java.util.*
import kotlin.math.max
import kotlin.math.min

class ChessAI {
    //Settings
    private val algorithm = "alphabeta"
    private val recursionDepth = 2
    private var cntHashHits = 0
    private var cntHashFails = 0

    //Fields
    var cnt_movements = 0

    var color: String

    constructor(color: String) {
        this.color = color
    }

    //Fields for move ordering
    private val transpositionTable = Hashtable<String, MovementValue>()

    fun calcMove(bitboard: Bitboard) : Movement{
        cnt_movements = 0
        when (algorithm) {
            "alphabeta" -> {
                return alphabeta(bitboard, 4, Int.MIN_VALUE, Int.MAX_VALUE).movement
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
        var _alpha = alpha
        var _beta = beta
        if(level <= 0){
            return MinimaxResult(emptyMovement(),getPointDif(bitboard))
        } else {
            val allMovesList = bitboard.getAllPossibleMovesAsList(bitboard.moveColor)
            if(allMovesList.isNotEmpty()){
                var targetMove = emptyMovement()
                var bestValue : Int
                if(bitboard.moveColor == "black"){
                    //find best move (highest points for black) by going through all possible moves
                    bestValue = Int.MIN_VALUE
                    for(move in allMovesList){
                        bitboard.move(bitboard.moveColor,move)
                        bitboard.undoMove(bitboard.moveColor,move)
                        val valuePosition = alphabeta(bitboard, level - 1, _alpha, _beta).value
                        if(valuePosition > bestValue){
                            targetMove = move
                            bestValue = getPointDif(bitboard)
                        }
                        //beta cutoff
                        if(valuePosition >= _beta)break
                        _alpha = max(_alpha,valuePosition)
                    }
                } else {
                    //find best move (highest points for white) by going through all possible moves
                    bestValue = Int.MAX_VALUE
                    for(move in allMovesList){
                        bitboard.move(bitboard.moveColor,move)
                        bitboard.undoMove(bitboard.moveColor,move)
                        val valuePosition = alphabeta(bitboard, level - 1, _alpha, _beta).value
                        if(valuePosition < bestValue){
                            targetMove = move
                            bestValue = getPointDif(bitboard)
                        }
                        //alpha cutoff
                        if(valuePosition <= _alpha)break
                        _beta = min(_beta,valuePosition)
                    }
                }
                return MinimaxResult(targetMove,bestValue)
            } else {
                return MinimaxResult(emptyMovement(),getPointDif(bitboard))
            }
        }
    }

    fun getPointDif(bitboard: Bitboard) : Int{
        ++cnt_movements
        return bitboard.pointsBlack() - bitboard.pointsWhite()
    }


    class MovementValue(val movement: Movement?, val value: Int)
}