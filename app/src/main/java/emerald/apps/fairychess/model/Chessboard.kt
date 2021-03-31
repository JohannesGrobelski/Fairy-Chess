package emerald.apps.fairychess.model.pieces

import android.content.Context
import emerald.apps.fairychess.utility.ChessFormationParser

class Chessboard(val context: Context) {
    lateinit var pieces: Array<Array<ChessPiece>>

    private enum class Value(val value: Int) {
        pawn(10), knight(30), bishop(30), rook(50), queen(90), king(1000);
    }

    private var moveColor = "white"
    private var moveCounter : Int = 0
    private var lastError = ""


    fun init(mode: String){
        //hier einen aufstellungsstring übergeben
        when(mode){
            "normal" -> {
                pieces = Array(8) {
                    Array(8) {
                        ChessPiece("", arrayOf(-1, -1), 0, "", "")
                    }
                }
                val chessFormationArray = ChessFormationParser.parseChessFormation(
                    context,
                    "normal"
                )
                if (chessFormationArray.size == 8 && chessFormationArray[0].size == 8) {
                    for (file in 0..7) {
                        for (rank in 0..7) {
                            var color = ""
                            if(file < 2)color = "white"
                            if (file > 5) color = "black"
                            val movingpattern = ""
                            when(getType(chessFormationArray[file][rank])){
                                "Leaper" -> {
                                    pieces[file][rank] = Leaper(
                                        chessFormationArray[file][rank], arrayOf(
                                            file,
                                            rank
                                        ), 10, color, movingpattern
                                    )
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    init {
        init("normal")
    }


    fun checkMovement(sourceRank:Int,sourceFile:Int,destinationRank: Int,destinationFile: Int): String {
        if(pieces[sourceRank][sourceFile].color == "")return "empty field"
        else if(pieces[sourceRank][sourceFile].color == pieces[destinationRank][destinationFile].color)return "same color"
        else if(pieces[sourceRank][sourceFile].color != moveColor)return "wrong player"
        else {
            when(pieces[sourceRank][sourceFile] instanceOf)
            return ""
        }
    }

    fun gameOver(): Boolean {
        var cntKing = 0
        for (i in pieces!!.indices) {
            for (j in pieces!!.indices) {
                if (pieces!![i][j]?.name == "king") ++cntKing
                if (cntKing == 2) return false
            }
        }
        return true
    }

    fun getWinner(): String? {
        for (i in pieces!!.indices) {
            for (j in pieces!!.indices) {
                if (pieces!![i][j]?.name == "king") {
                    return if (pieces!![i][j]?.color.equals("white")) {
                        "white"
                    } else {
                        "black"
                    }
                }
            }
        }
        return "remis"
    }

    fun move(sourceRank: Int, sourceFile: Int, destinationRank: Int, destinationFile: Int) : String{
        val check = checkMovement(sourceRank,sourceFile,destinationRank,destinationFile)
        if(check.isEmpty()){
            pieces[destinationRank][destinationFile] = ChessPiece(
                pieces[sourceRank][sourceFile].name,
                arrayOf(sourceFile,sourceRank),
                pieces[sourceRank][sourceFile].value,
                pieces[sourceRank][sourceFile].color,
                pieces[sourceRank][sourceFile].movingPattern,
            )
            pieces[sourceRank][sourceFile] = ChessPiece(
                "",
                arrayOf(sourceFile,sourceRank),
                0,
                "",
                "",
            )
            ++moveCounter
            switchColors()
            return ""
        } else {
            return check
        }
    }

    fun checkForPawnPromotion(): Array<Int>? {
        for (j in pieces!!.indices) {
            if (pieces!![j][0] is PawnPromotion) return arrayOf(0, j)
            if (pieces!![j][7] is PawnPromotion) return arrayOf(7, j)
        }
        return null
    }


    fun points_black(): Int {
        var punkte = 0
        for (a in 0..7) {
            for (b in 0..7) {
                if(pieces?.get(a)?.get(b)?.color == "black"){
                    punkte += pieces!![a][b]!!.value
                }
            }
        }
        return punkte
    }

    fun points_white(): Int {
        var punkte = 0
        for (a in 0..7) {
            for (b in 0..7) {
                if(pieces?.get(a)?.get(b)?.color == "white"){
                    punkte += pieces!![a][b]!!.value
                }
            }
        }
        return punkte
    }

    // do something with the data coming from the AlertDialog
    private fun promote(figure: String, position: Array<Int>?) {
        if (position == null) return
        val color: String = pieces[position[1]][position[0]].color
        pieces[position[1]][position[0]] = ChessPiece(figure,position,10,color,"")
    }

    fun switchColors(){
        if(moveColor == "white"){
            moveColor = "black"
        } else if(moveColor == "black"){
            moveColor = "white"
        }
    }


    fun getType(figure: String) : String{
        return "Leaper"
    }
}