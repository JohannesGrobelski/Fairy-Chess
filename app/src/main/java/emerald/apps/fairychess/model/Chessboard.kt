package emerald.apps.fairychess.model.pieces

import java.util.*

class Chessboard() {
    lateinit var pieces: Array<Array<ChessPiece>>

    private enum class Value(val value: Int) {
        pawn(10), knight(30), bishop(30), rook(50), queen(90), king(1000);
    }

    private var moveCounter = 0
    private var lastError = ""


    fun init(mode : String){
        //hier einen aufstellungsstring übergeben
        when(mode){
            "normal" ->  {
                pieces = Array(8) {
                    Array(8) {
                        ChessPiece("", arrayOf(-1,-1),0,"","")
                    }
                }
                pieces[0][0] = Leaper("rook", arrayOf(0,0),8,"white","")

            }
        }
    }

    init {
        init("normal")
    }


    fun checkMovement(x1: Int, y1: Int, x2: Int, y2: Int, color: String?): Boolean {
        return true
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

    fun move(x1: Int, y1: Int, x2: Int, y2: Int, color: String?): ChessPiece? {
        return null
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

    private fun contains(array: CharArray, x: Char, times: Int): Boolean {
        var cnt = 0
        for (c in array) {
            if (c == x) ++cnt
        }
        return cnt == times
    }


}