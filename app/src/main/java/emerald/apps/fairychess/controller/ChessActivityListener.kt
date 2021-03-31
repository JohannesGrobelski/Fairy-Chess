package emerald.apps.fairychess.controller

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.graphics.ColorUtils
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
    var selectionRank = -1
    var selectionFile = -1

    //Views
    var elterLayout: LinearLayout? = null
    private lateinit var imageViews: Array<Array<ImageView>> //imageViews[rank][file]

    constructor(chessActivity: ChessActivity) : this() {
        this.chessActivity = chessActivity
        chessboard = Chessboard(chessActivity)
        current_color = "white"

        initViews()
        displayFigures()
    }


    fun player_action(v: View){
        val fullName: String = chessActivity.resources.getResourceName(v.id)
        val name: String = fullName.substring(fullName.lastIndexOf("/") + 1)
        val fileRank = nameToIndex(name)
        val destinationFile = fileRank!![1]
        val destinationRank = fileRank!![0]
        if(selectionRank != -1 && selectionFile != -1
            && destinationFile != -1 && destinationRank != -1){
            val moveResult = chessboard.move(selectionRank,selectionFile,destinationRank,destinationFile)
            displayFigures()
            if(moveResult.isNotEmpty()){
                Toast.makeText(chessActivity,moveResult,Toast.LENGTH_LONG).show()
            }
        }
        markFigure(v)
        if(selectionRank != -1 && selectionFile != -1){
            displayTargetSquares()
        }
    }


    private fun displayFigures() {
        for (file in 0..7) {
            for (rank in 0..7) {
                val x: Int = getDrawableFromName(
                    chessboard.pieces[file][rank].name,
                    chessboard.pieces[file][rank].color
                )
                if (x != -1) imageViews[file][rank].setImageResource(x)
            }
        }
    }

    private fun displayTargetSquares() {
        val targetSquares = chessboard.getTargetSquares(selectionRank,selectionFile)
        for (targetSquare in targetSquares){
            markSquare(targetSquare[0],targetSquare[1])
        }
    }

    private fun initViews() {
        elterLayout = chessActivity.findViewById<LinearLayout>(R.id.elterLayout)
        imageViews = arrayOf(
            arrayOf(
                chessActivity.A1, chessActivity.B1, chessActivity.C1, chessActivity.D1,
                chessActivity.E1, chessActivity.F1, chessActivity.G1, chessActivity.H1
            ),
            arrayOf(
                chessActivity.A2, chessActivity.B2, chessActivity.C2, chessActivity.D2,
                chessActivity.E2, chessActivity.F2, chessActivity.G2, chessActivity.H2
            ),
            arrayOf(
                chessActivity.A3, chessActivity.B3, chessActivity.C3, chessActivity.D3,
                chessActivity.E3, chessActivity.F3, chessActivity.G3, chessActivity.H3
            ),
            arrayOf(
                chessActivity.A4, chessActivity.B4, chessActivity.C4, chessActivity.D4,
                chessActivity.E4, chessActivity.F4, chessActivity.G4, chessActivity.H4
            ),
            arrayOf(
                chessActivity.A5, chessActivity.B5, chessActivity.C5, chessActivity.D5,
                chessActivity.E5, chessActivity.F5, chessActivity.G5, chessActivity.H5
            ),
            arrayOf(
                chessActivity.A6, chessActivity.B6, chessActivity.C6, chessActivity.D6,
                chessActivity.E6, chessActivity.F6, chessActivity.G6, chessActivity.H6
            ),
            arrayOf(
                chessActivity.A7, chessActivity.B7, chessActivity.C7, chessActivity.D7,
                chessActivity.E7, chessActivity.F7, chessActivity.G7, chessActivity.H7
            ),
            arrayOf(
                chessActivity.A8, chessActivity.B8, chessActivity.C8, chessActivity.D8,
                chessActivity.E8, chessActivity.F8, chessActivity.G8, chessActivity.H8
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
        } else {
            return android.R.color.transparent
        }
    }

    fun markFigure(v: View) {
        val fullName: String = chessActivity.getResources().getResourceName(v.getId())
        val name: String = fullName.substring(fullName.lastIndexOf("/") + 1)
        val fileRank = nameToIndex(name)
        val file = fileRank!![1]
        val rank = fileRank!![0]
        resetFieldColor()
        if(selectionFile != -1 && selectionRank != -1){ //unselect
            selectionFile = -1
            selectionRank = -1
        } else {
            imageViews[rank][file].setBackgroundColor(
                getMixedColor(file, rank, Color.RED)
            )
            selectionFile = file
            selectionRank = rank
        }
    }

    fun markSquare(rank : Int, file : Int) {
        imageViews[rank][file].setBackgroundColor(
            getMixedColor(file, rank, Color.YELLOW)
        )
    }

    private fun resetFieldColor() {
        for(rank in 0..7){
            for(file in 0..7){
                if ((rank + file) % 2 != 0) imageViews[rank][file].setBackgroundColor(
                    chessActivity.resources.getColor(
                        R.color.colorWhite
                    )
                )
                if ((rank + file) % 2 == 0) imageViews[rank][file].setBackgroundColor(
                    chessActivity.resources.getColor(
                        R.color.colorBlack
                    )
                )
            }
        }
    }

    //Hilfsfunktionen
    private fun getMixedColor(x: Int, y: Int, color: Int): Int {
        return if ((x + y) % 2 == 0) ColorUtils.blendARGB(
            color,
            chessActivity.getResources().getColor(R.color.colorWhite),
            0.8f
        ) else ColorUtils.blendARGB(
            color,
            chessActivity.getResources().getColor(R.color.colorBlack),
            0.8f
        )
    }

    private fun nameToIndex(name: String): IntArray? {
        val result = intArrayOf(0, 0)
        result[0] = Integer.valueOf(name.substring(1, 2)) - 1
        result[1] = name.toLowerCase()[0] - 'a'
        return result
    }

}
