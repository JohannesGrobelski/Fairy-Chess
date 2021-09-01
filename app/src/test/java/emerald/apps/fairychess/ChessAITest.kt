package emerald.apps.fairychess

import emerald.apps.fairychess.model.*
import emerald.apps.fairychess.utility.FigureParser
import junit.framework.Assert.assertEquals
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

    companion object {
        const val DEBUG = true
    }

    @Before
    fun initNormalChessVariables(){
        chessFormationArray = ChessGameUnitTest.parseChessFormation("normal_chess")
        figureMap = ChessGameUnitTest.parseFigureMapFromFile()
        chessAIBlack = ChessAI("black")
        chessAIWhite = ChessAI("white")
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
        println("calcTime: $calcTime ms")
        println("cnt_movements: "+chessAIBlack.cnt_movements)
        println("transpositionTableHits: "+chessAIBlack.transpositionTableHits)
        println("transpositionTableSize: "+chessAIBlack.transpositionTable.size)
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
                timeZobrist += measureTimeMillis {val bbHash = zobristHash.generateHash(bitboard)}.toInt()
                timeChoose += measureTimeMillis {move = allMovesList[(random()*allMovesList.size).toInt()]}.toInt()
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

        if(DEBUG)println("timeOverall: $timeOverall ns ("+(timeOverall.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeParameters: $timeParameters ns ("+(timeParameters.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeMoveGeneration: $timeMoveGeneration ns ("+(timeMoveGeneration.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeDeleteIllegalMoves: $timeDeleteIllegalMoves ns ("+(timeDeleteIllegalMoves.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeSpecialMoveGeneration: $timeSpecialMoveGeneration ns ("+(timeSpecialMoveGeneration.toDouble()/timeOverall.toDouble())*100+"%)")
        if(DEBUG)println("timeTransformation: $timeTransformation ns ("+(timeTransformation.toDouble()/timeOverall.toDouble())*100+"%)")
    }

    @Test
    fun testZobristHash(){
        var bitBoardNormal = Bitboard(chessFormationArray, figureMap)
        val zobristHash = ZobristHash(bitBoardNormal.figureMap.keys.toList())

        val movePairs = listOf(
            arrayOf(
                Movement(1,0,2,2),
                Movement(1,7,2,5)
            ),
            arrayOf(
                Movement(6,0,5,2),
                Movement(6,7,5,5)
            ),
        )
        for(movepair in movePairs){
            assertEquals("",bitBoardNormal.move("white",movepair[0]))
            assertEquals("",bitBoardNormal.move("black",movepair[1]))
        }
        var hashOriginal = zobristHash.generateHash(bitBoardNormal)

        val permutations = getPermutations(movePairs, listOf())
        for(permutation in permutations){
            bitBoardNormal = Bitboard(chessFormationArray, figureMap)
            for(movepair in permutation){
                assertEquals("",bitBoardNormal.move("white",movepair[0]))
                assertEquals("",bitBoardNormal.move("black",movepair[1]))
            }
            assertEquals(hashOriginal,zobristHash.generateHash(bitBoardNormal))
        }
    }

    fun <T> getPermutations(original: List<T>, result: List<List<T>>): List<List<T>> {
        if(original.size == 1){
            return listOf(original)
        } else {
            val firstElement = original.first()
            val shorterList = original.subList(1,original.size)
            val shorterListPermutations = getPermutations(shorterList,result)
            val permutations = mutableListOf<List<T>>()
            for(shorterListPermutation in shorterListPermutations){
                for(i in 0..shorterListPermutation.size){
                    val permutation = mutableListOf<T>()
                    permutation.addAll(shorterList)
                    permutation.add(i,firstElement)
                    permutations.add(permutation)
                }
            }
            return permutations
        }
    }


    @Test
    fun testPermutations(){
        assertEquals(1,getPermutations(listOf(1), listOf()).size)
        for(i in 2..10){
            assertEquals(factorial(i),getPermutations((1..i).toList(), listOf()).size)
        }
    }

    fun factorial(i : Int) : Int {
        return if(i == 0 || i == 1) 1
        else i*factorial(i-1)
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

        if(DEBUG)println(chessBoardNormal.toString())
        if(DEBUG)println(stubChessAI.cnt_movements.toString()+" moves")
    }
}