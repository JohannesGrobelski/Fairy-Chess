package emerald.apps.fairychess.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
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

    @Test
    fun testDraw(){
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val chessBoardNormal = Chessboard(chessFormationArray, figureMap)

        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("3_1_3_2"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("3_6_3_5"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("2_0_3_1"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("2_7_3_6"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("3_1_2_0"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("3_6_2_7"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("2_0_3_1"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("2_7_3_6"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("3_1_2_0"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("3_6_2_7"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("2_0_3_1"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("2_7_3_6"))
        chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("3_1_2_0"))
        chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("3_6_2_7"))

        assert(chessBoardNormal.playerWithDrawOpportunity == "white")
    }

    @Test
    fun testShortCastling(){
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val chessBoardNormal = Chessboard(chessFormationArray, figureMap)
        //castle short on both white and black
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("6_1_6_2")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("6_6_6_5")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("6_0_5_2")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("6_7_5_5")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("5_0_6_1")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("5_7_6_6")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("4_0_6_0")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("4_7_6_7")).isEmpty())
    }

    @Test
    fun test50MovesRule(){
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val chessBoardNormal = Chessboard(chessFormationArray, figureMap)
        //phase 1: place all four rook on B file in the middle
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("7_1_7_3")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("7_6_7_4")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("7_0_7_2")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("7_7_7_5")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("0_1_0_3")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("0_6_0_4")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("0_0_0_2")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("0_7_0_5")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("0_2_1_2")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("0_5_1_5")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("1_2_1_3")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("1_5_1_4")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("7_2_1_2")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("7_5_1_5")).isEmpty())
        //phase 2: reset lastPawnMove by moving pawns
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("6_1_6_3")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("6_6_6_4")).isEmpty())
        //phase 3: systematically move all rooks between file 1 to 6 and back, first inner then outer rooks
        val prePhase3Moves = 16
        //name declaration: inner rook = has 2 neighboring rooks; outer rook = has 1 neighboring rook
        var outerBlackRookFile = 1; var outerBlackRookRank = 5
        var innerBlackRookFile = 1; var innerBlackRookRank = 4
        var innerWhiteRookFile = 1; var innerWhiteRookRank = 3
        var outerWhiteRookFile = 1; var outerWhiteRookRank = 2
        var moveCounter = 0
        while(true){
            //move white
            if(moveCounter % 16 == 0){//move outer white Rook
                val newInnerWhiteRookFile = max(1,innerWhiteRookFile+1)%6
                assert(chessBoardNormal.move("white",
                    ChessPiece.Movement.fromStringToMovement(
                        innerWhiteRookFile.toString()+"_"+innerWhiteRookRank+"_"+newInnerWhiteRookFile+"_"+innerWhiteRookRank)).isEmpty())
                innerWhiteRookFile = newInnerWhiteRookFile;
            } else {//move inner white rook
                val newOuterWhiteRookFile = max(1,outerWhiteRookFile+1)%8
                assert(chessBoardNormal.move("white",
                    ChessPiece.Movement.fromStringToMovement(
                        outerWhiteRookFile.toString()+"_"+outerWhiteRookRank+"_"+newOuterWhiteRookFile+"_"+outerWhiteRookRank)).isEmpty())
                outerWhiteRookFile = newOuterWhiteRookFile;
                ++moveCounter
            }
            //move black
            if(moveCounter % 16 == 0){//move outer white Rook
                val newInnerBlackRookFile = max(1,innerBlackRookFile+1)%6
                assert(chessBoardNormal.move("black",
                    ChessPiece.Movement.fromStringToMovement(
                        innerBlackRookFile.toString()+"_"+innerBlackRookRank+"_"+newInnerBlackRookFile+"_"+innerBlackRookRank)).isEmpty())
                innerBlackRookFile = newInnerBlackRookFile;
                moveCounter += 2
            } else {//move inner white rook
                val newOuterBlackRookFile = max(1,outerBlackRookFile+1)%8
                assert(chessBoardNormal.move("black",
                    ChessPiece.Movement.fromStringToMovement(
                        outerBlackRookFile.toString()+"_"+outerBlackRookRank+"_"+newOuterBlackRookFile+"_"+outerBlackRookRank)).isEmpty())
                outerBlackRookFile = newOuterBlackRookFile;
                ++moveCounter
            }

            if(moveCounter >= 50 - prePhase3Moves){
                assert(chessBoardNormal.playerWithDrawOpportunity.isNotEmpty())
                break
            } else if(moveCounter < 50 - prePhase3Moves){
                assert(chessBoardNormal.playerWithDrawOpportunity.isEmpty())
                //println("rep.-pos.:\n $chessBoardNormal")
            }
        }
    }


    @Test
    fun testLongCastling(){
        val chessFormationArray = parseChessFormation("normal_chess")
        val figureMap = parseFigureMapFromFile()
        val chessBoardNormal = Chessboard(chessFormationArray, figureMap)
        //castle long on both white and black
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("2_1_2_2")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("2_6_2_5")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("3_0_2_1")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("3_7_2_6")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("1_1_1_2")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("1_6_1_5")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("2_0_1_1")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("2_7_1_6")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("1_0_0_2")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("1_7_0_5")).isEmpty())
        assert(chessBoardNormal.move("white", ChessPiece.Movement.fromStringToMovement("4_0_2_0")).isEmpty())
        assert(chessBoardNormal.move("black", ChessPiece.Movement.fromStringToMovement("4_7_2_7")).isEmpty())
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