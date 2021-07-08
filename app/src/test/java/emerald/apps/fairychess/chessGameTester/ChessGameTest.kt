package emerald.apps.fairychess.chessGameTester

import androidx.test.ext.junit.runners.AndroidJUnit4
import emerald.apps.fairychess.model.ChessPiece
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.apache.tools.ant.taskdefs.Move
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Play games specified in gamesdb.
 */
@RunWith(AndroidJUnit4::class)
class ChessboardUnitTest {
    private lateinit var games : Array<Array<ChessPiece.Movement>>

    @Before
    fun parseGamesDB(){}

    @Test
    fun testGamesDB(){
       println(games.size)
    }
}