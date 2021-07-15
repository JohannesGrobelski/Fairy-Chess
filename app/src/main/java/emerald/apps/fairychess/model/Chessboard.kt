package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.ChessPiece.MovementNotation.Companion.CASTLING_MOVEMENT
import emerald.apps.fairychess.utility.FigureParser
import java.util.*
import kotlin.math.abs
import kotlin.math.sign


data class Chessboard(val chessFormationArray: Array<Array<String>>,val figureMap : Map<String, FigureParser.Figure> ) {
    /* pieces-array: (file,rank)-coordinates
       (7,0) ... (7,7)
       ...        ...
       (0,0) ... (0,7)
     */

    //2D-array of chesspieces to represent board
    var pieces: Array<Array<ChessPiece>> = Array(8) {
        Array(8) {
            ChessPiece("", -1,-1, 0, "", "", 0)
        }
    }

    //state variables
    var moveColor = "white"
    var moveCounter : Int = 0
    var gameFinished = false
    var gameWinner = ""

    var whiteCapturedPieces = mutableListOf<ChessPiece>()
    var blackCapturedPieces = mutableListOf<ChessPiece>()
    var promotion : ChessPiece? = null

    //special chess rules
    var moveHistory = mutableListOf<ChessPiece.Movement>() //castling
    var playerWithDrawOpportunity = ""
    var lastCaptureCounter = 0
    var lastPawnMove = 0
    var boardStateListSinceCastling = mutableListOf<String>()

