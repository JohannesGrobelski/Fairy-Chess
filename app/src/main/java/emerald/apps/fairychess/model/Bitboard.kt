@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package emerald.apps.fairychess.model

import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import emerald.apps.fairychess.view.ChessActivity
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.Math.random
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class Bitboard(
    private val chessFormationArray: Array<Array<String>>?,
    private val figureMap: Map<String, FigureParser.Figure>
) {

    /* TODO: implementation
        1. fullfillsCondition einbinden
        2. enpassante
        3. castling
        4. in chessgame einbinden und so movegeneration prüfen
        5. draw rules implen
     */

    /* TODO: performance improvements
    - HashTable of calculated moves for each tuple (color,figureName,file,rank)

     */
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
    val moveHistory = mutableListOf<Map<String, Array<ULong>>>() //move history, for each move map (figureName -> Bitboard)
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
    }

    fun promotePawn(coordinate: Coordinate, name : String){
        if(promotionCoordinate?.equals(coordinate) == true){
            if(bbFigures.containsKey(name)){
                val bbCoordinate = generate64BPositionFromCoordinates(coordinate)
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
        val bbFigure = generate64BPositionFromCoordinates(coordinate)
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
            bbFigures[key] = bitboard.bbFigures[key]!!
        }
        bbComposite = bitboard.bbComposite
        bbColorComposite = bitboard.bbColorComposite
        blackCapturedPieces.clear(); blackCapturedPieces.addAll(bitboard.blackCapturedPieces)
        whiteCapturedPieces.clear(); whiteCapturedPieces.addAll(bitboard.whiteCapturedPieces)
        moveColor = bitboard.moveColor
        moveHistory.clear(); moveHistory.addAll(bitboard.moveHistory)
    }

    fun clone() : Bitboard{
        val newBitboard = Bitboard(chessFormationArray,figureMap)
        newBitboard.bbFigures.clear()
        for(key in bbFigures.keys){
            newBitboard.bbFigures[key] = bbFigures[key]!! + 0uL
        }
        newBitboard.bbComposite = bbComposite + 0uL
        newBitboard.bbColorComposite = bbColorComposite + 0uL
        newBitboard.blackCapturedPieces.addAll(blackCapturedPieces)
        newBitboard.whiteCapturedPieces.addAll(whiteCapturedPieces)
        newBitboard.moveColor = moveColor
        newBitboard.moveHistory.addAll(moveHistory)
        return newBitboard
    }

    fun checkMoveAndMove(color:String, movement: Movement) : String{
        if(moveColor != color)return "wrong player!"
        val name = getPieceName(movement.sourceRank,movement.sourceFile)
        return preMoveCheck(name, color, movement)
    }

    fun preMoveCheck(name: String, color : String, movement: Movement) : String{
        if(moveColor != color)return "wrong color"
        if(movement.sourceFile == movement.targetFile && movement.sourceRank == movement.targetRank)return "same square"
        val pos = getPosition(color)
        val coordinate = Coordinate(movement.sourceRank,movement.sourceFile)
        val bbSource = generate64BPositionFromCoordinates(coordinate)
        if(bbColorComposite[pos] and bbSource != bbSource)return "wrong figure"
        val legalMovements = getTargetMovementsAsMovementList(color,coordinate)
        for(legalMove in legalMovements){
            if(legalMove.sourceRank == movement.sourceRank
            && legalMove.sourceFile == movement.sourceFile
            && legalMove.targetFile == movement.targetFile
            && legalMove.targetRank == movement.targetRank){
                return checkMoveAndMove(name, color, movement)
            }
        }
        return "not a legal move"
    }

    /** @param color of the piece set
     *  @param name of the piece set
     * moves figure from coordinate (sourceFile,sourceRow) to coordinate (targetFile,targetRow)
     * does not check if move is legal */
    fun checkMoveAndMove(name: String, color : String, movement: Movement) : String{
        val pos = getPosition(color)
        if(bbFigures.containsKey(name)){
            checkForAndMakeEnpassanteMove(name, color, pos, movement)

            var bbFigureColor = bbFigures[name]!![pos]
            val bbSource = generate64BPositionFromCoordinates(movement.getSourceCoordinate())
            val bbTarget = generate64BPositionFromCoordinates(movement.getTargetCoordinate())

            //no figure of opposite of color can stand at (targetRank,targetFile) therefore set bit to 0 on this position
            val targetName = getPieceName(movement.targetRank,movement.targetFile)
            if(targetName.isNotEmpty()){
                var bbFigureOppositeColor = bbFigures[targetName]!![1-pos]
                bbFigureOppositeColor = bbFigureOppositeColor and bbTarget.inv()
                bbFigures[targetName]?.set(1-pos,bbFigureOppositeColor)
            }

            //calculate change vector with set bits on old and new position
            val changeBB = bbSource or bbTarget

            if(movement is PromotionMovement){
                val bbPawns = bbFigures[name]!![pos] and bbSource.inv()
                bbFigures["pawn"]!![pos] = bbPawns
                val bbPromotion = bbFigures[movement.promotion]!![pos] or bbTarget
                bbFigures[movement.promotion]!![pos] = bbPromotion
            } else {
                //xor targetBB with changeBB to change bit of old position from 1 to 0 and bit from new position from 0 to 1
                //and thus move the piece
                bbFigureColor = bbFigureColor xor changeBB
                bbFigures[name]?.set(pos,bbFigureColor)
            }

            //write move into bbComposite
            //one figure must stand at (sourceRank,sourceFile) therefore set bit to 1 on this position
            bbComposite = bbComposite or bbTarget
            //no figure can stand at (targetRank,targetFile) therefore set bit to 0 on this position
            bbComposite = bbComposite and bbSource.inv()

            //write move into bbColorComposite for move color like above
            bbColorComposite[pos] = bbColorComposite[pos] xor changeBB
            //no figure of opposite of color can stand at (targetRank,targetFile) therefore set bit to 0 on this position
            bbColorComposite[1-pos] = bbColorComposite[1-pos] and bbTarget.inv()


            bbMovedCaptured = bbMovedCaptured or bbSource or bbTarget
            addEntryToHistory(bbFigures)
            //add current position (as map bbFigures) to history
            if(name == "king" && movement.getRankDif() == 2){
                makeCastlingMove(color,movement)
            } else {
                checkForPromotion()
                switchMoveColor()
            }
            return ""
        } else {
            return "wrong square!"
        }
    }

    fun checkForAndMakeEnpassanteMove(name: String, color : String, pos : Int, movement : Movement){
        //check for special case enpassante
        if(name == "pawn" && movement.getRankDif() == 1
            && bbFigures["pawn"]!![1-pos] and generate64BPositionFromCoordinates(movement.getTargetCoordinate()) == 0uL){
            //pawn moved diagonaly, but there is no target on target square
            val fileOffset = -((-1.0).pow(pos.toDouble())).toInt() //-1 for white, +1 for black
            val bbTarget = generate64BPositionFromCoordinates(movement.getTargetCoordinate().newCoordinateFromFileOffset(fileOffset))
            if(bbFigures["pawn"]!![1-pos] and bbTarget == bbTarget){
            //therefore an enemy pawn must be above (black pawn) or under (white pawn) the target square
            //if so make enpassante move
                bbFigures["pawn"]!![1-pos] = bbFigures["pawn"]!![1-pos] and bbTarget.inv()
            }
            return
        }
    }

    fun addEntryToHistory(bbFig : Map<String, Array<ULong>>){
        val newBB = mutableMapOf<String,Array<ULong>>()
        for(key in bbFig.keys){
            newBB[key] = bbFig[key]!!.copyOf()
        }
        moveHistory.add(newBB.toMap())
    }

    private fun makeCastlingMove(color: String, movement : Movement){
        if(color ==  "white"){
            if(movement.targetRank == 2){ //large white castling move
                checkMoveAndMove("rook",color, Movement(0,0,3,0))
            } else { //small white castling move
                checkMoveAndMove("rook",color,Movement(7,0,5,0))
            }
        } else {
            if(movement.targetRank == 2){ //large black castling move
                checkMoveAndMove("rook",color,Movement(0,7,3,7))
            } else { //small black castling move
                checkMoveAndMove("rook",color,Movement(7,7,5,7))
            }
        }
    }

    /** return a map of all posible moves for player of @color as bitmap*/
    fun getAllPossibleMoves(color : String, generatedCastlingMoves : Boolean) : Map<Coordinate,ULong> {
        val allPossibleMoves = mutableMapOf<Coordinate,ULong>()
        val pos = ("black" == color).toInt()
        for(rank in 0..7){
            for(file in 0..7){
                val bbFigure = generate64BPositionFromCoordinates(Coordinate(rank,file))
                if((bbColorComposite[pos] and bbFigure) == bbFigure){
                    val name = getNameOfFigure(pos, bbFigure)
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



    fun getNameOfFigure(pos: Int, bbFigure:ULong) : String{
        for(name in bbFigures.keys){
            if(bbFigures[name]!![pos] and bbFigure == bbFigure){
                return name
            }
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
        val bbFigure = generate64BPositionFromCoordinates(coordinate)
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

    private fun genSpecialMoves(name: String, color: String, coordinate : Coordinate, bbTargetsMap: MutableMap<MovementNotation,ULong>, generateCastlingMoves: Boolean)
        : MutableMap<MovementNotation,ULong> {
        if(name == "king" && generateCastlingMoves){//create castling moves, if possible
            return genCastlingMoves(color,coordinate,bbTargetsMap)
        } else if(name == "pawn"){
            return generateEnpassanteMove(color,coordinate,bbTargetsMap)
        }
        return bbTargetsMap
    }

    private fun generateEnpassanteMove(color : String, coordinate : Coordinate,bbTargetsMap: MutableMap<MovementNotation,ULong>)
        : MutableMap<MovementNotation,ULong> {
        val targetRankLeft = coordinate.rank - 1
        val targetRankRight = coordinate.rank + 1
        val pos = ("black" == color).toInt()
        for(targetRank in arrayOf(targetRankLeft,targetRankRight)){
            val bbtargetPawn = generate64BPositionFromCoordinates(Coordinate(targetRank,coordinate.file))
            if(bbtargetPawn and bbFigures["pawn"]!![1-pos] == bbtargetPawn){
                //there is an enemy pawn above/under target square
                val fileOffset = ((-1.0).pow(pos.toDouble())*2).toInt() //2 for white, -2 for black
                val bbtargetPawnInitialPosition = generate64BPositionFromCoordinates(Coordinate(targetRank,coordinate.file + fileOffset))
                if(moveHistory.size > 1
                    && moveHistory[moveHistory.lastIndex-1]["pawn"]!![1-pos] and bbtargetPawnInitialPosition == bbtargetPawnInitialPosition){
                    //last position of target pawn was 2 steps above/under the current position => target pawn moved 2 steps in the last move
                    val movement = Movement(coordinate.rank,coordinate.file,targetRank,coordinate.file + fileOffset/2)
                    addCoordinateToMovementBitboard(color, MovementNotation.PAWN_ENPASSANTE,movement,bbTargetsMap)
                }
            }
        }
        return bbTargetsMap
    }

    private fun genCastlingMoves(color: String, coordinate : Coordinate, bbTargetsMap: MutableMap<MovementNotation,ULong>)
        : MutableMap<MovementNotation,ULong> {
        val ownColorPos = ("black" == color).toInt()
        var bbEnemyMoves = moveMapToComposite(getAllPossibleMoves(colors[1-ownColorPos],false))

        //1. king has not moved
        if(bbFigures["king"]!![ownColorPos] and generate64BPositionFromCoordinates(coordinate) == bbFigures["king"]!![ownColorPos]){
            var bbRook: ULong
            var bbMoveRoom : ULong
            if(color == "white"){
                //small castling
                //2. check if king and space between rook and king are not under attack
                if(bbCastlingRoomSmallWhite and bbEnemyMoves.inv() == bbCastlingRoomSmallWhite){
                    //3. check if rook has not moved
                    bbRook = generate64BPositionFromCoordinates(Coordinate(7,0))
                    if(bbMovedCaptured and bbRook == 0uL){
                        //4. no pieces between rook and king
                        bbMoveRoom = bbCastlingRoomSmallWhite and bbRook.inv()
                        bbMoveRoom = bbMoveRoom and bbFigures["king"]!![ownColorPos].inv()
                        if(bbMoveRoom and bbComposite.inv() == bbMoveRoom){
                            addCoordinateToMovementBitboard(color, MovementNotation.KING,Movement(coordinate,6,0),bbTargetsMap)
                        }
                    }
                }
                //large castling
                //2. check if king and space between rook and king are not under attack
                if(bbCastlingRoomLargeWhite and bbEnemyMoves.inv() == bbCastlingRoomLargeWhite){
                    //3. check if rook has not moved
                    bbRook = generate64BPositionFromCoordinates(Coordinate(0,0))
                    if(bbMovedCaptured and bbRook == 0uL){
                        //4. no pieces between rook and king
                        bbMoveRoom = bbCastlingRoomLargeWhite and bbRook.inv()
                        bbMoveRoom = bbMoveRoom and bbFigures["king"]!![ownColorPos].inv()
                        if(bbMoveRoom and bbComposite.inv() == bbMoveRoom){
                            addCoordinateToMovementBitboard(color, MovementNotation.KING,Movement(coordinate,2,0),bbTargetsMap)
                        }
                    }
                }

            } else {
                //small castling
                //2. check if king and space between rook and king are not under attack
                if(bbCastlingRoomSmallBlack and bbEnemyMoves.inv() == bbCastlingRoomSmallBlack){
                    //3. check if rook has not moved
                    bbRook = generate64BPositionFromCoordinates(Coordinate(7,7))
                    if(bbMovedCaptured and bbRook == 0uL){
                        //4. no pieces between rook and king
                        bbMoveRoom = bbCastlingRoomSmallBlack and bbRook.inv()
                        bbMoveRoom = bbMoveRoom and bbFigures["king"]!![ownColorPos].inv()
                        if(bbMoveRoom and bbComposite.inv().toULong() == bbMoveRoom){
                            addCoordinateToMovementBitboard(color, MovementNotation.KING,Movement(coordinate,6,7),bbTargetsMap)
                        }
                    }
                }
                //large castling
                //2. check if king and space between rook and king are not under attack
                if(bbCastlingRoomLargeBlack and bbEnemyMoves.inv() == bbCastlingRoomLargeBlack){
                    //3. check if rook has not moved
                    bbRook = generate64BPositionFromCoordinates(Coordinate(0,7))
                    if(bbMovedCaptured and bbRook == 0uL){
                        //4. no pieces between rook and king
                        bbMoveRoom = bbCastlingRoomLargeBlack and bbRook.inv()
                        bbMoveRoom = bbMoveRoom and bbFigures["king"]!![ownColorPos].inv()
                        if(bbMoveRoom and bbComposite.inv() == bbMoveRoom){
                            addCoordinateToMovementBitboard(color, MovementNotation.KING, Movement(coordinate,2,7),bbTargetsMap)
                        }
                    }
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
        val bbFigure = generate64BPositionFromCoordinates(coordinate)
        if(bbComposite and bbFigure == 0uL)return emptyList()
        val movementList = mutableListOf<Movement>()
        val name = getNameOfFigure(pos, bbFigure)
        if(name in figureMap.keys){
            val movementString = (figureMap[name] as FigureParser.Figure).movementParlett
            val movementNotationList = getMovementNotation(movementString)
            var bbTargets = generateMovements(color,coordinate,movementNotationList)
            bbTargets = deleteIllegalMoves(name,color,bbFigure,bbTargets.toMutableMap(),movementNotationList)
            bbTargets = genSpecialMoves(name,color,coordinate,bbTargets,true)
            //transform bbTargets into movementList
            for(movementNotation in bbTargets.keys){
                if(bbTargets[movementNotation] == 0uL)continue
                val moveList = generateCoordinatesFrom64BPosition(bbTargets[movementNotation]!!)
                for(move in moveList){
                    movementList.add(Movement(movementNotation,coordinate.rank,coordinate.file,move.rank,move.file))
                }
            }
        }
        return movementList
    }

    /**
     * does the movement fullfil condition in Movement.MovementNotation.Condition?
     */
    private fun deleteIllegalMoves(figureName : String,
                           color: String,
                           bbFigure: ULong,
                           bbFigureNonRelativeTargets: MutableMap<MovementNotation,ULong>,
                           movementNotationList: List<MovementNotation> ) : MutableMap<MovementNotation,ULong>{
        val pos = (color != "black").toInt()
        for(movementNotation in movementNotationList){
            if(movementNotation.conditions.contains("o")) {//May not be used for a capture (e.g. pawn's forward move)
                //thus bbFigureNonRelativeTargets and bbColorComposite must be bitwise different
                bbFigureNonRelativeTargets[movementNotation] =
                    bbFigureNonRelativeTargets[movementNotation]!! and bbComposite.inv()
            }
            if(movementNotation.conditions.contains("c")) {//May only be made on a capture (e.g. pawn's diagonal capture)
                //thus bbFigureNonRelativeTargets and bbColorComposite must be bitwise the same
                bbFigureNonRelativeTargets[movementNotation] =
                    bbFigureNonRelativeTargets[movementNotation]!! and bbColorComposite[pos]
            }
            if(movementNotation.conditions.contains("i")) {//May only be made on the initial move (e.g. pawn's 2 moves forward)
                if(moveHistory.isNotEmpty()){
                    /* current position of the figure must match initial position (first entry of the movehistory)
                       or move history is empty (first movement) */
                    if(bbFigure and moveHistory[0][figureName]!![1-pos] == 0uL){ //1-pos is the index of player color (ai)
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
    private fun generateMovements(
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
            val bbNewTarget = generate64BPositionFromCoordinates(newPosition)
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
                generateDiagonalMovement(bbMovementMap, coordinate, color, movementNotation)
            }
        }
    }

    /** generate all orthogonal movements: horizontal (WEST,EAST movements) and vertical (NORTH,SOUTH)*/
    private fun generateOrthogonalMovement(bbMovementMap: MutableMap<MovementNotation, ULong>,
                                   coordinate: Coordinate,
                                   color: String,
                                   movementNotation : MovementNotation)  {
        var distance = 7
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
        for(newFile in (coordinate.file+1)..7){// ... inside board (between 0 and 7)
            if(abs(coordinate.file-newFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(Coordinate(coordinate.rank,newFile))
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(
                        "",
                        movementNotation,
                        Movement(coordinate,coordinate.rank,newFile),
                        bbMovementMap
                    )
                    break
                }
                addCoordinateToMovementBitboard(
                    "",
                    movementNotation,
                    Movement(coordinate,coordinate.rank,newFile),
                    bbMovementMap
                )
            }
            else break
        }
    }

    /** backward: decrease file */
    private fun generateSouthMovement(posOwnColor: Int,
                              bbMovementMap: MutableMap<MovementNotation, ULong>,
                              coordinate: Coordinate,
                              movementNotation: MovementNotation,
                              distance : Int) {
        for(newFile in coordinate.file-1 downTo 0){// ... inside board (between 0 and 7)
            if(abs(coordinate.file-newFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(Coordinate(coordinate.rank,newFile))
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(
                        colors[posOwnColor],
                        movementNotation,
                        Movement(coordinate,coordinate.rank,newFile),
                        bbMovementMap
                    )
                    break
                }
                addCoordinateToMovementBitboard(
                    colors[posOwnColor],
                    movementNotation,
                    Movement(coordinate,coordinate.rank,newFile),
                    bbMovementMap
                )
            }
            else break
        }
    }

    /** east: increase rank */
    private fun generateEastMovement(posOwnColor: Int,
                             bbMovementMap: MutableMap<MovementNotation, ULong>,
                             coordinate: Coordinate,
                             movementNotation: MovementNotation,
                             distance : Int) {
        for(newRank in coordinate.rank+1..7){// ... inside board (between 0 and 7)
            if(abs(coordinate.rank-newRank) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(Coordinate(newRank,coordinate.file))
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(
                        colors[posOwnColor],
                        movementNotation,
                        Movement(coordinate,newRank,coordinate.file),
                        bbMovementMap
                    )
                    break
                }
                addCoordinateToMovementBitboard(
                    colors[posOwnColor],
                    movementNotation,
                    Movement(coordinate,newRank,coordinate.file),
                    bbMovementMap
                )
            }
            else break
        }
    }

    /** left: decrease rank */
    private fun generateWestMovement(posOwnColor: Int,
                             bbMovementMap: MutableMap<MovementNotation, ULong>,
                             coordinate: Coordinate,
                             movementNotation: MovementNotation,
                             distance : Int) {
        //if coordinate is ...
        for(newRank in coordinate.rank-1 downTo 0){// ... inside board (between 0 and 7)
            if(abs(coordinate.rank-newRank) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(Coordinate(newRank,coordinate.file))
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(
                        colors[posOwnColor],
                        movementNotation,
                        Movement(coordinate,newRank,coordinate.file),
                        bbMovementMap
                    )
                    break
                }
                addCoordinateToMovementBitboard(
                    colors[posOwnColor],
                    movementNotation,
                    Movement(coordinate,newRank,coordinate.file),
                    bbMovementMap
                )
            }
            else break
        }
    }

    /** generate all diagonal rider movements */
    private fun generateDiagonalMovement(bbMovementMap: MutableMap<MovementNotation, ULong>,
                                 coordinate: Coordinate,
                                 color: String,
                                 movementNotation : MovementNotation)  {
        var distance = 7
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
        var difFile = 1; var difRank = -1
        //if coordinate is ...
        while(coordinate.file+difFile <= 7 && coordinate.rank+difRank >= 0) {// ... inside board (between 0 and 7)
            if(abs(difFile) <= distance && abs(difRank) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = coordinate.file+difFile
                val newRank = coordinate.rank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(Coordinate(newRank,newFile))
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(
                        colors[posOwnColor],
                        movementNotation,
                        Movement(coordinate,newRank,newFile),
                        bbMovementMap
                    )
                    break
                }
                addCoordinateToMovementBitboard(
                    colors[posOwnColor],
                    movementNotation,
                    Movement(coordinate,newRank,newFile),
                    bbMovementMap
                )
                ++difFile
                --difRank
            } else break
        }
    }

    /** NorthEastDiagonalMovement: right,forward: increase file, increase rank*/
    private fun generateNorthEastDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<MovementNotation, ULong>,
                                          coordinate: Coordinate,
                                          movementNotation: MovementNotation,
                                          distance : Int) {
        var difFile = 1; var difRank = 1
        //if coordinate is ...
        while(coordinate.file+difFile <= 7 && coordinate.rank+difRank <= 7) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = coordinate.file+difFile
                val newRank = coordinate.rank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(Coordinate(newRank,newFile))
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop

                    addCoordinateToMovementBitboard(
                        colors[posOwnColor],
                        movementNotation,
                        Movement(coordinate,newRank,newFile),
                        bbMovementMap
                    )
                    break
                }
                addCoordinateToMovementBitboard(
                    colors[posOwnColor],
                    movementNotation,
                    Movement(coordinate,newRank,newFile),
                    bbMovementMap
                )
                ++difFile
                ++difRank
            } else break
        }
    }

    /** SouthEastDiagonalMovement: right,backward: decrease file, increase rank*/
    private fun generateSouthEastDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<MovementNotation, ULong>,
                                          coordinate: Coordinate,
                                          movementNotation: MovementNotation,
                                          distance : Int) {
        var difFile = -1; var difRank = 1
        //if coordinate is ...
        while(coordinate.file+difFile >= 0 && coordinate.rank+difRank <= 7) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){ // ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = coordinate.file+difFile
                val newRank = coordinate.rank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(Coordinate(newRank,newFile))
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(
                        colors[posOwnColor],
                        movementNotation,
                        Movement(coordinate,newRank,newFile),
                        bbMovementMap
                    )
                    break
                }
                addCoordinateToMovementBitboard(
                    colors[posOwnColor],
                    movementNotation,
                    Movement(coordinate,newRank,newFile),
                    bbMovementMap
                )
                --difFile
                ++difRank
            } else break
        }
    }

    /** SouthWestDiagonalMovement: left,backward: decrease file, decrease rank*/
    private fun generateSouthWestDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<MovementNotation, ULong>,
                                          coordinate: Coordinate,
                                          movementNotation: MovementNotation,
                                          distance : Int) {
        var difRank = -1; var difFile = -1
        //if coordinate is ...
        while(coordinate.file+difFile >= 0 && coordinate.rank+difRank >= 0) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = coordinate.file+difFile
                val newRank = coordinate.rank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(Coordinate(newRank,newFile))
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(
                        colors[posOwnColor],
                        movementNotation,
                        Movement(coordinate,newRank,newFile),
                        bbMovementMap
                    )
                    break
                }
                addCoordinateToMovementBitboard(
                    colors[posOwnColor],
                    movementNotation,
                    Movement(coordinate,newRank,newFile),
                    bbMovementMap
                )
                --difRank
                --difFile
            } else break
        }
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
            val targetBB =  generate64BPositionFromCoordinates(movement.getTargetCoordinate())
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
                val bbCandidate = generate64BPositionFromCoordinates(Coordinate(targetRank,targetFile))
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

        val bbCastlingRoomSmallWhite = horizontalLineToBitboard(Movement(4,0,6,0))
        val bbCastlingRoomLargeWhite = horizontalLineToBitboard(Movement(4,0,1,0))
        val bbCastlingRoomSmallBlack = horizontalLineToBitboard(Movement(4,7,6,7))
        val bbCastlingRoomLargeBlack = horizontalLineToBitboard(Movement(4,7,1,7))

        /** generates bitboard that contains all squares from sourceSquare to targetSquare (including them) */
        private fun horizontalLineToBitboard(movement: Movement) : ULong {
            if(movement.sourceFile != movement.targetFile){
                return 0uL
            } else {
                val signRank = movement.getSignRank()
                val distance = movement.getRankDif()
                var result = generate64BPositionFromCoordinates(movement.getSourceCoordinate())
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

        fun generate64BPositionFromCoordinates(coordinate: Coordinate) : ULong {
            var pos : ULong = 1uL shl coordinate.file*8 // pos = 2 ^ (file*8)
            pos = pos shl coordinate.rank       // pos = pos * 2 ^ rank
            return pos         // pos = 2 ^ (file*8) * 2 ^ rank
        }

        fun add64BPositionFromCoordinates(_64B: ULong, coordinate: Coordinate) : ULong {
            return _64B or generate64BPositionFromCoordinates(coordinate)
        }

        fun generate64BPositionFromCoordinateList(list: List<Coordinate>) : ULong{
            var result = 0uL
            for(i in list.indices){
                result = if(i==0){
                    generate64BPositionFromCoordinates(list[i])
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


    fun getPieceName(rank: Int, file: Int) : String{
        val bbFigure = generate64BPositionFromCoordinates(Coordinate(rank,file))
        for(pieceName in bbFigures.keys){
            if((bbFigures[pieceName]!![0] or bbFigures[pieceName]!![1]) and bbFigure == bbFigure)return pieceName
        }
        return ""
    }

    fun getPieceColor(rank: Int, file: Int) : String{
        return when (val bbFigure = generate64BPositionFromCoordinates(Coordinate(rank,file))) {
            bbColorComposite[0] and bbFigure -> "white"
            bbColorComposite[1] and bbFigure -> "black"
            else -> ""
        }
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
    return str.toString()
}