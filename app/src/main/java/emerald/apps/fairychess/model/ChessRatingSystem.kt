package emerald.apps.fairychess.model

import kotlin.math.pow


class ChessRatingSystem {

    companion object {
        const val WINNING_SCORE = 1
        const val LOSING_SCORE = 0
        const val DRAW_SCORE = 0.5

        // Function to calculate the Probability
        fun Probability(rating1: Int, rating2: Int): Double {
            return 1.0 * 1.0 / (1 + 1.0 *
                    10.0.pow(1.0 * (rating1 - rating2) / 400))
        }

        // Function to calculate Elo rating
        // K is a constant.
        // d determines whether Player A wins or Player B.
        fun EloRating(ratingPlayerA: Double, ratingPlayerB: Double, K: Int, playerAWon: Boolean) : Array<Double>{

            // To calculate the Winning
            // Probability of Player B
            var newRatingPlayerA = ratingPlayerA
            var newRatingPlayerB = ratingPlayerB
            val probabilityPlayerB = Probability(newRatingPlayerA.toInt(), newRatingPlayerB.toInt()).toFloat()

            // To calculate the Winning
            // Probability of Player A
            val probabilityPlayerA = Probability(newRatingPlayerB.toInt(), newRatingPlayerA.toInt()).toFloat()

            // Case -1 When Player A wins
            // Updating the Elo Ratings
            if (playerAWon) {
                newRatingPlayerA += K * (1 - probabilityPlayerA)
                newRatingPlayerB += K * (0 - probabilityPlayerB)
            } else {
                newRatingPlayerA += K * (0 - probabilityPlayerA)
                newRatingPlayerB += K * (1 - probabilityPlayerB)
            }
            return arrayOf(newRatingPlayerA,newRatingPlayerB)
        }

        fun updatePlayerStats(playerAStats : MultiplayerDB.PlayerStats, playerBstats: MultiplayerDB.PlayerStats, playerAWon : Boolean){
            val resultRatings = EloRating(playerAStats.ELO,playerBstats.ELO,30,playerAWon)
            playerAStats.ELO = resultRatings[0]
            playerBstats.ELO = resultRatings[1]
        }
    }


}