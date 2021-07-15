package emerald.apps.fairychess.model

import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.pow

class Bitboard(
    private val chessFormationArray: Array<Array<String>>,
    private val figureMap: Map<String, FigureParser.Figure>
) {


    //map from name of figure to 2D-Array
    //2D-array of chesspieces to represents board (starting with H8 moving rows first und lines down to A1)
    //thus a bitboard represents H8H7H6H5H4H3H2H1G8G7G6....B2B1A8A7A6A5A4A3A2A1
    //first element are white figures, second black figures
    private var bbFigures : MutableMap<String, Array<ULong>> = mutableMapOf()
    private var bbComposite : ULong = 0uL
    private var bbColorComposite : Array<ULong> = arrayOf(0uL,0uL)

    var gameFinished = false
    var gameWinner = ""
    var promotion = arrayOf<Int>()
    var moveColor = ""
    val moveHistory = mutableListOf<MutableMap<String, Array<ULong>>>() //move history, for each move map (figureName -> Bitboard)


    init {
        //pass a string representing the chess formation here and update chessFormationArray
        if (chessFormationArray.size == 8 && chessFormationArray[0].size == 8) {
            for (file in 0..7) {
                for (rank in 0..7) {
                    var color = ""
                    val name = chessFormationArray[file][rank]
                    if(rank <= 4 && name.isNotEmpty())color = "white"
                    if (rank > 4 && name.isNotEmpty())color = "black"
                    if(figureMap.containsKey(name)){
                        val movement = figureMap[name]?.movementParlett
                        val value =  figureMap[name]?.value!!
                        if(movement != null){
                            if(!bbFigures.containsKey(name)){
                                bbFigures[name] = arrayOf(0uL, 0uL)
                            }
                            setFigure(name,color,file, rank)
                        }
                    }
                }
            }
        }

        val fList = mutableListOf<Coordinate>()
        for(key in bbFigures.keys){
            fList.addAll(generateCoordinatesFrom64BPosition(bbFigures[key]?.get(0) ?: 0uL))
            fList.addAll(generateCoordinatesFrom64BPosition(bbFigures[key]?.get(1) ?: 0uL))
        }
        println(fList.size)
    }

    /** @param color of the piece set
     *  @param name of the piece set
     * sets figure at coordinate */
    fun setFigure(name:String,color: String,file:Int,rank:Int){
        val pos = (color == "black").toInt()
        bbFigures[name]?.set(
            pos, add64BPositionFromCoordinates(
                bbFigures[name]?.get(
                    pos
                )!!, file, rank
            )
        )
        bbComposite = add64BPositionFromCoordinates(
            bbFigures[name]?.get(
                pos
            )!!, file, rank
        )
        bbColorComposite[pos] = add64BPositionFromCoordinates(
            bbFigures[name]?.get(
                pos
            )!!, file, rank
        )
    }

    /** @param color of the piece set
     *  @param name of the piece set
     * moves figure from coordinate (sourceFile,sourceRow) to coordinate (targetFile,targetRow)*/
    private fun moveFigure(color: String, name : String, sourceFile:Int, sourceRow:Int, targetFile:Int, targetRow:Int){
        var targetBB = bbFigures[name]?.get((color == "black").toInt())
        if (targetBB != null) {
            val newPosition = generate64BPositionFromCoordinates(sourceFile,sourceRow)
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


    /** return a list of all posible moves for player of @color as bitmap*/
    fun getAllPossibleMoves(color : String) : Map<Int,ULong> {
        val allPossibleMoves = mutableMapOf<Int,ULong>()
        val pos = ("black" == color).toInt()
        for(file in 0..7){
            for(rank in 0..7){
                val bbFigure = generate64BPositionFromCoordinates(file,rank)
                val name = getNameOfFigure(pos, bbFigure)
                if(name.isEmpty())continue //empty field
                if(bbColorComposite[pos] and bbFigure == bbFigure){
                    allPossibleMoves[file*10 + rank] = getTargetMovements(name,color,file,rank,true)
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
     * return a list of possible movements of the figure at (sourceFile,sourceRank)
     * */
    fun getTargetMovements(name:String, color: String, sourceFile:Int, sourceRank: Int, genCastlingMoves:Boolean) : ULong {
        val movementString = (figureMap[name] as FigureParser.Figure).movementParlett
        val movementNotationList = getMovementNotation(movementString)
        val bbTargets = generateMovements(color,sourceFile,sourceRank,genCastlingMoves,movementNotationList)

        var resultMovement = 0uL
        for(key in bbTargets){
            resultMovement = resultMovement or bbTargets[key]!!
        }
        return resultMovement
    }

    /** generate a bitboard representing the target squares of the non relative movement for a piece */
    fun generateMovements(
        color: String,
        sourceFile:Int,
        sourceRank: Int,
        genCastlingMoves:Boolean,
        movementNotationList: List<ChessPiece.MovementNotation>
    ) : Map<ChessPiece.MovementNotation,ULong> {
        val bbMovementMap = mutableMapOf<ChessPiece.MovementNotation,ULong>()
        for (movementNotation in movementNotationList) {
            bbMovementMap[movementNotation] = 0uL
            if (movementNotation.movetype == "~" || movementNotation.movetype == "^" || movementNotation.movetype == "g") { //leaper
                generateLeaperMovements(bbMovementMap,sourceFile,sourceRank,color,movementNotation)
            } else { //rider
                generateRiderMovements(bbMovementMap,sourceFile,sourceRank,color,movementNotation)
            }
        }
        return bbMovementMap
    }


    /** generate a list of movement matching the movementNotation (Leaper) */
    fun generateLeaperMovements(bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                sourceFile: Int,
                                sourceRank: Int,
                                color: String,
                                movementNotation : ChessPiece.MovementNotation){
        if (movementNotation.grouping == "/" && movementNotation.distances.size == 2) { //for now leaper movement consist of 2 subsequent movements
            //leaper-movements always have 8 sub-moves:
            //(2: increase/decrease)*(2: value1/value2)*(2: on File / on Rank) = 8 permutations
            val distance1 = movementNotation.distances[0]
            val distance2 = movementNotation.distances[1]
            if (distance1.matches("[0-9]".toRegex()) && distance2.matches("[0-9]".toRegex())) {
                val d1 = distance1.toInt()
                val d2 = distance2.toInt()
                /** generate all (8) leaper movements matching movementNotation (Leaper) */
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,d1, d2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,-d1, d2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,d1, -d2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,-d1, -d2)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,d2, d1)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,-d2, d1)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,d2, -d1)
                generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,-d2, -d1)
            } else {
                if (distance1 == "x" && distance2 == "x") {//only in pairs (x,x): any distance in the given direction equal to its twin or zero
                    for (distance in -7..7) {
                        //orthogonal
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,0, distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,distance, 0)
                        //diagonal
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,distance, distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,-distance, distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,distance, -distance)
                        generateLeaperMovement(color,bbMovementMap,movementNotation,sourceFile,sourceRank,-distance, -distance)
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
        positionFile: Int,
        positionRank: Int,
        fileDif: Int,
        rankDif: Int
    ) {
        val newFile = positionFile + fileDif
        val newRank = positionRank + rankDif
        if (newFile in 0..7 && newRank in 0..7) {
            val bbNewTarget = generate64BPositionFromCoordinates(newFile,newRank)
            val pos = (color =="black").toInt() //opponent color
            if(bbNewTarget and bbColorComposite[pos].inv() == bbNewTarget){//check if there is no figure of self color at targets
                addCoordinateToMovementBitboard(bbMovementMap,newFile,newRank,movementNotation)
            }
        }
    }

    /** generate a list of rider-movements matching the movementNotation (rider) */
    fun generateRiderMovements(bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                               positionFile: Int,
                               positionRank: Int,
                               color: String,
                               movementNotation : ChessPiece.MovementNotation) {
        if (movementNotation.distances.isNotEmpty()) {
            if(arrayOf(">","<","<>","=","<=",">=","+","*").contains(movementNotation.direction)){
                generateOrthogonalMovement(bbMovementMap, positionFile, positionRank, color, movementNotation)
            }
            if(arrayOf("X","X<","X>","*").contains(movementNotation.direction)){
                generateDiagonalMovement(bbMovementMap, positionFile, positionRank, color, movementNotation)
            }
        }
    }

    /** generate all orthogonal movements: horizontal (WEST,EAST movements) and vertical (NORTH,SOUTH)*/
    fun generateOrthogonalMovement(bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                   positionFile: Int,
                                   positionRank: Int,
                                   color: String,
                                   movementNotation : ChessPiece.MovementNotation)  {
        var distance = 7
        val posOwnColor = (color == "black").toInt()
        if(movementNotation.distances[0].matches("[1-9]+".toRegex()))distance = movementNotation.distances[0].toInt()
        //forward(>) and backwards(<) are color-dependent because they are depending on direction of the figures
        //color-independent movements
        if(movementNotation.direction.contains("=") || movementNotation.direction == "+" || movementNotation.direction == "*") {
            generateWestMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
            generateEastMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
        }
        if(movementNotation.direction == "+" || movementNotation.direction == "*" || movementNotation.direction == "<>"
            || movementNotation.direction.contains(">") || movementNotation.direction.contains("<")){
            //color-dependent movements
            if(movementNotation.direction.contains(">") && !movementNotation.direction.contains("<")){
                //forwards but not backwards
                if(color == "black"){
                    generateSouthMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
                } else {
                    generateNorthMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
                }
            } else if(movementNotation.direction.contains("<") && !movementNotation.direction.contains(">")){
                //backwards but not forwards
                if(color == "black"){
                    generateNorthMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
                } else {
                    generateSouthMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
                }
            } else { //color-independent movements
                generateNorthMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
                generateSouthMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
            }

        }
    }

    /** forward: increase rank */
    fun generateNorthMovement(posOwnColor: Int,
                              bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                              positionFile: Int,
                              positionRank: Int,
                              movementNotation: ChessPiece.MovementNotation,
                              distance : Int) {
        for(newFile in positionFile+1..7){// ... inside board (between 0 and 7)
            if(abs(positionFile-newFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(newFile,positionRank)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newFile,positionRank,movementNotation)
                    break
                }
            }
            else break
        }
    }

    /** backward: decrease rank */
    fun generateSouthMovement(posOwnColor: Int,
                              bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                              positionFile: Int,
                              positionRank: Int,
                              movementNotation: ChessPiece.MovementNotation,
                              distance : Int) {
        for(newFile in positionFile-1 downTo 0){// ... inside board (between 0 and 7)
            if(abs(positionFile-newFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(newFile,positionRank)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newFile,positionRank,movementNotation)
                    break
                }
            }
            else break
        }
    }

    /** right: increase file */
    fun generateEastMovement(posOwnColor: Int,
                             bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                             positionFile: Int,
                             positionRank: Int,
                             movementNotation: ChessPiece.MovementNotation,
                             distance : Int) {
        for(newRank in positionRank+1..7){// ... inside board (between 0 and 7)
            if(abs(positionRank-newRank) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(positionFile,newRank)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,positionFile,newRank,movementNotation)
                    break
                }
            }
            else break
        }
    }

    /** left: decrease file */
    fun generateWestMovement(posOwnColor: Int,
                             bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                             positionFile: Int,
                             positionRank: Int,
                             movementNotation: ChessPiece.MovementNotation,
                             distance : Int) {
        //if coordinate is ...
        for(newRank in positionRank-1 downTo 0){// ... inside board (between 0 and 7)
            if(abs(positionRank-newRank) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val bbNewTarget = generate64BPositionFromCoordinates(positionFile,newRank)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,positionFile,newRank,movementNotation)
                    break
                }
            }
            else break
        }
    }

    /** generate all diagonal rider movements */
    fun generateDiagonalMovement(bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                 positionFile: Int,
                                 positionRank: Int,
                                 color: String,
                                 movementNotation : ChessPiece.MovementNotation)  {
        var distance = 7
        val posOwnColor = (color == "black").toInt()
        if(movementNotation.distances[0].matches("[0-9]".toRegex())){
            distance = movementNotation.distances[0].toInt()
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X>"){
            if (color == "black" && movementNotation.direction == "X>"){
                generateSouthEastDiagonalMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
                generateSouthWestDiagonalMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
            } else {
                generateNorthEastDiagonalMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
                generateNorthWestDiagonalMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
            }
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X<") {
            if (color == "black" && movementNotation.direction == "X>"){
                generateNorthEastDiagonalMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
                generateNorthWestDiagonalMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
            } else {
                generateSouthEastDiagonalMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
                generateSouthWestDiagonalMovement(posOwnColor,bbMovementMap, positionFile, positionRank, movementNotation, distance)
            }
        }
    }

    /** NorthEastDiagonalMovement: right,forward: increase file, increase rank*/
    fun generateNorthEastDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                          positionFile: Int,
                                          positionRank: Int,
                                          movementNotation: ChessPiece.MovementNotation,
                                          distance : Int) {
        var difFile = 1; var difRank = 1
        //if coordinate is ...
        while(positionFile+difFile <= 7 && positionRank+difRank <= 7) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = positionFile+difFile
                val newRank = positionRank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(newFile,newRank)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newFile,newRank,movementNotation)
                    break
                }
                ++difFile
                ++difRank
            } else break
        }
    }

    /** NorthWestDiagonalMovement: left,forward: increase file, decrease rank*/
    fun generateNorthWestDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                          positionFile: Int,
                                          positionRank: Int,
                                          movementNotation: ChessPiece.MovementNotation,
                                          distance : Int) {
        var difFile = 1; var difRank = -1
        //if coordinate is ...
        while(positionFile+difFile >= 0 && positionRank+difRank <= 7) {// ... inside board (between 0 and 7)
            if(abs(difFile) <= distance && abs(difRank) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = positionFile+difFile
                val newRank = positionRank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(newFile,newRank)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newFile,newRank,movementNotation)
                    break
                }
                ++difFile
                --difRank
            } else break
        }
    }

    /** SouthEastDiagonalMovement: right,backward: decrease file, increase rank*/
    fun generateSouthEastDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                          positionFile: Int,
                                          positionRank: Int,
                                          movementNotation: ChessPiece.MovementNotation,
                                          distance : Int) {
        var difFile = -1; var difRank = -1
        //if coordinate is ...
        while(positionFile+difFile <= 7 && positionRank+difRank >= 0) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){ // ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = positionFile+difFile
                val newRank = positionRank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(newFile,newRank)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newFile,newRank,movementNotation)
                    break
                }
                --difFile
                ++difRank
            } else break
        }
    }

    /** SouthWestDiagonalMovement: left,backward: decrease file, decrease rank*/
    fun generateSouthWestDiagonalMovement(posOwnColor: Int,
                                          bbMovementMap: MutableMap<ChessPiece.MovementNotation, ULong>,
                                          positionFile: Int,
                                          positionRank: Int,
                                          movementNotation: ChessPiece.MovementNotation,
                                          distance : Int) {
        var difRank = -1; var difFile = -1
        //if coordinate is ...
        while(positionFile+difFile >= 0 && positionRank+difRank >= 0) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                val newFile = positionFile+difFile
                val newRank = positionRank+difRank
                val bbNewTarget = generate64BPositionFromCoordinates(newFile,newRank)
                if(bbNewTarget and bbColorComposite[posOwnColor] == bbNewTarget){//figure of your own color => break
                    break
                }
                if(bbNewTarget and bbColorComposite[1-posOwnColor] == bbNewTarget){//figure of your opponent color => add position, then stop
                    addCoordinateToMovementBitboard(bbMovementMap,newFile,newRank,movementNotation)
                    break
                }
                --difRank
                --difFile
            } else break
        }
    }

    fun addCoordinateToMovementBitboard(bbMovementMap : MutableMap<ChessPiece.MovementNotation,ULong>,
                                        positionFile: Int,
                                        positionRank: Int,
                                        movementNotation: ChessPiece.MovementNotation){
        bbMovementMap[movementNotation] = bbMovementMap[movementNotation]!! or Bitboard.generate64BPositionFromCoordinates(
            positionFile,
            positionRank,
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
        for(row in 0..7){
             loop@ for(file in 0..7){
                for(key in bbFigures.keys){
                    val num = 2.0.pow(file * 8 + row).toULong()
                    val white = bbFigures[key]?.get(0) ?: 0uL
                    val black = bbFigures[key]?.get(1) ?: 0uL
                    if(white and num == num){
                        str.append(key[0].toUpperCase())
                        if(file < 7)str.append(" | ")
                        ++cnt
                        continue@loop
                    }
                    if(black and num == num){
                        str.append(key[0])
                        if(file < 7)str.append(" | ")
                        ++cnt
                        continue@loop
                    }
                }
                str.append(" ")
                if(file < 7)str.append(" | ")
                continue
            }
            str.append("\n")
        }
        println(cnt)
        return str.toString()
    }



    /** calculate the winner (if one exists yet)*/
    private fun checkForWinner() {
       if(bbFigures["king"]?.get(0) == 0uL){
           gameWinner = "black"
           gameFinished = true
       }
       if(bbFigures["king"]?.get(0) == 0uL){
           gameWinner = "white"
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
        promotion = arrayOf()
        for (row in 0..7) {
            val firstRow = 2.0.pow((7 * 0 + row).toDouble()).toULong()
            val lastRow = 2.0.pow((7 * 8 + row).toDouble()).toULong()
            if(bbFigures["pawn"]?.get(0)!! and lastRow == lastRow){
                promotion = arrayOf(7, row)
            }
            if(bbFigures["pawn"]?.get(1)!! and firstRow == lastRow){
                promotion = arrayOf(7, row)
            }
        }
    }

    /** calculate all points of black player */
    fun pointsBlack(): Int {
        var points = 0
        for(name in bbFigures.keys){
            points += figureMap[name]?.value?.times(countSetBits(bbFigures[name]?.get(0)!!)) ?: 0
        }
        return points
    }

    /** calculate all points of white player */
    fun pointsWhite(): Int {
        var points = 0
        for(name in bbFigures.keys){
            points += figureMap[name]?.value?.times(countSetBits(bbFigures[name]?.get(1)!!)) ?: 0
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

        fun generate64BPositionFromCoordinates(file: Int, rank: Int) : ULong {
            var pos : ULong = 1uL shl file*8 // pos = 2 ^ (file*8)
            pos = pos shl rank       // pos = pos * 2 ^ rank
            return pos         // pos = 2 ^ (file*8) * 2 ^ rank
        }

        fun add64BPositionFromCoordinates(_64B: ULong, file: Int, rank: Int) : ULong {
            return _64B or generate64BPositionFromCoordinates(file, rank)
        }

        fun generate64BPositionFromCoordinateList(list: List<Coordinate>) : ULong{
            var result = 0uL
            for(i in list.indices){
                result = if(i==0){
                    generate64BPositionFromCoordinates(list[i].file,list[i].rank)
                } else {
                    add64BPositionFromCoordinates(result,list[i].file,list[i].rank)
                }
            }
            return result
        }

        class Coordinate(val file: Int, val rank: Int){
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
fun deleteViolatesCondition(figureName : String,
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
            //en passante
            newBBFigureNonRelativeTargets = specialMoveCheckEnpassante(pos,bbFigure,bbFigureNonRelativeTargets,color,movementNotation)
        }
        if(movementNotation.movetype == "g") {//moves by leaping over piece to an empty square (if leaped over enemy => capture)
            newBBFigureNonRelativeTargets = leapMoveCheck(pos,bbFigure,bbFigureNonRelativeTargets,color,movementNotation)
        }
    }
    return newBBFigureNonRelativeTargets
}



todo: 16.07
1. fullfillsCondition einbinden
2. enpassante
3. castling
4. in chessgame einbinden und so movegeneration prüfen
5. draw rules implen

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
        var returnValue = bbFigureNonRelativeTargets
        return returnValue
    }

/** deletes the calculated enpassante moves that are illegal */
private fun specialMoveCheckEnpassante(pos: Int,
                                       bbFigure: ULong,
                                       bbFigureNonRelativeTargets: MutableMap<ChessPiece.MovementNotation,ULong>,
                                       color: String,
                                       movementNotation: ChessPiece.MovementNotation) : MutableMap<ChessPiece.MovementNotation,ULong> {
    if(moveHistory.isEmpty()){
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
private fun leapMoveCheck(pos: Int,
                          bbFigure: ULong,
                          bbFigureNonRelativeTargets: MutableMap<ChessPiece.MovementNotation,ULong>,
                          color: String) : MutableMap<ChessPiece.MovementNotation,ULong> {
    //TODO
    return bbFigureNonRelativeTargets
}





private fun generateSpecialMoveCheckCastling(color : String, bbFigureNonRelativeTargets:ULong) : ULong{
    var returnValue = bbFigureNonRelativeTargets
    return returnValue
}