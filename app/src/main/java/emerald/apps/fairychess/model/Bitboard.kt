package emerald.apps.fairychess.model

import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
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
                            val pos = (color == "black").toInt()
                            bbFigures[name]?.set(
                                pos, add64BPositionFromCoordinates(
                                    bbFigures[name]?.get(
                                        pos
                                    )!!, file, rank
                                )
                            )
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

        fun generateBitboardFromChessboard(chessboard: Chessboard) : Bitboard{
            val chessFormationArray = parseChessFormation("normal_chess")
            val figureMap = parseFigureMapFromFile()
            val bitboard = Bitboard(chessFormationArray, figureMap)
            for(file in chessboard.pieces.indices){
                for (rank in chessboard.pieces.indices){
                    if(chessboard.pieces[file][rank].color.isEmpty())continue
                    val pos = (chessboard.pieces[file][rank].color == "black").toInt()
                    bitboard.bbFigures[chessFormationArray[file][rank]]?.set(
                        pos, generate64BPositionFromCoordinates(
                            file,
                            rank
                        )
                    )
                }
            }
            return bitboard
        }

        fun generateChessboardFromBitboard(bitboard: Bitboard) : Chessboard {
            val chessFormationArray = parseChessFormation("normal_chess")
            val figureMap = parseFigureMapFromFile()
            val chessboard = Chessboard(chessFormationArray, figureMap)

            var pieces: Array<Array<ChessPiece>> = Array(8) {
                Array(8) {
                    ChessPiece("", -1, -1, 0, "", "", 0)
                }
            }



            chessboard.pieces = pieces
            return chessboard
        }


        fun generate64BPositionFromCoordinates(line: Int, row: Int) : ULong {
            var pos : ULong = 1uL shl line*8 // pos = 2 ^ (line*8)
            pos = pos shl row       // pos = pos * 2 ^ row
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
