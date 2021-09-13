package emerald.apps.fairychess.model

/** evaluates transpositions in bitboard*/
class Evaluator {

    companion object {

        /** evaluate position in bitboard */
        fun evaluate(bitboard: Bitboard, level : Int) : Int {
            var pointScore = scoreBlack(bitboard) - scoreWhite(bitboard)
            return pointScore
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