    init {
        //pass a string representing the chess formation here and update chessFormationArray
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

    companion object {
        const val DEBUG = false
        /**
         * returns white or black randomly
         */
        fun randomColor() : String {
            if(Math.random() > 0.5)return "white"
            else return "black"
        }

        /**
         * returns the opposite color
         */
        fun oppositeColor(color : String) : String {
            return if(color == "white"){
                "black"
            } else if(color == "black"){
                "white"
            } else {
                ""
            }
        }

    }

    fun clone() : Chessboard{
        val chessboard = Chessboard(chessFormationArray,figureMap)
        chessboard.moveColor = moveColor
        //clone 2d array pieces
        for(line in chessboard.pieces.indices){
            for(row in chessboard.pieces[line].indices){
                chessboard.pieces[line][row] = pieces[line][row]
            }
        }
        return chessboard
    }

    /**
     * return a list of possible movements of the figure at (sourceFile,sourceRank)
     * */
    fun getTargetMovements(sourceFile:Int, sourceRank:Int, genCastlingMoves:Boolean) : MutableList<ChessPiece.Movement>{
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
        //add special movements
        if(genCastlingMoves){
            for(moveArray in generateSpecialMoveCheckCastling(pieces[sourceFile][sourceRank].color)){
                relativeMovements.add(moveArray[0])//king move
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
                    allPossibleMoves.addAll(getTargetMovements(file,rank,true))
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
            //en passante
            if(!returnValue)returnValue = specialMoveCheckEnpassante(movement) != null
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
    private fun checkForWinner() {
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

    /**
     * checks if a player has the right to draw
     * (50 move rule or 3-repetition rule)
     */
    private fun checkForDrawRight(){
        playerWithDrawOpportunity = ""
        //50 move rule
        if(lastCaptureCounter >= 50 || lastPawnMove >= 50){
            playerWithDrawOpportunity = oppositeColor(moveColor)
        }
        //3-repetition rule
        if(boardStateListSinceCastling.size >= 12){
            val boardStateMap = mutableMapOf<String,Int>()
            //count occurrences of board-state-Strings (with map)
            //and check whether same board-state-String occurred 3 times
            for(boardStateString in boardStateListSinceCastling){
                if(boardStateMap.containsKey(boardStateString)){
                    boardStateMap[boardStateString] = boardStateMap[boardStateString]!!.toInt() + 1
                    if(boardStateMap[boardStateString]!!.toInt() >= 3){
                        playerWithDrawOpportunity = oppositeColor(moveColor)
                    }
                } else {
                    boardStateMap[boardStateString] = 0
                }
            }
        }
    }

    /** switchMoveColor from white to black and vice versa */
    fun switchMoveColor(){
        moveColor = oppositeColor(moveColor)
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
            val targetMovements = getTargetMovements(movement.sourceFile,movement.sourceRank,true)
            for(targetMovement in targetMovements){
                if(targetMovement.movementNotation == CASTLING_MOVEMENT){
                    val castleMovements = generateSpecialMoveCheckCastling(color)//TODO: inefficent, idea: create var lastCastleMovement and use it here
                    for(castleMovement in castleMovements){
                        if(castleMovement[0].sourceFile == movement.sourceFile
                            && castleMovement[0].sourceRank == movement.sourceRank){
                            //move king
                            pieces[castleMovement[0].targetFile][castleMovement[0].targetRank] =
                                ChessPiece(
                                    pieces[castleMovement[0].sourceFile][castleMovement[0].sourceRank].name,
                                    castleMovement[0].targetFile,
                                    castleMovement[0].targetRank,
                                    pieces[castleMovement[0].sourceFile][castleMovement[0].sourceRank].value,
                                    pieces[castleMovement[0].sourceFile][castleMovement[0].sourceRank].color,
                                    pieces[castleMovement[0].sourceFile][castleMovement[0].sourceRank].movingPatternString,
                                    pieces[castleMovement[0].sourceFile][castleMovement[0].sourceRank].moveCounter+1,
                                )
                            pieces[castleMovement[0].sourceFile][castleMovement[0].sourceRank] = ChessPiece.emptyChessPiece
                            //move rook
                            pieces[castleMovement[1].targetFile][castleMovement[1].targetRank] =
                                ChessPiece(
                                    pieces[castleMovement[1].sourceFile][castleMovement[1].sourceRank].name,
                                    castleMovement[1].targetFile,
                                    castleMovement[1].targetRank,
                                    pieces[castleMovement[1].sourceFile][castleMovement[1].sourceRank].value,
                                    pieces[castleMovement[1].sourceFile][castleMovement[1].sourceRank].color,
                                    pieces[castleMovement[1].sourceFile][castleMovement[1].sourceRank].movingPatternString,
                                    pieces[castleMovement[1].sourceFile][castleMovement[1].sourceRank].moveCounter+1,
                                )
                            pieces[castleMovement[1].sourceFile][castleMovement[1].sourceRank] = ChessPiece.emptyChessPiece
                            addMoveToMoveHistory(castleMovement[0])
                            addMoveToMoveHistory(castleMovement[1])
                            boardStateListSinceCastling.clear()
                            ++lastCaptureCounter
                            ++lastPawnMove
                            checkForWinner()
                            checkForDrawRight()
                            if(!gameFinished){
                                ++moveCounter
                                switchMoveColor()
                            }
                            if(DEBUG)println(movement.asString2(moveColor))
                            return ""
                        }
                    }
                } else {
                    if(targetMovement.targetFile == movement.targetFile && targetMovement.targetRank == movement.targetRank){
                        userMovement = targetMovement
                    }
                }
            }
            if(userMovement == null)return "cannot move there"
        }
        ++lastCaptureCounter
        ++lastPawnMove

        if(specialMoveCheckEnpassante(userMovement) != null){
            capturePiece(specialMoveCheckEnpassante(userMovement)!!)
        }


        if(pieces[movement.sourceFile][movement.sourceRank].name == "pawn"){
            lastPawnMove = 0
        }

        //valid movement
        if(DEBUG)println(movement.asString2(moveColor))
        if (userMovement.movementNotation.movetype == "g") {
            //capture the piece hopped over
            val signFile = sign((userMovement.targetFile - userMovement.sourceFile).toDouble()).toInt()
            val signRank = sign((userMovement.targetRank - userMovement.sourceRank).toDouble()).toInt()
            val captureFile = userMovement.targetFile-signFile
            val captureRank = userMovement.targetRank-signRank
            if(pieces[captureFile][captureRank].color != moveColor){
                if(pieces[captureFile][captureRank].color == "white"){
                    whiteCapturedPieces.add(pieces[captureFile][captureRank])
                    lastCaptureCounter = 0
                } else if(pieces[captureFile][captureRank].color == "black"){
                    blackCapturedPieces.add(pieces[captureFile][captureRank])
                    lastCaptureCounter = 0
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
            lastCaptureCounter = 0
        } else if(pieces[movement.targetFile][movement.targetRank].color == "black"){
            blackCapturedPieces.add(pieces[movement.targetFile][movement.targetRank])
            lastCaptureCounter = 0
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
        addMoveToMoveHistory(userMovement)

        checkForPromotion()
        boardStateListSinceCastling.add(getBoardStateString())

        checkForWinner()
        checkForDrawRight()
        if(!gameFinished){
            ++moveCounter
            switchMoveColor()
        }
        return ""
    }

    private fun addMoveToMoveHistory(movement: ChessPiece.Movement){
        moveHistory.add(movement)
    }

    private fun capturePiece(chessPiece: ChessPiece){
        if(pieces[chessPiece.positionFile][chessPiece.positionRank].color == "black"){
            blackCapturedPieces.add(pieces[chessPiece.positionFile][chessPiece.positionRank])
        }
        if(pieces[chessPiece.positionFile][chessPiece.positionRank].color == "white"){
            whiteCapturedPieces.add(pieces[chessPiece.positionFile][chessPiece.positionRank])
        }
        pieces[chessPiece.positionFile][chessPiece.positionRank] = ChessPiece.emptyChessPiece
    }


    private fun specialMoveCheckEnpassante(movement: ChessPiece.Movement) : ChessPiece?{
        if(moveHistory.isNotEmpty()){
            val lastMoveSourceFile = moveHistory[moveHistory.lastIndex].sourceFile
            val lastMoveSourceRank = moveHistory[moveHistory.lastIndex].sourceRank
            val lastMoveTargetFile = moveHistory[moveHistory.lastIndex].targetFile
            val lastMoveTargetRank = moveHistory[moveHistory.lastIndex].targetRank

            if(pieces[movement.sourceFile][movement.sourceRank].name == "pawn"){
                if(abs(lastMoveSourceRank-lastMoveTargetRank) == 2
                    && lastMoveSourceFile == lastMoveTargetFile){
                    if(pieces[movement.sourceFile][movement.sourceRank].color == "white"
                        && pieces[movement.targetFile][movement.targetRank - 1].color == "black"
                        && pieces[movement.targetFile][movement.targetRank - 1].moveCounter == 1){
                        if(lastMoveTargetFile == movement.targetFile && lastMoveTargetRank == movement.targetRank - 1){
                            return pieces[movement.targetFile][movement.targetRank - 1]
                        }
                    }
                    if(pieces[movement.sourceFile][movement.sourceRank].color == "black"
                        && pieces[movement.targetFile][movement.targetRank + 1].color == "white"
                        && pieces[movement.targetFile][movement.targetRank + 1].moveCounter == 1){
                        if(lastMoveTargetFile == movement.targetFile && lastMoveTargetRank == movement.targetRank + 1){
                            return pieces[movement.targetFile][movement.targetRank + 1]
                        }
                    }
                }
            }
        }
        return null
    }

    private fun generateSpecialMoveCheckCastling(color : String) : List<Array<ChessPiece.Movement>>{
        val moveList = mutableListOf<Array<ChessPiece.Movement>>()
        val allEnemyMoves = mutableListOf<ChessPiece.Movement>()
        if(color == "black"){
            if(pieces[4][7].name == "king" && pieces[4][7].moveCounter == 0) {//rule: king not moved
                //small castling
                if(pieces[7][7].name == "rook" && pieces[7][7].moveCounter == 0) {//rule: rook not moved
                    if(pieces[5][7].color.isEmpty() && pieces[6][7].color.isEmpty()){//rule: move space is free of figures
                        allEnemyMoves.addAll(generateAllMovements("white",false))
                        var shortCastlingPossible = true
                        for(move in allEnemyMoves){
                            if(move.targetRank == 7 && (move.targetFile == 4 //rule: king is not attacked
                                        || move.targetFile == 5 //rule: transfer file of castling is not attacked
                                        || move.targetFile == 6)){ //rule: future position of king is not attacked
                                shortCastlingPossible = false
                            }
                        }
                        if(shortCastlingPossible){
                            moveList.add(
                                arrayOf(
                                    ChessPiece.Movement(CASTLING_MOVEMENT,4, 7, 6, 7),
                                    ChessPiece.Movement(CASTLING_MOVEMENT,7,7,5,7)
                                )
                            )
                        }
                    }
                }
                //large castling
                if (pieces[0][7].name == "rook" && pieces[0][7].moveCounter == 0) {//rule: rook not moved
                    if(pieces[1][7].color.isEmpty() && pieces[2][7].color.isEmpty() && pieces[3][7].color.isEmpty()){//rule: move space is free of figures
                        if(allEnemyMoves.isEmpty())allEnemyMoves.addAll(generateAllMovements("white",false))
                        var longCastlingPossible = true
                        for(move in allEnemyMoves){
                            if(move.targetRank == 7 && (move.targetFile == 4 //rule: king is not attacked
                                        || move.targetFile == 3 //rule: transfer file of castling is not attacked
                                        || move.targetFile == 2 //rule: future position of king is not attacked
                                        || move.targetFile == 1)){ //rule: transfer file of castling is not attacked
                                longCastlingPossible = false
                            }
                        }
                        if(longCastlingPossible){
                            moveList.add(
                                arrayOf(
                                    ChessPiece.Movement(CASTLING_MOVEMENT,4, 7, 2, 7),
                                    ChessPiece.Movement(CASTLING_MOVEMENT,0,7,3,7)
                                )
                            )
                        }
                    }
                }
            }
        } else if (color == "white") {
            if(pieces[4][0].name == "king" && pieces[4][0].moveCounter == 0) {//rule: king not moved
                //small castling
                if(pieces[7][0].name == "rook" && pieces[7][0].moveCounter == 0) {//rule: rook not moved
                    if(pieces[5][0].color.isEmpty() && pieces[6][0].color.isEmpty()){//rule: move space is free of figures
                        allEnemyMoves.addAll(generateAllMovements("black",false))
                        var shortCastlingPossible = true
                        for(move in allEnemyMoves){
                            if(move.targetRank == 0 && (move.targetFile == 4 //rule: king is not attacked
                                        || move.targetFile == 5 //rule: transfer file of castling is not attacked
                                        || move.targetFile == 6)){ //rule: future position of king is not attacked
                                shortCastlingPossible = false
                            }
                        }
                        if(shortCastlingPossible){
                            moveList.add(
                                arrayOf(
                                    ChessPiece.Movement(CASTLING_MOVEMENT,4, 0, 6, 0),
                                    ChessPiece.Movement(CASTLING_MOVEMENT,7,0,5,0)
                                )
                            )
                        }
                    }
                }
                //large castling
                if (pieces[0][0].name == "rook" && pieces[0][0].moveCounter == 0) {//rule: rook not moved
                    if(pieces[1][0].color.isEmpty() && pieces[2][0].color.isEmpty() && pieces[3][0].color.isEmpty()){//rule: move space is free of figures
                        if(allEnemyMoves.isEmpty())allEnemyMoves.addAll(generateAllMovements("black",false))
                        var longCastlingPossible = true
                        for(move in allEnemyMoves){
                            if(move.targetRank == 0 && (move.targetFile == 4 //rule: king is not attacked
                                        || move.targetFile == 3 //rule: transfer file of castling is not attacked
                                        || move.targetFile == 2 //rule: future position of king is not attacked
                                        || move.targetFile == 1)){ //rule: transfer file of castling is not attacked
                                longCastlingPossible = false
                            }
                        }
                        if(longCastlingPossible){
                            moveList.add(
                                arrayOf(
                                    ChessPiece.Movement(CASTLING_MOVEMENT,4, 0, 2, 0),
                                    ChessPiece.Movement(CASTLING_MOVEMENT,0,0,3,0)
                                )
                            )
                        }
                    }
                }
            }
        }
        return moveList
    }

    private fun generateAllMovements(color : String, generateCastlingMoves:Boolean) : List<ChessPiece.Movement>{
        val moveList = mutableListOf<ChessPiece.Movement>()
        for(file in pieces.indices){
            for(rank in pieces[0].indices){
                if(pieces[file][rank].color==color){
                    moveList.addAll(getTargetMovements(file,rank,generateCastlingMoves))
                }
            }
        }
        return moveList
    }

    private fun getBoardStateString(): String {
        val boardStateString = java.lang.StringBuilder("$moveColor:\n")
        for(file in pieces.indices){
            for(rank in pieces[0].indices){
                when(pieces[rank][file].color){
                    "white" -> {boardStateString.append(pieces[rank][file].name[0].toUpperCase())}
                    "black" -> {boardStateString.append(pieces[rank][file].name[0].toLowerCase())}
                    else -> {boardStateString.append(" ")}
                }
            }
            boardStateString.append("\n")
        }
        return return boardStateString.toString()
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
    fun pointsBlack(): Int {
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
    fun pointsWhite(): Int {
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


    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        var cStringBuilder = StringBuilder("")
        for(file in pieces.size-1 downTo 0){
            for(rank in pieces[0].indices){
                val f = pieces[rank][file].name
                if(f.isEmpty())cStringBuilder.append(" ")
                else {
                    if(pieces[rank][file].color == "white"){
                        cStringBuilder.append(f[0].toUpperCase())
                    } else {
                        cStringBuilder.append(f[0].toLowerCase())
                    }
                }
                if(rank < pieces[0].size-1)cStringBuilder.append(" | ")
            }
            if(file <= pieces.size-1)cStringBuilder.append("\n")
        }
        return cStringBuilder.toString()
    }

    fun reset(originalBoard: Chessboard) {
        this.pieces =  originalBoard.pieces.copy()
        this.moveColor = originalBoard.moveColor
    }

    private fun Array<Array<ChessPiece>>.copy() = map { it.clone() }.toTypedArray()


}