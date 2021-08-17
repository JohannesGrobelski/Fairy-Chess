package emerald.apps.fairychess.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.ChessFormationParser.Companion.parseChessFormation
import emerald.apps.fairychess.utility.FigureParser
import emerald.apps.fairychess.utility.FigureParser.Companion.parseFigureMapFromFile
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.max

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ChessGameUnitTest {




    @Test
    fun simpleCheckmate() {
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val chessBoardNormal = Bitboard(chessFormationArray, figureMap)
        chessBoardNormal.move("white", Movement.fromStringToMovement("5_1_5_2"))
        chessBoardNormal.move("black", Movement.fromStringToMovement("4_6_4_5"))
        chessBoardNormal.move("white", Movement.fromStringToMovement("6_1_6_3"))
        chessBoardNormal.move("black", Movement.fromStringToMovement("3_7_7_3"))
        chessBoardNormal.move("white", Movement.fromStringToMovement("6_0_7_2"))
        chessBoardNormal.move("black", Movement.fromStringToMovement("7_3_4_0"))

        println(chessBoardNormal.toString())
    }

    companion object {
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

        fun parseChessFormation(mode:String) : Array<Array<String>> {
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



}