package emerald.apps.fairychess.utility

import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.util.regex.Matcher
import java.util.regex.Pattern


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

        public fun parseChessFormation(context: Context, fileName: String) : Array<Array<String>> {
            try {
                val inputStream = context.resources.openRawResource(
                    context.resources.getIdentifier(
                        fileName,
                        "raw", context.packageName
                    )
                );

                "C:\\Users\\johan\\OneDrive\\Documents\\GitHub\\Fairy-Chess\\app\\src\\main\\res\\raw\\$fileName"
                return parseChessFormationString(
                    convertStreamToString(
                        inputStream
                    )
                )
            } catch (e: Exception){
                println(e.message.toString())
            }
            return arrayOf()
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