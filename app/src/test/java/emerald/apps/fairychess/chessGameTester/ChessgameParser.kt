package emerald.apps.fairychess.chessGameTester

import android.content.Context
import android.util.Log
import emerald.apps.fairychess.model.ChessPiece
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/** parse chessformation-File to a 2D-Array of figure-String (Array<Array<String>>)*/
class ChessgameParser {
    companion object {
        private val TAG: String = "ChessFormationParser"

        /** parse chessFormation string by splitting brackets and then, comma's */
        private fun parseChessFormationString(chessGamesString: String) : List<Array<ChessPiece.Movement>> {
            var gameList = mutableListOf<Array<ChessPiece.Movement>>()
            for (matchedText in chessGamesString.split("\n")) {
                val moveStrings = matchedText.split("\\d+\\. ".toRegex()).toTypedArray()
                val moves = mutableListOf<ChessPiece.Movement>()
                for(move in moveStrings){
                    moves.add(convertStringToMovement(move))
                }
                gameList.add(moves.toTypedArray())
            }
            return gameList.toList()
        }

        private fun convertStringToMovement(movementString: String) : ChessPiece.Movement {
            val moveArray = movementString.trim().split(" ")
            return ChessPiece.Movement.fromStringToMovement("")
        }

        /** create inputstream from file and then parse JSON string from inputstream */
         fun parseGamesDB() : Array<Array<ChessPiece.Movement>> {
            try {
                val absPath = "C:\\Users\\johan\\OneDrive\\Documents\\GitHub\\Fairy-Chess\\app\\src\\main\\res\\raw\\gamesdb"
                val initialFile = File(absPath)
                val inputStream: InputStream = FileInputStream(initialFile)
                return parseChessFormationString(
                        convertStreamToString(
                            inputStream
                        )
                    ).toTypedArray()

            } catch (e: Exception){
                println(e.message.toString())
            }
            return arrayOf()
        }



        private fun convertStreamToString(inputStream: InputStream): String {
            //create bufferedreader from InputStreamReader
            //and get input line by line from InputStream through bufferedReader
            val stringBuilder = StringBuilder("")
            val bufferedReader = inputStream.bufferedReader()
            try {
                val iterator = bufferedReader.lineSequence().iterator()
                while(iterator.hasNext()) {
                    val line = iterator.next()
                    stringBuilder.append(line).append("\n")
                }
                bufferedReader.close()
            } catch (e: IOException) {
                Log.e(TAG, e.message!!)
            }
            return stringBuilder.toString()
        }



    }

}