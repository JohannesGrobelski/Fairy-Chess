package emerald.apps.fairychess.BitboardTests

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import emerald.apps.fairychess.model.*
import emerald.apps.fairychess.model.Bitboard.Companion.generateCoordinatesFrom64BPosition
import emerald.apps.fairychess.model.Bitboard.Companion.getPosition
import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_LONG_BLACK
import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_LONG_WHITE
import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_SHORT_BLACK
import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_SHORT_WHITE
import emerald.apps.fairychess.utility.FigureParser
import junit.framework.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.StringBuilder

/**
 * Parse Moves from chess database (games.pgn) to movement
 * and test if moves are verified by my bitboard move generation
 */
@RunWith(AndroidJUnit4::class)
class MoveGenerationTests {
    companion object {
        private const val DEBUG = false
        private const val TAG = "ChessboardUnitTest"
        val figureAbbrMap = mapOf("K" to "king", "Q" to "queen", "R" to "rook", "B" to "bishop", "N" to "knight")
        val coordinateMap = mapOf("a" to 0, "b" to 1, "c" to 2, "d" to 3, "e" to 4, "f" to 5, "g" to 6, "h" to 7)

    }

    lateinit var gamesString : String
    lateinit var chessFormationArray : Array<Array<String>>
    lateinit var figureMap : Map<String, FigureParser.Figure>

    fun initNormalChessVariables(){
        chessFormationArray = ChessGameUnitTest.parseChessFormation("normal_chess")
        figureMap = ChessGameUnitTest.parseFigureMapFromFile()
    }

    fun writeGamelistToFile(gameList: MutableList<MutableList<Movement>>) {
        val fileName = "src/main/res/raw/games.CoordinateNotation"
        val myfile = File(fileName)
        for(game in gameList){
            myfile.printWriter().use { out ->
                out.println(gameListToString(game))
            }
        }
    }

    fun readPGNFile(filename:String) : String {
        try {
            val absPath =
                "C:\\Users\\johan\\OneDrive\\Documents\\GitHub\\Fairy-Chess\\app\\src\\main\\res\\raw\\$filename.pgn"
            val initialFile = File(absPath)
            val inputStream: InputStream = FileInputStream(initialFile)
            return convertStreamToString(
                    inputStream
                )
        } catch (e: Exception){
            if(DEBUG)println(e.message.toString())
        }
        return ""
    }

    /** test movegeneration by
     *   1. splitting newlines and formating (resulting in formatedGameString),
     *   2. then numbers and formating (resulting in formatMoveString)
     *   3. then spaces and finding move by parsing target square and finding source square from all possible moves (resulting in whiteMovement/blackMovement)
     *   4. try to apply move to bitboard
     */
    @Test
    fun testMoveGeneration() {
        initNormalChessVariables()
        gamesString = readPGNFile("games")
        val gameStringList = gamesString.split("\n")
        val gameList = mutableListOf<List<Movement>>()
        for(gameString in gameStringList){
            val formatedGameString = formatGameString(gameString)
            if(formatedGameString.isEmpty())continue
            if(DEBUG)println("game "+(gameList.size+1).toString())
            val bitboard = Bitboard(chessFormationArray,figureMap)
            val moveList = mutableListOf<Movement>()
            val movepairList = formatedGameString.replace("  "," ").split("\\d+\\.".toRegex())
            for(movepairString in movepairList){
                if(movepairString.isEmpty())continue
                //parse moveStrings
                var whiteMovestring = formatMoveString(movepairString.trim()); var blackMovestring = ""
                if(movepairString.trim().contains(" ")){
                    whiteMovestring = formatMoveString(movepairString.trim().split(" ")[0])
                    blackMovestring = formatMoveString(movepairString.trim().split(" ")[1])
                }
                //create and check white movement
                val whiteMovement = getMovementFromString("white",whiteMovestring,bitboard)
                assertNotNull(whiteMovement)
                assertEquals("",bitboard.checkMoveAndMove("white",whiteMovement!!))
                if(DEBUG)println(moveList.size.toString()+" "+whiteMovestring)
                if(DEBUG)println(bitboard.toString())
                moveList.add(whiteMovement)
                //create and check black movement
                var blackMovement : Movement? = null
                if(blackMovestring != ""){
                    blackMovement = getMovementFromString("black",blackMovestring,bitboard)
                    assertNotNull(blackMovement)
                    assertEquals("",bitboard.checkMoveAndMove("black",blackMovement!!))
                    if(DEBUG)println(moveList.size.toString()+" "+blackMovestring)
                    if(DEBUG)println(bitboard.toString())
                    moveList.add(blackMovement)
                }
            }
            gameList.add(moveList)
        }
    }

