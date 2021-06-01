package emerald.apps.fairychess

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import emerald.apps.fairychess.model.ChessPiece
import emerald.apps.fairychess.model.Chessboard
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ChessGameTest {
    @Test
    fun simpleCheckmate() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("emerald.apps.fairychess", appContext.packageName)

        val chessFormationArray: Array<Array<String>> = ChessFormationParser.parseChessFormation(appContext,"normal_chess")
        val figureMap : Map<String, FigureParser.Figure> = FigureParser.parseFigureMapFromFile(appContext)

        val chessBoardNormal = Chessboard(chessFormationArray,figureMap)
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("5_1_5_2"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("4_6_4_5"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("6_1_6_3"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("3_7_7_3"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("6_0_7_2"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("7_3_4_0"))

        assertEquals("black",chessBoardNormal.gameWinner)
    }


}