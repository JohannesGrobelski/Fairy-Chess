package emerald.apps.fairychess.model

import emerald.apps.fairychess.utility.FigureParser
import kotlin.math.sign


data class Chessboard(val chessFormationArray: Array<Array<String>>,val figureMap : Map<String, FigureParser.Figure> ) {
    /* pieces-array: (file,rank)-coordinates
       (7,0) ... (7,7)
       ...        ...
       (0,0) ... (0,7)
     */


    var pieces: Array<Array<ChessPiece>> = Array(8) {
        Array(8) {
            ChessPiece("", -1,-1, 0, "", "", 0)
        }
    }

    var moveColor = "white"
    var moveCounter : Int = 0
    var gameFinished = false
    var gameWinner = ""

    var whiteCapturedPieces = mutableListOf<ChessPiece>()
    var blackCapturedPieces = mutableListOf<ChessPiece>()
    var promotion : ChessPiece? = null

    init {
        //hier einen aufstellungsstring übergeben
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

    /** return a list of possible movements of the figure at (sourceFile,sourceRank)*/
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

    /** return a list of all posible moves for player of @color*/
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

    /** return if figure at (targetFile,targetRank) can be reached by figure at (sourceFile,sourceRank) or not
     * (is shadowed by a figure between both of them)*/
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

    /** return if figure at (targetFile,targetRank) can be reached by figure at (sourceFile,sourceRank) with a orthogonal movement
     * or not (is shadowed by a figure between both of them)*/
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

    /** return if figure at (targetFile,targetRank) can be reached by figure at (sourceFile,sourceRank) with a diagonal movement
     * or not (is shadowed by a figure between both of them)*/
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

    /** calculate the winner (if one exists yet)*/
    fun getWinner() {
        var blackKing = 0
        var whiteKing = 0
        //count kings
        for (i in pieces.indices) {
            for (j in pieces.indices) {
                if (pieces[i][j].name == "king") {
                    if (pieces[i][j].color == "white") {
                        ++whiteKing
                    } else {
                        ++blackKing
                    }
                }
            }
        }
        //calculate winner based on counts
        gameWinner = if(whiteKing == 0 && blackKing > 0) "black"
        else if(whiteKing > 0 && blackKing == 0) "white"
        else ""
        gameFinished = (whiteKing*blackKing) == 0
    }

    /** check if color can move and (if possible) execute movement */
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
                if(pieces[captureFile][captureRank].color == "white"){
                    whiteCapturedPieces.add(pieces[captureFile][captureRank])
                } else if(pieces[captureFile][captureRank].color == "black"){
                    blackCapturedPieces.add(pieces[captureFile][captureRank])
                }
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
        //if target is nonempty, add to captured pieces
        if(pieces[movement.targetFile][movement.targetRank].color == "white"){
            whiteCapturedPieces.add(pieces[movement.targetFile][movement.targetRank])
        } else if(pieces[movement.targetFile][movement.targetRank].color == "black"){
            blackCapturedPieces.add(pieces[movement.targetFile][movement.targetRank])
        }
        //move piece and replace target
        var pieceName = pieces[movement.sourceFile][movement.sourceRank].name
        if(movement is ChessPiece.PromotionMovement){
            pieceName
        }
        pieces[movement.targetFile][movement.targetRank] = ChessPiece(
            pieceName,
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
        checkForPromotion()
        getWinner()
        if(!gameFinished){
            ++moveCounter
            switchMoveColor()
        }
        return ""
    }

    private fun checkForPromotion() {
        promotion = null
        for (file in 0..7) {
            if(pieces[file][0].name == "pawn"){
                promotion = (pieces[file][0])
            }
            if(pieces[file][7].name == "pawn"){
                promotion = (pieces[file][7])
            }
        }
    }

    /** calculate all points of black player */
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

    /** calculate all points of white player */
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

    /** promote figure at (file,rank) to promotion*/
    private fun promote(promotion: String, file:Int, rank: Int) {
        val color: String = pieces[file][rank].color
        pieces[file][rank] = ChessPiece(promotion, file, rank, 10, color, "", 0)
    }

    /** switchMoveColor from white to black and vice versa */
    fun switchMoveColor(){
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

    override fun toString(): String {
        var cStringBuilder = StringBuilder("")
        for(file in pieces.indices){
            for(rank in pieces[0].indices){
                val f = pieces[file][rank].name
                if(f.isEmpty())cStringBuilder.append(" ")
                else cStringBuilder.append(f[0])
                if(rank < pieces[0].size-1)cStringBuilder.append(" | ")
            }
            if(file < pieces.size-1)cStringBuilder.append("\n")
        }
        return cStringBuilder.toString()
    }


}