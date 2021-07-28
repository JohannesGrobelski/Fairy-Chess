package emerald.apps.fairychess

import emerald.apps.fairychess.model.Bitboard
import emerald.apps.fairychess.model.Bitboard.Companion.add64BPositionFromCoordinates
import emerald.apps.fairychess.model.Bitboard.Companion.generate64BPositionFromCoordinates
import emerald.apps.fairychess.model.ChessGameUnitTest.Companion.parseChessFormation
import emerald.apps.fairychess.model.ChessGameUnitTest.Companion.parseFigureMapFromFile
import emerald.apps.fairychess.utility.FigureParser
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.math.pow
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis


class BitboardTest {

    lateinit var chessFormationArray : Array<Array<String>>
    lateinit var figureMap : Map<String, FigureParser.Figure>

    @Before
    fun initNormalChessVariables(){
        chessFormationArray = parseChessFormation("normal_chess")
        figureMap = parseFigureMapFromFile()
    }

    @Test
    fun testBitboardInit(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        var sum = 0.0
        for(i in 0..1000){
            sum += measureTimeMillis { bitboard.getAllPossibleMoves("white",false) }
        }
        println(sum / 1000)

        assertEquals("7 | r | k | b | q | k | b | k | r | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "6 | p | p | p | p | p | p | p | p | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "5 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "4 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "3 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "2 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "1 | P | P | P | P | P | P | P | P | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "0 | R | K | B | Q | K | B | K | R | \n" +
                "--+---+---+---+---+---+---+---+---+\n",bitboard.toString())
        assertEquals("7 | X | X | X | X | X | X | X | X | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "6 | X | X | X | X | X | X | X | X | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "5 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "4 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "3 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "2 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "1 | X | X | X | X | X | X | X | X | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "0 | X | X | X | X | X | X | X | X | \n" +
                "--+---+---+---+---+---+---+---+---+\n",bitboardToString(bitboard.bbComposite))
        assertEquals("7 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "6 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "5 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "4 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "3 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "2 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "1 | X | X | X | X | X | X | X | X | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "0 | X | X | X | X | X | X | X | X | \n" +
                "--+---+---+---+---+---+---+---+---+\n",bitboardToString(bitboard.bbColorComposite[0]))
        assertEquals("7 | X | X | X | X | X | X | X | X | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "6 | X | X | X | X | X | X | X | X | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "5 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "4 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "3 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "2 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "1 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "0 |   |   |   |   |   |   |   |   | \n" +
                "--+---+---+---+---+---+---+---+---+\n",bitboardToString(bitboard.bbColorComposite[1]))
        assertEquals(1039,bitboard.pointsBlack())
        assertEquals(1039,bitboard.pointsWhite())
    }

    @Test
    fun testMovegenerationKings(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //kings initial position
        assertEquals(0uL,bitboard.getTargetMovements("king", "black", 4, 7, true))
        assertEquals(0uL,bitboard.getTargetMovements("king", "white", 4, 0, true))
        //kings in middle
        assertEquals(88441966559232uL,bitboard.getTargetMovements("king", "black", 5, 5, true))
        assertEquals(31613639358152704uL,bitboard.getTargetMovements("king", "white", 5, 5, true))
        //kings on edge
        assertEquals(846636838289408uL,bitboard.getTargetMovements("king", "white", 0, 5, true))
        assertEquals(2211908157440uL,bitboard.getTargetMovements("king", "black", 0, 5, true))

        //TODO: test castling
        //small castling
        bitboard.moveFigure("knight","white",6,0,7,2)
        bitboard.moveFigure("knight","black",6,7,7,5)
        bitboard.moveFigure("pawn","white",6,1,6,2)
        bitboard.moveFigure("pawn","black",6,6,6,5)
        bitboard.moveFigure("bishop","white",5,0,6,1)
        bitboard.moveFigure("bishop","black",5,7,6,6)

        println(bitboardToString(bitboard.moveMapToComposite(bitboard.getAllPossibleMoves("white",false))))
        println(bitboard.toString())

        //println(bitboardToString(bitboard.getTargetMovements("king", "white", 4, 0, true)))

        /*println(bitboardToString(bitboard.getTargetMovements("king","black",0, 5)))
        println(bitboard.getTargetMovements("king","black",0,5))*/
    }

    @Test
    fun testMovegenerationQueens(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //queen initial position
        assertEquals(0uL,bitboard.getTargetMovements("queen", "black", 3, 7, true))
        assertEquals(0uL,bitboard.getTargetMovements("queen", "white", 3, 0, true))
        //queen in middle
        assertEquals(23706498137063424uL,bitboard.getTargetMovements("queen", "white", 4, 4, true))
        assertEquals(62600093405696uL,bitboard.getTargetMovements("queen", "black", 4, 4, true))
        //queen on edge
        assertEquals(1411764390789120uL,bitboard.getTargetMovements("queen", "white", 0, 4, true))
        assertEquals(4389507238144uL,bitboard.getTargetMovements("queen", "black", 0, 4, true))

        /*println(bitboardToString(bitboard.getTargetMovements("queen","white",0, 4)))
        println(bitboard.getTargetMovements("queen","white",0, 4))*/
    }

