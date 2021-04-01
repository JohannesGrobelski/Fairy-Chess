package emerald.apps.fairychess.model.pieces

import android.content.Context
import emerald.apps.fairychess.movementNotation.directionl.pieces.ChessPiece
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import java.util.*
import kotlin.math.sign


class Chessboard(val context: Context) {
    /* pieces-array: (file,rank)-coordinates
       (7,0) ... (7,7)
       ...        ...
       (0,0) ... (0,7)
     */
    lateinit var pieces: Array<Array<ChessPiece>>

    private var moveColor = "white"
    private var moveCounter : Int = 0

    fun init(mode: String){
        //hier einen aufstellungsstring übergeben
        when(mode){
            "normal" -> {
                pieces = Array(8) {
                    Array(8) {
                        ChessPiece("", -1,-1, 0, "", "", 0)
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
                            if(rank < 2)color = "white"
                            if (rank > 5) color = "black"
                            if(figureMap.containsKey(chessFormationArray[file][rank])){
                                val movement = figureMap[chessFormationArray[file][rank]]?.movementParlett
                                val value =  figureMap[chessFormationArray[file][rank]]?.value!!
                                if(movement != null){
                                    pieces[file][rank] = ChessPiece(
                                        chessFormationArray[file][rank],
                                        file,
                                        rank,
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


    fun checkMovement(sourceFile:Int,sourceRank:Int,destinationFile: Int,destinationRank: Int): String {
        if(pieces[sourceFile][sourceRank].color == "")return "empty field"
        else if(pieces[sourceFile][sourceRank].color == pieces[destinationFile][destinationRank].color)return "same color"
        else if(pieces[sourceFile][sourceRank].color != moveColor)return "wrong player"
        else {
            val targetSquares = getTargetSquares(sourceFile,sourceRank)
            val destinationSquare = TargetSquare(destinationFile,destinationRank)
            for(targetSquare in targetSquares){
                if(targetSquare.targetFile == destinationSquare.targetFile && targetSquare.targetRank == destinationSquare.targetRank){
                    return ""
                }
            }
            return "cannot move there"
        }
    }

    class TargetSquare(val targetFile: Int, val targetRank: Int)
    fun getTargetSquares(sourceFile:Int,sourceRank:Int) : List<TargetSquare>{
        val nonRelativeMovements = pieces[sourceFile][sourceRank].generateMovements()
        val relativeMovements = mutableListOf<TargetSquare>()
        //filter target squares
        for(nonRelativeMovement in nonRelativeMovements){
            if(!(
                nonRelativeMovement.sourceFile !in 0..7 || nonRelativeMovement.sourceRank !in 0..7
                || nonRelativeMovement.targetFile !in 0..7 || nonRelativeMovement.targetRank !in 0..7
                    || pieces[nonRelativeMovement.targetFile][nonRelativeMovement.targetRank].color == pieces[sourceFile][sourceRank].color)
                        && !isShadowedByFigure(sourceFile,sourceRank,nonRelativeMovement.targetFile,nonRelativeMovement.targetRank)
                            && fullfillsCondition(nonRelativeMovement)){
                relativeMovements.add(TargetSquare(nonRelativeMovement.targetFile,nonRelativeMovement.targetRank))
            }
        }
        return relativeMovements
    }

    fun isShadowedByFigure(sourceFile:Int,sourceRank:Int,targetFile: Int,targetRank: Int) : Boolean{
        for(movement in pieces[sourceFile][sourceRank].movingPatternString.split(",")){
            when {
                movement.contains(">") -> {
                    return isShadowedByFigureOrthogonal(sourceFile,sourceRank,targetFile,targetRank)
                }
                movement.contains("<") -> {
                    return isShadowedByFigureOrthogonal(sourceFile,sourceRank,targetFile,targetRank)
                }
                movement.contains("=") -> {
                    return isShadowedByFigureOrthogonal(sourceFile,sourceRank,targetFile,targetRank)
                }
                movement.contains("+") -> {
                    return isShadowedByFigureOrthogonal(sourceFile,sourceRank,targetFile,targetRank)
                }
                movement.contains("X") -> {
                    return isShadowedByFigureDiagonal(sourceFile,sourceRank,targetFile,targetRank)
                }
                movement.contains("*") -> {
                    return(isShadowedByFigureOrthogonal(sourceFile,sourceRank,targetFile,targetRank)
                          || isShadowedByFigureDiagonal(sourceFile,sourceRank,targetFile,targetRank))
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
                returnValue = returnValue && !(pieces[movement.sourceFile][movement.sourceRank].color != pieces[movement.targetFile][movement.targetRank].color
                        && pieces[movement.sourceFile][movement.sourceRank].color.isNotEmpty()
                        && pieces[movement.targetFile][movement.targetRank].color.isNotEmpty())
            }
            movement.movementNotation.conditions.contains("c") -> {//May only be made on a capture (e.g. pawn's diagonal capture)
                returnValue = returnValue && (pieces[movement.sourceFile][movement.sourceRank].color != pieces[movement.targetFile][movement.targetRank].color
                        && pieces[movement.sourceFile][movement.sourceRank].color.isNotEmpty()
                        && pieces[movement.targetFile][movement.targetRank].color.isNotEmpty())
            }
            movement.movementNotation.conditions.contains("i") -> {//May only be made on the initial move (e.g. pawn's 2 moves forward)
                returnValue = returnValue && (pieces[movement.sourceFile][movement.sourceRank].moveCounter == 0)
            }
        }
        return returnValue
    }

    fun isShadowedByFigureOrthogonal(sourceFile:Int,sourceRank:Int,targetFile:Int,targetRank: Int) : Boolean{
        if(sourceRank == targetRank && (Math.abs(targetFile-sourceFile) > 1)){//distance > 1 because a figure has to stand between them for shadow
            //move on file (horizontal)
            val signDifFile = sign((targetFile-sourceFile).toDouble()).toInt()
            var file = sourceFile + signDifFile
            while(file != targetFile){
                if(pieces[file][sourceRank].color != ""){
                    return true
                }
                file += signDifFile
            }
        }
        if(sourceFile == targetFile && (Math.abs(targetRank-sourceRank) > 1)){
            //move on rank (vertical)
            val signDifRank = sign((targetRank-sourceRank).toDouble()).toInt()
            var rank = sourceRank + signDifRank
            while(rank != targetRank){
                if(pieces[sourceFile][rank].color != ""){
                    return true
                }
                rank += signDifRank
            }
        }
        return false
    }

    fun isShadowedByFigureDiagonal(sourceFile:Int,sourceRank:Int,targetFile:Int,targetRank: Int) : Boolean{
        if(Math.abs(targetRank-sourceRank)>1 && Math.abs(targetFile-sourceFile)>1){
            val difRank = sign((targetRank-sourceRank).toDouble()).toInt()
            val difFile = sign((targetFile-sourceFile).toDouble()).toInt()
            if(Math.abs(targetRank-sourceRank) - Math.abs(targetFile-sourceFile) == 0){
                for(i in 1..Math.abs(targetFile-sourceFile)){
                    val rank = sourceRank+(difRank*i)
                    val file = sourceFile+(difFile*i)
                    if(rank in 0..7 && file in 0..7){
                        if(pieces[file][rank].color != ""){
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

    fun move(sourceFile: Int, sourceRank: Int, destinationFile: Int, destinationRank: Int) : String{
        val check = checkMovement(sourceFile,sourceRank,destinationFile,destinationRank)
        if(check.isEmpty()){
            pieces[destinationFile][destinationRank] = ChessPiece(
                pieces[sourceFile][sourceRank].name,
                destinationFile,
                destinationRank,
                pieces[sourceFile][sourceRank].value,
                pieces[sourceFile][sourceRank].color,
                pieces[sourceFile][sourceRank].movingPatternString,
                pieces[sourceFile][sourceRank].moveCounter+1,
            )
            pieces[sourceFile][sourceRank] = ChessPiece(
                "",
                sourceFile,
                sourceRank,
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
    private fun promote(figure: String, file:Int, rank: Int) {
        val color: String = pieces[file][rank].color
        pieces[file][rank] = ChessPiece(figure, file, rank, 10, color, "", 0)
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