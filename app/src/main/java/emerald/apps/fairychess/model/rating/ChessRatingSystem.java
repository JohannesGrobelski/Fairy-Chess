package emerald.apps.fairychess.model.rating;

import emerald.apps.fairychess.model.multiplayer.MultiplayerDB;
import java.lang.Math;

public final class ChessRatingSystem {
    public static final double WINNING_SCORE = 1.0;
    public static final double LOSING_SCORE = 0.0;
    public static final double DRAW_SCORE = 0.5;

    // Prevent instantiation
    private ChessRatingSystem() {}

    /**
     * Function to calculate the Probability
     */
    public static double getProbability(double rating1, double rating2) {
        return 1.0 / (1 + Math.pow(10.0, (rating1 - rating2) / 400));
    }

    /**
     * Function to calculate Elo rating
     * K is a constant.
     * d determines whether Player A wins or Player B.
     */
    public static double[] eloRating(
        double ratingPlayerA, 
        double ratingPlayerB, 
        int K, 
        boolean playerAWon
    ) {
        double newRatingPlayerA = ratingPlayerA;
        double newRatingPlayerB = ratingPlayerB;
        
        double probabilityPlayerB = getProbability(newRatingPlayerA, newRatingPlayerB);
        double probabilityPlayerA = getProbability(newRatingPlayerB, newRatingPlayerA);
        
        // Updating the Elo Ratings
        if (playerAWon) {
            newRatingPlayerA += K * (WINNING_SCORE - probabilityPlayerA);
            newRatingPlayerB += K * (LOSING_SCORE - probabilityPlayerB);
        } else {
            newRatingPlayerA += K * (LOSING_SCORE - probabilityPlayerA);
            newRatingPlayerB += K * (WINNING_SCORE - probabilityPlayerB);
        }
        
        return new double[]{newRatingPlayerA, newRatingPlayerB};
    }

    /**
     * Update player stats (ELO, gamesLost, gamesWon, gamesPlayed)
     */
    public static void updatePlayerStats(
        MultiplayerDB.PlayerStats playerAStats, 
        MultiplayerDB.PlayerStats playerBStats, 
        Boolean playerAWon
    ) {
        ++playerAStats.games_played;
        ++playerBStats.games_played;
        
        if (playerAWon != null) {
            double[] resultRatings = eloRating(playerAStats.ELO, playerBStats.ELO, 30, playerAWon);
            playerAStats.ELO = resultRatings[0];
            playerBStats.ELO = resultRatings[1];
            
            if (playerAWon) {
                ++playerAStats.games_won;
                ++playerBStats.games_lost;
            } else {
                ++playerBStats.games_won;
                ++playerAStats.games_lost;
            }
        }
    }
}