    @Test
    fun testMovegenerationBishops(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //bishop initial position
        assertEquals(0uL,bitboard.getTargetMovements("bishop", "black", 2, 7, true))
        assertEquals(0uL,bitboard.getTargetMovements("bishop", "white", 2, 0, true))
        //bishop in middle
        assertEquals(43981140689408uL,bitboard.getTargetMovements("bishop", "black", 4, 4, true))
        assertEquals(19184279556980736uL,bitboard.getTargetMovements("bishop", "white", 4, 4, true))
        //bishop on edge
        assertEquals(1128098963914752uL,bitboard.getTargetMovements("bishop", "white", 0, 4, true))
        assertEquals(2199057074176uL,bitboard.getTargetMovements("bishop", "black", 0, 4, true))

        /*println(bitboardToString(bitboard.getTargetMovements("bishop","black",0, 4)))
        println(bitboard.getTargetMovements("bishop","black",0, 4))*/
    }

    @Test
    fun testMovegenerationKnights(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //knight initial position
        assertEquals(327680uL,bitboard.getTargetMovements("knight", "white", 1, 0, true))
        assertEquals(5497558138880uL,bitboard.getTargetMovements("knight", "black", 1, 7, true))
        //knight in middle
        assertEquals(11333767002587136uL,bitboard.getTargetMovements("knight", "white", 4, 4, true))
        assertEquals(44272527353856uL,bitboard.getTargetMovements("knight", "black", 4, 3, true))
        //knight on edge
        assertEquals(567348067172352uL,bitboard.getTargetMovements("knight", "white", 0, 4, true))
        assertEquals(4398113751040uL,bitboard.getTargetMovements("knight", "black", 0, 4, true))

        /*println(bitboardToString(bitboard.getTargetMovements("knight","black",0, 4)))
        println(bitboard.getTargetMovements("knight","black",0, 4))*/
    }

    @Test
    fun testMovegenerationRooks(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //rook initial position
        assertEquals(0uL,bitboard.getTargetMovements("rook", "white", 0, 0, true))
        assertEquals(0uL,bitboard.getTargetMovements("rook", "black", 0, 7, true))
        //rook in middle
        assertEquals(4522218580082688uL,bitboard.getTargetMovements("rook", "white", 4, 4, true))
        assertEquals(18618952716288uL,bitboard.getTargetMovements("rook", "black", 4, 4, true))
        //rook on edge
        assertEquals(283665426874368uL,bitboard.getTargetMovements("rook", "white", 0, 4, true))
        assertEquals(2190450163968uL,bitboard.getTargetMovements("rook", "black", 0, 4, true))
        //rook in corner
        //preperation - move pawn and knight
        bitboard.moveFigure("pawn","white",0,1,0,3)
        bitboard.moveFigure("pawn","black",0,6,0,4)
        bitboard.moveFigure("knight","white",1,0,2,2)
        bitboard.moveFigure("knight","black",1,7,2,5)
        assertEquals(65794uL,bitboard.getTargetMovements("rook", "white", 0, 0, true))
        assertEquals(144397762564194304uL,bitboard.getTargetMovements("rook", "black", 0, 7, true))

       /* println(bitboardToString(bitboard.getTargetMovements("rook","black",0, 7)))
        println(bitboard.getTargetMovements("rook","black",0, 7))*/
    }

    @Test
    fun testMovegenerationPawns(){
        var bitboard = Bitboard(chessFormationArray,figureMap)

        //white pawn can capture black 2 pawns
        assertEquals(2814749767106560uL,bitboard.getTargetMovements("pawn", "white", 2, 5, true))
        //white pawn initial movement
        assertEquals(67371008uL,bitboard.getTargetMovements("pawn", "white", 2, 1, true))
        //black pawn can capture white 2 pawns
        assertEquals(2560uL,bitboard.getTargetMovements("pawn", "black", 2, 2, true))
        //black pawn initial movement
        assertEquals(4415226380288uL,bitboard.getTargetMovements("pawn", "black", 2, 6, true))

        //test capturing
        testMoveFigure()

        //TODO: test enpassante black
        bitboard = Bitboard(chessFormationArray,figureMap)
        bitboard.moveFigure("pawn","white",2,1,2,3)
        bitboard.moveFigure("pawn","black",3,6,3,4)
        bitboard.moveFigure("pawn","white",2,3,2,4)
        bitboard.moveFigure("pawn","black",1,6,1,4)

        println(bitboardToString(bitboard.getTargetMovements("pawn", "white", 2, 4, true)))
        //println(bitboardToString(moveMapToCompositeBB(bitboard.getAllPossibleMoves("white"))))
        //test enpassante white

        /*println(bitboardToString(bitboard.bbComposite))
        println(bitboardToString(bitboard.bbColorComposite[0]))
        println(bitboardToString(bitboard.bbFigures["pawn"]!![0]))*/


        //println(bitboard.getTargetMovements("pawn","white",2, 2))
        //println(bitboardToString(bitboard.getTargetMovements("pawn","white",2, 2)))


        //println(bitboardToString(bitboard.getTargetMovements("queen","white",4, 4)))
    }

