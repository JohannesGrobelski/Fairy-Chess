package emerald.apps.fairychess.controller

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import emerald.apps.fairychess.R
import emerald.apps.fairychess.model.pieces.Chessboard
import emerald.apps.fairychess.view.ChessActivity
import kotlinx.android.synthetic.main.activity_chess.*

class ChessActivityListener() {

    private lateinit var chessActivity : ChessActivity
    private lateinit var chessboard: Chessboard

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

    //Views
    var elterLayout: LinearLayout? = null
    private lateinit var imageViews: Array<Array<ImageView>>

    constructor(chessActivity: ChessActivity) : this() {
        this.chessActivity = chessActivity
        chessboard = Chessboard()
        current_color = "white"

        initViews()
        displayFigures()
    }


    fun player_action(v: View){

    }


    private fun displayFigures() {
        for (i in 0..7) {
            for (j in 0..7) {
                val x: Int = getDrawableFromName(
                    chessboard.pieces[i][j].name,
                    chessboard.pieces[i][j].color
                )
                if (x != -1) imageViews[i][j].setImageResource(x)
            }
        }
    }

    private fun initViews() {
        elterLayout = chessActivity.findViewById<LinearLayout>(R.id.elterLayout)
        imageViews = arrayOf(
            arrayOf(
                chessActivity.A1, chessActivity.A2, chessActivity.A3, chessActivity.A4,
                chessActivity.A5,
                chessActivity.A6,
                chessActivity.A7,
                chessActivity.A8
            ),
            arrayOf(
                chessActivity.B1,
                chessActivity.B2,
                chessActivity.B3,
                chessActivity.B4,
                chessActivity.B5,
                chessActivity.B6,
                chessActivity.B7,
                chessActivity.B8
            ),
            arrayOf(
                chessActivity.C1,
                chessActivity.C2,
                chessActivity.C3,
                chessActivity.C4,
                chessActivity.C5,
                chessActivity.C6,
                chessActivity.C7,
                chessActivity.C8
            ),
            arrayOf(
                chessActivity.D1,
                chessActivity.D2,
                chessActivity.D3,
                chessActivity.D4,
                chessActivity.D5,
                chessActivity.D6,
                chessActivity.D7,
                chessActivity.D8
            ),
            arrayOf(
                chessActivity.E1,
                chessActivity.E2,
                chessActivity.E3,
                chessActivity.E4,
                chessActivity.E5,
                chessActivity.E6,
                chessActivity.E7,
                chessActivity.E8
            ),
            arrayOf(
                chessActivity.F1,
                chessActivity.F2,
                chessActivity.F3,
                chessActivity.F4,
                chessActivity.F5,
                chessActivity.F6,
                chessActivity.F7,
                chessActivity.F8
            ),
            arrayOf(
                chessActivity.G1,
                chessActivity.G2,
                chessActivity.G3,
                chessActivity.G4,
                chessActivity.G5,
                chessActivity.G6,
                chessActivity.G7,
                chessActivity.G8
            ),
            arrayOf(
                chessActivity.H1,
                chessActivity.H2,
                chessActivity.H3,
                chessActivity.H4,
                chessActivity.H5,
                chessActivity.H6,
                chessActivity.H7,
                chessActivity.H8
            )
        )
    }

    fun getDrawableFromName(type: String, color: String): Int {
        if (color == "white" && type == "king") {
            return R.drawable.white_king
        } else if (color == "white" && type == "queen") {
            return R.drawable.white_queen
        } else if (color == "white" && type == "pawn") {
            return R.drawable.white_pawn
        } else if (color == "white" && type == "bishop") {
            return R.drawable.white_bishop
        } else if (color == "white" && type == "knight") {
            return R.drawable.white_knight
        } else if (color == "white" && type == "rook") {
            return R.drawable.white_rook
        } else if (color == "black" && type == "king") {
            return R.drawable.black_king
        } else if (color == "black" && type == "queen") {
            return R.drawable.black_queen
        } else if (color == "black" && type == "pawn") {
            return R.drawable.black_pawn
        } else if (color == "black" && type == "bishop") {
            return R.drawable.black_bishop
        } else if (color == "black" && type == "knight") {
            return R.drawable.black_knight
        } else if (color == "black" && type == "rook") {
            return R.drawable.black_rook
        }
        return -1
    }
}
