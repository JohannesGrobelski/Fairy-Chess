@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_LONG_BLACK
import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_LONG_WHITE
import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_SHORT_BLACK
import emerald.apps.fairychess.model.MovementNotation.Companion.CASTLING_SHORT_WHITE
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import emerald.apps.fairychess.view.ChessActivity
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.Math.random
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sign

class Bitboard(
    private val chessFormationArray: Array<Array<String>>?,
    val figureMap: Map<String, FigureParser.Figure>
) {

    private val colors = arrayOf("white","black")

    //map from name of figure to 2D-Array
    //2D-array of chesspieces to represents board (starting with H8 moving rows first und lines down to A1)
    //thus a bitboard represents H8H7H6H5H4H3H2H1G8G7G6....B2B1A8A7A6A5A4A3A2A1
    //first element are white figures, second black figures
    var bbFigures : MutableMap<String, Array<ULong>> = mutableMapOf() //bitboard represents all figures of name and color on chessboard (e.g. all white pawns)
    var gameFinished = false
    var bbMovedCaptured : ULong = 0uL //bitboard represents all figures that moved or are captured
    var bbComposite : ULong = 0uL //bitboard represents all figures on chessboard
    var bbColorComposite : Array<ULong> = arrayOf(0uL,0uL) //bitboard represents all figures of one color on chessboard
    var gameWinner = ""
    var playerWithDrawOpportunity = ""
    var promotionCoordinate : Coordinate? = null
    var moveColor = "white"
    val boardStateHistory = mutableListOf<Map<String, Array<ULong>>>() //move history, for each move map (figureName -> Bitboard)
    val movedCapturedHistory = mutableListOf<ULong>() //history of bbMovedCaptured for each move
    val blackCapturedPieces = mutableListOf<ChessActivity.CapturedPiece>()
    val whiteCapturedPieces = mutableListOf<ChessActivity.CapturedPiece>()

    constructor(figureMap: Map<String, FigureParser.Figure>) : this(null,figureMap)


    init {
        if(chessFormationArray != null){
            //pass a string representing the chess formation here and update chessFormationArray
            if (chessFormationArray.size == 8 && chessFormationArray[0].size == 8) {
                for (rank in 0..7) {
                    for (file in 0..7) {
                        var color = ""
                        val name = chessFormationArray[rank][file]
                        if(file <= 4 && name.isNotEmpty())color = "white"
                        if (file > 4 && name.isNotEmpty())color = "black"
                        if(figureMap.containsKey(name)){
                            if(!bbFigures.containsKey(name)){
                                bbFigures[name] = arrayOf(0uL, 0uL)
                            }
                            setFigure(name,color, Coordinate(rank,file))
                        }
                    }
                }
            }
            val figureList = mutableListOf<Coordinate>()
            for(key in bbFigures.keys){
                figureList.addAll(generateCoordinatesFrom64BPosition(bbFigures[key]?.get(0) ?: 0uL))
                figureList.addAll(generateCoordinatesFrom64BPosition(bbFigures[key]?.get(1) ?: 0uL))
            }
        }
        addEntryToHistory()
    }

    fun promotePawn(coordinate: Coordinate, name : String){
        if(promotionCoordinate?.equals(coordinate) == true){
            if(bbFigures.containsKey(name)){
                val bbCoordinate = generate64BPositionFromCoordinate(coordinate)
                val pos = (coordinate.file == 0).toInt() //black is on file 0, white on file 7
                bbFigures["pawn"]!![pos] = bbFigures["pawn"]!![pos] and bbCoordinate.inv()
                bbFigures[name]!![pos] = bbFigures[name]!![pos] or bbCoordinate
                promotionCoordinate = null
            }
        }
    }

    /** @param color of the piece set
     *  @param name of the piece set
     * sets figure at coordinate */
    fun setFigure(name:String,color: String,coordinate: Coordinate){
        val pos = getPosition(color)
        val bbFigure = generate64BPositionFromCoordinate(coordinate)
        if(!bbFigures.containsKey(name)){
            val value = arrayOf(0uL,0uL)
            value[pos] = bbFigure
            bbFigures[name] = value
        } else bbFigures[name]!![pos] = add64BPositionFromCoordinates(bbFigures[name]!![pos],coordinate)
        bbComposite = add64BPositionFromCoordinates(bbComposite,coordinate)
        bbColorComposite[pos] = add64BPositionFromCoordinates(bbColorComposite[pos],coordinate)
    }

    fun set(bitboard: Bitboard){
        bbFigures.clear()
        for(key in bitboard.bbFigures.keys){
            bbFigures[key] = bitboard.bbFigures[key]!!.clone()
        }
        bbComposite = bitboard.bbComposite
        bbColorComposite = bitboard.bbColorComposite
        blackCapturedPieces.clear(); blackCapturedPieces.addAll(bitboard.blackCapturedPieces)
        whiteCapturedPieces.clear(); whiteCapturedPieces.addAll(bitboard.whiteCapturedPieces)
        moveColor = bitboard.moveColor
        clearHistory(); boardStateHistory.addAll(bitboard.boardStateHistory)
        movedCapturedHistory.clear(); movedCapturedHistory.addAll(bitboard.movedCapturedHistory)
        bbMovedCaptured = bitboard.bbMovedCaptured
    }

    fun clone() : Bitboard{
        val newBitboard = Bitboard(figureMap)
        for(key in bbFigures.keys){
            newBitboard.bbFigures[key] = bbFigures[key]!!.clone()
        }
        newBitboard.bbMovedCaptured = bbMovedCaptured
        newBitboard.bbComposite = bbComposite
        newBitboard.bbColorComposite = bbColorComposite.clone()
        newBitboard.blackCapturedPieces.addAll(blackCapturedPieces)
        newBitboard.whiteCapturedPieces.addAll(whiteCapturedPieces)
        newBitboard.moveColor = moveColor
        newBitboard.copyHistory(this)
        newBitboard.movedCapturedHistory.addAll(movedCapturedHistory)
        return newBitboard
    }

    fun checkMoveAndMove(color:String, movement: Movement) : String{
        if(moveColor != color)return "wrong player!"
        val name = getPieceName(movement.getSourceCoordinate())
        return preMoveCheck(name, color, movement)
    }

    fun preMoveCheck(name: String, color : String, movement: Movement) : String{
        if(moveColor != color)return "wrong color"
        if(movement.sourceFile == movement.targetFile && movement.sourceRank == movement.targetRank)return "same square"
        val pos = getPosition(color)
        val coordinate = Coordinate(movement.sourceRank,movement.sourceFile)
        val bbSource = generate64BPositionFromCoordinate(coordinate)
        if(bbColorComposite[pos] and bbSource != bbSource)return "wrong figure"
        val legalMovements = getTargetMovementsAsMovementList(color,coordinate)
        for(legalMove in legalMovements){
            if(legalMove.sourceRank == movement.sourceRank
            && legalMove.sourceFile == movement.sourceFile
            && legalMove.targetFile == movement.targetFile
            && legalMove.targetRank == movement.targetRank){
                return move(color, movement)
            }
        }
        return "not a legal move"
    }

    /** @param color of the piece set
     *  @param name of the piece set
     * moves figure from coordinate (sourceFile,sourceRow) to coordinate (targetFile,targetRow)
     * does not check if move is legal */
    fun move(color : String, movement: Movement) : String{
        val pos = getPosition(color)
        val name = getPieceName(movement.getSourceCoordinate())
        if(checkForAndMakeEnpassanteMove(name, color, pos, movement))return ""

        var bbFigureColor = bbFigures[name]!![pos]
        val bbSource = generate64BPositionFromCoordinate(movement.getSourceCoordinate())
        val bbTarget = generate64BPositionFromCoordinate(movement.getTargetCoordinate())

        //no figure of opposite of color can stand at (targetRank,targetFile) therefore set bit to 0 on this position
        val targetName = getPieceName(movement.getTargetCoordinate())
        if(targetName.isNotEmpty()){
            var bbFigureOppositeColor = bbFigures[targetName]!![1-pos]
            bbFigureOppositeColor = bbFigureOppositeColor and bbTarget.inv()
            bbFigures[targetName]?.set(1-pos,bbFigureOppositeColor)
        }

        //calculate change vector with set bits on old and new position
        val bbPath = bbSource or bbTarget

        if(movement is PromotionMovement){
            val bbPawns = bbFigures[name]!![pos] and bbSource.inv()
            bbFigures["pawn"]!![pos] = bbPawns
            val bbPromotion = bbFigures[movement.promotion]!![pos] or bbTarget
            bbFigures[movement.promotion]!![pos] = bbPromotion
        } else {
            //xor targetBB with changeBB to change bit of old position from 1 to 0 and bit from new position from 0 to 1
            //and thus move the piece
            bbFigureColor = bbFigureColor xor bbPath
            bbFigures[name]?.set(pos,bbFigureColor)
        }

        //write move into bbComposite (important: cannot xor with bbChange because figure can stand on target square)
        //one figure must stand at (sourceRank,sourceFile) therefore set bit to 1 on this position
        bbComposite = bbComposite or bbTarget
        //no figure can stand at (targetRank,targetFile) therefore set bit to 0 on this position
        bbComposite = bbComposite and bbSource.inv()

        //write move into bbColorComposite for move color like above
        bbColorComposite[pos] = bbColorComposite[pos] xor bbPath
        //no figure of opposite of color can stand at (targetRank,targetFile) therefore set bit to 0 on this position
        bbColorComposite[1-pos] = bbColorComposite[1-pos] and bbTarget.inv()

        bbMovedCaptured = bbMovedCaptured or bbSource or bbTarget
        addEntryToHistory();
        movedCapturedHistory.add(bbMovedCaptured)
        //add current position (as map bbFigures) to history
        if(movement.movementNotation in arrayOf(CASTLING_LONG_BLACK, CASTLING_SHORT_WHITE,
                CASTLING_SHORT_BLACK, CASTLING_LONG_WHITE)){
            makeCastlingMove(color,movement)
        } else {
            checkForPromotion()
            switchMoveColor()
        }
        return ""
    }

    /** checks if movement is en passante and if so makes en passante move*/
    fun checkForAndMakeEnpassanteMove(name: String, color : String, pos : Int, movement : Movement) : Boolean {
        val isEnpassante = checkIfEnpassanteMove(name, pos, movement)
        if(isEnpassante){
            val bbSource = generate64BPositionFromCoordinate(movement.getSourceCoordinate())
            val bbTarget = generate64BPositionFromCoordinate(movement.getTargetCoordinate())
            val bbPath = bbSource or bbTarget
            val fileOffset = -((-1.0).pow(pos.toDouble())).toInt() //-1 for white, +1 for black
            val bbRemoveFigure = generate64BPositionFromCoordinate(movement.getTargetCoordinate().newCoordinateFromFileOffset(fileOffset))

            //remove enemy pawn
            bbFigures["pawn"]!![1-pos] = bbFigures["pawn"]!![1-pos] and bbRemoveFigure.inv()
            //move pawn
            bbFigures["pawn"]!![pos] = bbFigures["pawn"]!![pos] xor bbPath

            bbComposite = (bbComposite and bbSource.inv() and bbRemoveFigure.inv()) or bbTarget
            bbColorComposite[1-pos] = bbColorComposite[1-pos] and bbRemoveFigure.inv()
            bbColorComposite[pos] = bbColorComposite[pos] xor bbPath
            bbMovedCaptured = bbMovedCaptured or bbSource or bbTarget or bbRemoveFigure
            addEntryToHistory()
            movedCapturedHistory.add(bbMovedCaptured)
            switchMoveColor()
        }
        return isEnpassante
    }

    /** checks if movement is en passante */
    fun checkIfEnpassanteMove(name : String, pos: Int, movement: Movement) : Boolean {
        //check for special case enpassante
        if(boardStateHistory.size < 2)return false
        if(name == "pawn" && movement.getRankDif() == 1
            && bbFigures["pawn"]!![1-pos] and generate64BPositionFromCoordinate(movement.getTargetCoordinate()) == 0uL){
            //pawn moved diagonaly, but there is no target on target square
            val fileOffset = -((-1.0).pow(pos.toDouble())).toInt() //-1 for white, +1 for black
            val bbRemoveFigure = generate64BPositionFromCoordinate(movement.getTargetCoordinate().newCoordinateFromFileOffset(fileOffset))
            if(bbFigures["pawn"]!![1-pos] and bbRemoveFigure == bbRemoveFigure){
                //therefore an enemy pawn must be above (black pawn) or under (white pawn) the target square
                val removeFileActual = movement.targetFile + fileOffset
                val removeFileSupposed = round(3.5 + -fileOffset*.1).toInt() //4 for white, 3 for black
                val bbPawnPositionLastMove = boardStateHistory[boardStateHistory.lastIndex - 1]["pawn"]!![1-pos]
                val bbPawnPositionNow = bbFigures["pawn"]!![1-pos]
                val bbCurrentPositionOfPawnWhoMovedLast = bbPawnPositionNow and bbPawnPositionLastMove.inv()
                if(bbCurrentPositionOfPawnWhoMovedLast and bbRemoveFigure != 0uL && removeFileActual == removeFileSupposed){
                    //check if the pawn made his first move in last move and if this move took place on the correct file (3 or 4)
                    //all conditions are met, thus make enpassante move
                    return true
                }
                return false
            }
            return false
        }
        return false
    }


    fun addEntryToHistory() {
        val newBB = mutableMapOf<String,Array<ULong>>()
        for(key in bbFigures.keys){
            newBB[key] = bbFigures[key]!!.copyOf()
        }
        boardStateHistory.add(newBB.toMap())
    }

    fun clearHistory(numberOfEntries : Int = boardStateHistory.size){
        if(numberOfEntries == boardStateHistory.size){
            boardStateHistory.clear()
            movedCapturedHistory.clear()
        } else {
            repeat(numberOfEntries){
                boardStateHistory.removeLast()
                movedCapturedHistory.removeLast()
            }
        }
    }

    fun copyHistory(bitboard: Bitboard){
        clearHistory()
        boardStateHistory.addAll(bitboard.boardStateHistory)
    }

    private fun makeCastlingMove(color: String, movement : Movement){
        if(color ==  "white"){
            if(movement.targetRank == 2){ //LONG white castling move
                move(color, Movement(0,0,3,0))
            } else { //SHORT white castling move
                move(color,Movement(7,0,5,0))
            }
        } else {
            if(movement.targetRank == 2){ //LONG black castling move
                move(color,Movement(0,7,3,7))
            } else { //SHORT black castling move
                move(color,Movement(7,7,5,7))
            }
        }
    }

    /** return a map of all posible moves for player of @color as bitmap*/
    fun getAllPossibleMoves(color : String, generatedCastlingMoves : Boolean) : Map<Coordinate,ULong> {
        val allPossibleMoves = mutableMapOf<Coordinate,ULong>()
        val pos = ("black" == color).toInt()
        for(rank in 0..7){
            for(file in 0..7){
                val bbFigure = generate64BPositionFromCoordinate(Coordinate(rank,file))
                if((bbColorComposite[pos] and bbFigure) == bbFigure){
                    val name = getPieceName(pos, bbFigure)
                    if(name.isEmpty())continue //empty field
                    allPossibleMoves[Coordinate(rank,file)] = getTargetMovements(
                        name,
                        color,
                        Coordinate(rank,file),
                        generatedCastlingMoves
                    )
                } else {
                    allPossibleMoves.remove(Coordinate(rank,file))
                }
            }
        }
        return allPossibleMoves.toMap()
    }

    fun getAllPossibleMovesAsList(color : String) : List<Movement> {
        val allPosibleMoves = mutableListOf<Movement>()
        val pos = ("black" == color).toInt()
        for(rank in 0..7) {
            for (file in 0..7) {
                val bbFigure = generate64BPositionFromCoordinate(Coordinate(rank,file))
                if((bbColorComposite[pos] and bbFigure) == bbFigure){
                    allPosibleMoves.addAll(getTargetMovementsAsMovementList(color,Coordinate(rank,file)))
                }
            }
        }
        return allPosibleMoves
    }



    fun getPieceName(pos: Int, bbFigure:ULong) : String{
        for(name in bbFigures.keys){
            if(bbFigures[name]!![pos] and bbFigure == bbFigure){
                return name
            }
        }
        return ""
    }

    fun getPieceName(coordinate: Coordinate) : String{
        val bbFigure = generate64BPositionFromCoordinate(coordinate)
        for(pieceName in bbFigures.keys){
            if((bbFigures[pieceName]!![0] or bbFigures[pieceName]!![1]) and bbFigure == bbFigure)return pieceName
        }
        return ""
    }

    /**
     * return a list of possible movements of the figure at (sourceRank,sourceFile)
     * */
    fun getTargetMovements(
        name: String,
        color: String,
        coordinate: Coordinate,
        generateCastlingMoves: Boolean
    ) : ULong {
        if(!coordinate.inRange())return 0uL
        val movementString = (figureMap[name] as FigureParser.Figure).movementParlett
        val movementNotationList = getMovementNotation(movementString)
        val bbFigure = generate64BPositionFromCoordinate(coordinate)
        var bbTargets = generateMovements(color,coordinate,movementNotationList)
        bbTargets = deleteIllegalMoves(name,color,bbFigure,bbTargets.toMutableMap(),movementNotationList)
        bbTargets = genSpecialMoves(name,color,coordinate,bbTargets,generateCastlingMoves)
        var resultMovement = 0uL
        for(key in bbTargets.keys){
            val targets : ULong = bbTargets[key] ?: error("")
            resultMovement = resultMovement or targets
        }
        return resultMovement
    }

    fun genSpecialMoves(name: String, color: String, coordinate : Coordinate, bbTargetsMap: MutableMap<MovementNotation,ULong>, generateCastlingMoves: Boolean)
        : MutableMap<MovementNotation,ULong> {
        if(name == "king" && generateCastlingMoves){//create castling moves, if possible
            return genCastlingMoves(color,coordinate,bbTargetsMap)
        } else if(name == "pawn"){
            return generateEnpassanteMove(color,coordinate,bbTargetsMap)
        }
        return bbTargetsMap
    }

    //TODO: implement
    fun getEnpassanteSquares() : List<Coordinate> {
        return emptyList()
    }

    private fun generateEnpassanteMove(color : String, coordinate : Coordinate,bbTargetsMap: MutableMap<MovementNotation,ULong>)
        : MutableMap<MovementNotation,ULong> {
        val targetRankLeft = coordinate.rank - 1
        val targetRankRight = coordinate.rank + 1
        val pos = ("black" == color).toInt()
        for(targetRank in arrayOf(targetRankLeft,targetRankRight)){
            val bbtargetPawn = generate64BPositionFromCoordinate(Coordinate(targetRank,coordinate.file))
            if(bbtargetPawn and bbFigures["pawn"]!![1-pos] == bbtargetPawn){
                //there is an enemy pawn above/under target square
                val fileOffset = ((-1.0).pow(pos.toDouble())*2).toInt() //2 for white, -2 for black
                val bbtargetPawnInitialPosition = generate64BPositionFromCoordinate(Coordinate(targetRank,coordinate.file + fileOffset))
                if(boardStateHistory.size > 1
                    && boardStateHistory[boardStateHistory.lastIndex-1]["pawn"]!![1-pos] and bbtargetPawnInitialPosition == bbtargetPawnInitialPosition){
                    //last position of target pawn was 2 steps above/under the current position => target pawn moved 2 steps in the last move
                    val movement = Movement(coordinate.rank,coordinate.file,targetRank,coordinate.file + fileOffset/2)
                    addCoordinateToMovementBitboard(color, MovementNotation.PAWN_ENPASSANTE,movement,bbTargetsMap)
                }
            }
        }
        return bbTargetsMap
    }

    fun getCastlingRights(color: String) : List<MovementNotation> {
        val ownColorPos = ("black" == color).toInt()
        var bbEnemyMoves = moveMapToComposite(getAllPossibleMoves(colors[1-ownColorPos],false))
        var castlingRights = mutableListOf<MovementNotation>()

        var bbRook: ULong
        var bbMoveRoom : ULong
        if(color == "white"){
            //1. king has not moved
            val bbWhiteKingCurrentPosition = bbFigures["king"]!![0]
            val whiteKingNotMoved = bbKingOriginalPosition[ownColorPos] and bbWhiteKingCurrentPosition and bbMovedCaptured.inv() != 0uL
            if(whiteKingNotMoved) {
                //SHORT castling
                //2. check if king and space between rook and king are not under attack
                if (bbCastlingRoomShortWhite and bbEnemyMoves.inv() == bbCastlingRoomShortWhite) {
                    //3. check if rook has not moved
                    bbRook = generate64BPositionFromCoordinate(Coordinate(7, 0))
                    if (bbMovedCaptured and bbRook == 0uL) {
                        //4. no pieces between rook and king
                        bbMoveRoom = bbCastlingRoomShortWhite and bbRook.inv()
                        bbMoveRoom = bbMoveRoom and bbFigures["king"]!![ownColorPos].inv()
                        if (bbMoveRoom and bbComposite.inv() == bbMoveRoom) {
                            castlingRights.add(CASTLING_SHORT_WHITE)
                        }
                    }
                }
                //LONG castling
                //2. check if king and space between rook and king are not under attack
                if (bbCastlingRoomLongWhite and bbEnemyMoves.inv() == bbCastlingRoomLongWhite) {
                    //3. check if rook has not moved
                    bbRook = generate64BPositionFromCoordinate(Coordinate(0, 0))
                    if (bbMovedCaptured and bbRook == 0uL) {
                        //4. no pieces between rook and king
                        bbMoveRoom = bbCastlingRoomLongWhite and bbRook.inv()
                        bbMoveRoom = bbMoveRoom and bbFigures["king"]!![ownColorPos].inv()
                        if (bbMoveRoom and bbComposite.inv() == bbMoveRoom) {
                            castlingRights.add(CASTLING_LONG_WHITE)
                        }
                    }
                }
            }
        } else {
            //1. king has not moved
            val bbBlackKingCurrentPosition = bbFigures["king"]!![1]
            val blackKingNotMoved = bbKingOriginalPosition[ownColorPos] and bbBlackKingCurrentPosition and bbMovedCaptured.inv() != 0uL
            if(blackKingNotMoved) {
                //SHORT castling
                //2. check if king and space between rook and king are not under attack
                if(bbCastlingRoomShortBlack and bbEnemyMoves.inv() == bbCastlingRoomShortBlack){
                    //3. check if rook has not moved
                    bbRook = generate64BPositionFromCoordinate(Coordinate(7,7))
                    if(bbMovedCaptured and bbRook == 0uL){
                        //4. no pieces between rook and king
                        bbMoveRoom = bbCastlingRoomShortBlack and bbRook.inv()
                        bbMoveRoom = bbMoveRoom and bbFigures["king"]!![ownColorPos].inv()
                        if(bbMoveRoom and bbComposite.inv().toULong() == bbMoveRoom){
                            castlingRights.add(CASTLING_SHORT_BLACK)
                        }
                    }
                }
                //LONG castling
                //2. check if king and space between rook and king are not under attack
                if(bbCastlingRoomLongBlack and bbEnemyMoves.inv() == bbCastlingRoomLongBlack){
                    //3. check if rook has not moved
                    bbRook = generate64BPositionFromCoordinate(Coordinate(0,7))
                    if(bbMovedCaptured and bbRook == 0uL){
                        //4. no pieces between rook and king
                        bbMoveRoom = bbCastlingRoomLongBlack and bbRook.inv()
                        bbMoveRoom = bbMoveRoom and bbFigures["king"]!![ownColorPos].inv()
                        if(bbMoveRoom and bbComposite.inv() == bbMoveRoom){
                            castlingRights.add(CASTLING_LONG_BLACK)
                        }
                    }
                }
            }
        }
        return castlingRights
    }

    /**adds (if legal) castling moves (move of king) to bbTargetsMap
     * calls getCastlingRights */
    private fun genCastlingMoves(color: String, coordinate : Coordinate, bbTargetsMap: MutableMap<MovementNotation,ULong>)
        : MutableMap<MovementNotation,ULong> {
        val castlingRightsList = getCastlingRights(color)
        for(castlingRight in castlingRightsList){
            when(castlingRight){
                CASTLING_SHORT_WHITE -> {
                    addCoordinateToMovementBitboard(color, MovementNotation.KING,Movement(
                        CASTLING_SHORT_WHITE,coordinate,6,0),bbTargetsMap)
                }
                CASTLING_LONG_WHITE -> {
                    addCoordinateToMovementBitboard(color, MovementNotation.KING,Movement(
                        CASTLING_LONG_WHITE,coordinate,2,0),bbTargetsMap)
                }
                CASTLING_SHORT_BLACK -> {
                    addCoordinateToMovementBitboard(color, MovementNotation.KING,Movement(
                        CASTLING_SHORT_BLACK,coordinate,6,7),bbTargetsMap)
                }
                CASTLING_LONG_BLACK -> {
                    addCoordinateToMovementBitboard(color, MovementNotation.KING, Movement(
                        CASTLING_LONG_BLACK,coordinate,2,7),bbTargetsMap)
                }
            }
        }
        return bbTargetsMap
    }


    /**
     * return a list of possible movements of the figure at (sourceRank,sourceFile)
     * */
    fun getTargetMovementsAsMovementList(color: String, coordinate : Coordinate) : List<Movement> {
        val pos = ("black" == color).toInt()
        val bbFigure = generate64BPositionFromCoordinate(coordinate)
        if(bbComposite and bbFigure == 0uL)return emptyList()
        val movementList = mutableListOf<Movement>()
        val name = getPieceName(pos, bbFigure)
        if(name in figureMap.keys){
            val movementString = (figureMap[name] as FigureParser.Figure).movementParlett
            val movementNotationList = getMovementNotation(movementString)
            var bbTargets = generateMovements(color,coordinate,movementNotationList)
            bbTargets = deleteIllegalMoves(name,color,bbFigure,bbTargets.toMutableMap(),movementNotationList)
            bbTargets = genSpecialMoves(name,color,coordinate,bbTargets,true)
            //transform bbTargets into movementList
            for(movementNotation in bbTargets.keys){
                if(bbTargets[movementNotation] == 0uL)continue
                val targetList = generateCoordinatesFrom64BPosition(bbTargets[movementNotation]!!)
                for(target in targetList){
                    val move = Movement(movementNotation,coordinate.rank,coordinate.file,target.rank,target.file)
                    if(move !in movementList){
                        movementList.add(move)
                    }
                }
            }
        }
        return movementList
    }

    /**
     * does the movement fullfil condition in Movement.MovementNotation.Condition?
     */
    fun deleteIllegalMoves(figureName : String,
                           color: String,
                           bbFigure: ULong,
                           bbFigureNonRelativeTargets: MutableMap<MovementNotation,ULong>,
                           movementNotationList: List<MovementNotation> ) : MutableMap<MovementNotation,ULong>{
        val pos = getPosition(color)
        for(movementNotation in movementNotationList){
            if(movementNotation.conditions.contains("o")) {//May not be used for a capture (e.g. pawn's forward move)
                //thus bbFigureNonRelativeTargets and bbColorComposite must be bitwise different
                bbFigureNonRelativeTargets[movementNotation] =
                    bbFigureNonRelativeTargets[movementNotation]!! and bbComposite.inv()
            }
            if(movementNotation.conditions.contains("c")) {//May only be made on a capture (e.g. pawn's diagonal capture)
                //thus bbFigureNonRelativeTargets and bbColorComposite of opponent must be bitwise the same
                bbFigureNonRelativeTargets[movementNotation] =
                    bbFigureNonRelativeTargets[movementNotation]!! and bbColorComposite[1-pos]
            }
            if(movementNotation.conditions.contains("i")) {//May only be made on the initial move (e.g. pawn's 2 moves forward)
                if(boardStateHistory.isNotEmpty()){
                    /* current position of the figure must match initial position (first entry of the movehistory)
                       or move history is empty (first movement) */
                    if(bbFigure and boardStateHistory[0][figureName]!![pos] == 0uL){ //1-pos is the index of player color (ai)
                        bbFigureNonRelativeTargets[movementNotation] = 0uL
                    }
                }
            }
            if(movementNotation.movetype == "g") {//moves by leaping over piece to an empty square (if leaped over enemy => capture)
                leapMoveCheck(figureName,color,bbFigure,bbFigureNonRelativeTargets,movementNotation)
            }
        }
        return bbFigureNonRelativeTargets
    }

    /** deletes the leap moves that are illegal */
    private fun leapMoveCheck(figureName : String,
                              color: String,
                              bbFigure: ULong,
                              bbFigureNonRelativeTargets: MutableMap<MovementNotation,ULong>,
                              movementNotation: MovementNotation){
        //TODO
    }

    /** generate a bitboard representing the target squares of the non relative movement for a piece */
    fun generateMovements(
        color: String,
        coordinate: Coordinate,
        movementNotationList: List<MovementNotation>
    ) : MutableMap<MovementNotation,ULong> {
        val bbMovementMap = mutableMapOf<MovementNotation,ULong>()
        for (movementNotation in movementNotationList) {
            bbMovementMap[movementNotation] = 0uL
            if (movementNotation.movetype == "~" || movementNotation.movetype == "^" || movementNotation.movetype == "g") { //leaper
                generateLeaperMovements(bbMovementMap,coordinate,color,movementNotation)
            } else { //rider
                generateRiderMovements(bbMovementMap,coordinate,color,movementNotation)
            }
        }
        return bbMovementMap
    }


    /** generate a list of movement matching the movementNotation (Leaper) */
    private fun generateLeaperMovements(bbMovementMap: MutableMap<MovementNotation, ULong>,
                                coordinate : Coordinate,
                                color: String,
                                movementNotation : MovementNotation){
        if (movementNotation.grouping == "/" && movementNotation.distances.size == 2) { //for now leaper movement consist of 2 subsequent movements
            //leaper-movements always have 8 sub-moves:
            //(2: increase/decrease)*(2: value1/value2)*(2: on File / on Rank) = 8 permutations
            val distance1 = movementNotation.distances[0]
            val distance2 = movementNotation.distances[1]
            if (distance1.matches("[0-9]".toRegex()) && distance2.matches("[0-9]".toRegex())) {
                val dis1 = distance1.toInt()
                val dis2 = distance2.toInt()
                /** generate all (8) leaper movements matching movementNotation (Leaper) */
                generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,dis1, dis2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,-dis1, dis2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,dis1, -dis2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,-dis1, -dis2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,dis2, dis1)
                generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,-dis2, dis1)
                generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,dis2, -dis1)
                generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,-dis2, -dis1)
            } else {
                if (distance1 == "x" && distance2 == "x") {//only in pairs (x,x): any distance in the given direction equal to its twin or zero
                    for (distance in -7..7) {
                        //orthogonal
                        generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,0, distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,distance, 0)
                        //diagonal
                        generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,distance, distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,-distance, distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,distance, -distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,coordinate,-distance, -distance)
                    }
                }
            }
        }
    }

    /** add a leaper movement to targetSquares defined by an delta (fileDif,rankDif) */
    private fun generateLeaperMovement(
        color: String,
        bbMovementMap : MutableMap<MovementNotation,ULong>,
        movementNotation: MovementNotation,
        position : Coordinate,
        rankDif: Int,
        fileDif: Int
    ) {
        val newRank = position.rank + rankDif
        val newFile = position.file + fileDif
        if (newFile in 0..7 && newRank in 0..7) {
            val newPosition = Coordinate(newRank,newFile)
            val bbNewTarget = generate64BPositionFromCoordinate(newPosition)
            val pos = (color =="black").toInt() //opponent color
            if(bbNewTarget and bbColorComposite[pos].inv() == bbNewTarget){//check if there is no figure of self color at targets
                addCoordinateToMovementBitboard(
                    color,
                    movementNotation,
                    Movement(position,newPosition),
                    bbMovementMap
                )
            }
        }
    }


    /** generate a list of rider-movements matching the movementNotation (rider) */
    private fun generateRiderMovements(bbMovementMap: MutableMap<MovementNotation, ULong>,
                               coordinate: Coordinate,
                               color: String,
                               movementNotation : MovementNotation) {
        if (movementNotation.distances.isNotEmpty()) {
            if(arrayOf(">","<","<>","=","<=",">=","+","*").contains(movementNotation.direction)){
                generateOrthogonalMovement(bbMovementMap, coordinate, color, movementNotation)
            }
            if(arrayOf("X","X<","X>","*").contains(movementNotation.direction)){
                generateDiagonalOrthogonalMovement(bbMovementMap, coordinate, color, movementNotation)
            }
        }
    }

    /** generate all orthogonal movements: horizontal (WEST,EAST movements) and vertical (NORTH,SOUTH)*/
    private fun generateOrthogonalMovement(bbMovementMap: MutableMap<MovementNotation, ULong>,
                                   coordinate: Coordinate,
                                   color: String,
                                   movementNotation : MovementNotation)  {
        var distance = UNLIMITED_DISTANCE
        val posOwnColor = getPosition(color)
        if(movementNotation.distances[0].matches("[1-9]+".toRegex()))distance = movementNotation.distances[0].toInt()
        //forward(>) and backwards(<) are color-dependent because they are depending on direction of the figures
        //color-independent movements
        if(movementNotation.direction.contains("=") || movementNotation.direction == "+" || movementNotation.direction == "*") {
            generateWestMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
            generateEastMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
        }
        if(movementNotation.direction == "+" || movementNotation.direction == "*" || movementNotation.direction == "<>"
            || movementNotation.direction.contains(">") || movementNotation.direction.contains("<")){
            //color-dependent movements
            if(movementNotation.direction.contains(">") && !movementNotation.direction.contains("<")){
                //forwards but not backwards
                if(color == "black"){
                    generateSouthMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
                } else {
                    generateNorthMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
                }
            } else if(movementNotation.direction.contains("<") && !movementNotation.direction.contains(">")){
                //backwards but not forwards
                if(color == "black"){
                    generateNorthMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
                } else {
                    generateSouthMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
                }
            } else { //color-independent movements
                generateNorthMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
                generateSouthMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
            }

        }
    }

    /** forward: increase file */
    private fun generateNorthMovement(posOwnColor: Int,
                              bbMovementMap: MutableMap<MovementNotation, ULong>,
                              coordinate: Coordinate,
                              movementNotation: MovementNotation,
                              distance : Int) {
        generateDiagonalOrthogonalMovement(posOwnColor,bbMovementMap,coordinate,movementNotation,distance,0,+1)
    }

    /** backward: decrease file */
    private fun generateSouthMovement(posOwnColor: Int,
                              bbMovementMap: MutableMap<MovementNotation, ULong>,
                              coordinate: Coordinate,
                              movementNotation: MovementNotation,
                              distance : Int) {
        generateDiagonalOrthogonalMovement(posOwnColor,bbMovementMap,coordinate,movementNotation,distance,0,-1)
    }

    /** east: increase rank */
    private fun generateEastMovement(posOwnColor: Int,
                             bbMovementMap: MutableMap<MovementNotation, ULong>,
                             coordinate: Coordinate,
                             movementNotation: MovementNotation,
                             distance : Int) {
        generateDiagonalOrthogonalMovement(posOwnColor,bbMovementMap,coordinate,movementNotation,distance,+1,0)
    }

    /** left: decrease rank */
    private fun generateWestMovement(posOwnColor: Int,
                             bbMovementMap: MutableMap<MovementNotation, ULong>,
                             coordinate: Coordinate,
                             movementNotation: MovementNotation,
                             distance : Int) {
        generateDiagonalOrthogonalMovement(posOwnColor,bbMovementMap,coordinate,movementNotation,distance,-1,0)
    }

    /** generate all diagonal rider movements */
    private fun generateDiagonalOrthogonalMovement(bbMovementMap: MutableMap<MovementNotation, ULong>,
                                                   coordinate: Coordinate,
                                                   color: String,
                                                   movementNotation : MovementNotation)  {
        var distance = UNLIMITED_DISTANCE
        val posOwnColor = getPosition(color)
        if(movementNotation.distances[0].matches("[0-9]".toRegex())){
            distance = movementNotation.distances[0].toInt()
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X>"){
            if (color == "black" && movementNotation.direction == "X>"){
                generateSouthEastDiagonalMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
                generateSouthWestDiagonalMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
            } else {
                generateNorthEastDiagonalMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
                generateNorthWestDiagonalMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
            }
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X<") {
            if (color == "black" && movementNotation.direction == "X>"){
                generateNorthEastDiagonalMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
                generateNorthWestDiagonalMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
            } else {
                generateSouthEastDiagonalMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
                generateSouthWestDiagonalMovement(posOwnColor,bbMovementMap, coordinate, movementNotation, distance)
            }
        }
    }

    /** NorthWestDiagonalMovement: left,forward: increase file, decrease rank*/
    private fun generateNorthWestDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<MovementNotation, ULong>,
                                          coordinate: Coordinate,
                                          movementNotation: MovementNotation,
                                          distance : Int) {
        generateDiagonalOrthogonalMovement(posOwnColor,bbMovementMap,coordinate,movementNotation,distance,-1,+1)
    }

    /** NorthEastDiagonalMovement: right,forward: increase file, increase rank*/
    private fun generateNorthEastDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<MovementNotation, ULong>,
                                          coordinate: Coordinate,
                                          movementNotation: MovementNotation,
                                          distance : Int) {
        generateDiagonalOrthogonalMovement(posOwnColor,bbMovementMap,coordinate,movementNotation,distance,+1,+1)
    }

    /** SouthEastDiagonalMovement: right,backward: decrease file, increase rank*/
    private fun generateSouthEastDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<MovementNotation, ULong>,
                                          coordinate: Coordinate,
                                          movementNotation: MovementNotation,
                                          distance : Int) {
        generateDiagonalOrthogonalMovement(posOwnColor,bbMovementMap,coordinate,movementNotation,distance,+1,-1)
    }

    /** SouthWestDiagonalMovement: left,backward: decrease file, decrease rank*/
    private fun generateSouthWestDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<MovementNotation, ULong>,
                                          coordinate: Coordinate,
                                          movementNotation: MovementNotation,
                                          distance : Int) {
        generateDiagonalOrthogonalMovement(posOwnColor,bbMovementMap,coordinate,movementNotation,distance,-1,-1)
    }

    /** Generates diagonal / orthogonal movement by
     *  adding signRank to rank and signFile to file while the new coordinate(newRank,newFile) is in board
     *  and no opponent figure is in the way. */
    private fun generateDiagonalOrthogonalMovement(posOwnColor: Int,
                                                   bbMovementMap: MutableMap<MovementNotation, ULong>,
                                                   coordinate: Coordinate,
                                                   movementNotation: MovementNotation,
                                                   distance : Int,
                                                   signRank : Int, signFile : Int) {
        var difRank = signRank; var difFile = signFile
        while(coordinate.file+difFile in 0..7 && coordinate.rank+difRank in 0..7) {// ... inside board (between 0 and 7)
            val newFile = coordinate.file+difFile
            val newRank = coordinate.rank+difRank
            val bbNewTarget = generate64BPositionFromCoordinate(Coordinate(newRank,newFile))
            if(figureOfOwnColor(posOwnColor, bbNewTarget))break
            if(distance == UNLIMITED_DISTANCE){
                addCoordinateToMovementBitboard(movementNotation,coordinate,newRank,newFile,bbMovementMap)
            } else {
                if(difFile*difRank == 0 && abs(difFile+difRank) == distance //orthogonal
                || difFile*difRank != 0 && abs(difFile) == distance){ //diagonal
                    addCoordinateToMovementBitboard(movementNotation,coordinate,newRank,newFile,bbMovementMap)
                }
            }
            if(figureOfOpponent(posOwnColor,bbNewTarget))break
            difRank+=signRank
            difFile+=signFile
        }
    }

    fun figureOfOwnColor(posOwnColor: Int, bbNewTarget:ULong) : Boolean {
        if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){
            return true
        }
        return false
    }

    fun figureOfOpponent(
        posOwnColor: Int,
        bbNewTarget: ULong
    ) : Boolean {
        if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
            return true
        }
        return false
    }

    fun addCoordinateToMovementBitboard(movementNotation: MovementNotation, 
                                        coordinate: Coordinate,
                                        targetRank: Int, targetFile: Int,
                                        bbMovementMap: MutableMap<MovementNotation, ULong>){
        addCoordinateToMovementBitboard(
            "",
            movementNotation,
            Movement(coordinate,targetRank,targetFile),
            bbMovementMap
        )
    }



    private fun addCoordinateToMovementBitboard(
        color: String,
        movementNotation: MovementNotation,
        movement : Movement,
        bbMovementMap: MutableMap<MovementNotation, ULong>
    ) {
        //TODO delete fullfills condition
            //check if move is legal
            //if(fullfillsCondition(color,sourceFile, sourceRank, targetFile, targetRank, movementNotation)){
            val a = bbMovementMap.keys.toTypedArray()[0].hashCode()
            val b = movementNotation.hashCode()
            val targetBB =  generate64BPositionFromCoordinate(movement.getTargetCoordinate())
            if(bbMovementMap.keys.contains(movementNotation)){
                bbMovementMap[movementNotation] = bbMovementMap[movementNotation]!! or targetBB
            } else {
                bbMovementMap[movementNotation] = targetBB
            }
            //}
    }

    /**
     * does the movement fullfil condition in Movement.MovementNotation.Condition?
     */
    fun fullfillsCondition(color: String,
                           movement : Movement,
                           movementNotation: MovementNotation) : Boolean {
        var returnValue = true
        if(movementNotation.conditions.contains("o")) {//May not be used for a capture (e.g. pawn's forward move)
           /* returnValue = returnValue && !(pieces[movement.sourceFile][movement.sourceRank].color != pieces[movement.targetFile][movement.targetRank].color
                    && pieces[movement.sourceFile][movement.sourceRank].color.isNotEmpty()
                    && pieces[movement.targetFile][movement.targetRank].color.isNotEmpty())*/
        }
        if(movementNotation.conditions.contains("c")) {//May only be made on a capture (e.g. pawn's diagonal capture)
            /*returnValue = returnValue && (pieces[movement.sourceFile][movement.sourceRank].color != pieces[movement.targetFile][movement.targetRank].color
                    && pieces[movement.sourceFile][movement.sourceRank].color.isNotEmpty()
                    && pieces[movement.targetFile][movement.targetRank].color.isNotEmpty())*/
            //en passante
            //if(!returnValue)returnValue = specialMoveCheckEnpassante(movement) != null
        }
        if(movementNotation.conditions.contains("i")) {//May only be made on the initial move (e.g. pawn's 2 moves forward)
            //returnValue = returnValue && (pieces[movement.sourceFile][movement.sourceRank].moveCounter == 0)
        }
        if(movementNotation.movetype == "g") {//moves by leaping over piece to an empty square (if leaped over enemy => capture)
           /* val signFile = sign((movement.targetFile - movement.sourceFile).toDouble()).toInt()
            val signRank = sign((movement.targetRank - movement.sourceRank).toDouble()).toInt()
            returnValue = pieces[movement.targetFile][movement.targetRank].color.isEmpty()
                    && pieces[movement.targetFile-signFile][movement.targetRank-signRank].color.isNotEmpty()
                    && (Math.abs(movement.targetFile-movement.sourceFile) > 1 || Math.abs(movement.targetRank-movement.sourceRank) > 1)*/
        }
        return returnValue
    }

    override fun toString(): String {
        val fList = mutableListOf<Coordinate>()
        for(key in bbFigures.keys){
            fList.addAll(generateCoordinatesFrom64BPosition(bbFigures[key]?.get(0) ?: 0uL))
            fList.addAll(generateCoordinatesFrom64BPosition(bbFigures[key]?.get(1) ?: 0uL))
        }
        val str = StringBuilder("")
        var cnt = 0
        for(file in 7 downTo 0){
             str.append(file.toString()+" | ")
             for(rank in 0 .. 7){
                 var empty = true
                 for(key in bbFigures.keys){
                    if(key.isEmpty())continue
                    val num = 2.0.pow(file * 8 + rank).toULong()
                    val white = bbFigures[key]?.get(0) ?: 0uL
                    val black = bbFigures[key]?.get(1) ?: 0uL
                    if(white and num == num){
                        if(key.isNotEmpty()){
                            if(key == "knight")str.append("N")
                            else str.append(key[0].toUpperCase())
                        }
                        str.append(" | ")
                        ++cnt
                        empty = false
                    }
                    else if(black and num == num){
                        if(key.isNotEmpty()){
                            if(key == "knight")str.append("n")
                            else str.append(key[0])
                        }
                        str.append(" | ")
                        ++cnt
                        empty = false
                    }
                }
                if(empty){
                    str.append("  | ")
                }
            }
            str.append("\n--+---+---+---+---+---+---+---+---+\n")
        }
        str.append("  | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |\n")
        str.append("  +---+---+---+---+---+---+---+---+\n")
        return str.toString()
    }

    /** calculate the winner (if one exists yet)*/
    private fun checkForWinner() {
       if(bbFigures["king"]?.get(0) == 0uL){
           gameWinner = "white"
           gameFinished = true
       }
       if(bbFigures["king"]?.get(0) == 0uL){
           gameWinner = "black"
           gameFinished = true
       }
    }

    private fun checkForPromotion() {
        for (rank in 0..7) {
            val firstFile = 2.0.pow((0 * 8 + rank).toDouble()).toULong()
            val lastFile = 2.0.pow((7 * 8 + rank).toDouble()).toULong()
            if((bbFigures["pawn"]?.get(0)!! and lastFile) == lastFile){
                promotionCoordinate = Coordinate(rank,7)
            }
            if((bbFigures["pawn"]?.get(1)!! and firstFile) == firstFile){
                promotionCoordinate = Coordinate(rank,0)
            }
        }
    }

    /** calculate all points of black player */
    fun pointsBlack(): Int {
        var points = 0
        for(name in bbFigures.keys){
            points += figureMap?.get(name)?.value?.times(countSetBits(bbFigures[name]?.get(1)!!)) ?: 0
        }
        return points
    }

    /** calculate all points of white player */
    fun pointsWhite(): Int {
        var points = 0
        for(name in bbFigures.keys){
            points += figureMap?.get(name)?.value?.times(countSetBits(bbFigures[name]?.get(0)!!)) ?: 0
        }
        return points
    }

    /** recursive function to count set bits */
    private fun countSetBits(n: ULong): Int {
        // base case
        return if (n == 0uL) 0
            else 1 + countSetBits(n and n - 1uL)
    }

    /** switchMoveColor from white to black and vice versa */
    fun switchMoveColor(){
        moveColor = oppositeColor(moveColor)
    }

    companion object {
        val castlingMoves = arrayOf(
            MovementNotation.CASTLING_SHORT_WHITE,
            MovementNotation.CASTLING_SHORT_BLACK,
            MovementNotation.CASTLING_LONG_WHITE,
            MovementNotation.CASTLING_LONG_WHITE,
        )
        val enpassanteSquares = arrayOf(
            Coordinate(0,2), Coordinate(1,2), Coordinate(2,2), Coordinate(3,2),
            Coordinate(4,2), Coordinate(5,2), Coordinate(6,2), Coordinate(7,2),
            Coordinate(0,5), Coordinate(1,5), Coordinate(2,5), Coordinate(3,5),
            Coordinate(4,5), Coordinate(5,5), Coordinate(6,5), Coordinate(7,5),
        )


        const val UNLIMITED_DISTANCE = Int.MAX_VALUE
        val colors = arrayOf("white","black")
        fun randomColor() : String {
            return colors[(random()*2).toInt()]
        }

        fun getPosition(color: String) : Int {
            return (color == "black").toInt()
        }

        fun moveMapToComposite(moveMap : Map<Coordinate,ULong>) : ULong {
            var bbMoves = 0uL
            for(move in moveMap.keys){
                bbMoves = bbMoves or moveMap[move]!!
            }
            return bbMoves
        }

        fun moveBitboardToMovementList(sourceCoordinate: Coordinate, bbMove: ULong) : List<Movement> {
            val targetMovements = mutableListOf<Movement>()
            for(i in 0..64){
                val targetRank = i % 8
                val targetFile = i / 8
                val bbCandidate = generate64BPositionFromCoordinate(Coordinate(targetRank,targetFile))
                if(bbCandidate and bbMove == bbCandidate){
                    targetMovements.add(Movement(sourceCoordinate.rank,sourceCoordinate.file,targetRank,targetFile))
                }
            }
            return targetMovements
        }

        fun oppositeColor(color : String) : String{
            when(color){
                "white" -> return "black"
                "black" -> return "white"
            }
            return ""
        }

        class Coordinate(val rank: Int, val file: Int){
            override fun equals(other: Any?): Boolean {
                if(other is Coordinate){
                    return (file == other.file) && (rank == other.rank)
                }
                return super.equals(other)
            }
            fun inRange() : Boolean {
                return rank in 0..7 && file in 0..7
            }
            fun getSign() : Int{
                return sign((rank - file).toDouble()).toInt()
            }
            fun getDistance(): Int {
                return abs(rank - file).toInt()
            }
            fun newCoordinateFromFileOffset(fileOffset : Int): Coordinate {
                return Coordinate(rank, file + fileOffset)
            }
            fun newCoordinateFromRankOffset(rankOffset : Int): Coordinate {
                return Coordinate(rank + rankOffset, file)
            }
        }

        val bbCastlingRoomShortWhite = horizontalLineToBitboard(Movement(4,0,6,0))
        val bbCastlingRoomLongWhite = horizontalLineToBitboard(Movement(4,0,2,0))
        val bbCastlingRoomShortBlack = horizontalLineToBitboard(Movement(4,7,6,7))
        val bbCastlingRoomLongBlack = horizontalLineToBitboard(Movement(4,7,2,7))
        val bbKingOriginalPosition = arrayOf(generate64BPositionFromCoordinate(Coordinate(4,0)),
                                     generate64BPositionFromCoordinate(Coordinate(4,7)))


        /** generates bitboard that contains all squares from sourceSquare to targetSquare (including them) */
        private fun horizontalLineToBitboard(movement: Movement) : ULong {
            if(movement.sourceFile != movement.targetFile){
                return 0uL
            } else {
                val signRank = movement.getSignRank()
                val distance = movement.getRankDif()
                var result = generate64BPositionFromCoordinate(movement.getSourceCoordinate())
                for(i in 1..distance){
                    if(signRank > 0){
                        result = result or (result shl 1)
                    } else {
                        result = result or (result shr 1)
                    }
                }
                return result
            }
        }

        private val movingStringTomovementNotationsMap = mutableMapOf<String, List<MovementNotation>>()



        fun getMovementNotation(movingPatternString : String) : List<MovementNotation>{
            val movementNotations = mutableListOf<MovementNotation>()
            if (movingStringTomovementNotationsMap.containsKey(movingPatternString)) {
                movementNotations.addAll(movingStringTomovementNotationsMap[movingPatternString]!!)
            } else {
                movementNotations.addAll(
                    MovementNotation.parseMovementString(
                        movingPatternString
                    )
                )
                movingStringTomovementNotationsMap[movingPatternString] = movementNotations
            }
            return movementNotations
        }

        fun generate64BPositionFromCoordinate(coordinate: Coordinate) : ULong {
            var pos : ULong = 1uL shl coordinate.file*8 // pos = 2 ^ (file*8)
            pos = pos shl coordinate.rank       // pos = pos * 2 ^ rank
            return pos         // pos = 2 ^ (file*8) * 2 ^ rank
        }

        fun add64BPositionFromCoordinates(_64B: ULong, coordinate: Coordinate) : ULong {
            return _64B or generate64BPositionFromCoordinate(coordinate)
        }

        fun generate64BPositionFromCoordinateList(list: List<Coordinate>) : ULong{
            var result = 0uL
            for(i in list.indices){
                result = if(i==0){
                    generate64BPositionFromCoordinate(list[i])
                } else {
                    add64BPositionFromCoordinates(result,list[i])
                }
            }
            return result
        }

        fun generateCoordinatesFrom64BPosition(long: ULong) : MutableList<Coordinate> {
            val coordinateList = mutableListOf<Coordinate>()
            for(i in 0..63){
                val pos : ULong = 2.0.pow(i.toDouble()).toULong()
                if(long and pos == pos){
                    coordinateList.add(transformNumberToCoordinate(i))
                }
            }
            return coordinateList
        }

        private fun transformNumberToCoordinate(index: Int) : Coordinate {
            return Coordinate((index % 8), (index / 8))
        }

        fun parseFigureMapFromFile() : Map<String, FigureParser.Figure> {
            try {
                val absPath = "C:\\Users\\johan\\OneDrive\\Documents\\GitHub\\Fairy-Chess\\app\\src\\main\\res\\raw\\figures.json"
                val initialFile = File(absPath)
                val inputStream: InputStream = FileInputStream(initialFile)
                return FigureParser.parseFigureMapFromJSONString(
                    ChessFormationParser.convertStreamToString(
                        inputStream
                    )
                )
            } catch (e: Exception){
                println(e.message.toString())
            }
            return mapOf()
        }

        private fun parseChessFormation(mode: String) : Array<Array<String>> {
            try {
                val absPath =
                    "C:\\Users\\johan\\OneDrive\\Documents\\GitHub\\Fairy-Chess\\app\\src\\main\\res\\raw\\$mode.chessformation"
                val initialFile = File(absPath)
                val inputStream: InputStream = FileInputStream(initialFile)
                return ChessFormationParser.rotate2DArray(
                    ChessFormationParser.parseChessFormationString(
                        ChessFormationParser.convertStreamToString(
                            inputStream
                        )
                    )
                )
            } catch (e: Exception){
                println(e.message.toString())
            }
            return arrayOf()
        }
    }




    fun getPieceColor(rank: Int, file: Int) : String{
        return when (val bbFigure = generate64BPositionFromCoordinate(Coordinate(rank,file))) {
            bbColorComposite[0] and bbFigure -> "white"
            bbColorComposite[1] and bbFigure -> "black"
            else -> ""
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bitboard

        if (chessFormationArray != null) {
            if (other.chessFormationArray == null) return false
            if (!chessFormationArray.contentDeepEquals(other.chessFormationArray)) return false
        } else if (other.chessFormationArray != null) return false
        if (figureMap != other.figureMap) return false
        if (!colors.contentEquals(other.colors)) return false
        if (gameFinished != other.gameFinished) return false
        if (bbMovedCaptured != other.bbMovedCaptured) return false
        if (bbComposite != other.bbComposite) return false
        if (!bbColorComposite.contentEquals(other.bbColorComposite)) return false
        if (gameWinner != other.gameWinner) return false
        if (playerWithDrawOpportunity != other.playerWithDrawOpportunity) return false
        if (promotionCoordinate != other.promotionCoordinate) return false
        if (moveColor != other.moveColor) return false
        if (boardStateHistory != other.boardStateHistory) return false
        if (movedCapturedHistory != other.movedCapturedHistory) return false
        if (blackCapturedPieces != other.blackCapturedPieces) return false
        if (whiteCapturedPieces != other.whiteCapturedPieces) return false
        if (bbFigures.keys != other.bbFigures.keys) return false
        for(key in bbFigures.keys){
            if(!bbFigures[key].contentEquals(other.bbFigures[key])) {
                return false
            }
        }
        return true
    }



    override fun hashCode(): Int {
        var result = chessFormationArray?.contentDeepHashCode() ?: 0
        result = 31 * result + figureMap.hashCode()
        result = 31 * result + colors.contentHashCode()
        result = 31 * result + bbFigures.hashCode()
        result = 31 * result + gameFinished.hashCode()
        result = 31 * result + bbMovedCaptured.hashCode()
        result = 31 * result + bbComposite.hashCode()
        result = 31 * result + bbColorComposite.contentHashCode()
        result = 31 * result + gameWinner.hashCode()
        result = 31 * result + playerWithDrawOpportunity.hashCode()
        result = 31 * result + (promotionCoordinate?.hashCode() ?: 0)
        result = 31 * result + moveColor.hashCode()
        result = 31 * result + boardStateHistory.hashCode()
        result = 31 * result + movedCapturedHistory.hashCode()
        result = 31 * result + blackCapturedPieces.hashCode()
        result = 31 * result + whiteCapturedPieces.hashCode()
        return result
    }
}

fun Boolean.toInt(): Int {
   return if (this) 1 else 0
}

fun bitboardToString(bitboard: ULong) : String{
    val str = StringBuilder("")
    var cnt = 0
    for(file in 7 downTo 0){
        str.append(file.toString()+" | ")
        for(rank in 0..7){
            val num = 1uL shl rank shl (8*file)
            if(bitboard and num == num){
                str.append("X")
                str.append(" | ")
                ++cnt
            } else {
                str.append("  | ")
            }
        }
        str.append("\n--+---+---+---+---+---+---+---+---+\n")
    }
    str.append("  | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 |\n")
    return str.toString()
}