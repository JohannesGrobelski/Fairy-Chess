package emerald.apps.fairychess

import android.R.bool
import emerald.apps.fairychess.model.ChessRatingSystem.Companion.EloRating
import emerald.apps.fairychess.model.ChessRatingSystem.Companion.updatePlayerStats
import emerald.apps.fairychess.model.MultiplayerDB
import org.junit.Test


class ChessRatingSystemTest {

    @Test
    fun testELORating() {
        // Ra and Rb are current ELO ratings
        val Ra = 1200.0
        val Rb = 1000.0

        val K = 30
        val playerAWon = true

        var results = EloRating(Ra, Rb, K, playerAWon)
        assert(results[0] in 1207.2..1208.3)
        assert(results[1] in 992.7..993.8)
    }

    @Test
    fun testUpdatePlayerStats() {
        // Ra and Rb are current ELO ratings
        val playerStatsTom = MultiplayerDB.PlayerStats(30,15,10,1200.0)
        val playerStatsKaty = MultiplayerDB.PlayerStats(30,15,10,1000.0)

        updatePlayerStats(playerStatsTom,playerStatsKaty,true)
        assert(playerStatsTom.ELO in 1207.2..1208.3)
        assert(playerStatsKaty.ELO in 992.7..993.8)
    }
}
