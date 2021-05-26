package emerald.apps.fairychess.model

import kotlin.math.pow


class ChessRatingSystem {

    companion object {
        const val WINNING_SCORE = 1
        const val LOSING_SCORE = 0
        const val DRAW_SCORE = 0.5

        // Function to calculate the Probability
        fun Probability(rating1: Double, rating2: Double): Double {
            return 1.0 * 1.0 / (1 + 1.0 *
                    10.0.pow(1.0 * (rating1 - rating2) / 400))
        }

        /** Function to calculate Elo rating
        * K is a constant.
        * d determines whether Player A wins or Player B. */
        fun EloRating(ratingPlayerA: Double, ratingPlayerB: Double, K: Int, playerAWon: Boolean) : Array<Double>{

            // To calculate the Winning
            // Probability of Player B
            var newRatingPlayerA = ratingPlayerA
            var newRatingPlayerB = ratingPlayerB
            val probabilityPlayerB = Probability(newRatingPlayerA, newRatingPlayerB).toFloat()

            // To calculate the Winning
            // Probability of Player A
            val probabilityPlayerA = Probability(newRatingPlayerB, newRatingPlayerA).toFloat()

            // Case -1 When Player A wins
            // Updating the Elo Ratings
            if (playerAWon) {
                newRatingPlayerA += K * (WINNING_SCORE - probabilityPlayerA)
                newRatingPlayerB += K * (LOSING_SCORE - probabilityPlayerB)
            } else {
                newRatingPlayerA += K * (LOSING_SCORE - probabilityPlayerA)
                newRatingPlayerB += K * (WINNING_SCORE - probabilityPlayerB)
            }
            return arrayOf(newRatingPlayerA,newRatingPlayerB)
        }

        /** udpate playerstats (ELO,gamesLost,gamesWon,gamesPlayed)*/
        fun updatePlayerStats(playerAStats : MultiplayerDB.PlayerStats, playerBStats: MultiplayerDB.PlayerStats, playerAWon : Boolean?){
            ++playerAStats.games_played
            ++playerBStats.games_played
            if(playerAWon != null){
                val resultRatings = EloRating(playerAStats.ELO,playerBStats.ELO,30,playerAWon)
                playerAStats.ELO = resultRatings[0]
                playerBStats.ELO = resultRatings[1]
                if(playerAWon){
                    ++playerAStats.games_won
                    ++playerBStats.games_lost
                } else {
                    ++playerBStats.games_won
                    ++playerAStats.games_lost
                }
            }
        }
    }


}