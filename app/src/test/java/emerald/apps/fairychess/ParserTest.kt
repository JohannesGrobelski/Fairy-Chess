package emerald.apps.fairychess

import androidx.test.ext.junit.runners.AndroidJUnit4
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

@RunWith(AndroidJUnit4::class)
class ParserTest {

    @Test
    fun testNormalChessFormation(){
        val chessFormationArray: Array<Array<String>> = parseChessFormation("normal_chess")
        val normalChessFormationArray : Array<Array<String>> =
            arrayOf(arrayOf("rook", "pawn", "", "", "", "", "pawn", "rook"),
                arrayOf("knight", "pawn", "", "", "", "", "pawn", "knight"),
                arrayOf("bishop", "pawn", "", "", "", "", "pawn", "bishop"),
                arrayOf("queen", "pawn", "", "", "", "", "pawn", "queen"),
                arrayOf("king", "pawn", "", "", "", "", "pawn", "king"),
                arrayOf("bishop", "pawn", "", "", "", "", "pawn", "bishop"),
                arrayOf("knight", "pawn", "", "", "", "", "pawn", "knight"),
                arrayOf("rook", "pawn", "", "", "", "", "pawn", "rook"))
        assertArrayEquals(normalChessFormationArray,chessFormationArray)
    }

    @Test
    fun testParsing() {
        val chessFormationArray: Array<Array<String>> = parseChessFormation("normal_chess")
        val figureMap : Map<String, FigureParser.Figure> = parseFigureMapFromFile()

        //test figureArray for some names ...
        for(figureName in listOf("king","queen","bishop","rook","pawn","knight","berolina","grasshopper")){
            assertTrue(figureMap.keys.contains(figureName))
        }
        //... and test values of king for ensuring parsing works right
        assertEquals("king",figureMap["king"]?.name)
        assertEquals(1000,figureMap["king"]?.value)
        assertEquals("1*",figureMap["king"]?.movementParlett)

        //test chessformation array
        for(row in 0..7){
            for(line in 0..7){
                if(line in 2..5){
                    assertEquals("",chessFormationArray[row][line])
                }
                if(line == 1 || line == 6){
                    assertEquals("pawn",chessFormationArray[row][line])
                }
                if(line == 0 || line == 7){
                    if(row == 0 || row == 7)assertEquals("rook",chessFormationArray[row][line])
                    if(row == 1 || row == 6)assertEquals("knight",chessFormationArray[row][line])

                }

            }
        }
        assertTrue(chessFormationArray[0][0] == "rook")
        assertTrue(chessFormationArray[4][1] == "pawn")
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