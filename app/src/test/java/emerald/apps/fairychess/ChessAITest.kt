package emerald.apps.fairychess

import emerald.apps.fairychess.model.Bitboard
import emerald.apps.fairychess.model.ChessAI
import emerald.apps.fairychess.model.ChessGameUnitTest
import emerald.apps.fairychess.model.Movement
import emerald.apps.fairychess.utility.FigureParser
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.Math.random
import kotlin.math.pow
import kotlin.system.measureTimeMillis

class ChessAITest {

    lateinit var chessFormationArray : Array<Array<String>>
    lateinit var figureMap : Map<String, FigureParser.Figure>
    lateinit var chessAIBlack: ChessAI
    lateinit var chessAIWhite: ChessAI

    @Before
    fun initNormalChessVariables(){
        chessFormationArray = ChessGameUnitTest.parseChessFormation("normal_chess")
        figureMap = ChessGameUnitTest.parseFigureMapFromFile()
        chessAIBlack = ChessAI("black")
        chessAIWhite = ChessAI("black")
    }

    @Test
    fun testOneMove(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        Assert.assertEquals("",bitboard.checkMoveAndMove("white", Movement(4,1,4,3)))
        val move = chessAIBlack.calcMove(bitboard)
        Assert.assertEquals("",bitboard.checkMoveAndMove("black",move))
        println(bitboard.toString())
    }

    @Test
    fun testSimpleGame(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        Assert.assertEquals("",bitboard.checkMoveAndMove("white", Movement(4,1,4,3)))
        for(i in 0 .. 10){
            val moveBlack = chessAIBlack.calcMove(bitboard)
            bitboard.checkMoveAndMove("black",moveBlack)
            println(moveBlack.asString("black"))
            println(bitboard.toString())
            if(bitboard.gameFinished)break

            val moveWhite = chessAIWhite.calcMove(bitboard)
            bitboard.checkMoveAndMove("white",moveWhite)
            println(moveWhite.asString("white"))
            println(bitboard.toString())
            if(bitboard.gameFinished)break
        }
    }

    @Test
    fun testAIPerformance(){
        val movesPerIteration = 30
        val depth = 4
        val iterations = movesPerIteration.toDouble().pow(depth).toInt()
        val bitboard = Bitboard(chessFormationArray,figureMap)

        var allMovesList = bitboard.getAllPossibleMovesAsList(bitboard.moveColor)
        var copyBitboard : Bitboard; var move : Movement
        var timeCopy = 0; var timeSearch = 0; var timeMove = 0; var timeSet = 0; var timeChoose = 0
        val timeOverall = measureTimeMillis {
            for(i in 0..iterations){
                timeSearch += measureTimeMillis {if(i%movesPerIteration == 0)allMovesList = bitboard.getAllPossibleMovesAsList(bitboard.moveColor)}.toInt()
                timeChoose += measureTimeMillis {move = allMovesList[(random()*allMovesList.size).toInt()]}.toInt()
                timeCopy += measureTimeMillis{copyBitboard = bitboard.clone()}.toInt()
                timeMove += measureTimeMillis {bitboard.move(bitboard.moveColor,move)}.toInt()
                timeSet += measureTimeMillis {bitboard.set(copyBitboard)}.toInt()
            }
        }

        println("$iterations iterations: $timeOverall ms")
        println("timeSearch: $timeSearch ms ("+(timeSearch.toDouble()/timeOverall.toDouble())*100+"%)")
        println("timeCopy: $timeCopy ms ("+(timeCopy.toDouble()/timeOverall.toDouble())*100+"%)")
        println("timeMove: $timeMove ms ("+(timeMove.toDouble()/timeOverall.toDouble())*100+"%)")
        println("timeSet: $timeSet ms ("+(timeSet.toDouble()/timeOverall.toDouble())*100+"%)")
        //println("timeChoose: $timeChoose ms ("+(timeChoose.toDouble()/timeOverall.toDouble())*100+"%)")
    }

    @Test
    fun testSearchPerformance(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        bitboard.getAllPossibleMovesAsList(bitboard.moveColor)
    }

    @Test
    fun testAlgorithmCapturePiece() {
        val bitBoardNormal = Bitboard(chessFormationArray, figureMap)
        val stubChessAI = ChessAI("black")
        //open
        assert(bitBoardNormal.checkMoveAndMove("white", Movement.fromStringToMovement("4_1_4_3")).isEmpty())
        assert(bitBoardNormal.checkMoveAndMove("black",stubChessAI.calcMove(bitBoardNormal)).isEmpty())

        assert(bitBoardNormal.checkMoveAndMove("white", Movement.fromStringToMovement("4_3_4_4")).isEmpty())
        assert(bitBoardNormal.checkMoveAndMove("black",stubChessAI.calcMove(bitBoardNormal)).isEmpty())

        assert(bitBoardNormal.checkMoveAndMove("white", Movement.fromStringToMovement("4_4_4_5")).isEmpty())
        assert(bitBoardNormal.checkMoveAndMove("black",stubChessAI.calcMove(bitBoardNormal)).isEmpty())

        println(bitBoardNormal.toString())
    }

    @Test
    fun testMinimax() {
        val chessBoardNormal = Bitboard(chessFormationArray, figureMap)
        val stubChessAI = ChessAI("black")

        assert(chessBoardNormal.checkMoveAndMove("white", Movement.fromStringToMovement("1_1_1_3")).isEmpty())
        assert(chessBoardNormal.checkMoveAndMove("black", Movement.fromStringToMovement("0_6_0_5")).isEmpty())
        assert(chessBoardNormal.checkMoveAndMove("white", Movement.fromStringToMovement("1_3_1_4")).isEmpty())
        assert(chessBoardNormal.checkMoveAndMove("black", Movement.fromStringToMovement("0_5_1_4")).isEmpty())
        assert(chessBoardNormal.checkMoveAndMove("white", Movement.fromStringToMovement("2_1_2_3")).isEmpty())
        assert(chessBoardNormal.checkMoveAndMove("black", stubChessAI.calcMove(chessBoardNormal)!!).isEmpty())

        println(chessBoardNormal.toString())
        println(stubChessAI.cnt_movements.toString()+" moves")
    }
}