package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.ChessPiece.MovementNotation.Companion.CASTLING_MOVEMENT
import emerald.apps.fairychess.utility.FigureParser
import kotlin.math.abs
import kotlin.math.sign


data class Chessboard(val chessFormationArray: Array<Array<String>>,val figureMap : Map<String, FigureParser.Figure> ) {
    /* pieces-array: (rank,file)-coordinates
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
            for (rank in 0..7) {
                for (file in 0..7) {
                    var color = ""
                    if(file <= 4 && chessFormationArray[rank][file].isNotEmpty())color = "white"
                    if (file > 4 && chessFormationArray[rank][file].isNotEmpty()) color = "black"
                    if(figureMap.containsKey(chessFormationArray[rank][file])){
                        val movement = figureMap[chessFormationArray[rank][file]]?.movementParlett
                        val value =  figureMap[chessFormationArray[rank][file]]?.value!!
                        if(movement != null){
                            pieces[rank][file] = ChessPiece(
                                chessFormationArray[rank][file],
                                rank,
                                file,
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
        for(rank in chessboard.pieces.indices){
            for(line in chessboard.pieces[rank].indices){
                chessboard.pieces[rank][line] = pieces[rank][line]
            }
        }
        return chessboard
    }

    /**
     * return a list of possible movements of the figure at (sourceFile,sourceRank)
     * */
    fun getTargetMovements(sourceRank:Int, sourceFile:Int, genCastlingMoves:Boolean) : MutableList<ChessPiece.Movement>{
        val nonRelativeMovements = pieces[sourceRank][sourceFile].generateMovements()
        val relativeMovements = mutableListOf<ChessPiece.Movement>()
        //filter target squares
        for(nonRelativeMovement in nonRelativeMovements){
            if(!(
                nonRelativeMovement.sourceFile !in 0..7 || nonRelativeMovement.sourceRank !in 0..7
                || nonRelativeMovement.targetFile !in 0..7 || nonRelativeMovement.targetRank !in 0..7
                    || pieces[nonRelativeMovement.targetRank][nonRelativeMovement.targetFile].color == pieces[sourceRank][sourceFile].color)
                        && !isShadowedByFigure(sourceRank,sourceFile,nonRelativeMovement.targetRank,nonRelativeMovement.targetFile)
                            && fullfillsCondition(nonRelativeMovement)){
                relativeMovements.add(nonRelativeMovement)
            }
        }
        //add special movements
        if(genCastlingMoves){
            for(moveArray in generateSpecialMoveCheckCastling(pieces[sourceRank][sourceFile].color)){
                relativeMovements.add(moveArray[0])//king move
            }
        }
        return relativeMovements
    }

    /** return a list of all posible moves for player of @color*/
    fun getAllPossibleMoves(color : String) : List<ChessPiece.Movement>{
        var allPossibleMoves = mutableListOf<ChessPiece.Movement>()
        for(rank in 0..7){
            for(file in 0..7){
                if(pieces[rank][file].color == color){
                    allPossibleMoves.addAll(getTargetMovements(rank,file,true))
                }
            }
        }
        return allPossibleMoves
    }

