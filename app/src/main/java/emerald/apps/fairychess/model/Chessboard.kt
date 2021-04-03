package emerald.apps.fairychess.model.pieces

import android.content.Context
import emerald.apps.fairychess.model.ChessPiece
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import kotlin.math.sign


data class Chessboard(val context: Context, val mode : String) {
    /* pieces-array: (file,rank)-coordinates
       (7,0) ... (7,7)
       ...        ...
       (0,0) ... (0,7)
     */
    lateinit var pieces: Array<Array<ChessPiece>>

    var moveColor = "white"
    var moveCounter : Int = 0
    var gameFinished = false

    fun init(mode: String){
        //hier einen aufstellungsstring übergeben
        pieces = Array(8) {
            Array(8) {
                ChessPiece("", -1,-1, 0, "", "", 0)
            }
        }
        if(mode == "normal chess" || mode == "berolina chess" || mode == "grasshopper chess") {
            val chessFormationArray = ChessFormationParser.parseChessFormation(
                context,
                mode.toLowerCase().replace(" ","_")
            )
            val figureMap = FigureParser.parseFigureMapFromFile(
                context,
                "figures"
            )
            if (chessFormationArray.size == 8 && chessFormationArray[0].size == 8) {
                for (file in 0..7) {
                    for (rank in 0..7) {
                        var color = ""
                        if(rank <= 4 && chessFormationArray[file][rank].isNotEmpty())color = "white"
                        if (rank > 4 && chessFormationArray[file][rank].isNotEmpty()) color = "black"
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

    init {
        init(mode)
    }


    fun getTargetMovements(sourceFile:Int, sourceRank:Int) : List<ChessPiece.Movement>{
        val nonRelativeMovements = pieces[sourceFile][sourceRank].generateMovements()
        val relativeMovements = mutableListOf<ChessPiece.Movement>()
        //filter target squares
        for(nonRelativeMovement in nonRelativeMovements){
            if(!(
                nonRelativeMovement.sourceFile !in 0..7 || nonRelativeMovement.sourceRank !in 0..7
                || nonRelativeMovement.targetFile !in 0..7 || nonRelativeMovement.targetRank !in 0..7
                    || pieces[nonRelativeMovement.targetFile][nonRelativeMovement.targetRank].color == pieces[sourceFile][sourceRank].color)
                        && !isShadowedByFigure(sourceFile,sourceRank,nonRelativeMovement.targetFile,nonRelativeMovement.targetRank)
                            && fullfillsCondition(nonRelativeMovement)){
                relativeMovements.add(nonRelativeMovement)
            }
        }
        return relativeMovements
    }

    fun getAllPossibleMoves(color : String) : List<ChessPiece.Movement>{
        var allPossibleMoves = mutableListOf<ChessPiece.Movement>()
        for(file in 0..7){
            for(rank in 0..7){
                if(pieces[file][rank].color == color){
                    allPossibleMoves.addAll(getTargetMovements(file,rank))
                }
            }
        }
        return allPossibleMoves
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
        var returnValue = true
        if(movement.movementNotation.conditions.contains("o")) {//May not be used for a capture (e.g. pawn's forward move)
            returnValue = returnValue && !(pieces[movement.sourceFile][movement.sourceRank].color != pieces[movement.targetFile][movement.targetRank].color
                    && pieces[movement.sourceFile][movement.sourceRank].color.isNotEmpty()
                    && pieces[movement.targetFile][movement.targetRank].color.isNotEmpty())
        }
        if(movement.movementNotation.conditions.contains("c")) {//May only be made on a capture (e.g. pawn's diagonal capture)
            returnValue = returnValue && (pieces[movement.sourceFile][movement.sourceRank].color != pieces[movement.targetFile][movement.targetRank].color
                    && pieces[movement.sourceFile][movement.sourceRank].color.isNotEmpty()
                    && pieces[movement.targetFile][movement.targetRank].color.isNotEmpty())
        }
        if(movement.movementNotation.conditions.contains("i")) {//May only be made on the initial move (e.g. pawn's 2 moves forward)
            returnValue = returnValue && (pieces[movement.sourceFile][movement.sourceRank].moveCounter == 0)
        }
        if(movement.movementNotation.movetype == "g") {//moves by leaping over piece to an empty square (if leaped over enemy => capture)
            val signFile = sign((movement.targetFile - movement.sourceFile).toDouble()).toInt()
            val signRank = sign((movement.targetRank - movement.sourceRank).toDouble()).toInt()
            returnValue = pieces[movement.targetFile][movement.targetRank].color.isEmpty()
                    && pieces[movement.targetFile-signFile][movement.targetRank-signRank].color.isNotEmpty()
                    && (Math.abs(movement.targetFile-movement.sourceFile) > 1 || Math.abs(movement.targetRank-movement.sourceRank) > 1)
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

    fun move(color: String, movement: ChessPiece.Movement) : String{
        if(color != moveColor)return "wrong player"
        //check movement
        var userMovement : ChessPiece.Movement? = null
        if(pieces[movement.sourceFile][movement.sourceRank].color == "")return "empty field"
        else if(pieces[movement.sourceFile][movement.sourceRank].color == pieces[movement.targetFile][movement.targetRank].color)return "same color"
        else if(pieces[movement.sourceFile][movement.sourceRank].color != moveColor)return "wrong figure"
        else {
            val targetMovements = getTargetMovements(movement.sourceFile,movement.sourceRank)
            for(targetMovement in targetMovements){
                if(targetMovement.targetFile == movement.targetFile && targetMovement.targetRank == movement.targetRank){
                    userMovement = targetMovement
                }
            }
            if(userMovement == null)return "cannot move there"
        }

        //valid movement
        if (userMovement.movementNotation.movetype == "g") {
            //capture the piece hopped over
            val signFile = sign((userMovement.targetFile - userMovement.sourceFile).toDouble()).toInt()
            val signRank = sign((userMovement.targetRank - userMovement.sourceRank).toDouble()).toInt()
            val captureFile = userMovement.targetFile-signFile
            val captureRank = userMovement.targetRank-signRank
            if(pieces[captureFile][captureRank].color != moveColor){
                pieces[captureFile][captureRank] = ChessPiece(
                    "",
                    movement.sourceFile,
                    movement.sourceRank,
                    0,
                    "",
                    "",
                    0,
                )
            }
        }
        pieces[movement.targetFile][movement.targetRank] = ChessPiece(
            pieces[movement.sourceFile][movement.sourceRank].name,
            movement.targetFile,
            movement.targetRank,
            pieces[movement.sourceFile][movement.sourceRank].value,
            pieces[movement.sourceFile][movement.sourceRank].color,
            pieces[movement.sourceFile][movement.sourceRank].movingPatternString,
            pieces[movement.sourceFile][movement.sourceRank].moveCounter+1,
        )
        pieces[movement.sourceFile][movement.sourceRank] = ChessPiece(
            "",
            movement.sourceFile,
            movement.sourceRank,
            0,
            "",
            "",
            0,
        )

        ++moveCounter
        switchColors()
        return ""
    }

    fun checkForPawnPromotion(): Array<Int>? {
        for (j in pieces!!.indices) {
            if (pieces!![j][0].name == "PawnPromotion") return arrayOf(0, j)
            if (pieces!![j][7].name == "PawnPromotion") return arrayOf(7, j)
        }
        return null
    }

    fun checkForGameEnd(){
        
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
        moveColor = oppositeColor(moveColor)
    }

    fun oppositeColor(color : String) : String {
        return if(color == "white"){
            "black"
        } else if(color == "black"){
            "white"
        } else {
            ""
        }
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }


}