package emerald.apps.fairychess

import emerald.apps.fairychess.model.Bitboard
import emerald.apps.fairychess.model.Bitboard.Companion.add64BPositionFromCoordinates
import emerald.apps.fairychess.model.Bitboard.Companion.generate64BPositionFromCoordinates
import emerald.apps.fairychess.model.Bitboard.Companion.generateCoordinatesFrom64BPosition
import emerald.apps.fairychess.model.BitboardChessPiece
import emerald.apps.fairychess.model.ChessGameUnitTest
import junit.framework.Assert.assertEquals
import org.junit.Test

class BitboardChessPieceTest {

    val chessFormationArray = ChessGameUnitTest.parseChessFormation("normal_chess")
    val figureMap = ChessGameUnitTest.parseFigureMapFromFile()

    @Test
    fun testLeaperMovementGeneration(){
        val bbFigure = Bitboard.generate64BPositionFromCoordinates(1,1)
        val movementNotation = ""
        val bitboardChessPiece = BitboardChessPiece("white",1,1,bbFigure, figureMap["pawn"]!!.movementParlett)
        assertEquals(
            add64BPositionFromCoordinates(
                generate64BPositionFromCoordinates(
                    2,1
                ), 3,1
            ),
            bitboardChessPiece.generateNonRelativeMovements()
        )
    }
}