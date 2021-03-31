package emerald.apps.fairychess.utility

import android.content.Context
import org.json.JSONObject


class FigureParser {

    data class Figure(val name: String, val value: Int, val movementParlett: String)

    companion object {
        private val TAG: String = "FigureParser"


        fun parseFigureMapFromFile(context: Context, fileName: String) : Map<String,Figure> {
            try {
                val inputStream = context.resources.openRawResource(
                    context.resources.getIdentifier(
                        fileName,
                        "raw", context.packageName
                    )
                );
                "C:\\Users\\johan\\OneDrive\\Documents\\GitHub\\Fairy-Chess\\app\\src\\main\\res\\raw\\$fileName"
                return parseFigureMapFromJSONString(
                    ChessFormationParser.convertStreamToString(
                        inputStream
                    )
                )
            } catch (e: Exception){
                println(e.message.toString())
            }
            return mapOf()
        }

        fun parseFigureMapFromJSONString(jsonString: String): Map<String,Figure> {
            val figureList = mutableMapOf<String,Figure>()
            //erstelle JSON-Objekt aus String
            val outer = JSONObject(jsonString)
            val jsonArray = outer.getJSONArray("figures")
            //gehe durch jsonArray und hole die drei Zitat-Eigenschaften mithilfe der 3 folgenden
            //keys: id, author, text
            for (i in 0 until jsonArray.length()) {
                val figure = jsonArray.getJSONObject(i)
                val name = figure.getString("name")
                val value = figure.getInt("value")
                val movement = figure.getString("movement")
                figureList[name] = Figure(name, value, movement)
            }
            return figureList
        }
    }

}