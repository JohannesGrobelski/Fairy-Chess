package emerald.apps.fairychess.utility

import android.content.Context
import android.util.Log
import android.util.Range
import java.io.IOException
import java.io.InputStream
import java.lang.Math.*
import java.util.*

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
            var outputArray = arrayOf("","","","","","","","")

            outputArray = placeFigureRandomly("king",outputArray,-1,1 until 7)
            val kingIndex = outputArray.toList().indexOf("king")
            outputArray = placeFigureRandomly("rook",outputArray,-1, 0 until kingIndex)
            outputArray = placeFigureRandomly("rook",outputArray,-1,(kingIndex+1)..7)
            outputArray = placeFigureRandomly("bishop",outputArray)
            val bishopIndex = outputArray.toList().indexOf("bishop")
            outputArray = placeFigureRandomly("bishop",outputArray,(bishopIndex+1)%2)
            if(Collections.frequency(outputArray.toList(),"bishop") != 2){
                outputArray = placeFigureRandomly("bishop",outputArray,(bishopIndex+1)%2)
            }

            outputArray = placeFigureRandomly("knight",outputArray)
            outputArray = placeFigureRandomly("knight",outputArray)
            outputArray = placeFigureRandomly("queen",outputArray)
            return outputArray
        }

        fun placeFigureRandomly(figure: String, inputArray: Array<String>, modulo2 : Int = -1, range : IntRange = (0..7)) : Array<String>{
            //find number of free spaces and select a random index
            var freeSpaces = Collections.frequency(inputArray.toList().subList(range.first,range.last+1),"")
            var randomFreeSpaceIndex = (random()*freeSpaces).toInt()
            var freeSpaceCount = 0
            //find the list index of the random free space index
            for(i in range){
                if(inputArray[i] == ""){
                    if((modulo2 == -1 && freeSpaceCount == randomFreeSpaceIndex)
                    || (modulo2 != -1 && i%2 == modulo2)){
                        inputArray[i] = figure
                        return inputArray
                    }
                    ++freeSpaceCount
                }
            }
            return inputArray
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