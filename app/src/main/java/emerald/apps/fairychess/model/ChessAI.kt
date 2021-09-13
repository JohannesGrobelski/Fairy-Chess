package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.Evaluator.Companion.scoreBlack
import emerald.apps.fairychess.model.Evaluator.Companion.scoreWhite
import emerald.apps.fairychess.model.Movement.Companion.emptyMovement
import kotlin.collections.HashMap
import kotlin.math.max
import kotlin.math.min

@ExperimentalUnsignedTypes
class ChessAI {
    companion object {
        private enum class ALGORITHMS {
           ALPHA_BETA, ITERATIVE_DEEPENING
        }
    }
    //Settings
    private val algorithm = ALGORITHMS.ITERATIVE_DEEPENING
    private var maxDistance = 100
    private var searchtimeMS = 1000

    //DEBUG
    var moveCounter = 0
    var transpositionTableHits = 0
    var transpositionTableFails = 0

    //fields
    var color: String

    //helper-fields
    lateinit var zobristHash : ZobristHash
    //one transpositionTable for each distance, a map from zobristHash(Bitboard) -> value(Bitboard)
    val transpositionTables : HashMap<Int, MutableMap<ULong,Int>> = hashMapOf()

    constructor(color: String) {
        this.color = color
    }

    constructor(color: String, maxDistance : Int) {
        this.color = color
        this.maxDistance = maxDistance
    }

    //Fields for move ordering

    fun calcMove(bitboard: Bitboard) : Movement{
        moveCounter = 0
        zobristHash = ZobristHash(bitboard.figureMap.keys.toMutableList())
        when (algorithm) {
            ALGORITHMS.ITERATIVE_DEEPENING -> {
                return iterativeDeepening(bitboard).movement
            }
        }
        return Movement(sourceRank = -1,sourceFile = -1,targetRank = -1,targetFile = -1)
    }

    fun getPromotion() : String {
        //TODO: calculate best figure
        return "queen"
    }


    class MinimaxResult(val movement: Movement, val value: Int)

    fun iterativeDeepening(bitboard: Bitboard) : MinimaxResult {
        var distance = 1
        var outOfTime = false
        var bestmove = MinimaxResult(emptyMovement(), Int.MIN_VALUE)
        val endTimeMS = System.currentTimeMillis() + searchtimeMS
        while (distance < maxDistance && !outOfTime) {
            bestmove = alphabeta(bitboard, distance, Int.MIN_VALUE, Int.MAX_VALUE, endTimeMS)
            distance++
            outOfTime = System.currentTimeMillis() >= endTimeMS
        }
        return bestmove
    }

    fun alphabeta(bitboard: Bitboard, distance: Int, alpha:Int, beta:Int, endtimeMS : Long) : MinimaxResult{
        var newAlpha = alpha
        var newBeta = beta
        if(distance <= 0){
            return MinimaxResult(emptyMovement(),getPointDifBW(bitboard))
        } else {
            val equalMoves = mutableListOf<Movement>()
            var bestValue : Int
            val allMovesList = bitboard.getAllPossibleMovesAsList(bitboard.moveColor)
            if(allMovesList.isNotEmpty()){
                if(bitboard.moveColor == "black"){
                    bestValue = Int.MIN_VALUE //find best move (max(getPointDifBW) for black)
                    for(move in allMovesList){
                        val copyBitboard = bitboard.clone()
                        bitboard.move(bitboard.moveColor,move); ++moveCounter
                        val valuePosition = getValueOfPosition(bitboard, distance, newAlpha, newBeta, endtimeMS)
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
                        bitboard.move(bitboard.moveColor,move); ++moveCounter
                        val valuePosition = getValueOfPosition(bitboard, distance, newAlpha, newBeta, endtimeMS)
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
                return heuristic(equalMoves,bestValue)
            } else {
                return MinimaxResult(emptyMovement(),getPointDifBW(bitboard))
            }
        }
    }

    fun getValueOfPosition(bitboard: Bitboard, distance: Int, _alpha: Int, _beta: Int, endtimeMS : Long) : Int{
        var valuePosition: Int
        val hash = zobristHash.generateHash(bitboard)
        if(!transpositionTables.containsKey(distance))transpositionTables[distance] = mutableMapOf()
        //search in levels above and equals in transpositionTable saved for value
        for(searchLevel in distance..maxDistance){
            if(!transpositionTables.containsKey(searchLevel))break
            if(transpositionTables[searchLevel]!!.keys.contains(hash)){
                ++transpositionTableHits
                return transpositionTables[searchLevel]!![hash]!!
            }
        }
        //transposition not found -> calculate value via alpha-beta
        ++transpositionTableFails
        valuePosition =  alphabeta(bitboard, distance - 1, _alpha, _beta, endtimeMS).value
        transpositionTables[distance]!![hash] = valuePosition
        return valuePosition
    }

    /** chooses a move from equal moves (point-wise) with the help of different heuristics */
    fun heuristic(equalMoves: List<Movement>, value: Int): MinimaxResult {
        return MinimaxResult(equalMoves[0],value)
    }

    fun getPointDifBW(bitboard: Bitboard) : Int{
        return scoreBlack(bitboard) - scoreWhite(bitboard)
    }
}