    private fun formatGameString(gameString: String): String {
        var formatedGameString = gameString
        formatedGameString = gameString.replace("  "," ")

        //delete match result comment like 0-1 or 1/2-1/2
        val matchResult = "\\d-\\d|1\\/2-1\\/2".toRegex().find(formatedGameString)
        if(matchResult != null){
            formatedGameString = formatedGameString.replace(matchResult.value,"")
        }

        return formatedGameString.trim()
    }

    fun formatMoveString(movestring: String) : String {
        return movestring
            .replace("+","")
            .replace("#","")
            .replace("=","")
            .replace("?","")
            .replace("!","")
    }

    fun gameListToString(gameList : MutableList<Movement>) : String {
        val result = StringBuilder("[")
        for(move in gameList){
            result.append("["+move.sourceRank+","+move.sourceFile+","+move.targetRank+","+move.targetFile+"]")
        }
        return result.append("]").toString()
    }

    fun getMovementFromString(color:String, movestring : String, bitboard: Bitboard) : Movement? {
        if(movestring.contains("O-O"))return getCastleMovement(color,movestring)
        var name = "pawn"
        var sourceRank = -1
        var sourceFile = -1
        var targetRank = -1
        var targetFile = -1
        var promotion = ""
        if(movestring.matches("[a-z]\\d".toRegex())){//pawn moved
            targetRank = coordinateMap[movestring[0].toString()]!!
            targetFile = movestring[1].toString().toInt() - 1
        }
        else if(movestring.matches("[a-z]x[a-z]\\d(ep)?".toRegex())){//pawn captures (enpassante)
            sourceRank = coordinateMap[movestring[0].toString()]!!
            targetRank = coordinateMap[movestring[2].toString()]!!
            targetFile = movestring[3].toString().toInt() - 1
        }
        else if(movestring.matches("[a-z]?x?[a-z]\\d\\=?[A-Z]".toRegex())) {//pawn promoted
            var sourceFileOffset = 0
            if(movestring.contains("[a-z]x[a-z]\\d".toRegex())){
               sourceFileOffset = 1
               sourceRank = coordinateMap[movestring[0].toString()]!!
            }
            val equalsOffset = (movestring.contains("=")).toInt()
            val xOffset = (movestring.contains("x")).toInt()
            if(figureAbbrMap.containsKey(movestring[sourceFileOffset + xOffset + equalsOffset + 2].toString())){
                promotion = figureAbbrMap[movestring[sourceFileOffset + xOffset + equalsOffset + 2].toString()]!!
            }
            targetRank = coordinateMap[movestring[sourceFileOffset + xOffset].toString()]!!
            targetFile = movestring[sourceFileOffset + xOffset + 1].toString().toInt() - 1
        }
        else if(movestring.matches("[A-Z]x?[a-z]\\d".toRegex())){//piece moved/captured
            if(figureAbbrMap.containsKey(movestring[0].toString())){
                name = figureAbbrMap[movestring[0].toString()]!!
            }
            val xOffset = (movestring.contains("x")).toInt()
            targetRank = coordinateMap[movestring[1+xOffset].toString()]!!
            targetFile = movestring[2+xOffset].toString().toInt() - 1
        }
        else if(movestring.matches("[A-Z][a-z]x?[a-z]\\d".toRegex())){//sourceRank specified
            if(figureAbbrMap.containsKey(movestring[0].toString())){
                name = figureAbbrMap[movestring[0].toString()]!!
            }
            val xOffset = (movestring.contains("x")).toInt()
            sourceRank = coordinateMap[movestring[1].toString()]!!
            targetRank = coordinateMap[movestring[2+xOffset].toString()]!!
            targetFile = movestring[3+xOffset].toString().toInt() - 1
        }
        else if(movestring.matches("[A-Z][0-9]x?[a-z]\\d".toRegex())){//sourceFile specified
            if(figureAbbrMap.containsKey(movestring[0].toString())){
                name = figureAbbrMap[movestring[0].toString()]!!
            }
            val xOffset = (movestring.contains("x")).toInt()
            sourceFile = movestring[1].toString().toInt() - 1
            targetRank = coordinateMap[movestring[2+xOffset].toString()]!!
            targetFile = movestring[3+xOffset].toString().toInt() - 1
        }

        val allMoves = getTargetMovesForFigure(color, name, bitboard)
        for(move in allMoves){
            if(move.targetRank == targetRank && move.targetFile == targetFile){
                if((sourceFile == -1 && sourceRank == -1)
                || (sourceFile == -1 && sourceRank == move.sourceRank)
                || (sourceRank == -1 && sourceFile == move.sourceFile)){
                    return if(promotion.isNotEmpty()){
                        PromotionMovement(move.sourceRank,move.sourceFile,targetRank,targetFile,promotion)
                    }else Movement(move.sourceRank,move.sourceFile,targetRank,targetFile)
                }
            }
        }
        return null
    }

