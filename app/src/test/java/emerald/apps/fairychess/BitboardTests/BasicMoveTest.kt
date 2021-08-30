package emerald.apps.fairychess.BitboardTests

import emerald.apps.fairychess.model.Bitboard
import emerald.apps.fairychess.model.Bitboard.Companion.add64BPositionFromCoordinates
import emerald.apps.fairychess.model.Bitboard.Companion.generate64BPositionFromCoordinate
import emerald.apps.fairychess.model.ChessGameUnitTest.Companion.parseChessFormation
import emerald.apps.fairychess.model.ChessGameUnitTest.Companion.parseFigureMapFromFile
import emerald.apps.fairychess.model.Movement
import emerald.apps.fairychess.model.MovementNotation
import emerald.apps.fairychess.model.PromotionMovement
import emerald.apps.fairychess.utility.FigureParser
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.Math.random
import kotlin.math.pow
import kotlin.system.measureTimeMillis


@kotlin.ExperimentalUnsignedTypes
/** test basic movements of all figure types (pawn,rook,knight,bishop,king,queen) and special moves (en passante and castling)*/
class BasicMoveTest {

    lateinit var chessFormationArray : Array<Array<String>>
    lateinit var figureMap : Map<String, FigureParser.Figure>

    @Before
    fun initNormalChessVariables(){
        chessFormationArray = parseChessFormation("normal_chess")
        figureMap = parseFigureMapFromFile()
    }



    @Test
    fun testClone(){
        val bitboardOriginal = Bitboard(chessFormationArray,figureMap)
        val bitboardClone = bitboardOriginal.clone()
        assertEquals("",bitboardOriginal.preMoveCheck("pawn","white", Movement(1,1,1,3)))
        assertEquals("7 | r | n | b | q | k | b | n | r | \n" +
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
                "0 | R | N | B | Q | K | B | N | R | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "  | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |\n" +
                "  +---+---+---+---+---+---+---+---+\n", bitboardClone.toString())
        assertEquals(18446462598732906495uL,bitboardClone.bbComposite)
        assertEquals(65535uL,bitboardClone.bbColorComposite[0])
        assertEquals(18446462598732840960uL,bitboardClone.bbColorComposite[1])
        assertEquals(0uL,bitboardClone.bbMovedCaptured)
    }

    @Test
    fun testBitboardInit(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        assertTrue(equalsInitBoard(bitboard))
    }

    fun equalsInitBoard(bitboard: Bitboard) : Boolean{
        var equal = true
        equal = equal && ("7 | r | n | b | q | k | b | n | r | \n" +
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
                "0 | R | N | B | Q | K | B | N | R | \n" +
                "--+---+---+---+---+---+---+---+---+\n" +
                "  | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |\n" +
                "  +---+---+---+---+---+---+---+---+\n" == bitboard.toString())
        equal = equal && (18446462598732906495uL==bitboard.bbComposite)
        equal = equal && (65535uL==bitboard.bbColorComposite[0])
        equal = equal && (18446462598732840960uL==bitboard.bbColorComposite[1])
        equal = equal && (0uL==bitboard.bbMovedCaptured)
        equal = equal && (1039==bitboard.pointsBlack())
        equal = equal && (1039==bitboard.pointsWhite())
        return equal
    }

    @Test
    fun testUndoNormalMove(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        val copyBitboard = bitboard.clone()
        assertEquals("", bitboard.move("white",Movement(4,1,4,3)))
        bitboard.undoLastMove("white", Movement(4,1,4,3))
        assertTrue(copyBitboard.equals(bitboard))
    }

    @Test
    fun testUndoPromotionMove(){
        //push white kingside pawn and black queenside pawn to enemy pawn line
        val bitboard = Bitboard(chessFormationArray,figureMap)
        assertEquals("",bitboard.preMoveCheck("pawn","white", Movement(4,1,4,3)))
        assertEquals("",bitboard.preMoveCheck("pawn","black", Movement(3,6,3,4)))
        assertEquals("",bitboard.preMoveCheck("pawn","white", Movement(4,3,4,4)))
        assertEquals("",bitboard.preMoveCheck("pawn","black", Movement(3,4,3,3)))
        assertEquals("",bitboard.preMoveCheck("pawn","white", Movement(4,4,4,5)))
        assertEquals("",bitboard.preMoveCheck("pawn","black", Movement(3,3,3,2)))
        //... and take pawn
        assertEquals("",bitboard.preMoveCheck("pawn","white", Movement(4,5,5,6)))
        assertEquals("",bitboard.preMoveCheck("pawn","black", Movement(3,2,2,1)))

        //take knight with kingside pawn and promote it to queen
        val copyBitboard = bitboard.clone()
        val promotionMovement = PromotionMovement( 5,6,6,7,"queen")
        assertEquals("",bitboard.preMoveCheck("pawn","white", promotionMovement))
        //bitboard.promotePawn(Bitboard.Companion.Coordinate(6,7),"queen")
        bitboard.undoLastMove("white",promotionMovement)
        assertTrue(copyBitboard.equals(bitboard))
    }



