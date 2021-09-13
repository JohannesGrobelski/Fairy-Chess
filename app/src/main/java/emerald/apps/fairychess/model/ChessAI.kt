package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.Evaluator.Companion.evaluate
import emerald.apps.fairychess.model.Evaluator.Companion.heuristic
import emerald.apps.fairychess.model.Movement.Companion.emptyMovement
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
    private var maxDistance = 4
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
    val transpositionTables : HashMap<Int, MutableMap<ULong,Double>> = hashMapOf()

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


    class MinimaxResult(val movement: Movement, val value: Double)

    fun iterativeDeepening(bitboard: Bitboard) : MinimaxResult {
        var distance = 1
        var outOfTime = false
        var bestmove = MinimaxResult(emptyMovement(), -Double.MAX_VALUE)
        val endTimeMS = System.currentTimeMillis() + searchtimeMS
        while (distance < maxDistance && !outOfTime) {
            bestmove = alphabeta(bitboard, distance, -Double.MAX_VALUE, Double.MAX_VALUE, endTimeMS)
            distance++
            outOfTime = System.currentTimeMillis() >= endTimeMS
        }
        return bestmove
    }

    fun alphabeta(bitboard: Bitboard, distance: Int, alpha:Double, beta:Double, endtimeMS : Long) : MinimaxResult{
        var newAlpha : Double = alpha
        var newBeta : Double = beta
        if(distance <= 0){
            return MinimaxResult(emptyMovement(),evaluate(bitboard))
        } else {
            val equalMoves = mutableListOf<Movement>()
            var bestValue : Double
            val allMovesList = bitboard.getAllPossibleMovesAsList(bitboard.moveColor)
            if(allMovesList.isNotEmpty()){
                if(bitboard.moveColor == "black"){
                    bestValue = -Double.MAX_VALUE //find best move (max(getPointDifBW) for black)
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
                    bestValue = Double.MAX_VALUE //find best move (min(getPointDifBW) for white)
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
                return heuristic(bitboard,equalMoves,bestValue)
            } else {
                return MinimaxResult(emptyMovement(), evaluate(bitboard))
            }
        }
    }

    fun getValueOfPosition(bitboard: Bitboard, distance: Int, _alpha: Double, _beta: Double, endtimeMS : Long) : Double {
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
        val valuePosition: Double = alphabeta(bitboard, distance - 1, _alpha, _beta, endtimeMS).value
        transpositionTables[distance]!![hash] = valuePosition
        return valuePosition
    }

}