    fun getCastleMovement(color: String, movestring : String) : Movement? {
        when {
            movestring.matches("O-O".toRegex()) -> {
                return if(color == "white") Movement(CASTLING_SHORT_WHITE,4,0,6,0)
                else Movement(CASTLING_SHORT_BLACK,4,7,6,7)
            }
            movestring.matches("O-O-O".toRegex()) -> {
                return if(color == "white") Movement(CASTLING_LONG_WHITE,4,0,2,0)
                else Movement(CASTLING_LONG_BLACK,4,7,2,7)
            }
        }
        return null
    }

    fun getTargetMovesForFigure(color:String, name : String, bitboard: Bitboard) : MutableList<Movement> {
        val allMoves = mutableListOf<Movement>()
        val pos = getPosition(color)
        if(!bitboard.bbFigures.containsKey(name))return mutableListOf()
        for(coordinate in generateCoordinatesFrom64BPosition(bitboard.bbFigures[name]!![pos])){
            allMoves.addAll(bitboard.getTargetMovementsAsMovementList(color,coordinate))
        }
        return allMoves
    }


    fun convertStreamToString(inputStream: InputStream): String {
        //create bufferedreader from InputStreamReader
        //and get input file by file from InputStream through bufferedReader
        val stringBuilder = StringBuilder("")
        val bufferedReader = inputStream.bufferedReader()
        try {
            val iterator = bufferedReader.lineSequence().iterator()
            val game = StringBuilder("")
            while(iterator.hasNext()) {
                val line = iterator.next()
                if(line.startsWith("[")) {
                    continue
                }
                else if(line == "") {
                    if(game.isNotEmpty()){
                        stringBuilder.append(game.toString()+"\n")
                        game.clear()
                    }
                }
                else game.append(line.replace("\n"," ")).append(" ")
            }
            bufferedReader.close()
        } catch (e: IOException) {
            Log.e(TAG, e.message!!)
        }
        return stringBuilder.toString().replace("\n\n\n","\n")
    }

}