    @Test
    fun testUndoCastleMove(){

    }

    @Test
    fun testUndoEnpassanteMove(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //white enpassante left
        assertEquals(67371008uL,bitboard.getTargetMovements("pawn", "white", Bitboard.Companion.Coordinate(2,1), true))
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(2,1,2,3)))
        assertEquals(4415226380288uL,bitboard.getTargetMovements("pawn", "white", Bitboard.Companion.Coordinate(2,3), true))
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(3,6,3,4)))
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(2,3,2,4)))
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(1,6,1,4)))
        assertEquals(6597069766656uL, bitboard.getTargetMovements("pawn", "white", Bitboard.Companion.Coordinate(2, 4), true))
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(2,4,1,5)))
        assertEquals(2199023319808uL, bitboard.bbFigures["pawn"]!![0])
        assertEquals(68961403653849088uL, bitboard.bbFigures["pawn"]!![1])
    }

    @Test
    fun testUndoCaptureMove(){
        //push white kingside pawn and black queenside pawn to enemy pawn line
        val bitboard = Bitboard(chessFormationArray,figureMap)
        assertEquals("",bitboard.preMoveCheck("pawn","white", Movement(4,1,4,3)))
        assertEquals("",bitboard.preMoveCheck("pawn","black", Movement(3,6,3,4)))
        val copyBitboard = bitboard.clone()
        val move = Movement(MovementNotation("", listOf("c"),"", emptyList(),""), 4,3,3,4)
        assertEquals("",bitboard.preMoveCheck("pawn","white", move))
        bitboard.undoLastMove("white",move)
        assertTrue(bitboard.equals(copyBitboard))
    }

    @Test
    /** result: undoMove is 5x faster for >10E6 moves */
    fun testUndoMovePerformance(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        val iterations = 100000
        val implUndoMove = (measureTimeMillis {
            for(i in 0..iterations){
                val allMoves = bitboard.getAllPossibleMovesAsList("white")
                val randomMove = allMoves[(random()*allMoves.size).toInt()]
                assertEquals("", bitboard.move("white",randomMove))
                bitboard.undoLastMove("white",randomMove)
                assertTrue(equalsInitBoard(bitboard))
            }
        })
        val implResetBitboard = (measureTimeMillis {
            for(i in 0..iterations){
                val allMoves = bitboard.getAllPossibleMovesAsList("white")
                val randomMove = allMoves[(random()*allMoves.size).toInt()]
                val copyBitboard = bitboard.clone()
                assertEquals("", bitboard.move("white",randomMove))
                bitboard.set(copyBitboard)
                assertTrue(equalsInitBoard(bitboard))
            }
        })
        println("factor performance dif: "+(implResetBitboard.toDouble()/implUndoMove.toDouble()).toString())
    }


    @Test
    fun testPromotion(){
        //push white kingside pawn and black queenside pawn to enemy pawn line
        val bitboard = Bitboard(chessFormationArray,figureMap)
        assertEquals("",bitboard.preMoveCheck("pawn","white", Movement(4,1,4,3)))
        assertEquals("",bitboard.preMoveCheck("pawn","black", Movement(3,6,3,4)))
        assertEquals("",bitboard.preMoveCheck("pawn","white", Movement(4,3,4,4)))
        assertEquals("",bitboard.preMoveCheck("pawn","black", Movement(3,4,3,3)))
        assertEquals("",bitboard.preMoveCheck("pawn","white", Movement(4,4,4,5)))
        assertEquals("",bitboard.preMoveCheck("pawn","black", Movement(3,3,3,2)))
        //... and take pawn
        assertEquals("",bitboard.preMoveCheck("pawn","white", Movement(4,5,5,6)))
        assertEquals("",bitboard.preMoveCheck("pawn","black", Movement(3,2,2,1)))

        //take knight and promote white kingside pawn to queen
        assertEquals("",bitboard.preMoveCheck("pawn","white", Movement(5,6,6,7)))
        assertEquals(Bitboard.Companion.Coordinate(6,7),bitboard.promotionCoordinate)
        bitboard.promotePawn(Bitboard.Companion.Coordinate(6,7),"queen")
        assertEquals(60160uL,bitboard.bbFigures["pawn"]!![0])
        assertEquals(4611686018427387912uL,bitboard.bbFigures["queen"]!![0])


        //take knight and promote black queenside pawn to rook
        assertEquals("",bitboard.preMoveCheck("pawn","black", Movement(2,1,1,0)))
        assertEquals(Bitboard.Companion.Coordinate(1,0),bitboard.promotionCoordinate)
        bitboard.promotePawn(Bitboard.Companion.Coordinate(1,0),"rook")
        assertEquals(60517119992791040uL,bitboard.bbFigures["pawn"]!![1])
        assertEquals(9295429630892703746uL,bitboard.bbFigures["rook"]!![1])
    }

    @Test
    fun testMovegenerationKings(){
        var bitboard = Bitboard(chessFormationArray,figureMap)
        //kings initial position
        assertEquals(0uL,bitboard.getTargetMovements("king", "black",
            Bitboard.Companion.Coordinate(4, 7), true))
        assertEquals(0uL,bitboard.getTargetMovements("king", "white", Bitboard.Companion.Coordinate(4, 0), true))
        //kings in middle
        assertEquals(88441966559232uL,bitboard.getTargetMovements("king", "black", Bitboard.Companion.Coordinate(5, 5), true))
        assertEquals(31613639358152704uL,bitboard.getTargetMovements("king", "white", Bitboard.Companion.Coordinate(5, 5), true))
        //kings on edge
        assertEquals(846636838289408uL,bitboard.getTargetMovements("king", "white", Bitboard.Companion.Coordinate(0, 5), true))
        assertEquals(2211908157440uL,bitboard.getTargetMovements("king", "black", Bitboard.Companion.Coordinate(0, 5), true))

        //small castling
        //move knights and bishops
        assertEquals("",bitboard.preMoveCheck("knight","white",Movement(6,0,7,2)))
        assertEquals("",bitboard.preMoveCheck("knight","black",Movement(6,7,7,5)))
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(6,1,6,2)))
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(6,6,6,5)))
        assertEquals("",bitboard.preMoveCheck("bishop","white",Movement(5,0,6,1)))
        assertEquals("",bitboard.preMoveCheck("bishop","black",Movement(5,7,6,6)))
        //check if both kings can castle kingside
        assertEquals(96uL,bitboard.getTargetMovements("king", "white", Bitboard.Companion.Coordinate(4, 0), true))
        assertEquals(6917529027641081856uL,bitboard.getTargetMovements("king", "black", Bitboard.Companion.Coordinate(4, 7), true))
        //make castling move and check positions of rook and king
        assertEquals("",bitboard.preMoveCheck("king","white",Movement(4,0,6,0)))
        assertEquals(64uL,bitboard.bbFigures["king"]!![0])
        assertEquals(33uL,bitboard.bbFigures["rook"]!![0])
        //make castling move and check positions of rook and king
        assertEquals("",bitboard.preMoveCheck("king","black",Movement(4,7,6,7)))
        assertEquals(4611686018427387904uL,bitboard.bbFigures["king"]!![1])
        assertEquals(2377900603251621888uL,bitboard.bbFigures["rook"]!![1])

        //large castling
        //move knights, bishops and queens
        bitboard = Bitboard(chessFormationArray,figureMap)
        assertEquals("",bitboard.preMoveCheck("knight","white",Movement(1,0,0,2)))
        assertEquals("",bitboard.preMoveCheck("knight","black",Movement(1,7,0,5)))
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(2,1,2,2)))
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(2,6,2,5)))
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(3,1,3,2)))
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(3,6,3,5)))
        assertEquals("",bitboard.preMoveCheck("bishop","white", Movement(2,0,3,1)))
        assertEquals("",bitboard.preMoveCheck("bishop","black",Movement(2,7,3,6)))
        assertEquals("",bitboard.preMoveCheck("queen","white",Movement(3,0,2,1)))
        assertEquals("",bitboard.preMoveCheck("queen","black",Movement(3,7,2,6)))
        //check if both kings can castle queenside
        assertEquals(12uL,bitboard.getTargetMovements("king", "white", Bitboard.Companion.Coordinate(4, 0), true))
        assertEquals(864691128455135232uL,bitboard.getTargetMovements("king", "black", Bitboard.Companion.Coordinate(4, 7), true))
        //make castling move and check positions of rook and king
        assertEquals("",bitboard.preMoveCheck("king","white",Movement(4,0,2,0)))
        assertEquals(4uL,bitboard.bbFigures["king"]!![0])
        assertEquals(136uL,bitboard.bbFigures["rook"]!![0])
        //make castling move and check positions of rook and king
        assertEquals("",bitboard.preMoveCheck("king","black",Movement(4,7,2,7)))
        assertEquals(288230376151711744uL,bitboard.bbFigures["king"]!![1])
        assertEquals(9799832789158199296uL,bitboard.bbFigures["rook"]!![1])
    }

    @Test
    fun testMovegenerationQueens(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //queen initial position
        assertEquals(0uL,bitboard.getTargetMovements("queen", "black", Bitboard.Companion.Coordinate(3, 7), true))
        assertEquals(0uL,bitboard.getTargetMovements("queen", "white", Bitboard.Companion.Coordinate(3, 0), true))
        //queen in middle
        assertEquals(23706498137063424uL,bitboard.getTargetMovements("queen", "white", Bitboard.Companion.Coordinate(4, 4), true))
        assertEquals(62600093405696uL,bitboard.getTargetMovements("queen", "black", Bitboard.Companion.Coordinate(4, 4), true))
        //queen on edge
        assertEquals(1411764390789120uL,bitboard.getTargetMovements("queen", "white", Bitboard.Companion.Coordinate(0, 4), true))
        assertEquals(4389507238144uL,bitboard.getTargetMovements("queen", "black", Bitboard.Companion.Coordinate(0, 4), true))

        /*println(bitboardToString(bitboard.getTargetMovements("queen","white",0, 4)))
        println(bitboard.getTargetMovements("queen","white",0, 4))*/
    }



    @Test
    fun testCapture(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //push kingside (white) and queenside (black) pawn
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(4,1,4,3)))
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(3,6,3,4)))
        //capture black pawn with white pawn
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(4,3,3,4)))
        //capture white pawn with queen
        assertEquals("",bitboard.preMoveCheck("queen","black",Movement(3,7,3,4)))
        assertEquals(1038,bitboard.pointsWhite())
        assertEquals(1038,bitboard.pointsBlack())
        assertEquals(61184uL,bitboard.bbFigures["pawn"]!![0])
        assertEquals(69524319247532032uL,bitboard.bbFigures["pawn"]!![1])
        assertEquals(8uL,bitboard.bbFigures["queen"]!![0])
        assertEquals(34359738368uL,bitboard.bbFigures["queen"]!![1])
    }

    @Test
    fun testMovegenerationBishops(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //bishop initial position
        assertEquals(0uL,bitboard.getTargetMovements("bishop", "black", Bitboard.Companion.Coordinate(2, 7), true))
        assertEquals(0uL,bitboard.getTargetMovements("bishop", "white", Bitboard.Companion.Coordinate(2, 0), true))
        //bishop in middle
        assertEquals(43981140689408uL,bitboard.getTargetMovements("bishop", "black", Bitboard.Companion.Coordinate(4, 4), true))
        assertEquals(19184279556980736uL,bitboard.getTargetMovements("bishop", "white", Bitboard.Companion.Coordinate(4, 4), true))
        //bishop on edge
        assertEquals(1128098963914752uL,bitboard.getTargetMovements("bishop", "white", Bitboard.Companion.Coordinate(0, 4), true))
        assertEquals(2199057074176uL,bitboard.getTargetMovements("bishop", "black", Bitboard.Companion.Coordinate(0, 4), true))

        /*println(bitboardToString(bitboard.getTargetMovements("bishop","black",0, 4)))
        println(bitboard.getTargetMovements("bishop","black",0, 4))*/
    }

    @Test
    fun testMovegenerationKnights(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //knight initial position
        assertEquals(327680uL,bitboard.getTargetMovements("knight", "white", Bitboard.Companion.Coordinate(1, 0), true))
        assertEquals(5497558138880uL,bitboard.getTargetMovements("knight", "black", Bitboard.Companion.Coordinate(1, 7), true))
        //knight in middle
        assertEquals(11333767002587136uL,bitboard.getTargetMovements("knight", "white", Bitboard.Companion.Coordinate(4, 4), true))
        assertEquals(44272527353856uL,bitboard.getTargetMovements("knight", "black", Bitboard.Companion.Coordinate(4, 3), true))
        //knight on edge
        assertEquals(567348067172352uL,bitboard.getTargetMovements("knight", "white", Bitboard.Companion.Coordinate(0, 4), true))
        assertEquals(4398113751040uL,bitboard.getTargetMovements("knight", "black", Bitboard.Companion.Coordinate(0, 4), true))

        /*println(bitboardToString(bitboard.getTargetMovements("knight","black",0, 4)))
        println(bitboard.getTargetMovements("knight","black",0, 4))*/
    }

    @Test
    fun testMovegenerationRooks(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //rook initial position
        assertEquals(0uL,bitboard.getTargetMovements("rook", "white", Bitboard.Companion.Coordinate(0, 0), true))
        assertEquals(0uL,bitboard.getTargetMovements("rook", "black", Bitboard.Companion.Coordinate(0, 7), true))
        //rook in middle
        assertEquals(4522218580082688uL,bitboard.getTargetMovements("rook", "white", Bitboard.Companion.Coordinate(4, 4), true))
        assertEquals(18618952716288uL,bitboard.getTargetMovements("rook", "black", Bitboard.Companion.Coordinate(4, 4), true))
        //rook on edge
        assertEquals(283665426874368uL,bitboard.getTargetMovements("rook", "white", Bitboard.Companion.Coordinate(0, 4), true))
        assertEquals(2190450163968uL,bitboard.getTargetMovements("rook", "black", Bitboard.Companion.Coordinate(0, 4), true))
        //rook in corner
        //preperation - move pawn and knight
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(0,1,0,3)))
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(0,6,0,4)))
        assertEquals("",bitboard.preMoveCheck("knight","white",Movement(1,0,2,2)))
        assertEquals("",bitboard.preMoveCheck("knight","black",Movement(1,7,2,5)))
        assertEquals(65794uL,bitboard.getTargetMovements("rook", "white", Bitboard.Companion.Coordinate(0, 0), true))
        assertEquals(144397762564194304uL,bitboard.getTargetMovements("rook", "black", Bitboard.Companion.Coordinate(0, 7), true))

       /* println(bitboardToString(bitboard.getTargetMovements("rook","black",0, 7)))
        println(bitboard.getTargetMovements("rook","black",0, 7))*/
    }

    @Test
    fun testMoveHistory(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        val entry1 = mapOf<String,Array<ULong>>(
            "entry1" to arrayOf()
        )
        val entry2 = mapOf<String,Array<ULong>>(
            "entry2" to arrayOf()
        )
        bitboard.addEntryToHistory(entry1)
        assertTrue(bitboard.moveHistory[0].containsKey("entry1"))
        bitboard.addEntryToHistory(entry2)
        assertTrue(bitboard.moveHistory[0].containsKey("entry1"))
        assertTrue(bitboard.moveHistory[1].containsKey("entry2"))
    }

    @Test
    fun testMovegenerationPawns(){
        var bitboard = Bitboard(chessFormationArray,figureMap)

        //white pawn can capture 2 black pawns
        assertEquals(2814749767106560uL,bitboard.getTargetMovements("pawn", "white", Bitboard.Companion.Coordinate(2, 5), true))
        //white pawn initial movement
        assertEquals(67371008uL,bitboard.getTargetMovements("pawn", "white", Bitboard.Companion.Coordinate(2, 1), true))
        //black pawn can capture white 2 pawns
        assertEquals(2560uL,bitboard.getTargetMovements("pawn", "black", Bitboard.Companion.Coordinate(2, 2), true))
        //black pawn initial movement
        assertEquals(4415226380288uL,bitboard.getTargetMovements("pawn", "black", Bitboard.Companion.Coordinate(2, 6), true))

        //test capturing
        testMoveFigure()

        bitboard = Bitboard(chessFormationArray,figureMap)
        //white enpassante left
        assertEquals(67371008uL,bitboard.getTargetMovements("pawn", "white", Bitboard.Companion.Coordinate(2,1), true))
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(2,1,2,3)))
        assertEquals(4415226380288uL,bitboard.getTargetMovements("pawn", "white", Bitboard.Companion.Coordinate(2,3), true))
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(3,6,3,4)))
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(2,3,2,4)))
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(1,6,1,4)))
        assertEquals(6597069766656uL, bitboard.getTargetMovements("pawn", "white", Bitboard.Companion.Coordinate(2, 4), true))
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(2,4,1,5)))
        assertEquals(2199023319808uL, bitboard.bbFigures["pawn"]!![0])
        assertEquals(68961403653849088uL, bitboard.bbFigures["pawn"]!![1])

        //black enpassante right
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(3,4,3,3)))
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(4,1,4,3)))
        assertEquals(1572864uL, bitboard.getTargetMovements("pawn", "black", Bitboard.Companion.Coordinate(3,3), true))
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(3,3,4,2)))
        assertEquals(2199023315712uL, bitboard.bbFigures["pawn"]!![0])
        assertEquals(68961369295159296uL, bitboard.bbFigures["pawn"]!![1])
    }

    @Test
    /** test if moving figure works */
    fun testMoveFigure(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        //initial pawn move
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(2,1,2,3)))
        assertEquals(18446462598800014335uL,bitboard.bbComposite)
        assertEquals(67173375uL,bitboard.bbColorComposite[0])
        assertEquals(67173120uL,bitboard.bbFigures["pawn"]!![0])
        assertEquals(71776119061217280uL,bitboard.bbFigures["pawn"]!![1])
        assertEquals(67173120uL,bitboard.bbFigures["pawn"]!![0])

        //capture black pawn with white pawn
        assertEquals("",bitboard.preMoveCheck("pawn","black",Movement(3,6,3,4)))
        assertEquals("",bitboard.preMoveCheck("pawn","white",Movement(2,3,3,4)))
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
        assertEquals(2.0.pow(0*8 + 0).toULong(), generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(0,0)))
        assertEquals(2.0.pow(0*8 + 1).toULong(), generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(1,0)))
        assertEquals(2.0.pow(1*8 + 0).toULong(), generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(0,1)))
        assertEquals(2.0.pow(1*8 + 1).toULong(), generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(1,1)))
        assertEquals(2.0.pow(4*8 + 5).toULong(), generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(5,4)))

        assertEquals(Bitboard.Companion.Coordinate(0,0),
            Bitboard.generateCoordinatesFrom64BPosition(
                generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(0,0))
            )[0])

        assertEquals(Bitboard.Companion.Coordinate(1,0),
            Bitboard.generateCoordinatesFrom64BPosition(
                generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(1,0))
            )[0])

        for(rank in 0..7){
            for(line in 0..7){
                assertEquals(2.0.pow(line*8 + rank).toULong(),
                    generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(rank,line))
                )
                assertEquals(Bitboard.Companion.Coordinate(rank,line).file,
                    Bitboard.generateCoordinatesFrom64BPosition(
                        generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(rank,line))
                    )[0].file)
                assertEquals(Bitboard.Companion.Coordinate(rank,line).rank,
                    Bitboard.generateCoordinatesFrom64BPosition(
                        generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(rank,line))
                    )[0].rank)
            }
        }

        //test bitboards with multiple figures
        var bb = 0uL
        bb = add64BPositionFromCoordinates(bb,Bitboard.Companion.Coordinate(0,0))
        bb = add64BPositionFromCoordinates(bb,Bitboard.Companion.Coordinate(4,5))
        bb = add64BPositionFromCoordinates(bb,Bitboard.Companion.Coordinate(2,3))

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
        assertEquals(28uL,Bitboard.bbCastlingRoomLargeWhite)
        assertEquals(8070450532247928832uL,Bitboard.bbCastlingRoomSmallBlack)
        assertEquals(2017612633061982208uL,Bitboard.bbCastlingRoomLargeBlack)
    }

    companion object {
        fun bitboardToString(bitboard: ULong) : String{
            val str = StringBuilder("")
            var cnt = 0
            for(file in 7 downTo 0){
                str.append("$file | ")
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