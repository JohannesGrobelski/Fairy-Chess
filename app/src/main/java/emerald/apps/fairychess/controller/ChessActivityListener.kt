package emerald.apps.fairychess.controller

import android.view.View
import emerald.apps.fairychess.view.ChessActivity

class ChessActivityListener(var chessActivity: ChessActivity) {

    //Variables für KI
    var game_mode_ai = true
    var hints = false

    var current_color: String? = null
    var color_human: String? = null
    var color_ai: String? = null

    var selectionName = ""
    var selectionX = -1
    var selectionY = -1
    var marked = false
    var markedX = -1
    var markedY = -1


    fun player_action(v : View){

    }
}
