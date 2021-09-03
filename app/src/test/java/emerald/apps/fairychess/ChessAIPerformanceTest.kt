package emerald.apps.fairychess

import emerald.apps.fairychess.model.*
import emerald.apps.fairychess.model.Bitboard.Companion.parseFigureMapFromFile
import emerald.apps.fairychess.model.ChessGameUnitTest.Companion.parseChessFormation
import emerald.apps.fairychess.utility.FigureParser
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.math.pow
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

@ExperimentalUnsignedTypes
class ChessAIPerformanceTest {

    lateinit var chessFormationArray : Array<Array<String>>
    lateinit var figureMap : Map<String, FigureParser.Figure>
    lateinit var chessAIBlack: ChessAI
    lateinit var chessAIWhite: ChessAI

    companion object {
        const val DEBUG = true
    }

    @Before
    fun initNormalChessVariables(){
        chessFormationArray = parseChessFormation("normal_chess")
        figureMap = parseFigureMapFromFile()
        chessAIBlack = ChessAI("black")
        chessAIWhite = ChessAI("white")
    }

    @Test
    fun testSimpleGame(){
        val bitboard = Bitboard(chessFormationArray,figureMap)
        Assert.assertEquals("",bitboard.checkMoveAndMove("white", Movement(4,1,4,3)))
        for(i in 0 .. 10){
            val moveBlack = chessAIBlack.calcMove(bitboard)
            bitboard.checkMoveAndMove("black",moveBlack)
            if(DEBUG)println(moveBlack.asString("black"))
            if(DEBUG)println(bitboard.toString())
            if(bitboard.gameFinished)break

            val moveWhite = chessAIWhite.calcMove(bitboard)
            bitboard.checkMoveAndMove("white",moveWhite)
            if(DEBUG)println(moveWhite.asString("white"))
            if(DEBUG)println(bitboard.toString())
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

        if(DEBUG)println("$iterations iterations: $timeOverall ms")
        if(DEBUG)println("timeSearch: $timeSearch ms ("+(timeSearch.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeZobrist: $timeZobrist ms ("+(timeZobrist.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeCopy: $timeCopy ms ("+(timeCopy.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeMove: $timeMove ms ("+(timeMove.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeSet: $timeSet ms ("+(timeSet.toDouble()/timeOverall.toDouble())*100+"%)")
        //println("timeChoose: $timeChoose ms ("+(timeChoose.toDouble()/timeOverall.toDouble())*100+"%)")
    }



    @Test
    fun testSearchPerformance(){
        var timeParameters = 0; var timeMoveGeneration = 0; var timeDeleteIllegalMoves = 0; var timeSpecialMoveGeneration = 0; var timeTransformation = 0
        val timeOverall: Int

        val bitboard = Bitboard(chessFormationArray,figureMap)
        lateinit var coordinate: Bitboard.Companion.Coordinate
        lateinit var color : String
        lateinit var name : String
        lateinit var movementString : String
        var pos: Int
        var bbFigure: ULong
        lateinit var movementList : MutableList<Movement>
        lateinit var movementNotationList : List<MovementNotation>

        timeOverall = measureNanoTime {
            timeParameters += measureNanoTime {
                coordinate = Bitboard.Companion.Coordinate(1,1)
                color = "white"
                movementList = mutableListOf()
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

        if(DEBUG)println("timeOverall: $timeOverall ns ("+(timeOverall.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeParameters: $timeParameters ns ("+(timeParameters.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeMoveGeneration: $timeMoveGeneration ns ("+(timeMoveGeneration.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeDeleteIllegalMoves: $timeDeleteIllegalMoves ns ("+(timeDeleteIllegalMoves.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeSpecialMoveGeneration: $timeSpecialMoveGeneration ns ("+(timeSpecialMoveGeneration.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeTransformation: $timeTransformation ns ("+(timeTransformation.toDouble()/timeOverall.toDouble())*100+"%)")
    }

}