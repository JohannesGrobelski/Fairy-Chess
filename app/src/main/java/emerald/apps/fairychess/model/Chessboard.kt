package emerald.apps.fairychess.model.pieces

import android.content.Context
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import java.util.*
import kotlin.math.sign

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
                        ChessPiece("", arrayOf(-1, -1), 0, "", "", 0)
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
                                        movement,
                                        0
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
        val nonRelativeMovements = pieces[sourceRank][sourceFile].generateMovements()
        val relativeMovements = mutableListOf<Array<Int>>()
        //filter target squares
        for(nonRelativeMovement in nonRelativeMovements){
            if(!(
                nonRelativeMovement.sourceRank !in 0..7 || nonRelativeMovement.sourceFile !in 0..7
                    || pieces[nonRelativeMovement.targetRank][nonRelativeMovement.targetFile].color == pieces[sourceRank][sourceFile].color)
                        && !isShadowedByFigure(sourceRank,sourceFile,nonRelativeMovement.targetRank,nonRelativeMovement.targetFile)
                            && fullfillsCondition(nonRelativeMovement)){
                relativeMovements.add(arrayOf(nonRelativeMovement.targetRank,nonRelativeMovement.targetFile))
            }
        }
        return relativeMovements
    }

    fun isShadowedByFigure(sourceRank:Int,sourceFile:Int,targetRank:Int,targetFile:Int) : Boolean{
        for(movement in pieces[sourceRank][sourceFile].movingPatternString.split(",")){
            when {
                movement.contains("+") -> {
                    return isShadowedByFigureOrthogonal(sourceRank,sourceFile,targetRank,targetFile)
                }
                movement.contains("X") -> {
                    return isShadowedByFigureDiagonal(sourceRank,sourceFile,targetRank,targetFile)
                }
                movement.contains("*") -> {
                    return(isShadowedByFigureOrthogonal(sourceRank,sourceFile,targetRank,targetFile)
                          || isShadowedByFigureDiagonal(sourceRank,sourceFile,targetRank,targetFile))
                }
            }
        }
        return false
    }

    /**
     * does the movement fullfil condition in Movement.MovementNotation.Condition?
     */
    fun fullfillsCondition(movement : ChessPiece.Movement) : Boolean {
        if(movement.movementNotation.conditions.isEmpty())return true
        var returnValue = true
        when {
            movement.movementNotation.conditions.contains("o") -> {//May not be used for a capture (e.g. pawn's forward move)
                returnValue = returnValue && !(pieces[movement.sourceRank][movement.sourceFile].color != pieces[movement.targetRank][movement.targetFile].color
                        && pieces[movement.sourceRank][movement.sourceFile].color.isNotEmpty()
                        && pieces[movement.targetRank][movement.targetFile].color.isNotEmpty())
            }
            movement.movementNotation.conditions.contains("i") -> {//May only be made on a capture (e.g. pawn's diagonal capture)
                returnValue = returnValue && (pieces[movement.sourceRank][movement.sourceFile].color != pieces[movement.targetRank][movement.targetFile].color
                        && pieces[movement.sourceRank][movement.sourceFile].color.isNotEmpty()
                        && pieces[movement.targetRank][movement.targetFile].color.isNotEmpty())
            }
            movement.movementNotation.conditions.contains("c") -> {//May only be made on the initial move (e.g. pawn's 2 moves forward)
                returnValue = returnValue && (pieces[movement.sourceRank][movement.sourceFile].moveCounter == 0)
            }
        }
        return returnValue
    }

    fun isShadowedByFigureOrthogonal(sourceRank:Int,sourceFile:Int,targetRank:Int,targetFile:Int) : Boolean{
        if(sourceRank == targetRank && (Math.abs(targetFile-sourceFile) > 1)){//distance > 1 because a figure has to stand between them for shadow
            //move on file
            val difFile = sign((targetFile-sourceFile).toDouble()).toInt()
            var file = sourceFile + difFile
            while(file != targetFile){
                if(pieces[sourceRank][file].color != ""){
                    return true
                }
                file += difFile
            }
        }
        if(sourceFile == targetFile && (Math.abs(targetRank-sourceRank) > 1)){
            //move on file
            val difRank = sign((targetRank-sourceRank).toDouble()).toInt()
            var rank = sourceRank + difRank
            while(rank != targetRank){
                if(pieces[rank][sourceFile].color != ""){
                    return true
                }
                rank += difRank
            }
        }
        return false
    }

    fun isShadowedByFigureDiagonal(sourceRank:Int,sourceFile:Int,targetRank:Int,targetFile:Int) : Boolean{
        if(Math.abs(targetRank-sourceRank)>1 && Math.abs(targetRank-sourceRank)>1){
            val difRank = sign((targetRank-sourceRank).toDouble()).toInt()
            val difFile = sign((targetFile-sourceFile).toDouble()).toInt()
            if(Math.abs(targetFile-sourceFile) - Math.abs(targetRank-sourceRank) == 0){
                for(i in 1..Math.abs(targetFile-sourceFile)){
                    val rank = sourceRank+(difRank*i)
                    val file = sourceFile+(difFile*i)
                    if(rank in 0..7 && file in 0..7){
                        if(pieces[rank][file].color != ""){
                            if(rank != targetRank && file != targetFile){
                                return true
                            }
                        }
                    }
                }
            }
        }
        return false
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
                arrayOf(destinationRank,destinationFile),
                pieces[sourceRank][sourceFile].value,
                pieces[sourceRank][sourceFile].color,
                pieces[sourceRank][sourceFile].movingPatternString,
                pieces[sourceRank][sourceFile].moveCounter+1,
            )
            pieces[sourceRank][sourceFile] = ChessPiece(
                "",
                arrayOf(sourceFile,sourceRank),
                0,
                "",
                "",
                0,
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
        pieces[position[1]][position[0]] = ChessPiece(figure, position, 10, color, "", 0)
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

    class MovementNotation(val grouping: String, val conditions: List<String>, val movetype: String, val distances: List<String>, val direction: String){
        companion object {
            fun parseMovementString(movementString : String) : List<MovementNotation> {
                if(movementString.isEmpty())return emptyList()
                val movementList = mutableListOf<MovementNotation>()
                val movementArray = movementString.split(",")
                for(submovement in movementArray){
                    var submovementString = submovement
                    var grouping = ""
                    var conditions = mutableListOf<String>()
                    var movetype = ""
                    var distances = mutableListOf<String>()
                    var direction = ""
                    //move type
                    if(submovementString.contains("~")){movetype = "~";submovementString = submovementString.replace("~","")}
                    if(submovementString.contains("^")){movetype = "^";submovementString = submovementString.replace("^","")}
                    //grouping
                    if(submovementString.contains("/")){grouping = "/";submovementString = submovementString.replace("/","")}
                    if(submovementString.contains("&")){grouping = "&";submovementString = submovementString.replace("&","")}
                    if(submovementString.contains(".")){grouping = ".";submovementString = submovementString.replace(".","")}
                    //move conditions
                    if(submovementString.contains("i")){conditions.add("i");submovementString = submovementString.replace("i","")}
                    if(submovementString.contains("c")){conditions.add("c");submovementString = submovementString.replace("c","")}
                    if(submovementString.contains("o")){conditions.add("o");submovementString = submovementString.replace("o","")}
                    //direction
                    if(submovementString.contains(">=")){direction = ">=";submovementString = submovementString.replace(">=","")}
                    if(submovementString.contains("<=")){direction = "<=";submovementString = submovementString.replace("<=","")}
                    if(submovementString.contains("<>")){direction = "<>";submovementString = submovementString.replace("<>","")}
                    if(submovementString.contains("=")){direction = "=";submovementString = submovementString.replace("=","")}
                    if(submovementString.contains("X>")){direction = "X>";submovementString = submovementString.replace("X>","")}
                    if(submovementString.contains("X<")){direction = "X<";submovementString = submovementString.replace("X<","")}
                    if(submovementString.contains("X")){direction = "X";submovementString = submovementString.replace("X","")}
                    if(submovementString.contains(">")){direction = ">";submovementString = submovementString.replace(">","")}
                    if(submovementString.contains("<")){direction = "<";submovementString = submovementString.replace("<","")}
                    if(submovementString.contains("+")){direction = "+";submovementString = submovementString.replace("+","")}
                    if(submovementString.contains("*")){direction = "*";submovementString = submovementString.replace("*","")}
                    //distance
                    if(grouping == ""){
                        if(submovementString.contains("n"))distances.add("n")
                        if(submovementString.contains("[0-9]".toRegex()))distances.add(submovementString.replace("\\D+".toString(),""))
                    } else {
                        distances = submovementString.split("").toMutableList()
                        distances.removeAll(Collections.singleton(""))
                    }
                    movementList.add(MovementNotation(grouping,conditions,movetype,distances.toList(),direction))
                }
                return movementList
            }
        }
    }
}