    @Test
    /** test if moving figure works */
    fun testMoveFigure(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //initial pawn move
        bitboard.moveFigure("pawn","white",2,1,2,3)
        assertEquals(18446462598800014335uL,bitboard.bbComposite)
        assertEquals(67173375uL,bitboard.bbColorComposite[0])
        assertEquals(67173120uL,bitboard.bbFigures["pawn"]!![0])
        assertEquals(71776119061217280uL,bitboard.bbFigures["pawn"]!![1])
        assertEquals(67173120uL,bitboard.bbFigures["pawn"]!![0])

        //capture black pawn with white pawn
        bitboard.moveFigure("pawn","black",3,6,3,4)
        bitboard.moveFigure("pawn","white",2,3,3,4)
        assertEquals(18444210833278958591uL,bitboard.bbComposite)
        assertEquals(34359802879uL,bitboard.bbColorComposite[0])
        assertEquals(18444210798919155712uL,bitboard.bbColorComposite[1])
        assertEquals(34359802624uL,bitboard.bbFigures["pawn"]!![0])
        assertEquals(69524319247532032uL,bitboard.bbFigures["pawn"]!![1])

        /*println(bitboardToString(bitboard.bbColorComposite[1]))
        println(bitboard.bbColorComposite[1])*/
    }


    @Test
    fun testBitboardCoordinateTransformation(){
        //test bitboards with one figure
        assertEquals(2.0.pow(0*8 + 0).toULong(), generate64BPositionFromCoordinates(0,0))
        assertEquals(2.0.pow(0*8 + 1).toULong(), generate64BPositionFromCoordinates(1,0))
        assertEquals(2.0.pow(1*8 + 0).toULong(), generate64BPositionFromCoordinates(0,1))
        assertEquals(2.0.pow(1*8 + 1).toULong(), generate64BPositionFromCoordinates(1,1))
        assertEquals(2.0.pow(4*8 + 5).toULong(), generate64BPositionFromCoordinates(5,4))

        assertEquals(Bitboard.Companion.Coordinate(0,0),
            Bitboard.generateCoordinatesFrom64BPosition(
                generate64BPositionFromCoordinates(0,0)
            )[0])

        assertEquals(Bitboard.Companion.Coordinate(1,0),
            Bitboard.generateCoordinatesFrom64BPosition(
                generate64BPositionFromCoordinates(1,0)
            )[0])

        for(rank in 0..7){
            for(line in 0..7){
                assertEquals(2.0.pow(line*8 + rank).toULong(),
                    generate64BPositionFromCoordinates(rank,line)
                )
                assertEquals(Bitboard.Companion.Coordinate(rank,line).file,
                    Bitboard.generateCoordinatesFrom64BPosition(
                        generate64BPositionFromCoordinates(rank,line)
                    )[0].file)
                assertEquals(Bitboard.Companion.Coordinate(rank,line).rank,
                    Bitboard.generateCoordinatesFrom64BPosition(
                        generate64BPositionFromCoordinates(rank,line)
                    )[0].rank)
            }
        }

        //test bitboards with multiple figures
        var bb = 0uL
        bb = add64BPositionFromCoordinates(bb,0,0)
        bb = add64BPositionFromCoordinates(bb,4,5)
        bb = add64BPositionFromCoordinates(bb,2,3)

        val coordinateList = Bitboard.generateCoordinatesFrom64BPosition(bb)
        assertEquals(listOf(
            Bitboard.Companion.Coordinate(0,0),
            Bitboard.Companion.Coordinate(2,3),
            Bitboard.Companion.Coordinate(4,5)
        ),coordinateList)
    }

    @Test
    fun testhorizontalLineToBitboard(){
        assertEquals(112uL,Bitboard.bbCastlingRoomSmallWhite)
        assertEquals(30uL,Bitboard.bbCastlingRoomLargeWhite)
        assertEquals(8070450532247928832uL,Bitboard.bbCastlingRoomSmallBlack)
        assertEquals(2161727821137838080uL,Bitboard.bbCastlingRoomLargeBlack)
    }

    companion object {
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



    fun moveMapToCompositeBB(moveMap: Map<Bitboard.Companion.Coordinate,ULong>) : ULong{
        var composite = 0uL
        for(coordinate in moveMap.keys){
            composite = moveMap[coordinate]!! or composite
        }
        return composite
    }
}