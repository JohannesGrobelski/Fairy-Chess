package emerald.apps.fairychess.chessGameTester

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import emerald.apps.fairychess.model.*
import emerald.apps.fairychess.model.Bitboard.Companion.generateCoordinatesFrom64BPosition
import emerald.apps.fairychess.model.Bitboard.Companion.getPosition
import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_LARGE_BLACK
import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_LARGE_WHITE
import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_SMALL_BLACK
import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_SMALL_WHITE
import emerald.apps.fairychess.utility.FigureParser
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.StringBuilder

/**
 * Play games specified in gamesdb.
 */
@RunWith(AndroidJUnit4::class)
class ChessboardUnitTest {
    companion object {
        private const val TAG = "ChessboardUnitTest"
        val figureAbbrMap = mapOf("K" to "king", "Q" to "queen", "R" to "rook", "B" to "bishop", "N" to "knight")
        val coordinateMap = mapOf("a" to 0, "b" to 1, "c" to 2, "d" to 3, "e" to 4, "f" to 5, "g" to 6, "h" to 7)

    }

    private var games : MutableList<MutableList<Movement>> = mutableListOf()
    lateinit var chessFormationArray : Array<Array<String>>
    lateinit var figureMap : Map<String, FigureParser.Figure>

    fun initNormalChessVariables(){
        chessFormationArray = ChessGameUnitTest.parseChessFormation("normal_chess")
        figureMap = ChessGameUnitTest.parseFigureMapFromFile()
    }

    @Before
    fun parseGamesDB(){
        initNormalChessVariables()
        games.add(
            mutableListOf(
                Movement(1,1,1,3),
            )
        )
        games = parsePGNFile("games")
    }

    @Test
    fun parseDB(){
        writeGamelistToFile(games)
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


    @Test
    fun testGamesDB(){
       for(game in games){
           val bitboard = Bitboard(chessFormationArray,figureMap)
           for(move in game){
               Assert.assertEquals("",bitboard.checkMoveAndMove(bitboard.moveColor,move))
           }
       }
    }

    fun parsePGNFile(filename:String) : MutableList<MutableList<Movement>> {
        try {
            val absPath =
                "C:\\Users\\johan\\OneDrive\\Documents\\GitHub\\Fairy-Chess\\app\\src\\main\\res\\raw\\$filename.pgn"
            val initialFile = File(absPath)
            val inputStream: InputStream = FileInputStream(initialFile)
            return parseGamesDBString(
                convertStreamToString(
                    inputStream
                )
            )
        } catch (e: Exception){
            println(e.message.toString())
        }
        return mutableListOf()
    }

    //TODO: erstes game durch fehler im zweiten finden

    /** parse chessFormation string by splitting brackets and then, comma's */
    fun parseGamesDBString(gamesString: String) : MutableList<MutableList<Movement>> {
        val gameStringList = gamesString.split("\n")
        val gamesList = mutableListOf<MutableList<Movement>>()
        for(gameString in gameStringList){
            if(gameString.isEmpty())continue
            val formatedGameString = formatGameString(gameString)
            val bitboard = Bitboard(chessFormationArray,figureMap)
            val gameList = mutableListOf<Movement>()
            val movepairList = formatedGameString.replace("  "," ").split("\\d+\\.".toRegex())
            for(movepair in movepairList){
                if(movepair.isEmpty())continue
                var whiteMovestring = movepair.trim(); var blackMovestring = ""
                if(movepair.trim().contains(" ")){
                    whiteMovestring = formatMoveString(movepair.trim().split(" ")[0])
                    blackMovestring = formatMoveString(movepair.trim().split(" ")[1])
                }
                val whiteMovement = getMovementFromString("white",whiteMovestring,bitboard)
                if(whiteMovement != null){
                    assert(bitboard.checkMoveAndMove("white",whiteMovement).isEmpty())
                    println(gameList.size.toString()+" "+whiteMovestring)
                    println(bitboard.toString())
                }
                var blackMovement : Movement? = null
                if(blackMovestring != ""){
                    blackMovement = getMovementFromString("black",blackMovestring,bitboard)
                    if(blackMovement != null){
                        assert(bitboard.checkMoveAndMove("black",blackMovement).isEmpty())
                        println(gameList.size.toString()+" "+blackMovestring)
                        println(bitboard.toString())
                    }
                }
                if(whiteMovement != null && (blackMovement != null || gameString.endsWith(blackMovestring))){
                    gameList.add(whiteMovement);
                    if(blackMovement!=null)gameList.add(blackMovement)
                } else {
                    if(whiteMovement == null || blackMovement == null){
                        if(whiteMovement==null)Log.e("parseGamesDBString", "whiteMovement not parsable: $whiteMovestring")
                        if(blackMovement==null)Log.e("parseGamesDBString", "whiteMovement not parsable: $blackMovestring")
                        error("Gamestring not parsable: $gameString")
                        gameList.clear()
                        break
                    }
                }
            }
            if(gameList.isNotEmpty()){
                gamesList.add(gameList)
            }
        }
        return gamesList
    }

    private fun formatGameString(gameString: String): String {
        var formatedGameString = gameString
        formatedGameString = gameString.replace("  "," ")

        val matchResult =  "\\d\\-\\d".toRegex().find(formatedGameString)
        if(matchResult != null){
            formatedGameString = formatedGameString.replace(matchResult.value,"")
        }

        return formatedGameString
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
        else if(movestring.matches("[a-z]\\d[A-Z]".toRegex())) {//pawn promoted
            targetRank = coordinateMap[movestring[0].toString()]!!
            targetFile = movestring[1].toString().toInt() - 1
            if(figureAbbrMap.containsKey(movestring[2].toString())){
                promotion = figureAbbrMap[movestring[2].toString()]!!
            }
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
                return if(color == "white") Movement(CASTLING_SMALL_WHITE,4,0,6,0)
                else Movement(CASTLING_SMALL_BLACK,4,7,6,7)
            }
            movestring.matches("O-O-O".toRegex()) -> {
                return if(color == "white") Movement(CASTLING_LARGE_WHITE,4,0,2,0)
                else Movement(CASTLING_LARGE_BLACK,4,7,2,7)
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