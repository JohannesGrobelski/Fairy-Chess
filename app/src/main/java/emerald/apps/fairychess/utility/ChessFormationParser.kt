package emerald.apps.fairychess.utility

import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.InputStream

/** parse chessformation-File to a 2D-Array of figure-String (Array<Array<String>>)*/
class ChessFormationParser {
    companion object {
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
        fun parseChessFormation(context: Context, fileName: String) : Array<Array<String>> {
            try {
                val inputStream = context.resources.openRawResource(
                    context.resources.getIdentifier(
                        fileName,
                        "raw", context.packageName
                    )
                )
                return rotate2DArray(
                    parseChessFormationString(
                        convertStreamToString(
                            inputStream
                        )
                    )
                )
            } catch (e: Exception){
                println(e.message.toString())
            }
            return arrayOf()
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