    /** return if figure at (targetFile,targetRank) can be reached by figure at (sourceFile,sourceRank) or not
     * (is shadowed by a figure between both of them)*/
    fun isShadowedByFigure(sourceRank:Int, sourceFile:Int, targetRank: Int, targetFile: Int) : Boolean{
        for(movement in pieces[sourceRank][sourceFile].movingPatternString.split(",")){
            when {
                movement.contains(">") -> {
                    return isShadowedByFigureOrthogonal(sourceRank,sourceFile,targetRank,targetFile)
                }
                movement.contains("<") -> {
                    return isShadowedByFigureOrthogonal(sourceRank,sourceFile,targetRank,targetFile)
                }
                movement.contains("=") -> {
                    return isShadowedByFigureOrthogonal(sourceRank,sourceFile,targetRank,targetFile)
                }
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
        var returnValue = true
        if(movement.movementNotation.conditions.contains("o")) {//May not be used for a capture (e.g. pawn's forward move)
            //thus source square and target square do have same color or are empty
            returnValue = returnValue && !(pieces[movement.sourceRank][movement.sourceFile].color != pieces[movement.targetRank][movement.targetFile].color
                    && pieces[movement.sourceRank][movement.sourceFile].color.isNotEmpty()
                    && pieces[movement.targetRank][movement.targetFile].color.isNotEmpty())
        }
        if(movement.movementNotation.conditions.contains("c")) {//May only be made on a capture (e.g. pawn's diagonal capture)
            //thus source square and target square do have different colors and are not empty
            returnValue = returnValue && (pieces[movement.sourceRank][movement.sourceFile].color != pieces[movement.targetRank][movement.targetFile].color
                    && pieces[movement.sourceRank][movement.sourceFile].color.isNotEmpty()
                    && pieces[movement.targetRank][movement.targetFile].color.isNotEmpty())
            //en passante
            if(!returnValue)returnValue = specialMoveCheckEnpassante(movement) != null
        }
        if(movement.movementNotation.conditions.contains("i")) {//May only be made on the initial move (e.g. pawn's 2 moves forward)
            //thus the movecounter of the source square is 0
            returnValue = returnValue && (pieces[movement.sourceRank][movement.sourceFile].moveCounter == 0)
        }
        if(movement.movementNotation.movetype == "g") {//moves by leaping over piece to an empty square (if leaped over enemy => capture)

            val signFile = sign((movement.targetFile - movement.sourceFile).toDouble()).toInt()
            val signRank = sign((movement.targetRank - movement.sourceRank).toDouble()).toInt()
            returnValue = pieces[movement.targetRank][movement.targetFile].color.isEmpty()
                    && pieces[movement.targetRank-signRank][movement.targetFile-signFile].color.isNotEmpty()
                    && (abs(movement.targetFile-movement.sourceFile) > 1 || abs(movement.targetRank-movement.sourceRank) > 1)
        }
        return returnValue
    }

    /** return true if figure at (targetRank,targetFile) can be reached by figure at (sourceRank,sourceFile) with a orthogonal movement
     * or false if not (thus figure is blocked by another figure between both of them (shadowed))*/
    fun isShadowedByFigureOrthogonal(sourceRank:Int, sourceFile:Int, targetRank:Int, targetFile: Int) : Boolean{
        if(sourceFile == targetFile && (abs(targetRank-sourceRank) > 1)){//distance > 1 because a figure has to stand between them for shadow
            //move on file (horizontal)
            val signDifRank = sign((targetRank-sourceRank).toDouble()).toInt()
            var rank = sourceRank + signDifRank
            while(rank != targetRank){
                if(pieces[rank][sourceFile].color != ""){
                    return true
                }
                rank += signDifRank
            }
        }
        if(sourceRank == targetRank && (abs(targetFile-sourceFile) > 1)){
            //move on rank (vertical)
            val signDifFile = sign((targetFile-sourceFile).toDouble()).toInt()
            var file = sourceFile + signDifFile
            while(file != targetFile){
                if(pieces[sourceRank][file].color != ""){
                    return true
                }
                file += signDifFile
            }
        }
        return false
    }

    /** return true if figure at (targetRank,targetFile) can be reached by figure at (sourceRank,sourceFile) with a diagonal movement
     * or false if not (thus figure is blocked by another figure between both of them (shadowed))*/
    fun isShadowedByFigureDiagonal(sourceFile:Int,sourceRank:Int,targetFile:Int,targetRank: Int) : Boolean{
        if(abs(targetRank-sourceRank) > 1 && abs(targetFile-sourceFile) > 1){
            val signRank = sign((targetRank-sourceRank).toDouble()).toInt()
            val signFile = sign((targetFile-sourceFile).toDouble()).toInt()
            if(abs(targetRank-sourceRank) - abs(targetFile-sourceFile) == 0){
                //movement vector is truly diagonally, thus difference between sourceFile and targetFile and sourceRank and targetRank is 0
                for(i in 1..abs(targetFile-sourceFile)){
                    val rank = sourceRank+(signRank*i)
                    val file = sourceFile+(signFile*i)
                    if(rank in 0..7 && file in 0..7
                        && pieces[rank][file].color != ""){
                            return true
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
        for (rank in pieces.indices) {
            for (file in pieces[rank].indices) {
                if (pieces[rank][file].name == "king") {
                    if (pieces[rank][file].color == "white") {
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
        if(pieces[movement.sourceRank][movement.sourceFile].color == "")return "empty field"
        else if(pieces[movement.sourceRank][movement.sourceFile].color == pieces[movement.targetRank][movement.targetFile].color)return "same color"
        else if(pieces[movement.sourceRank][movement.sourceFile].color != moveColor)return "wrong figure"
        else {
            val targetMovements = getTargetMovements(movement.sourceRank,movement.sourceFile,true)
            for(targetMovement in targetMovements){
                if(targetMovement.movementNotation == CASTLING_MOVEMENT){
                    val castleMovements = generateSpecialMoveCheckCastling(color)//TODO: inefficent, idea: create var lastCastleMovement and use it here
                    for(castleMovement in castleMovements){
                        if(castleMovement[0].sourceFile == movement.sourceFile
                            && castleMovement[0].sourceRank == movement.sourceRank){
                            //move king
                            pieces[castleMovement[0].targetRank][castleMovement[0].targetFile] =
                                ChessPiece(
                                    pieces[castleMovement[0].sourceRank][castleMovement[0].sourceFile].name,
                                    castleMovement[0].targetRank,
                                    castleMovement[0].targetFile,
                                    pieces[castleMovement[0].sourceRank][castleMovement[0].sourceFile].value,
                                    pieces[castleMovement[0].sourceRank][castleMovement[0].sourceFile].color,
                                    pieces[castleMovement[0].sourceRank][castleMovement[0].sourceFile].movingPatternString,
                                    pieces[castleMovement[0].sourceRank][castleMovement[0].sourceFile].moveCounter+1,
                                )
                            pieces[castleMovement[0].sourceRank][castleMovement[0].sourceFile] = ChessPiece.emptyChessPiece
                            //move rook
                            pieces[castleMovement[1].targetFile][castleMovement[1].targetRank] =
                                ChessPiece(
                                    pieces[castleMovement[1].sourceRank][castleMovement[1].sourceFile].name,
                                    castleMovement[1].targetRank,
                                    castleMovement[1].targetFile,
                                    pieces[castleMovement[1].sourceRank][castleMovement[1].sourceFile].value,
                                    pieces[castleMovement[1].sourceRank][castleMovement[1].sourceFile].color,
                                    pieces[castleMovement[1].sourceRank][castleMovement[1].sourceFile].movingPatternString,
                                    pieces[castleMovement[1].sourceRank][castleMovement[1].sourceFile].moveCounter+1,
                                )
                            pieces[castleMovement[1].sourceRank][castleMovement[1].sourceFile] = ChessPiece.emptyChessPiece
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

        if(pieces[movement.sourceRank][movement.sourceFile].name == "pawn"){
            lastPawnMove = 0
        }

        //valid movement
        if(DEBUG)println(movement.asString2(moveColor))
        if(userMovement.movementNotation.movetype == "g") {
            //capture the piece hopped over
            val signFile = sign((userMovement.targetFile - userMovement.sourceFile).toDouble()).toInt()
            val signRank = sign((userMovement.targetRank - userMovement.sourceRank).toDouble()).toInt()
            val captureFile = userMovement.targetFile-signFile
            val captureRank = userMovement.targetRank-signRank
            if(pieces[captureRank][captureFile].color != moveColor){
                if(pieces[captureRank][captureFile].color == "white"){
                    whiteCapturedPieces.add(pieces[captureRank][captureFile])
                    lastCaptureCounter = 0
                } else if(pieces[captureRank][captureFile].color == "black"){
                    blackCapturedPieces.add(pieces[captureRank][captureFile])
                    lastCaptureCounter = 0
                }
                pieces[captureRank][captureFile] = ChessPiece(
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
        if(pieces[movement.targetRank][movement.targetFile].color == "white"){
            whiteCapturedPieces.add(pieces[movement.targetRank][movement.targetFile])
            lastCaptureCounter = 0
        } else if(pieces[movement.targetRank][movement.targetFile].color == "black"){
            blackCapturedPieces.add(pieces[movement.targetRank][movement.targetFile])
            lastCaptureCounter = 0
        }
        //move piece and replace target
        var pieceName = pieces[movement.sourceRank][movement.sourceFile].name
        if(movement is ChessPiece.PromotionMovement){
            pieceName
        }
        pieces[movement.targetRank][movement.targetFile] = ChessPiece(
            pieceName,
            movement.targetFile,
            movement.targetRank,
            pieces[movement.sourceRank][movement.sourceFile].value,
            pieces[movement.sourceRank][movement.sourceFile].color,
            pieces[movement.sourceRank][movement.sourceFile].movingPatternString,
            pieces[movement.sourceRank][movement.sourceFile].moveCounter+1,
        )
        pieces[movement.sourceRank][movement.sourceFile] = ChessPiece(
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
        if(pieces[chessPiece.positionRank][chessPiece.positionFile].color == "black"){
            blackCapturedPieces.add(pieces[chessPiece.positionRank][chessPiece.positionFile])
        }
        if(pieces[chessPiece.positionRank][chessPiece.positionFile].color == "white"){
            whiteCapturedPieces.add(pieces[chessPiece.positionRank][chessPiece.positionFile])
        }
        pieces[chessPiece.positionRank][chessPiece.positionFile] = ChessPiece.emptyChessPiece
    }


    private fun specialMoveCheckEnpassante(movement: ChessPiece.Movement) : ChessPiece?{
        if(moveHistory.isNotEmpty()){
            val lastMoveSourceFile = moveHistory[moveHistory.lastIndex].sourceFile
            val lastMoveSourceRank = moveHistory[moveHistory.lastIndex].sourceRank
            val lastMoveTargetFile = moveHistory[moveHistory.lastIndex].targetFile
            val lastMoveTargetRank = moveHistory[moveHistory.lastIndex].targetRank

            if(pieces[movement.sourceRank][movement.sourceFile].name == "pawn"){
                if(abs(lastMoveSourceRank-lastMoveTargetRank) == 2
                    && lastMoveSourceFile == lastMoveTargetFile){
                    if(pieces[movement.sourceRank][movement.sourceFile].color == "white"
                        && pieces[movement.targetRank - 1][movement.targetFile].color == "black"
                        && pieces[movement.targetRank - 1][movement.targetFile].moveCounter == 1){
                        if(lastMoveTargetRank == (movement.targetFile - 1) && lastMoveTargetFile == movement.targetFile){
                            return pieces[movement.targetRank - 1][movement.targetFile]
                        }
                    }
                    if(pieces[movement.sourceRank][movement.sourceFile].color == "black"
                        && pieces[movement.targetRank + 1][movement.targetFile].color == "white"
                        && pieces[movement.targetRank + 1][movement.targetFile].moveCounter == 1){
                        if(lastMoveTargetRank == (movement.targetFile + 1) && lastMoveTargetFile == movement.targetFile){
                            return pieces[movement.targetRank + 1][movement.targetFile]
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
                            if(move.targetFile == 7 && (move.targetRank == 4 //rule: king is not attacked
                                        || move.targetRank == 5 //rule: transfer file of castling is not attacked
                                        || move.targetRank == 6)){ //rule: future position of king is not attacked
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
                            if(move.targetFile == 7 && (move.targetRank == 4 //rule: king is not attacked
                                        || move.targetRank == 3 //rule: transfer file of castling is not attacked
                                        || move.targetRank == 2 //rule: future position of king is not attacked
                                        || move.targetRank == 1)){ //rule: transfer file of castling is not attacked
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
                            if(move.targetFile == 0 && (move.targetRank == 4 //rule: king is not attacked
                                        || move.targetRank == 5 //rule: transfer file of castling is not attacked
                                        || move.targetRank == 6)){ //rule: future position of king is not attacked
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
                            if(move.targetFile == 0 && (move.targetRank == 4 //rule: king is not attacked
                                        || move.targetRank == 3 //rule: transfer file of castling is not attacked
                                        || move.targetRank == 2 //rule: future position of king is not attacked
                                        || move.targetRank == 1)){ //rule: transfer file of castling is not attacked
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
                if(pieces[rank][file].color==color){
                    moveList.addAll(getTargetMovements(rank,file,generateCastlingMoves))
                }
            }
        }
        return moveList
    }

    private fun getBoardStateString(): String {
        val boardStateString = java.lang.StringBuilder("$moveColor:\n")
        for(rank in pieces.indices){
            for(file in pieces[0].indices){
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
        for (rank in 0..7) {
            if(pieces[rank][0].name == "pawn"){
                promotion = (pieces[rank][0])
            }
            if(pieces[rank][7].name == "pawn"){
                promotion = (pieces[rank][7])
            }
        }
    }

    /** calculate all points of black player */
    fun pointsBlack(): Int {
        var punkte = 0
        for (rank in 0..7) {
            for (file in 0..7) {
                if(pieces[rank][file].color == "black"){
                    punkte += pieces[rank][file].value
                }
            }
        }
        return punkte
    }

    /** calculate all points of white player */
    fun pointsWhite(): Int {
        var punkte = 0
        for (rank in 0..7) {
            for (file in 0..7) {
                if(pieces[rank][file].color == "white"){
                    punkte += pieces[rank][file].value
                }
            }
        }
        return punkte
    }

    /** promote figure at (file,rank) to promotion*/
    private fun promote(promotion: String, rank:Int, file: Int) {
        val color: String = pieces[rank][file].color
        pieces[rank][file] = ChessPiece(promotion, rank, file, 10, color, "", 0)
    }


    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        var cStringBuilder = StringBuilder("")
        for(rank in pieces.size-1 downTo 0){
            for(file in pieces[0].indices){
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
            if(rank <= pieces.size-1)cStringBuilder.append("\n")
        }
        return cStringBuilder.toString()
    }

    fun reset(originalBoard: Chessboard) {
        this.pieces =  originalBoard.pieces.copy()
        this.moveColor = originalBoard.moveColor
    }

    private fun Array<Array<ChessPiece>>.copy() = map { it.clone() }.toTypedArray()


}