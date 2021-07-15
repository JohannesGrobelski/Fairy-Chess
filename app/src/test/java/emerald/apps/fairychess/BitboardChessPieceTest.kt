package emerald.apps.fairychess

import emerald.apps.fairychess.model.Bitboard
import emerald.apps.fairychess.model.Bitboard.Companion.generate64BPositionFromCoordinateList
import emerald.apps.fairychess.model.Bitboard.Companion.generate64BPositionFromCoordinates
import emerald.apps.fairychess.model.Bitboard.Companion.getMovementNotation
import emerald.apps.fairychess.model.BitboardChessPiece.Companion.longToString
import emerald.apps.fairychess.model.ChessGameUnitTest
import junit.framework.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class BitboardChessPieceTest {

    val chessFormationArray = ChessGameUnitTest.parseChessFormation("normal_chess")
    val figureMap = ChessGameUnitTest.parseFigureMapFromFile()


    @Test
    fun testPawnMovementGeneration(){
        for(testrun in 0..100){
            val colors =  arrayOf("white","black")
            val randomColor = (Math.random()).toInt() //0 = white, 1 = black
            val randomFile = (Math.random()*8).toInt()
            val randomRank = (Math.random()*8).toInt()
            val movementList = mutableListOf<Bitboard.Companion.Coordinate>()
            if((-1.0).pow(randomColor.toDouble()).toInt() + randomFile in 0..7)movementList.add(Bitboard.Companion.Coordinate(randomFile+randomColor,randomRank))
            if((-1.0).pow(randomColor.toDouble()).toInt()*2 + randomFile in 0..7)movementList.add(Bitboard.Companion.Coordinate(randomFile+(randomColor*2),randomRank))
            if(randomRank+1 in 0..7)movementList.add(Bitboard.Companion.Coordinate(randomFile,randomRank+1))
            if(randomRank-1 in 0..7)movementList.add(Bitboard.Companion.Coordinate(randomFile,randomRank-1))

            testFigure("pawn",colors[randomColor],
                Bitboard.Companion.Coordinate(randomFile,randomRank),movementList)
        }
    }

    @Test
    fun test() {
        println(longToString(2uL))
    }

    @Test
    fun testRiderMovementGeneration(){
        testFigure("pawn","white", Bitboard.Companion.Coordinate(1, 1),
            listOf(
                Bitboard.Companion.Coordinate(2,1),
                Bitboard.Companion.Coordinate(3,1),
                Bitboard.Companion.Coordinate(2,2),
                Bitboard.Companion.Coordinate(2,0)
            )
        )
        testFigure("knight","white", Bitboard.Companion.Coordinate(0,1),
            listOf(
                Bitboard.Companion.Coordinate(2,2),
                Bitboard.Companion.Coordinate(2,0),
                Bitboard.Companion.Coordinate(1,3)
            )
        )
        testFigure("knight","white", Bitboard.Companion.Coordinate(4,4),
            listOf(
                Bitboard.Companion.Coordinate(5,6),
                Bitboard.Companion.Coordinate(5,2),
                Bitboard.Companion.Coordinate(3,6),
                Bitboard.Companion.Coordinate(3,2),
                Bitboard.Companion.Coordinate(6,5),
                Bitboard.Companion.Coordinate(6,3),
                Bitboard.Companion.Coordinate(2,5),
                Bitboard.Companion.Coordinate(2,3),
            )
        )
        testFigure("rook","white", Bitboard.Companion.Coordinate(4, 4),
            listOf(
                Bitboard.Companion.Coordinate(2,1),
                Bitboard.Companion.Coordinate(3,1),
                Bitboard.Companion.Coordinate(2,2),
                Bitboard.Companion.Coordinate(2,0)
            )
        )
    }

    fun testFigure(name: String, color: String, position: Bitboard.Companion.Coordinate, movement: List<Bitboard.Companion.Coordinate>){
        val bbPosition = generate64BPositionFromCoordinates(position.file,position.rank)
        val movementNotationList = getMovementNotation(figureMap[name]!!.movementParlett)
        val figure = BitboardChessPiece(bbPosition,position.file,position.rank,color,movementNotationList)
        assertEquals(
            generate64BPositionFromCoordinateList(
                movement
            ),
            figure.generateNonRelativeMovements()
        )
    }
}