package emerald.apps.fairychess

import emerald.apps.fairychess.model.Bitboard
import emerald.apps.fairychess.model.Bitboard.Companion.add64BPositionFromCoordinates
import emerald.apps.fairychess.model.Bitboard.Companion.chessboardToBitboard
import emerald.apps.fairychess.model.Bitboard.Companion.generate64BPositionFromCoordinates
import emerald.apps.fairychess.model.ChessGameUnitTest.Companion.parseChessFormation
import emerald.apps.fairychess.model.ChessGameUnitTest.Companion.parseFigureMapFromFile
import emerald.apps.fairychess.model.ChessPiece
import emerald.apps.fairychess.model.Chessboard
import emerald.apps.fairychess.utility.FigureParser
import junit.framework.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow


class BitboardTest {

    @Test
    fun testBitboardInit(){
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val bitboard = Bitboard(chessFormationArray,figureMap)
        assertEquals("0 | R | K | B | Q | K | B | K | R | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "1 | P | P | P | P | P | P | P | P | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "2 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "3 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "4 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "5 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "6 | p | p | p | p | p | p | p | p | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "7 | r | k | b | q | k | b | k | r | \n" +
                "--+---+---+---+---+---+---+---+---+\n",bitboard.toString())
        println(bitboardToString(bitboard.bbComposite))
        println(bitboardToString(bitboard.bbColorComposite[0]))
        println(bitboardToString(bitboard.bbColorComposite[1]))
        println(bitboard.pointsBlack())
        println(bitboard.pointsWhite())
    }

    @Test
    fun testMovegeneration(){
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val chessboard = Chessboard(chessFormationArray,figureMap)
        val bitboard = chessboardToBitboard(chessboard)

        /*println(bitboardToString(bitboard.bbComposite))
        println(bitboardToString(bitboard.bbColorComposite[0]))
        println(bitboardToString(bitboard.bbColorComposite[1]))
        println(bitboard.toString())*/

        //println(bitboardToString((2.0).pow(17).toULong()))
        //println(bitboardToString(generate64BPositionFromCoordinates(2,1)))

        /*println(bitboardToString(131072uL))
        println(bitboardToString(327680uL))
        println(bitboardToString( 33685504uL))*/
        //println(bitboardToString( 18446462598732840960uL.inv()))


        //println(bitboardToString(bitboard.getTargetMovements("pawn","white",1, 4,true)))
        //println(bitboardToString(bitboard.getTargetMovements("queen","white",1, 7,true)))
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



    fun bitboardToString(bitboard: ULong) : String{
        val str = StringBuilder("")
        var cnt = 0
        for(file in 7 downTo 0){
            str.append(file.toString()+" | ")
            for(rank in 0..7){
                val num = 1uL shl rank shl (8*file)
                if(bitboard and num == num){
                    str.append("X")
                    str.append(" | ")
                    ++cnt
                } else {
                    str.append("  | ")
                }
            }
            str.append("\n--+---+---+---+---+---+---+---+---+\n")
        }
        return str.toString()
    }
}