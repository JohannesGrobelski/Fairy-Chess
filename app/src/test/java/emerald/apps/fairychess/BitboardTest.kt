package emerald.apps.fairychess

import emerald.apps.fairychess.model.Bitboard
import emerald.apps.fairychess.model.ChessGameUnitTest.Companion.parseChessFormation
import emerald.apps.fairychess.model.ChessGameUnitTest.Companion.parseFigureMapFromFile
import junit.framework.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow


class BitboardTest {

    @Test
    fun testBitboardInit(){
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val bitboard = Bitboard(chessFormationArray,figureMap)
        assertEquals("R | K | B | Q | K | B | K | R\n" +
                "P | P | P | P | P | P | P | P\n" +
                "  |   |   |   |   |   |   |  \n" +
                "  |   |   |   |   |   |   |  \n" +
                "  |   |   |   |   |   |   |  \n" +
                "  |   |   |   |   |   |   |  \n" +
                "p | p | p | p | p | p | p | p\n" +
                "r | k | b | q | k | b | k | r\n",bitboard.toString())

        println(bitboard.pointsBlack())
        println(bitboard.pointsWhite())
    }






    @Test
    fun bitboardCoordinateTransformation(){
    //test bitboards with one figure
        assertEquals(2.0.pow(0*8 + 0).toULong(),Bitboard.generate64BPositionFromCoordinates(0,0))
        assertEquals(2.0.pow(0*8 + 1).toULong(),Bitboard.generate64BPositionFromCoordinates(0,1))
        assertEquals(2.0.pow(1*8 + 0).toULong(),Bitboard.generate64BPositionFromCoordinates(1,0))
        assertEquals(2.0.pow(1*8 + 1).toULong(),Bitboard.generate64BPositionFromCoordinates(1,1))
        assertEquals(2.0.pow(4*8 + 5).toULong(),Bitboard.generate64BPositionFromCoordinates(4,5))

        assertEquals(Bitboard.Companion.Coordinate(0,0),
            Bitboard.generateCoordinatesFrom64BPosition(
                Bitboard.generate64BPositionFromCoordinates(0,0))[0])

        assertEquals(Bitboard.Companion.Coordinate(0,1),
            Bitboard.generateCoordinatesFrom64BPosition(
                Bitboard.generate64BPositionFromCoordinates(0,1))[0])

        for(line in 0..7){
            for(row in 0..7){
                assertEquals(2.0.pow(line*8 + row).toULong(),Bitboard.generate64BPositionFromCoordinates(line,row))
                assertEquals(Bitboard.Companion.Coordinate(line,row).file,
                    Bitboard.generateCoordinatesFrom64BPosition(
                        Bitboard.generate64BPositionFromCoordinates(line,row))[0].file)
                assertEquals(Bitboard.Companion.Coordinate(line,row).rank,
                    Bitboard.generateCoordinatesFrom64BPosition(
                        Bitboard.generate64BPositionFromCoordinates(line,row))[0].rank)
            }
        }

    //test bitboards with multiple figures
        var bb = 0uL
        bb = Bitboard.add64BPositionFromCoordinates(bb,0,0)
        bb = Bitboard.add64BPositionFromCoordinates(bb,4,5)
        bb = Bitboard.add64BPositionFromCoordinates(bb,2,3)

        val coordinateList = Bitboard.generateCoordinatesFrom64BPosition(bb)
        assertEquals(listOf(
            Bitboard.Companion.Coordinate(0,0),
            Bitboard.Companion.Coordinate(2,3),
            Bitboard.Companion.Coordinate(4,5)
        ),coordinateList)


    }
}