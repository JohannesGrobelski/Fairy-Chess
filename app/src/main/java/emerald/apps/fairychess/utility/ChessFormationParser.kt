package emerald.apps.fairychess.utility

import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.lang.Math.*

/** parse chessformation-File to a 2D-Array of figure-String (Array<Array<String>>)
 * in (row,file) format */
class ChessFormationParser {
    companion object {
        const val CHESS960 = "chess960"
        private val TAG: String = "ChessFormationParser"

        /** parse chessFormation string by splitting brackets and then, comma's */
        fun parseChessFormationString(chessFormation: String) : Array<Array<String>> {
            var fileList = mutableListOf<Array<String>>()
            var chessFormationWOC = chessFormation.replace("[\\s]*\\/\\/[^\\n]*".toRegex(), "")
            val matchedResults = Regex(pattern = "\\[.+\\]").findAll(input = chessFormationWOC)
            for (matchedText in matchedResults) {
                val array = matchedText.value.
                    replace("[","")
                    .replace("]","")
                    .split(",").toTypedArray()
                fileList.add(array)
            }
            return fileList.toTypedArray()
        }

        /** create inputstream from file and then parse JSON string from inputstream */
        fun parseChessFormation(context: Context, fileName: String, isChess960 : Boolean = false) : Array<Array<String>> {
            try {
                val inputStream = context.resources.openRawResource(
                    context.resources.getIdentifier(
                        fileName,
                        "raw", context.packageName
                    )
                )
                val originalChessFormation = rotate2DArray(
                    parseChessFormationString(
                        convertStreamToString(
                            inputStream
                        )
                    )
                )
                return if(isChess960)generateChess960Position(originalChessFormation)
                else originalChessFormation
            } catch (e: Exception){
                println(e.message.toString())
            }
            return arrayOf()
        }

        fun generateChess960Position(inputArray: Array<Array<String>>): Array<Array<String>> {
            val chessPermutation = getChess960Permutation()
            val rotatedArray = rotate2DArray(inputArray)
            return arrayOf(
                chessPermutation,
                rotatedArray[1].clone(), rotatedArray[2].clone(), rotatedArray[3].clone(),
                rotatedArray[4].clone(), rotatedArray[5].clone(), rotatedArray[6].clone(),
                chessPermutation,
            )
        }



        fun getChess960Permutation() : Array<String> {
            val outputArray = arrayOf("","","","","","","","")
            var firstBishop = -1; var secondBishop = -1
            var firstRook = -1; var secondRook = -1
            var king = -1
            var firstKnight = -1; var secondKnight = -1

            //place first bishop
            firstBishop = (0..7).toList()[(Math.random()*8).toInt()]
            outputArray[firstBishop] = "bishop"
            //place second bishop
            while(secondBishop == -1 || secondBishop == firstBishop%2){
                secondBishop = (0..7).toList()[(Math.random()*8).toInt()]
            }
            outputArray[secondBishop] = "bishop"
            //place first rook
            while(firstRook == -1 || outputArray[firstRook].isNotEmpty()){
                firstRook = (0..7).toList()[(Math.random()*8).toInt()]
            }
            outputArray[firstRook] = "rook"
            //place second rook
            while(secondRook == -1 || outputArray[secondRook].isNotEmpty()){
                val leftRook = min(firstRook,secondRook)
                val rightRook = max(firstRook,secondRook)
                var placeForKing = false
                for(i in leftRook+1 until rightRook){
                    if(outputArray[i].isEmpty()){
                        placeForKing = (secondRook != -1 && outputArray[secondRook].isEmpty())
                        break
                    }
                }
                if(placeForKing)break
                secondRook = (0..7).toList()[(Math.random()*8).toInt()]
            }
            outputArray[secondRook] = "rook"
            //place king
            val leftRook = min(firstRook,secondRook)
            val rightRook = max(firstRook,secondRook)
            while(king == -1 || outputArray[king].isNotEmpty()){
                king = (leftRook+1 until rightRook).toList()[(Math.random()*abs(secondRook - firstRook)).toInt()]
            }
            outputArray[king] = "king"
            //place first knight
            while(firstKnight == -1 || outputArray[firstKnight].isNotEmpty()){
                firstKnight = (0..7).toList()[(Math.random()*8).toInt()]
            }
            outputArray[firstKnight] = "knight"
            //place second knight
            while(secondKnight == -1 || outputArray[secondKnight].isNotEmpty()){
                secondKnight = (0..7).toList()[(Math.random()*8).toInt()]
            }
            outputArray[secondKnight] = "knight"
            //place queen
            for(i in 0..7){
                if(outputArray[i].isEmpty()){
                    outputArray[i] = "queen"
                    break
                }
            }
            return outputArray
        }



        /** rotate the 2D array like this:
         *  AB  to   AC
         *  CD       CD
         *
         * */
        fun rotate2DArray(inputArray: Array<Array<String>>) : Array<Array<String>>{
            //create empty array
            var outputArray = arrayOf<Array<String>>()
            for (i in 0..inputArray.size-1) {
                var array = arrayOf<String>()
                for (j in 0..inputArray[i].size-1) {
                    array += inputArray[j][i]
                }
                outputArray += array
            }
            return outputArray
        }

        fun convertStreamToString(inputStream: InputStream): String {
            //create bufferedreader from InputStreamReader
            //and get input file by file from InputStream through bufferedReader
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