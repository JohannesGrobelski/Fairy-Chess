package emerald.apps.fairychess.model.pieces

import android.content.Context
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser

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
                val figureMap = FigureParser.parseFigureMapFromFile(
                    context,
                    "figures"
                )
                if (chessFormationArray.size == 8 && chessFormationArray[0].size == 8) {
                    for (file in 0..7) {
                        for (rank in 0..7) {
                            var color = ""
                            if(file < 2)color = "white"
                            if (file > 5) color = "black"
                            if(figureMap.containsKey(chessFormationArray[file][rank])){
                                val movement = figureMap[chessFormationArray[file][rank]]?.movementParlett
                                val value =  figureMap[chessFormationArray[file][rank]]?.value!!
                                if(movement != null){
                                    pieces[file][rank] = ChessPiece(
                                        chessFormationArray[file][rank],
                                        arrayOf(file,rank),
                                        value,
                                        color,
                                        movement)
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
            val targetSquares = getTargetSquares(sourceRank,sourceFile)
            val destinationSquare = arrayOf(destinationRank,destinationFile)
            for(targetSquare in targetSquares){
                if(targetSquare[0] == destinationSquare[0] && targetSquare[1] == destinationSquare[1]){
                    return ""
                }
            }
            return "cannot move there"
        }
    }

    fun getTargetSquares(sourceRank:Int,sourceFile:Int) : List<Array<Int>>{
        val preFilterTargetSquareList = pieces[sourceRank][sourceFile].generateTargetSquares()
        val filteredTargetSquareList = mutableListOf<Array<Int>>()
        for(targetSquare in preFilterTargetSquareList){
            if(!(
                targetSquare[0] < 0 || targetSquare[0] > 7 || targetSquare[1] < 0 || targetSquare[1] > 7
                || pieces[targetSquare[0]][targetSquare[1]].color == pieces[sourceRank][sourceFile].color)){
                filteredTargetSquareList.add(targetSquare)
            }
        }
        return filteredTargetSquareList
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
                pieces[sourceRank][sourceFile].movingPatternString,
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
            if (pieces!![j][0].name == "PawnPromotion") return arrayOf(0, j)
            if (pieces!![j][7].name == "PawnPromotion") return arrayOf(7, j)
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