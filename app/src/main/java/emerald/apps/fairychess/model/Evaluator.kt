package emerald.apps.fairychess.model

/** evaluates transpositions in bitboard*/
class Evaluator {

    @ExperimentalUnsignedTypes
    companion object {

        /** chooses a move from equal moves (point-wise) with the help of different heuristics */
        fun heuristic(bitboard: Bitboard, equalMoves: List<Movement>, value: Double): ChessAI.MinimaxResult {
            /*val copyBitboard = bitboard.clone()
            val bestMove = ChessAI.MinimaxResult(equalMoves[0],value)
            for(move in equalMoves){
                assert(bitboard.move(bitboard.moveColor,move) == "")

                bitboard.set(copyBitboard)
            }*/
            return ChessAI.MinimaxResult(equalMoves[0],value)
        }

        /** evaluate position in bitboard */
        fun evaluate(bitboard: Bitboard) : Double {
            var pointScore = (scoreBlack(bitboard) - scoreWhite(bitboard)).toDouble()

            //important: value of positional terms shoud always be < 1 (value of pawn, the cheapest piece)
            pointScore += evaluatePawnStructure(bitboard) * 0.5

            return pointScore
        }

        fun evaluatePawnStructure(bitboard: Bitboard) : Double {
            val doubledPawnsDif = 0
            return 0.0
        }

        fun getDoubledPawnDif(bitboard: Bitboard) : Int {
            val whitePawns = bitboard.bbFigures["pawn"]!![0]
            val blackPawns = bitboard.bbFigures["pawn"]!![1]
            var doubledPawnsWhite = 0
            var doubledPawnsBlack = 0
            for(color in 0..1){
                for(rank in 1..6){
                    var lastPawnFileWhite = 0
                    var lastPawnFileBlack = 0
                    for(file in 0..7){

                    }
                }
            }

        }

        fun evaluateMobility(bitboard: Bitboard) : Double {
            return 0.0
        }

        /** calculate all points of black player */
        fun scoreBlack(bitboard: Bitboard): Int {
            var points = 0
            for(name in bitboard.bbFigures.keys){
                points +=  bitboard.figureMap?.get(name)?.value?.times(countSetBits(bitboard.bbFigures[name]?.get(1)!!)) ?: 0
            }
            return points
        }

        /** calculate all points of white player */
        fun scoreWhite(bitboard: Bitboard): Int {
            var points = 0
            for(name in bitboard.bbFigures.keys){
                points += bitboard.figureMap?.get(name)?.value?.times(countSetBits(bitboard.bbFigures[name]?.get(0)!!)) ?: 0
            }
            return points
        }


        /** recursive function to count set bits */
        private fun countSetBits(n: ULong): Int {
            // base case
            return if (n == 0uL) 0
            else 1 + countSetBits(n and n - 1uL)
        }
    }
}