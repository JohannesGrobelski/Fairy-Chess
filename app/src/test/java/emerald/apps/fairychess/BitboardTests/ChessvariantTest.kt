package emerald.apps.fairychess.BitboardTests

import emerald.apps.fairychess.model.Bitboard
import emerald.apps.fairychess.model.ChessGameUnitTest
import emerald.apps.fairychess.model.Movement
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.ChessFormationParser.Companion.getChess960PermAsString
import emerald.apps.fairychess.utility.FigureParser
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test

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
    fun testCastlingChess960() {
        val permString = "rkbrnbqn" //getChess960PermAsString(), "rkqbbnnr"
        val onlyRooksAndKingPermString = permString.replace("n|q|b".toRegex()," ")
        val chess960FormationArray = parseChessFormation(chessFormationArray,onlyRooksAndKingPermString)
        val bitboard = Bitboard(chess960FormationArray,figureMap,isChess960 = true)

        val kingRank = permString.indexOf('k')
        val rookRankLeft = permString.indexOf('r')
        val rookRankRight = permString.indexOf('r',permString.indexOf('r')+1)
        var kingMove = bitboard.getTargetMovements("king","white",Bitboard.Companion.Coordinate(kingRank,0),false)
        //white castling moves
        var copyBitboard = bitboard.clone()
        var moves = bitboard.getTargetMovements("king","white",
            Bitboard.Companion.Coordinate(kingRank,0),true)
        assertEquals(Bitboard.generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(rookRankLeft,0))
                or Bitboard.generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(rookRankRight,0))
                or kingMove,moves)
        //long white castling
        assertEquals("",bitboard.checkMoveAndMove("white",Movement(kingRank,0,rookRankLeft,0)))
        assertEquals(4uL,bitboard.bbFigures["king"]!![0])
        assertEquals(136uL,bitboard.bbFigures["rook"]!![0])
        bitboard.set(copyBitboard)
        //short white castling
        assertEquals("",bitboard.checkMoveAndMove("white",Movement(kingRank,0,rookRankRight,0)))
        assertEquals(64uL,bitboard.bbFigures["king"]!![0])
        assertEquals(33uL,bitboard.bbFigures["rook"]!![0])
        bitboard.set(copyBitboard)

        //black castling moves
        bitboard.moveColor = "black"
        kingMove = bitboard.getTargetMovements("king","black",Bitboard.Companion.Coordinate(kingRank,7),false)

        copyBitboard = bitboard.clone()
        moves = bitboard.getTargetMovements("king","black",
            Bitboard.Companion.Coordinate(kingRank,7),true)
        assertEquals(Bitboard.generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(rookRankLeft,7))
                or Bitboard.generate64BPositionFromCoordinate(Bitboard.Companion.Coordinate(rookRankRight,7))
                or kingMove,moves)        //long black castling
        assertEquals("",bitboard.checkMoveAndMove("black",Movement(kingRank,7,rookRankLeft,7)))
        assertEquals(288230376151711744uL,bitboard.bbFigures["king"]!![1])
        assertEquals(9799832789158199296uL,bitboard.bbFigures["rook"]!![1])
        //short black castling
        bitboard.set(copyBitboard)
        assertEquals("",bitboard.checkMoveAndMove("black",Movement(kingRank,7,rookRankRight,7)))
        assertEquals(4611686018427387904uL,bitboard.bbFigures["king"]!![1])
        assertEquals(2377900603251621888uL,bitboard.bbFigures["rook"]!![1])
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
