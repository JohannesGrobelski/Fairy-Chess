package emerald.apps.fairychess

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

class FairyStockfishWrapperTest {

    // Native method declarations
    private external fun initializeEngine()
    private external fun setPosition(fen: String)
    private external fun makeMove(uciMove: String): String
    private external fun getLegalMoves(square: String): Array<String>
    private external fun getCurrentFen(): String
    private external fun getPiece(square: String): Pair<String, String>
    private external fun quit()
    private external fun getAIMove(colorStr: String): String

    @Before
    fun setup() {
        System.loadLibrary("stockfish")
        initializeEngine()
    }

    @Test
    fun testLibraryLoading() {
        try {
            System.loadLibrary("stockfish")
            println("Successfully loaded stockfish library")
        } catch (e: UnsatisfiedLinkError) {
            println("Failed to load library. Available libraries:")
            System.getProperty("java.library.path")?.split(":")?.forEach {
                println("Directory: $it")
                File(it).listFiles()?.forEach { file ->
                    println("  - ${file.name}")
                }
            }
            throw e
        }
    }

    @After
    fun tearDown() {
        quit()
    }

    @Test
    fun testInitialPosition() {
        val startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        setPosition(startingFen)
        assertEquals(startingFen, getCurrentFen())
    }

    @Test
    fun testPieceIdentification() {
        val startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        setPosition(startingFen)

        // Test white pieces
        assertEquals(Pair("ROOK", "WHITE"), getPiece("a1"))
        assertEquals(Pair("KNIGHT", "WHITE"), getPiece("b1"))
        assertEquals(Pair("BISHOP", "WHITE"), getPiece("c1"))
        assertEquals(Pair("QUEEN", "WHITE"), getPiece("d1"))
        assertEquals(Pair("KING", "WHITE"), getPiece("e1"))
        assertEquals(Pair("PAWN", "WHITE"), getPiece("e2"))

        // Test black pieces
        assertEquals(Pair("ROOK", "BLACK"), getPiece("a8"))
        assertEquals(Pair("KNIGHT", "BLACK"), getPiece("b8"))
        assertEquals(Pair("BISHOP", "BLACK"), getPiece("c8"))
        assertEquals(Pair("QUEEN", "BLACK"), getPiece("d8"))
        assertEquals(Pair("KING", "BLACK"), getPiece("e8"))
        assertEquals(Pair("PAWN", "BLACK"), getPiece("e7"))
    }

    @Test
    fun testLegalMoves() {
        val startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        setPosition(startingFen)

        // Test knight moves from b1
        val knightMoves = getLegalMoves("b1").toSet()
        assertTrue(knightMoves.containsAll(setOf("b1a3", "b1c3")))
        assertEquals(2, knightMoves.size)

        // Test pawn moves from e2
        val pawnMoves = getLegalMoves("e2").toSet()
        assertTrue(pawnMoves.containsAll(setOf("e2e3", "e2e4")))
        assertEquals(2, pawnMoves.size)
    }

    @Test
    fun testMakeMove() {
        val startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        setPosition(startingFen)

        // Make a move and verify the new position
        val newFen = makeMove("e2e4")
        assertTrue(newFen.isNotEmpty())
        assertEquals(newFen, getCurrentFen())

        // Verify the pawn has moved
        assertEquals(Pair("PAWN", "WHITE"), getPiece("e4"))
    }

    @Test
    fun testIllegalMove() {
        val startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        setPosition(startingFen)

        // Try to make an illegal move
        val result = makeMove("e2e5")
        assertEquals("", result)
    }

    @Test
    fun testAIMove() {
        val startingFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        setPosition(startingFen)

        // Get AI move for white
        val whiteMove = getAIMove("WHITE")
        assertTrue(whiteMove.isNotEmpty())

        // Make the AI's move and verify it's legal
        val newFen = makeMove(whiteMove)
        assertTrue(newFen.isNotEmpty())
    }
}