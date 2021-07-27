package emerald.apps.fairychess.model

import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.pow

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


    //map from name of figure to 2D-Array
    //2D-array of chesspieces to represents board (starting with H8 moving rows first und lines down to A1)
    //thus a bitboard represents H8H7H6H5H4H3H2H1G8G7G6....B2B1A8A7A6A5A4A3A2A1
    //first element are white figures, second black figures
    private var bbFigures : MutableMap<String, Array<ULong>> = mutableMapOf()
    var bbComposite : ULong = 0uL
    var bbColorComposite : Array<ULong> = arrayOf(0uL,0uL)

    var gameFinished = false
    var gameWinner = ""
    var promotion : Coordinate? = null
    var moveColor = ""
    val moveHistory = mutableListOf<MutableMap<String, Array<ULong>>>() //move history, for each move map (figureName -> Bitboard)

    constructor(figureMap: Map<String, FigureParser.Figure>) : this(null,figureMap) {

    }

    init {
        if(chessFormationArray != null && figureMap != null){
            //pass a string representing the chess formation here and update chessFormationArray
            if (chessFormationArray.size == 8 && chessFormationArray[0].size == 8) {
                for (rank in 0..7) {
                    for (file in 0..7) {
                        var color = ""
                        val name = chessFormationArray[rank][file]
                        if(file <= 4 && name.isNotEmpty())color = "white"
                        if (file > 4 && name.isNotEmpty())color = "black"
                        if(figureMap.containsKey(name)){
                            val movement = figureMap[name]?.movementParlett
                            val value =  figureMap[name]?.value!!
                            if(movement != null){
                                if(!bbFigures.containsKey(name)){
                                    bbFigures[name] = arrayOf(0uL, 0uL)
                                }
                                setFigure(name,color,rank,file)
                            }
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

    /** @param color of the piece set
     *  @param name of the piece set
     * sets figure at coordinate */
    fun setFigure(name:String,color: String,rank:Int,file:Int){
        val pos = (color == "black").toInt()
        val bbFigure = generate64BPositionFromCoordinates(rank,file)
        if(!bbFigures.containsKey(name)){
            val value = arrayOf(0uL,0uL)
            value[pos] = bbFigure
            bbFigures[name] = value
        } else bbFigures[name]!![pos] = add64BPositionFromCoordinates(bbFigures[name]!![pos],rank,file)
        bbComposite = add64BPositionFromCoordinates(bbComposite,rank,file)
        bbColorComposite[pos] = add64BPositionFromCoordinates(bbColorComposite[pos],rank,file)
    }

    /** @param color of the piece set
     *  @param name of the piece set
     * moves figure from coordinate (sourceFile,sourceRow) to coordinate (targetFile,targetRow)*/
    private fun moveFigure(color: String, name : String, sourceRank:Int, sourceFile:Int, targetRank:Int, targetFile:Int){
        var targetBB = bbFigures[name]?.get((color == "black").toInt())
        if (targetBB != null) {
            val newPosition = generate64BPositionFromCoordinates(sourceRank,sourceFile)
            targetBB = targetBB and newPosition
            targetBB = targetBB or newPosition
            bbFigures[name]?.set((color == "black").toInt(),targetBB)
            bbComposite = bbComposite and newPosition
            bbComposite = bbComposite or newPosition
            val pos = (color == "black").toInt()
            bbColorComposite[pos] = bbColorComposite[pos] and newPosition
            bbColorComposite[pos] = bbColorComposite[pos] or newPosition
            //add current position (as map bbFigures) to history
            moveHistory.add(bbFigures)
        }
    }


    /** return a map of all posible moves for player of @color as bitmap*/
    fun getAllPossibleMoves(color : String) : Map<Coordinate,ULong> {
        val allPossibleMoves = mutableMapOf<Coordinate,ULong>()
        val pos = ("black" == color).toInt()
        for(rank in 0..7){
            for(file in 0..7){
                val bbFigure = generate64BPositionFromCoordinates(rank,file)
                val name = getNameOfFigure(pos, bbFigure)
                if(name.isEmpty())continue //empty field
                if(bbColorComposite[pos] and bbFigure == bbFigure){
                    allPossibleMoves[Coordinate(rank,file)] = getTargetMovements(name,color,file,rank,true)
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
    fun getTargetMovements(name:String, color: String, sourceRank:Int, sourceFile: Int, genCastlingMoves:Boolean) : ULong {
        if(!(sourceRank in 0..7 && sourceFile in 0..7))return 0uL
        val movementString = (figureMap[name] as FigureParser.Figure).movementParlett
        val movementNotationList = getMovementNotation(movementString)
        val bbFigure = generate64BPositionFromCoordinates(sourceRank,sourceFile)
        var bbTargets = generateMovements(color,sourceRank,sourceFile,genCastlingMoves,movementNotationList)
        bbTargets = deleteIllegalMoves(name,color,bbFigure,bbTargets.toMutableMap(),movementNotationList)

        var resultMovement = 0uL
        for(key in bbTargets.keys){
            val targets : ULong = bbTargets[key] ?: error("")
            resultMovement = resultMovement or targets
        }
        return resultMovement
    }

    /**
     * return a list of possible movements of the figure at (sourceRank,sourceFile)
     * */
    fun getTargetMovementsAsMovementList(color: String, sourceFile:Int, sourceRank: Int, genCastlingMoves:Boolean) : List<ChessPiece.Movement> {
        val pos = ("black" == color).toInt()
        val bbFigure = generate64BPositionFromCoordinates(sourceRank,sourceFile)
        val name = getNameOfFigure(pos, bbFigure)
        val movementString = (figureMap[name] as FigureParser.Figure).movementParlett
        val movementNotationList = getMovementNotation(movementString)
        var bbTargets = generateMovements(color,sourceRank,sourceFile,genCastlingMoves,movementNotationList)
        bbTargets = deleteIllegalMoves(name,color,bbFigure,bbTargets.toMutableMap(),movementNotationList)

        val movementList = mutableListOf<ChessPiece.Movement>()
        for(movementNotation in bbTargets.keys){
            val moveList = generateCoordinatesFrom64BPosition(bbTargets[movementNotation]!!)
            for(move in moveList){
                movementList.add(ChessPiece.Movement(movementNotation,sourceRank,sourceFile,move.file,move.rank))
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
                           bbFigureNonRelativeTargets: MutableMap<ChessPiece.MovementNotation,ULong>,
                           movementNotationList: List<ChessPiece.MovementNotation> ) : MutableMap<ChessPiece.MovementNotation,ULong>{
        var newBBFigureNonRelativeTargets = bbFigureNonRelativeTargets
        val pos = (color != "black").toInt()
        for(movementNotation in movementNotationList){
            if(movementNotation.conditions.contains("o")) {//May not be used for a capture (e.g. pawn's forward move)
                //thus bbFigureNonRelativeTargets and bbColorComposite must be bitwise different
                newBBFigureNonRelativeTargets[movementNotation] =
                    newBBFigureNonRelativeTargets[movementNotation]!! and bbColorComposite[pos].inv()
            }
            if(movementNotation.conditions.contains("c")) {//May only be made on a capture (e.g. pawn's diagonal capture)
                //thus bbFigureNonRelativeTargets and bbColorComposite must be bitwise the same
                newBBFigureNonRelativeTargets[movementNotation] =
                    newBBFigureNonRelativeTargets[movementNotation]!! and bbColorComposite[pos]
            }
            if(movementNotation.conditions.contains("i")) {//May only be made on the initial move (e.g. pawn's 2 moves forward)
                if(moveHistory.isNotEmpty()){
                    /* current position of the figure must match initial position (first entry of the movehistory)
                       or move history is empty (first movement) */
                    if(bbFigure and moveHistory[0][figureName]!![1-pos] == 0uL){ //1-pos is the index of player color (ai)
                        newBBFigureNonRelativeTargets[movementNotation] = 0uL
                    }
                }
            }
            if(movementNotation.movetype == "g") {//moves by leaping over piece to an empty square (if leaped over enemy => capture)
                newBBFigureNonRelativeTargets = leapMoveCheck(figureName,color,bbFigure,bbFigureNonRelativeTargets,movementNotation)
            }
        }
        return newBBFigureNonRelativeTargets
    }

    /** deletes the calculated enpassante moves that are illegal */
    private fun specialMoveCheckEnpassante(pos: Int,
                                           bbFigure: ULong,
                                           bbFigureNonRelativeTargets: MutableMap<ChessPiece.MovementNotation,ULong>,
                                           color: String,
                                           movementNotation: ChessPiece.MovementNotation) : MutableMap<ChessPiece.MovementNotation,ULong> {
        if(moveHistory.isNotEmpty()){
            bbFigureNonRelativeTargets[movementNotation] = 0uL//en passante moves must be illegal
        } else {
            //try to match the former position of pawn to the target position by moving it one line up/down (depending on the color)
            var bbFormerToTargetPositionOpponent = 0uL
            if(color == "white"){
                bbFormerToTargetPositionOpponent = moveHistory[moveHistory.size-1]["pawn"]!!.get(pos) shr 8 //one line down
            } else if(color == "black"){
                bbFormerToTargetPositionOpponent = moveHistory[moveHistory.size-1]["pawn"]!!.get(pos)  shl 8 //one line up
            }

            //try to match the former position of pawn to the target position by moving it one line up/down (depending on the color)
            var bbCurrentToTargetPositionOpponent = 0uL //provides the admissible former positions of the opponent in order to make the en passante movement legal
            if(color == "white"){
                bbCurrentToTargetPositionOpponent = bbFigures["pawn"]!!.get(pos) shl 8 //one line down
            } else if(color == "black"){
                bbCurrentToTargetPositionOpponent = bbFigures["pawn"]!!.get(pos)  shr 8 //one line up
            }

            //connect the three masks (matching the masks shows that the two requirements are met)
            bbFigureNonRelativeTargets[movementNotation] = bbFormerToTargetPositionOpponent and
                    bbCurrentToTargetPositionOpponent and bbFigureNonRelativeTargets[movementNotation]!!
        }
        return bbFigureNonRelativeTargets
    }

    /** deletes the leap moves that are illegal */
    private fun leapMoveCheck(figureName : String,
                              color: String,
                              bbFigure: ULong,
                              bbFigureNonRelativeTargets: MutableMap<ChessPiece.MovementNotation,ULong>,
                              movementNotation: ChessPiece.MovementNotation) : MutableMap<ChessPiece.MovementNotation,ULong>{
        //TODO
        return bbFigureNonRelativeTargets
    }


    /** generate a bitboard representing the target squares of the non relative movement for a piece */
    fun generateMovements(
        color: String,
        sourceRank:Int,
        sourceFile: Int,
        genCastlingMoves:Boolean,
        movementNotationList: List<ChessPiece.MovementNotation>
    ) : Map<ChessPiece.MovementNotation,ULong> {
        val bbMovementMap = mutableMapOf<ChessPiece.MovementNotation,ULong>()
        for (movementNotation in movementNotationList) {
            bbMovementMap[movementNotation] = 0uL
            if (movementNotation.movetype == "~" || movementNotation.movetype == "^" || movementNotation.movetype == "g") { //leaper
                generateLeaperMovements(bbMovementMap,sourceRank,sourceFile,color,movementNotation)
            } else { //rider
                generateRiderMovements(bbMovementMap,sourceRank,sourceFile,color,movementNotation)
            }
        }
        return bbMovementMap
    }


    /** generate a list of movement matching the movementNotation (Leaper) */
    fun generateLeaperMovements(bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                sourceRank: Int,
                                sourceFile: Int,
                                color: String,
                                movementNotation : ChessPiece.MovementNotation){
        if (movementNotation.grouping == "/" && movementNotation.distances.size == 2) { //for now leaper movement consist of 2 subsequent movements
            //leaper-movements always have 8 sub-moves:
            //(2: increase/decrease)*(2: value1/value2)*(2: on File / on Rank) = 8 permutations
            val distance1 = movementNotation.distances[0]
            val distance2 = movementNotation.distances[1]
            if (distance1.matches("[0-9]".toRegex()) && distance2.matches("[0-9]".toRegex())) {
                val dis1 = distance1.toInt()
                val dis2 = distance2.toInt()
                /** generate all (8) leaper movements matching movementNotation (Leaper) */
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,dis1, dis2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,-dis1, dis2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,dis1, -dis2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,-dis1, -dis2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,dis2, dis1)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,-dis2, dis1)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,dis2, -dis1)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,-dis2, -dis1)
            } else {
                if (distance1 == "x" && distance2 == "x") {//only in pairs (x,x): any distance in the given direction equal to its twin or zero
                    for (distance in -7..7) {
                        //orthogonal
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,0, distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,distance, 0)
                        //diagonal
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,distance, distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,-distance, distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,distance, -distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceRank,sourceFile,-distance, -distance)
                    }
                }
            }
        }
    }

    /** add a leaper movement to targetSquares defined by an delta (fileDif,rankDif) */
    fun generateLeaperMovement(
        color: String,
        bbMovementMap : MutableMap<ChessPiece.MovementNotation,ULong>,
        movementNotation: ChessPiece.MovementNotation,
        positionRank: Int,
        positionFile: Int,
        rankDif: Int,
        fileDif: Int
    ) {
        val newRank = positionRank + rankDif
        val newFile = positionFile + fileDif
        if (newFile in 0..7 && newRank in 0..7) {
            val bbNewTarget = generate64BPositionFromCoordinates(newRank,newFile)
            val pos = (color =="black").toInt() //opponent color
            if(bbNewTarget and bbColorComposite[pos].inv() == bbNewTarget){//check if there is no figure of self color at targets
                addCoordinateToMovementBitboard(bbMovementMap,newRank,newFile,movementNotation)
            }
        }
    }

    /** generate a list of rider-movements matching the movementNotation (rider) */
    fun generateRiderMovements(bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                               positionRank: Int,
                               positionFile: Int,
                               color: String,
                               movementNotation : ChessPiece.MovementNotation) {
        if (movementNotation.distances.isNotEmpty()) {
            if(arrayOf(">","<","<>","=","<=",">=","+","*").contains(movementNotation.direction)){
                generateOrthogonalMovement(bbMovementMap, positionRank, positionFile, color, movementNotation)
            }
            if(arrayOf("X","X<","X>","*").contains(movementNotation.direction)){
                generateDiagonalMovement(bbMovementMap, positionRank, positionFile, color, movementNotation)
            }
        }
    }

    /** generate all orthogonal movements: horizontal (WEST,EAST movements) and vertical (NORTH,SOUTH)*/
    fun generateOrthogonalMovement(bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                   positionRank: Int,
                                   positionFile: Int,
                                   color: String,
                                   movementNotation : ChessPiece.MovementNotation)  {
        var distance = 7
        val posOwnColor = (color == "black").toInt()
        if(movementNotation.distances[0].matches("[1-9]+".toRegex()))distance = movementNotation.distances[0].toInt()
        //forward(>) and backwards(<) are color-dependent because they are depending on direction of the figures
        //color-independent movements
        if(movementNotation.direction.contains("=") || movementNotation.direction == "+" || movementNotation.direction == "*") {
            generateWestMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
            generateEastMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
        }
        if(movementNotation.direction == "+" || movementNotation.direction == "*" || movementNotation.direction == "<>"
            || movementNotation.direction.contains(">") || movementNotation.direction.contains("<")){
            //color-dependent movements
            if(movementNotation.direction.contains(">") && !movementNotation.direction.contains("<")){
                //forwards but not backwards
                if(color == "black"){
                    generateSouthMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
                } else {
                    generateNorthMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
                }
            } else if(movementNotation.direction.contains("<") && !movementNotation.direction.contains(">")){
                //backwards but not forwards
                if(color == "black"){
                    generateNorthMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
                } else {
                    generateSouthMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
                }
            } else { //color-independent movements
                generateNorthMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
                generateSouthMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
            }

        }
    }

    /** forward: increase file */
    fun generateNorthMovement(posOwnColor: Int,
                              bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                              positionRank: Int,
                              positionFile: Int,
                              movementNotation: ChessPiece.MovementNotation,
                              distance : Int) {
        for(newFile in (positionFile+1)..7){// ... inside board (between 0 and 7)
            if(abs(positionFile-newFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(positionRank,newFile)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,positionRank,newFile,movementNotation)
                    break
                }
                addCoordinateToMovementBitboard(bbMovementMap,positionRank,newFile,movementNotation)
            }
            else break
        }
    }

    /** backward: decrease file */
    fun generateSouthMovement(posOwnColor: Int,
                              bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                              positionRank: Int,
                              positionFile: Int,
                              movementNotation: ChessPiece.MovementNotation,
                              distance : Int) {
        for(newFile in positionFile-1 downTo 0){// ... inside board (between 0 and 7)
            if(abs(positionFile-newFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(positionRank,newFile)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,positionRank,newFile,movementNotation)
                    break
                }
                addCoordinateToMovementBitboard(bbMovementMap,positionRank,newFile,movementNotation)
            }
            else break
        }
    }

    /** east: increase rank */
    fun generateEastMovement(posOwnColor: Int,
                             bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                             positionRank: Int,
                             positionFile: Int,
                             movementNotation: ChessPiece.MovementNotation,
                             distance : Int) {
        for(newRank in positionRank+1..7){// ... inside board (between 0 and 7)
            if(abs(positionRank-newRank) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(newRank,positionFile)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newRank,positionFile,movementNotation)
                    break
                }
                addCoordinateToMovementBitboard(bbMovementMap,newRank,positionFile,movementNotation)
            }
            else break
        }
    }

    /** left: decrease rank */
    fun generateWestMovement(posOwnColor: Int,
                             bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                             positionRank: Int,
                             positionFile: Int,
                             movementNotation: ChessPiece.MovementNotation,
                             distance : Int) {
        //if coordinate is ...
        for(newRank in positionRank-1 downTo 0){// ... inside board (between 0 and 7)
            if(abs(positionRank-newRank) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(newRank,positionFile)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newRank,positionFile,movementNotation)
                    break
                }
                addCoordinateToMovementBitboard(bbMovementMap,newRank,positionFile,movementNotation)
            }
            else break
        }
    }

    /** generate all diagonal rider movements */
    fun generateDiagonalMovement(bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                 positionRank: Int,
                                 positionFile: Int,
                                 color: String,
                                 movementNotation : ChessPiece.MovementNotation)  {
        var distance = 7
        val posOwnColor = (color == "black").toInt()
        if(movementNotation.distances[0].matches("[0-9]".toRegex())){
            distance = movementNotation.distances[0].toInt()
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X>"){
            if (color == "black" && movementNotation.direction == "X>"){
                generateSouthEastDiagonalMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
                generateSouthWestDiagonalMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
            } else {
                generateNorthEastDiagonalMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
                generateNorthWestDiagonalMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
            }
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X<") {
            if (color == "black" && movementNotation.direction == "X>"){
                generateNorthEastDiagonalMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
                generateNorthWestDiagonalMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
            } else {
                generateSouthEastDiagonalMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
                generateSouthWestDiagonalMovement(posOwnColor,bbMovementMap, positionRank, positionFile, movementNotation, distance)
            }
        }
    }

    /** NorthWestDiagonalMovement: left,forward: increase file, decrease rank*/
    fun generateNorthWestDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                          positionRank: Int,
                                          positionFile: Int,
                                          movementNotation: ChessPiece.MovementNotation,
                                          distance : Int) {
        var difFile = 1; var difRank = -1
        //if coordinate is ...
        while(positionFile+difFile <= 7 && positionRank+difRank >= 0) {// ... inside board (between 0 and 7)
            if(abs(difFile) <= distance && abs(difRank) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = positionFile+difFile
                val newRank = positionRank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(newRank,newFile)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newRank,newFile,movementNotation)
                    break
                }
                addCoordinateToMovementBitboard(bbMovementMap,newRank,newFile,movementNotation)
                ++difFile
                --difRank
            } else break
        }
    }

    /** NorthEastDiagonalMovement: right,forward: increase file, increase rank*/
    fun generateNorthEastDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                          positionRank: Int,
                                          positionFile: Int,
                                          movementNotation: ChessPiece.MovementNotation,
                                          distance : Int) {
        var difFile = 1; var difRank = 1
        //if coordinate is ...
        while(positionFile+difFile <= 7 && positionRank+difRank <= 7) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = positionFile+difFile
                val newRank = positionRank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(newRank,newFile)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newRank,newFile,movementNotation)
                    break
                }
                addCoordinateToMovementBitboard(bbMovementMap,newRank,newFile,movementNotation)
                ++difFile
                ++difRank
            } else break
        }
    }

    /** SouthEastDiagonalMovement: right,backward: decrease file, increase rank*/
    fun generateSouthEastDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                          positionRank: Int,
                                          positionFile: Int,
                                          movementNotation: ChessPiece.MovementNotation,
                                          distance : Int) {
        var difFile = -1; var difRank = 1
        //if coordinate is ...
        while(positionFile+difFile >= 0 && positionRank+difRank <= 7) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){ // ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = positionFile+difFile
                val newRank = positionRank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(newRank,newFile)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newRank,newFile,movementNotation)
                    break
                }
                addCoordinateToMovementBitboard(bbMovementMap,newRank,newFile,movementNotation)
                --difFile
                ++difRank
            } else break
        }
    }

    /** SouthWestDiagonalMovement: left,backward: decrease file, decrease rank*/
    fun generateSouthWestDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                          positionRank: Int,
                                          positionFile: Int,
                                          movementNotation: ChessPiece.MovementNotation,
                                          distance : Int) {
        var difRank = -1; var difFile = -1
        //if coordinate is ...
        while(positionFile+difFile >= 0 && positionRank+difRank >= 0) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = positionFile+difFile
                val newRank = positionRank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(newRank,newFile)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newRank,newFile,movementNotation)
                    break
                }
                addCoordinateToMovementBitboard(bbMovementMap,newRank,newFile,movementNotation)
                --difRank
                --difFile
            } else break
        }
    }

    fun addCoordinateToMovementBitboard(bbMovementMap : MutableMap<ChessPiece.MovementNotation,ULong>,
                                        positionRank: Int,
                                        positionFile: Int,
                                        movementNotation: ChessPiece.MovementNotation){
        bbMovementMap[movementNotation] = bbMovementMap[movementNotation]!! or generate64BPositionFromCoordinates(
            positionRank,
            positionFile,
        )
    }

    override fun toString(): String {
        val fList = mutableListOf<Coordinate>()
        for(key in bbFigures.keys){
            fList.addAll(generateCoordinatesFrom64BPosition(bbFigures[key]?.get(0) ?: 0uL))
            fList.addAll(generateCoordinatesFrom64BPosition(bbFigures[key]?.get(1) ?: 0uL))
        }
        println(fList.size)
        val str = StringBuilder("")
        var cnt = 0
        for(rank in 0..7){
             str.append(rank.toString()+" | ")
             for(file in 0..7){
                 var empty = true
                 for(key in bbFigures.keys){
                    if(key.isEmpty())continue
                    val num = 2.0.pow(file * 8 + rank).toULong()
                    val white = bbFigures[key]?.get(0) ?: 0uL
                    val black = bbFigures[key]?.get(1) ?: 0uL
                    if(white and num == num){
                        if(key.isNotEmpty())str.append(key[0].toUpperCase())
                        str.append(" | ")
                        ++cnt
                        empty = false
                    }
                    if(black and num == num){
                        if(key.isNotEmpty())str.append(key[0])
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
        println(cnt)
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

    /** switchMoveColor from white to black and vice versa */
    fun switchMoveColor(){
        moveColor = Chessboard.oppositeColor(moveColor)
    }

    private fun addMoveToMoveHistory(movement: ChessPiece.Movement){
       // moveHistory.add(movement)
    }

    private fun checkForPromotion() {
        for (rank in 0..7) {
            val firstFile = 2.0.pow((0 * 8 + rank).toDouble()).toULong()
            val lastFile = 2.0.pow((7 * 8 + rank).toDouble()).toULong()
            if((bbFigures["pawn"]?.get(0)!! and lastFile) == lastFile){
                promotion = Coordinate(7, rank)
            }
            if((bbFigures["pawn"]?.get(1)!! and firstFile) == firstFile){
                promotion = Coordinate(0, rank)
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
    fun countSetBits(n: ULong): Int {
        // base case
        return if (n == 0uL) 0
            else 1 + countSetBits(n and n - 1uL)
    }

    companion object {
        fun chessboardToBitboard(chessboard: Chessboard) : Bitboard {
            val bitboard = Bitboard(chessboard.figureMap)
            for(rank in chessboard.pieces.indices){
                for(file in chessboard.pieces[rank].indices) {
                    val piece = chessboard.pieces[rank][file]
                    if(piece.name.isEmpty())continue
                    bitboard.setFigure(piece.name,piece.color,rank,file)
                }
            }
            return bitboard
        }

        val movingStringTomovementNotationsMap = mutableMapOf<String, List<ChessPiece.MovementNotation>>()

        fun getMovementNotation(movingPatternString : String) : List<ChessPiece.MovementNotation>{
            val movementNotations = mutableListOf<ChessPiece.MovementNotation>()
            if (movingStringTomovementNotationsMap.containsKey(movingPatternString)) {
                movementNotations.addAll(movingStringTomovementNotationsMap[movingPatternString]!!)
            } else {
                movementNotations.addAll(
                    ChessPiece.MovementNotation.parseMovementString(
                        movingPatternString
                    )
                )
                movingStringTomovementNotationsMap[movingPatternString] = movementNotations
            }
            return movementNotations
        }

        fun generate64BPositionFromCoordinates(rank: Int, file: Int) : ULong {
            var pos : ULong = 1uL shl file*8 // pos = 2 ^ (file*8)
            pos = pos shl rank       // pos = pos * 2 ^ rank
            return pos         // pos = 2 ^ (file*8) * 2 ^ rank
        }

        fun add64BPositionFromCoordinates(_64B: ULong, rank: Int, file: Int) : ULong {
            return _64B or generate64BPositionFromCoordinates(rank, file)
        }

        fun generate64BPositionFromCoordinateList(list: List<Coordinate>) : ULong{
            var result = 0uL
            for(i in list.indices){
                result = if(i==0){
                    generate64BPositionFromCoordinates(list[i].rank,list[i].file)
                } else {
                    add64BPositionFromCoordinates(result,list[i].rank,list[i].file)
                }
            }
            return result
        }

        class Coordinate(val rank: Int, val file: Int){
            override fun equals(other: Any?): Boolean {
                if(other is Coordinate){
                    return (file == other.file) && (rank == other.rank)
                }
                return super.equals(other)
            }
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

        fun transformNumberToCoordinate(index: Int) : Coordinate {
            return Coordinate((index / 8), (index % 8))
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

        fun bitboardToString(bitboard : ULong) : String {
            val picture = java.lang.StringBuilder("")
            var pointer = 1uL
            for(i in 63 downTo 0){
                if((2.0).pow(i.toDouble()).toULong() and bitboard == (2.0).pow(i.toDouble()).toULong()){
                    picture.append("X")
                } else {
                    picture.append(".")
                }
                if((i-1) % 8 == 0)picture.append("\n")
            }
            return picture.toString()
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



}

private fun Boolean.toInt(): Int {
   return if (this) 1 else 0
}









/**
 * does the movement fullfil condition in Movement.MovementNotation.Condition?
 */
    fun fullfillsCondition(color: String,
                           sourceFile: Int,
                           sourceRank: Int,
                           targetFile: Int,
                           targetRank: Int,
                           movementNotation: ChessPiece.MovementNotation) : Boolean {
        var returnValue = true
        /*if(movementNotation.conditions.contains("o")) {//May not be used for a capture (e.g. pawn's forward move)
            returnValue = returnValue && !(pieces[movement.sourceFile][movement.sourceRank].color != pieces[movement.targetFile][movement.targetRank].color
                    && pieces[movement.sourceFile][movement.sourceRank].color.isNotEmpty()
                    && pieces[movement.targetFile][movement.targetRank].color.isNotEmpty())
        }
        if(movementNotation.conditions.contains("c")) {//May only be made on a capture (e.g. pawn's diagonal capture)
            returnValue = returnValue && (pieces[movement.sourceFile][movement.sourceRank].color != pieces[movement.targetFile][movement.targetRank].color
                    && pieces[movement.sourceFile][movement.sourceRank].color.isNotEmpty()
                    && pieces[movement.targetFile][movement.targetRank].color.isNotEmpty())
            //en passante
            if(!returnValue)returnValue = specialMoveCheckEnpassante(movement) != null
        }
        if(movementNotation.conditions.contains("i")) {//May only be made on the initial move (e.g. pawn's 2 moves forward)
            returnValue = returnValue && (pieces[movement.sourceFile][movement.sourceRank].moveCounter == 0)
        }
        if(movementNotation.movetype == "g") {//moves by leaping over piece to an empty square (if leaped over enemy => capture)
            val signFile = sign((movement.targetFile - movement.sourceFile).toDouble()).toInt()
            val signRank = sign((movement.targetRank - movement.sourceRank).toDouble()).toInt()
            returnValue = pieces[movement.targetFile][movement.targetRank].color.isEmpty()
                    && pieces[movement.targetFile-signFile][movement.targetRank-signRank].color.isNotEmpty()
                    && (Math.abs(movement.targetFile-movement.sourceFile) > 1 || Math.abs(movement.targetRank-movement.sourceRank) > 1)
        }*/
        return returnValue
    }

private fun generateSpecialMoveCheckCastling(color : String, bbFigureNonRelativeTargets:ULong) : ULong{
    var returnValue = bbFigureNonRelativeTargets
    return returnValue
}