package emerald.apps.fairychess

import emerald.apps.fairychess.model.Bitboard
import emerald.apps.fairychess.model.ChessAI
import emerald.apps.fairychess.model.ChessGameUnitTest
import emerald.apps.fairychess.utility.FigureParser
import org.junit.Before
import org.junit.Test

class ChessAITest {

    lateinit var chessFormationArray : Array<Array<String>>
    lateinit var figureMap : Map<String, FigureParser.Figure>
    lateinit var chessAI: ChessAI

    @Before
    fun initNormalChessVariables(){
        chessFormationArray = ChessGameUnitTest.parseChessFormation("normal_chess")
        figureMap = ChessGameUnitTest.parseFigureMapFromFile()
        chessAI = ChessAI("black")
    }

    @Test
    fun testSimpleGame(){
        var bitboard = Bitboard(chessFormationArray,figureMap)


        
    }

}