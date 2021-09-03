package emerald.apps.fairychess

import emerald.apps.fairychess.model.*
import emerald.apps.fairychess.utility.FigureParser
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
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
        println("transpositionTableFails: "+chessAIBlack.transpositionTableFails)
        println("transpositionTableSize: "+chessAIBlack.transpositionTable.size)
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
        val transpoTable = mutableMapOf<ULong,ChessAI.MinimaxResult>()
        transpoTable[hashOriginal] = ChessAI.MinimaxResult(
            Movement.emptyMovement(),0)

        val permutations = getPermutations(movePairs, listOf())
        for(permutation in permutations){
            bitBoardNormal = Bitboard(chessFormationArray, figureMap)
            for(movepair in permutation){
                assertEquals("",bitBoardNormal.move("white",movepair[0]))
                assertEquals("",bitBoardNormal.move("black",movepair[1]))
            }
            assertTrue(transpoTable.containsKey(zobristHash.generateHash(bitBoardNormal)))
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
    fun testTrapsForAI() {
        var bitboard = Bitboard(chessFormationArray, figureMap)
        val chessAi = ChessAI("black")
        var aiMove : Movement

        //simple trap: ai can capture pawn but loses rook
        assertEquals("",bitboard.checkMoveAndMove("white", Movement(2,1,2,3)))
        assertEquals("",bitboard.checkMoveAndMove("black", Movement(0,6,0,4)))
        assertEquals("",bitboard.checkMoveAndMove("white", Movement(1,1,1,3)))
        assertEquals("",bitboard.checkMoveAndMove("black", Movement(0,4,1,3)))
        assertEquals("",bitboard.checkMoveAndMove("white", Movement(3,1,3,2)))
        var calcTime = measureTimeMillis {aiMove = chessAi.calcMove(bitboard)}
        println("calcTime: $calcTime ms")
        println("cnt_movements: "+chessAIBlack.cnt_movements)
        println("transpositionTableHits: "+chessAIBlack.transpositionTableHits)
        println("transpositionTableSize: "+chessAIBlack.transpositionTable.size)
        bitboard.move("black",aiMove)
        assertEquals(bitboard.pointsBlack() - 1,bitboard.pointsWhite())

        println(bitboard.toString())

    }

    @Test
    fun testAIObligatoryAbilities(){
        //ai captures "free" pieces - case 1: kingside-rook captures unprotected pawn
        var bitboard = Bitboard(chessFormationArray, figureMap)
        val chessAi = ChessAI("black",4)
        var aiMove : Movement
        assertEquals("",bitboard.checkMoveAndMove("white", Movement(2,1,2,3)))
        assertEquals("",bitboard.checkMoveAndMove("black", Movement(0,6,0,5)))
        assertEquals("",bitboard.checkMoveAndMove("white", Movement(1,1,1,3)))
        assertEquals("",bitboard.checkMoveAndMove("black", Movement(0,5,0,4)))
        assertEquals("",bitboard.checkMoveAndMove("white", Movement(1,3,0,4)))
        var calcTime = measureTimeMillis {aiMove = chessAi.calcMove(bitboard)}
        bitboard.move("black",aiMove)
        println("move: ${aiMove.asString("black")}")
        println("calcTime: $calcTime ms")
        println("cnt_movements: "+chessAIBlack.cnt_movements)
        println("transpositionTableHits: "+chessAIBlack.transpositionTableHits)
        println("transpositionTableSize: "+chessAIBlack.transpositionTable.size)

       // assertEquals(bitboard.pointsBlack(),bitboard.pointsWhite())

        //(>= depth2) ai doesnt capture protected piece with a more valuable piece (case: rook with queen)


    }

    @Test
    fun testFoolsMate(){
        var bitboard = Bitboard(chessFormationArray, figureMap)
        val chessAi = ChessAI("black",4)
        val moves = arrayOf(
            Movement(4,1,4,2),
            Movement(5,0,2,3),
            Movement(3,0,7,4),
            Movement(7,4,5,6),
            Movement(2,3,5,6),
        )
        var moveCounter = 0
        var foolsMateSuccessful = true
        for(move in moves){
            foolsMateSuccessful = ("" == bitboard.checkMoveAndMove("white",move))
            if(!foolsMateSuccessful)break
            if(!bitboard.gameFinished)assertEquals("",bitboard.move("black",chessAi.calcMove(bitboard)))
        }
        foolsMateSuccessful = bitboard.gameFinished
        println("fools mate accomplished: $foolsMateSuccessful")
        println(bitboard.toString())
    }

    //TODO: test ai still hangs figures

    @Test
    fun test(){
        val bitboard = Bitboard(chessFormationArray, figureMap)
        val chessAi = ChessAI("black")
        val zobristHash = ZobristHash(bitboard.figureMap.keys.toList())
        assertTrue(zobristHash.enpassanteSquareMap.containsKey(Bitboard.Companion.Coordinate(2,2).hashCode()))
    }
}