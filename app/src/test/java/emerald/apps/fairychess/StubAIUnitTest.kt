package emerald.apps.fairychess

import androidx.test.ext.junit.runners.AndroidJUnit4
import emerald.apps.fairychess.model.ChessPiece
import emerald.apps.fairychess.model.Chessboard
import emerald.apps.fairychess.model.ChessAI
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
class StubAIUnitTest {

    @Test
    fun testAlgorithmCapturePiece() {
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val chessBoardNormal = Chessboard(chessFormationArray, figureMap)
        val stubChessAI = ChessAI("black")
        //open
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("4_1_4_3")).isEmpty())
        assert(chessBoardNormal.move("black",stubChessAI.calcMove(chessBoardNormal)!!).isEmpty())

        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("4_3_4_4")).isEmpty())
        assert(chessBoardNormal.move("black",stubChessAI.calcMove(chessBoardNormal)!!).isEmpty())

        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("4_4_4_5")).isEmpty())
        assert(chessBoardNormal.move("black",stubChessAI.calcMove(chessBoardNormal)!!).isEmpty())

        println(chessBoardNormal.toString())
    }

    @Test
    fun testMinimax() {
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val chessBoardNormal = Chessboard(chessFormationArray, figureMap)
        val stubChessAI = ChessAI("black")

        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("1_1_1_3")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("0_6_0_5")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("1_3_1_4")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("0_5_1_4")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("2_1_2_3")).isEmpty())
        assert(chessBoardNormal.move("black", stubChessAI.calcMove(chessBoardNormal)!!).isEmpty())



        println(chessBoardNormal.toString())
        println(stubChessAI.cnt_movements.toString()+" moves")
    }





    fun parseFigureMapFromFile() : Map<String, FigureParser.Figure> {
        try {
            val absPath = "C:\\Users\\johan\\OneDrive\\Documents\\GitHub\\Fairy-Chess\\app\\src\\main\\res\\raw\\figures.json"
            val initialFile = File(absPath)
            val inputStream: InputStream = FileInputStream(initialFile)
            return FigureParser.parseFigureMapFromJSONString(
                ChessFormationParser.convertStreamToString(
                    inputStream
                )
            )
        } catch (e: Exception){
            println(e.message.toString())
        }
        return mapOf()
    }


    private fun parseChessFormation(mode:String) : Array<Array<String>> {
        try {
            val absPath =
                "C:\\Users\\johan\\OneDrive\\Documents\\GitHub\\Fairy-Chess\\app\\src\\main\\res\\raw\\$mode.chessformation"
            val initialFile = File(absPath)
            val inputStream: InputStream = FileInputStream(initialFile)
            return ChessFormationParser.rotate2DArray(
                ChessFormationParser.parseChessFormationString(
                    ChessFormationParser.convertStreamToString(
                        inputStream
                    )
                )
            )
        } catch (e: Exception){
            println(e.message.toString())
        }
        return arrayOf()
    }
}
