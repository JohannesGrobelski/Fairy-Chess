package emerald.apps.fairychess

import android.media.midi.MidiOutputPort
import emerald.apps.fairychess.model.*
import emerald.apps.fairychess.utility.FigureParser
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.Math.random
import kotlin.math.pow
import kotlin.system.measureNanoTime
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
        var move : Movement
        val calcTime = measureTimeMillis {
            move = chessAIBlack.calcMove(bitboard)
        }
        Assert.assertEquals("",bitboard.checkMoveAndMove("black",move))
        println(calcTime)
        println(chessAIBlack.cnt_movements)
        println(chessAIBlack.transpositionTableHits)
        println(chessAIBlack.transpositionTable.size)

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
        var timeParameters = 0; var timeMoveGeneration = 0; var timeDeleteIllegalMoves = 0; var timeSpecialMoveGeneration = 0; var timeTransformation = 0
        var timeOverall = 0

        var bitboard = Bitboard(chessFormationArray,figureMap)
        lateinit var coordinate: Bitboard.Companion.Coordinate
        lateinit var color : String
        lateinit var name : String
        lateinit var movementString : String
        var pos = 0
        var bbFigure = 0uL
        lateinit var movementList : MutableList<Movement>
        lateinit var movementNotationList : List<MovementNotation>

        timeOverall = measureNanoTime {
            timeParameters += measureNanoTime {
                coordinate = Bitboard.Companion.Coordinate(1,1)
                color = "white"
                movementList = mutableListOf<Movement>();
                pos = ("black" == color).toInt()
                bbFigure = Bitboard.generate64BPositionFromCoordinate(coordinate)
                name = bitboard.getNameOfFigure(pos, bbFigure)
            }.toInt()

            if(name in figureMap.keys){
                timeParameters += measureNanoTime {
                    movementString = (figureMap[name] as FigureParser.Figure).movementParlett
                    movementNotationList = Bitboard.getMovementNotation(movementString)
                }.toInt()
                var bbTargets : MutableMap<MovementNotation, ULong>
                timeMoveGeneration += measureNanoTime {
                    bbTargets = bitboard.generateMovements(color,coordinate,movementNotationList)
                }.toInt()
                timeDeleteIllegalMoves += measureNanoTime {
                    bbTargets = bitboard.deleteIllegalMoves(name,color,bbFigure,bbTargets.toMutableMap(),movementNotationList)
                }.toInt()
                timeSpecialMoveGeneration += measureNanoTime {
                    bbTargets = bitboard.genSpecialMoves(name,color,coordinate,bbTargets,true)
                }.toInt()
                timeTransformation += measureNanoTime {
                    //transform bbTargets into movementList
                    for(movementNotation in bbTargets.keys){
                        if(bbTargets[movementNotation] == 0uL)continue
                        val targetList =
                            Bitboard.generateCoordinatesFrom64BPosition(bbTargets[movementNotation]!!)
                        for(target in targetList){
                            val move = Movement(movementNotation,coordinate.rank,coordinate.file,target.rank,target.file)
                            if(move !in movementList){
                                movementList.add(move)
                            }
                        }
                    }
                }.toInt()
            }
        }.toInt()

        println("timeOverall: $timeOverall ns ("+(timeOverall.toDouble()/timeOverall.toDouble())*100+"%)")
        println("timeParameters: $timeParameters ns ("+(timeParameters.toDouble()/timeOverall.toDouble())*100+"%)")
        println("timeMoveGeneration: $timeMoveGeneration ns ("+(timeMoveGeneration.toDouble()/timeOverall.toDouble())*100+"%)")
        println("timeDeleteIllegalMoves: $timeDeleteIllegalMoves ns ("+(timeDeleteIllegalMoves.toDouble()/timeOverall.toDouble())*100+"%)")
        println("timeSpecialMoveGeneration: $timeSpecialMoveGeneration ns ("+(timeSpecialMoveGeneration.toDouble()/timeOverall.toDouble())*100+"%)")
        println("timeTransformation: $timeTransformation ns ("+(timeTransformation.toDouble()/timeOverall.toDouble())*100+"%)")

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