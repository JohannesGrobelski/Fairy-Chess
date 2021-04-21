package emerald.apps.fairychess.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ChessboardUnitTest {


    @Test
    fun simpleCheckmate() {
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val chessBoardNormal = Chessboard(chessFormationArray, figureMap)
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("5_1_5_2"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("4_6_4_5"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("6_1_6_3"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("3_7_7_3"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("6_0_7_2"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("7_3_4_0"))

        println(chessBoardNormal.toString())
    }

    @Test
    fun enpassanteCheck(){
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val chessBoardNormal = Chessboard(chessFormationArray, figureMap)
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("5_1_5_3"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("4_6_4_4"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("5_3_5_4"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("6_6_6_4"))
        //first en-passante (white captures black)
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("5_4_6_5")).isEmpty())

        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("4_4_4_3"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("3_1_3_3"))
        //second en-passante (black captures white)
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("4_3_3_2")).isEmpty())

        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("2_1_2_2"))
        //failed en-passante (white only moved one rank)
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("3_2_2_1")).isNotEmpty())
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("3_2_3_1"))

        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("2_2_2_3"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("1_6_1_4"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("2_3_2_4"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("3_6_3_4"))

        //failed en-passante (black did move )
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("2_4_1_5")).isNotEmpty())

        //third en-passante (white captures black)
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("2_4_3_5")).isEmpty())
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
            return ChessFormationParser.invert2DArray(
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