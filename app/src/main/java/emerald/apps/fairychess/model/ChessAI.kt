package emerald.apps.fairychess.model

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



    fun calcMove(chessboard: Chessboard) : ChessPiece.Movement?{
        cnt_movements = 0
        when (algorithm) {
            "random" -> {
                return calcRandomMove(chessboard)
            }
            "nextBestMoveAlgorithm" -> {
                return nextBestMoveAlgorithm(chessboard)
            }
            "minimax" -> {
                return minimax(chessboard,2)!!.movement
            }
            "alphabeta" -> {
                return alphabeta(chessboard,2,Int.MIN_VALUE,Int.MAX_VALUE)!!.movement
            }
        }
        return ChessPiece.Movement(sourceRank = 0,sourceFile = 0,targetRank = 0,targetFile = 0)
    }

    fun nextBestMoveAlgorithm(chessboard: Chessboard) : ChessPiece.Movement?{
        val allMoves = chessboard.getAllPossibleMoves(color)
        if(allMoves.isNotEmpty()){
            var targetMove = allMoves[0]
            val originalValue = getPointDif(chessboard)
            val originalBoard = chessboard.clone()
            var maxValue = originalValue
            //find best move (highest points for black)
            //go through all possible moves
            for(possibleMove in allMoves){
                chessboard.reset(originalBoard)
                //chessboard.moveColor = color
                chessboard.move(color,possibleMove)
                if(getPointDif(chessboard) > maxValue){
                    targetMove = possibleMove
                    maxValue = getPointDif(chessboard)
                }
            }
            chessboard.reset(originalBoard)
            return targetMove
        } else {
            return null
        }
    }

    class MinimaxResult(val movement: ChessPiece.Movement?, val value: Int)
    fun minimax(chessboard: Chessboard, level: Int) : MinimaxResult?{
        if(level <= 0){
            return MinimaxResult(ChessPiece.Movement.emptyMovement(),getPointDif(chessboard))
        } else {
            val allMoves = chessboard.getAllPossibleMoves(chessboard.moveColor)
            if(allMoves.isNotEmpty()){
                var targetMove = allMoves[0]
                val originalBoard = chessboard.clone()
                var bestValue : Int
                if(chessboard.moveColor == "black"){
                    //find best move (highest points for black) by going through all possible moves
                    bestValue = Int.MIN_VALUE
                    for(possibleMove in allMoves){
                        chessboard.reset(originalBoard)
                        chessboard.move(chessboard.moveColor,possibleMove)
                        val valuePosition = minimax(chessboard,level-1)!!.value
                        if(valuePosition > bestValue){
                            targetMove = possibleMove
                            bestValue = getPointDif(chessboard)
                        }
                    }
                } else {
                    //find best move (highest points for white) by going through all possible moves
                    bestValue = Int.MAX_VALUE
                    for(possibleMove in allMoves){
                        chessboard.reset(originalBoard)
                        chessboard.move(chessboard.moveColor,possibleMove)
                        val valuePosition = minimax(chessboard,level-1)!!.value
                        if(valuePosition < bestValue){
                            targetMove = possibleMove
                            bestValue = getPointDif(chessboard)
                        }
                    }
                }
                chessboard.reset(originalBoard)
                return MinimaxResult(targetMove,bestValue)
            } else {
                return MinimaxResult(ChessPiece.Movement.emptyMovement(),getPointDif(chessboard))
            }
        }
    }

    fun alphabeta(chessboard: Chessboard, level: Int, alpha:Int, beta:Int) : MinimaxResult?{
        var _alpha = alpha
        var _beta = beta
        if(level <= 0){
            return MinimaxResult(ChessPiece.Movement.emptyMovement(),getPointDif(chessboard))
        } else {
            val allMoves = chessboard.getAllPossibleMoves(chessboard.moveColor)
            if(allMoves.isNotEmpty()){
                var targetMove = allMoves[0]
                val originalBoard = chessboard.clone()
                var bestValue : Int
                if(chessboard.moveColor == "black"){
                    //find best move (highest points for black) by going through all possible moves
                    bestValue = Int.MIN_VALUE
                    for(possibleMove in allMoves){
                        chessboard.reset(originalBoard)
                        chessboard.move(chessboard.moveColor,possibleMove)
                        val valuePosition = alphabeta(chessboard,level-1,_alpha,_beta)!!.value
                        if(valuePosition > bestValue){
                            targetMove = possibleMove
                            bestValue = getPointDif(chessboard)
                        }
                        //beta cutoff
                        if(valuePosition >= _beta)break
                        _alpha = max(_alpha,valuePosition)
                    }
                } else {
                    //find best move (highest points for white) by going through all possible moves
                    bestValue = Int.MAX_VALUE
                    for(possibleMove in allMoves){
                        chessboard.reset(originalBoard)
                        chessboard.move(chessboard.moveColor,possibleMove)
                        val valuePosition = alphabeta(chessboard,level-1,_alpha,_beta)!!.value
                        if(valuePosition < bestValue){
                            targetMove = possibleMove
                            bestValue = getPointDif(chessboard)
                        }
                        //alpha cutoff
                        if(valuePosition <= _alpha)break
                        _beta = min(_beta,valuePosition)
                    }
                }
                chessboard.reset(originalBoard)
                return MinimaxResult(targetMove,bestValue)
            } else {
                return MinimaxResult(ChessPiece.Movement.emptyMovement(),getPointDif(chessboard))
            }
        }
    }


    fun getPointDif(chessboard: Chessboard) : Int{
        ++cnt_movements
        return chessboard.pointsBlack() - chessboard.pointsWhite()
    }



    fun calcRandomMove(chessboard: Chessboard) : ChessPiece.Movement{
        val allMoves = chessboard.getAllPossibleMoves(color)
        return allMoves[(Math.random()*allMoves.size).toInt()]
    }



    class MovementValue(val movement: ChessPiece.Movement?, val value: Int)

}