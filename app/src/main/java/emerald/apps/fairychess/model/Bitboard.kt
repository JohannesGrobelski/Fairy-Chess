package emerald.apps.fairychess.model

import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.pow
import kotlin.math.sign

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
    val moveHistory = mutableListOf<ChessPiece.Movement>()


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
        }
    }


    /** return a list of all posible moves for player of @color as bitmap*/
    fun getAllPossibleMoves(color : String) : Map<Int,ULong> {
        val allPossibleMoves = mutableMapOf<Int,ULong>()
        val pos = ("black" == color).toInt()
        for(file in 0..7){
            for(rank in 0..7){
                val bbPosition = generate64BPositionFromCoordinates(file,rank)
                if(bbColorComposite[pos] and bbPosition == bbPosition){
                    allPossibleMoves[file*10 + rank] = getTargetMovements(color,file,rank,true)
                }
            }
        }
        return allPossibleMoves.toMap()
    }

    /** generate a bitboard representing the target squares of the non relative movement for a piece */
    fun generateNonRelativeMovements(color: String, sourceFile:Int, sourceRank:Int) : ULong {
        val name = ""
        val bitboard = 0uL
        val
        return BitboardChessPiece.generateNonRelativeMovements(name,bitboard)
    }

    /**
     * return a list of possible movements of the figure at (sourceFile,sourceRank)
     * */
    fun getTargetMovements(color: String,sourceFile:Int, sourceRank:Int, genCastlingMoves:Boolean) : ULong {
        val nonRelativeMovements = generateNonRelativeMovements(color,sourceFile,sourceRank)
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
        moveHistory.add(movement)
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

        fun generate64BPositionFromCoordinates(file: Int, rank: Int) : ULong {
            var pos : ULong = 1uL shl file*8 // pos = 2 ^ (line*8)
            pos = pos shl rank       // pos = pos * 2 ^ row
            return pos         // pos = 2 ^ (line*8) * 2 ^ row
        }

        fun add64BPositionFromCoordinates(_64B: ULong, line: Int, row: Int) : ULong {
            return _64B or generate64BPositionFromCoordinates(line, row)
        }

        class Coordinate(val line: Int, val row: Int){
            override fun equals(other: Any?): Boolean {
                if(other is Coordinate){
                    return (line == other.line) && (row == other.row)
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
