package emerald.apps.fairychess.utility

import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.InputStream


class ChessFormationParser {
    companion object {
        private val TAG: String = "ChessFormationParser"
        private fun parseChessFormationString(chessFormation: String) : Array<Array<String>> {
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

        fun parseChessFormation(context: Context, fileName: String) : Array<Array<String>> {
            try {
                val inputStream = context.resources.openRawResource(
                    context.resources.getIdentifier(
                        fileName,
                        "raw", context.packageName
                    )
                )
                return invert2DArray(
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

        fun invert2DArray(inputArray: Array<Array<String>>) : Array<Array<String>>{
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