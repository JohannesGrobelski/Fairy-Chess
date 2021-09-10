package emerald.apps.fairychess.BitboardTests

import emerald.apps.fairychess.model.Bitboard
import emerald.apps.fairychess.model.Bitboard.Companion.generate64BPositionFromCoordinate
import emerald.apps.fairychess.model.ChessGameUnitTest
import emerald.apps.fairychess.model.Movement
import emerald.apps.fairychess.model.MovementNotation
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.ChessFormationParser.Companion.getAllChess960Permutations
import emerald.apps.fairychess.utility.ChessFormationParser.Companion.chessPermArrayToString
import emerald.apps.fairychess.utility.FigureParser
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.*

@kotlin.ExperimentalUnsignedTypes
/** test basic movements of all figure types (pawn,rook,knight,bishop,king,queen) and special moves (en passante and castling)*/
class ChessvariantTest {

    lateinit var chessFormationArray: Array<Array<String>>
    lateinit var figureMap: Map<String, FigureParser.Figure>

    @Before
    fun initNormalChessVariables() {
        chessFormationArray = ChessGameUnitTest.parseChessFormation("normal_chess")
        figureMap = ChessGameUnitTest.parseFigureMapFromFile()
    }

    @Test
    fun testGetAllChess960Permutations(){
        //check size
        var allPermutations : Set<String> = getAllChess960Permutations()
        //check rules
        for(permutation in allPermutations){
            val leftRookIndex = permutation.indexOf('r')
            val rightRookIndex = permutation.indexOf('r',leftRookIndex+1)
            val kingIndex = permutation.indexOf('k')
            val firstBishopIndex = permutation.indexOf('b')
            val secondBishopIndex = permutation.indexOf('b',firstBishopIndex+1)
            assertTrue(kingIndex>leftRookIndex)
            assertTrue(kingIndex<rightRookIndex)
            assertTrue(firstBishopIndex%2 != secondBishopIndex%2)
            assertEquals(2,Collections.frequency(permutation.toList(),'r'))
            assertEquals(2,Collections.frequency(permutation.toList(),'n'))
            assertEquals(2,Collections.frequency(permutation.toList(),'b'))
            assertEquals(1,Collections.frequency(permutation.toList(),'k'))
            assertEquals(1,Collections.frequency(permutation.toList(),'q'))
        }
        assertEquals(960,allPermutations.size)
    }


    @Test
    fun testCastlingChess960() {
        for(permString in getAllChess960Permutations()){
            val onlyRooksAndKingPermString = permString.replace("n|q|b".toRegex(), " ")
            val chess960FormationArray =
                parseChessFormation(chessFormationArray, onlyRooksAndKingPermString)
            val bitboard = Bitboard(chess960FormationArray, figureMap, isChess960 = true)

            val kingRank = permString.indexOf('k')
            val rookRankLeft = permString.indexOf('r')
            val rookRankRight = permString.indexOf('r', permString.indexOf('r') + 1)
            var kingMove = bitboard.getTargetMovements(
                "king",
                "white",
                Bitboard.Companion.Coordinate(kingRank, 0),
                false
            )
            //white castling moves
            var copyBitboard = bitboard.clone()
            var moves = bitboard.getTargetMovements(
                "king", "white",
                Bitboard.Companion.Coordinate(kingRank, 0), true
            )

            //calculate which castling moves are possible, therefore where king can move to
            var shortCastlingPossible =
                bitboard.getCastlingRights("white").contains(MovementNotation.CASTLING_SHORT_WHITE)
            var longCastlingPossible =
                bitboard.getCastlingRights("white").contains(MovementNotation.CASTLING_LONG_WHITE)
            assertTrue(shortCastlingPossible)
            assertTrue(longCastlingPossible)
            var expectedMoves = kingMove
            if (shortCastlingPossible) expectedMoves =
                expectedMoves or generate64BPositionFromCoordinate(
                    Bitboard.Companion.Coordinate(
                        rookRankRight,
                        0
                    )
                )
            if (longCastlingPossible) expectedMoves =
                expectedMoves or generate64BPositionFromCoordinate(
                    Bitboard.Companion.Coordinate(
                        rookRankLeft,
                        0
                    )
                )
            assertEquals(expectedMoves, moves)
            //long white castling
            if (longCastlingPossible) {
                assertEquals(
                    "",
                    bitboard.checkMoveAndMove("white", Movement(kingRank, 0, rookRankLeft, 0))
                )
                assertEquals(4uL, bitboard.bbFigures["king"]!![0])
                val bbNewRook = generate64BPositionFromCoordinate(
                    Bitboard.Companion.Coordinate(
                        rookRankRight,
                        0
                    )
                ) or generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(3, 0))
                assertEquals(bbNewRook, bitboard.bbFigures["rook"]!![0])
                bitboard.set(copyBitboard)
            }
            //short white castling
            if (shortCastlingPossible) {
                assertEquals(
                    "",
                    bitboard.checkMoveAndMove("white", Movement(kingRank, 0, rookRankRight, 0))
                )
                assertEquals(64uL, bitboard.bbFigures["king"]!![0])
                val bbNewRook = generate64BPositionFromCoordinate(
                    Bitboard.Companion.Coordinate(
                        rookRankLeft,
                        0
                    )
                ) or generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(5, 0))
                assertEquals(bbNewRook, bitboard.bbFigures["rook"]!![0])
                bitboard.set(copyBitboard)
            }


