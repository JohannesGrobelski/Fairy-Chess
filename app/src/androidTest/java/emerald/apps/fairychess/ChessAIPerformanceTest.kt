package emerald.apps.fairychess

import emerald.apps.fairychess.model.*
import emerald.apps.fairychess.utility.FigureParser
import org.junit.Assert
import org.junit.Test
import kotlin.math.pow
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class ChessAIPerformanceTest {

    lateinit var chessFormationArray : Array<Array<String>>
    lateinit var figureMap : Map<String, FigureParser.Figure>
    lateinit var chessAIBlack: ChessAI
    lateinit var chessAIWhite: ChessAI

    companion object {
        const val DEBUG = true
    }


    @Test
    fun testSimpleGame(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        Assert.assertEquals("",bitboard.checkMoveAndMove("white", Movement(4,1,4,3)))
        for(i in 0 .. 10){
            val moveBlack = chessAIBlack.calcMove(bitboard)
            bitboard.checkMoveAndMove("black",moveBlack)
            if(ChessAITest.DEBUG)println(moveBlack.asString("black"))
            if(ChessAITest.DEBUG)println(bitboard.toString())
            if(bitboard.gameFinished)break

            val moveWhite = chessAIWhite.calcMove(bitboard)
            bitboard.checkMoveAndMove("white",moveWhite)
            if(ChessAITest.DEBUG)println(moveWhite.asString("white"))
            if(ChessAITest.DEBUG)println(bitboard.toString())
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
        var timeCopy = 0; var timeSearch = 0; var timeZobrist = 0; var timeMove = 0; var timeSet = 0; var timeChoose = 0
        val timeOverall = measureTimeMillis {
            val zobristHash = ZobristHash(bitboard.figureMap.keys.toList())
            for(i in 0..iterations){
                timeSearch += measureTimeMillis {if(i%movesPerIteration == 0)allMovesList = bitboard.getAllPossibleMovesAsList(bitboard.moveColor)}.toInt()
                timeChoose += measureTimeMillis {move = allMovesList[(Math.random() *allMovesList.size).toInt()]}.toInt()
                timeZobrist += measureTimeMillis {val bbHash = zobristHash.generateHash(bitboard)}.toInt()
                timeCopy += measureTimeMillis{copyBitboard = bitboard.clone()}.toInt()
                timeMove += measureTimeMillis {bitboard.move(bitboard.moveColor,move)}.toInt()
                timeSet += measureTimeMillis {bitboard.set(copyBitboard)}.toInt()
            }
        }

        if(ChessAITest.DEBUG)println("$iterations iterations: $timeOverall ms")
        if(ChessAITest.DEBUG)println("timeSearch: $timeSearch ms ("+(timeSearch.toDouble()/timeOverall.toDouble())*100+"%)")
        if(ChessAITest.DEBUG)println("timeZobrist: $timeZobrist ms ("+(timeZobrist.toDouble()/timeOverall.toDouble())*100+"%)")
        if(ChessAITest.DEBUG)println("timeCopy: $timeCopy ms ("+(timeCopy.toDouble()/timeOverall.toDouble())*100+"%)")
        if(ChessAITest.DEBUG)println("timeMove: $timeMove ms ("+(timeMove.toDouble()/timeOverall.toDouble())*100+"%)")
        if(ChessAITest.DEBUG)println("timeSet: $timeSet ms ("+(timeSet.toDouble()/timeOverall.toDouble())*100+"%)")
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
                name = bitboard.getPieceName(pos, bbFigure)
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

        if(ChessAITest.DEBUG)println("timeOverall: $timeOverall ns ("+(timeOverall.toDouble()/timeOverall.toDouble())*100+"%)")
        if(ChessAITest.DEBUG)println("timeParameters: $timeParameters ns ("+(timeParameters.toDouble()/timeOverall.toDouble())*100+"%)")
        if(ChessAITest.DEBUG)println("timeMoveGeneration: $timeMoveGeneration ns ("+(timeMoveGeneration.toDouble()/timeOverall.toDouble())*100+"%)")
        if(ChessAITest.DEBUG)println("timeDeleteIllegalMoves: $timeDeleteIllegalMoves ns ("+(timeDeleteIllegalMoves.toDouble()/timeOverall.toDouble())*100+"%)")
        if(ChessAITest.DEBUG)println("timeSpecialMoveGeneration: $timeSpecialMoveGeneration ns ("+(timeSpecialMoveGeneration.toDouble()/timeOverall.toDouble())*100+"%)")
        if(ChessAITest.DEBUG)println("timeTransformation: $timeTransformation ns ("+(timeTransformation.toDouble()/timeOverall.toDouble())*100+"%)")
    }

}