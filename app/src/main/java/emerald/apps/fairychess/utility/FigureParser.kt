package emerald.apps.fairychess.utility

import android.content.Context
import org.json.JSONObject

/** parses the figures.json file to a figure map from
 *  name to figure (Map<String,Figure>)*/
class FigureParser {

    data class Figure(val name: String, val value: Int, val movementParlett: String)

    companion object {
        private val TAG: String = "FigureParser"

        /** create inputstream from file and then parse JSON string from inputstream */
        fun parseFigureMapFromFile(context: Context) : Map<String,Figure> {
            try {
                val inputStream = context.resources.openRawResource(
                    context.resources.getIdentifier(
                        "figures",
                        "raw", context.packageName
                    )
                )
                val s =
                    "C:\\Users\\johan\\OneDrive\\Documents\\GitHub\\Fairy-Chess\\app\\src\\main\\res\\raw\\figures"
                val figureMap : MutableMap<String,Figure> = parseFigureMapFromJSONString(
                    ChessFormationParser.convertStreamToString(
                        inputStream
                    )
                ).toMutableMap()
                figureMap["queen"] = Figure("queen",9,"n*")//ensure queen is in figure map
                return figureMap
            } catch (e: Exception){
                println(e.message.toString())
            }
            return mapOf()
        }

        /** parse figures.json string to Map<String,Figure>*/
        fun parseFigureMapFromJSONString(jsonString: String): Map<String,Figure> {
            val figureList = mutableMapOf<String,Figure>()
            //create JSON object from string
            val outer = JSONObject(jsonString)
            val jsonArray = outer.getJSONArray("figures")
            //go through jsonArray and get the three quote properties using the
            //following 4 keys (figure, name, value, movement) and create figure object
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