            //black castling moves
            bitboard.moveColor = "black"
            kingMove = bitboard.getTargetMovements(
                "king",
                "black",
                Bitboard.Companion.Coordinate(kingRank, 7),
                false
            )
            copyBitboard = bitboard.clone()
            moves = bitboard.getTargetMovements(
                "king", "black",
                Bitboard.Companion.Coordinate(kingRank, 7), true
            )
            //calculate which castling moves are possible, therefore where king can move to
            shortCastlingPossible =
                bitboard.getCastlingRights("black").contains(MovementNotation.CASTLING_SHORT_BLACK)
            longCastlingPossible =
                bitboard.getCastlingRights("black").contains(MovementNotation.CASTLING_LONG_BLACK)
            //TODO: assertTrue(shortCastlingPossible)
            //TODO: assertTrue(longCastlingPossible)
            expectedMoves = kingMove
            if (shortCastlingPossible) expectedMoves =
                expectedMoves or generate64BPositionFromCoordinate(
                    Bitboard.Companion.Coordinate(
                        rookRankRight,
                        7
                    )
                )
            if (longCastlingPossible) expectedMoves =
                expectedMoves or generate64BPositionFromCoordinate(
                    Bitboard.Companion.Coordinate(
                        rookRankLeft,
                        7
                    )
                )
            assertEquals(expectedMoves, moves)
            //long black castling
            if (longCastlingPossible) {
                assertEquals(
                    "",
                    bitboard.checkMoveAndMove("black", Movement(kingRank, 7, rookRankLeft, 7))
                )
                assertEquals(288230376151711744uL, bitboard.bbFigures["king"]!![1])
                val bbNewRook = generate64BPositionFromCoordinate(
                    Bitboard.Companion.Coordinate(
                        rookRankRight,
                        7
                    )
                ) or generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(3, 7))
                assertEquals(bbNewRook, bitboard.bbFigures["rook"]!![1])
                bitboard.set(copyBitboard)
            }
            //short black castling
            if (shortCastlingPossible) {
                assertEquals(
                    "",
                    bitboard.checkMoveAndMove("black", Movement(kingRank, 7, rookRankRight, 7))
                )
                assertEquals(4611686018427387904uL, bitboard.bbFigures["king"]!![1])
                val bbNewRook = generate64BPositionFromCoordinate(
                    Bitboard.Companion.Coordinate(
                        rookRankLeft,
                        7
                    )
                ) or generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(5, 7))
                assertEquals(bbNewRook, bitboard.bbFigures["rook"]!![1])
            }
        }
    }


    @Test
    fun testMovegenerationGrashopper(){
        chessFormationArray = ChessGameUnitTest.parseChessFormation("grasshopper_chess")
        val bitboard = Bitboard(chessFormationArray,figureMap)
        var moves = bitboard.getTargetMovements("grasshopper","black",
            Bitboard.Companion.Coordinate(3,6),true)
        Assert.assertEquals(180388626432uL, moves)

        Assert.assertEquals("", bitboard.move("white", Movement(4, 1, 4, 3)))
        Assert.assertEquals("", bitboard.move("black", Movement(5, 6, 5, 4)))
        Assert.assertEquals("", bitboard.move("white", Movement(4, 3, 4, 6)))
        moves = bitboard.getTargetMovements("grasshopper","black",
            Bitboard.Companion.Coordinate(0,6),true)
        Assert.assertEquals(21474836480uL, moves)
        Assert.assertEquals("", bitboard.move("black", Movement(4, 6, 4, 6)))
    }


    fun parseChessFormation(originalChessFormation : Array<Array<String>>, chessFormationString : String) : Array<Array<String>> {
        try {
            return if(chessFormationString.isNotEmpty()) ChessFormationParser.generateChess960Position(
                originalChessFormation,
                chessFormationString
            )
            else originalChessFormation
        } catch (e: Exception){
            println(e.message.toString())
        }
        return arrayOf()
    }


}
