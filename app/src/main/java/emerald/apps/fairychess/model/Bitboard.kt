package emerald.apps.fairychess.model

import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class Bitboard {

    //2D-array of chesspieces to represent board (starting with H8 moving rows first und lines down to A1)
    //thus a bitboard represents H8H7H6H5H4H3H2H1G8G7G6....B2B1A8A7A6A5A4A3A2A1
    //first element are white figures, second black figures
    var bbRooks : Array<Long> = arrayOf(0L,0L)
    var bbKnights : Array<Long> = arrayOf(0L,0L)
    var bbBishops : Array<Long> = arrayOf(0L,0L)
    var bbKing : Array<Long> = arrayOf(0L,0L)
    var bbQueens : Array<Long> = arrayOf(0L,0L)
    var bbPawns : Array<Long> = arrayOf(0L,0L)

    companion object {
        fun generateBitboardFromChessboard(chessboard: Chessboard) : Bitboard{
            val bitboard = Bitboard()
            for(line in chessboard.pieces.indices){
                for (row in chessboard.pieces.indices){
                    if(chessboard.pieces[line][row].color.isEmpty())continue
                    val pos = (chessboard.pieces[line][row].color == "black").toInt()
                    when(chessboard.pieces[line][row].name){
                        "king" -> {bitboard.bbKing[pos] = bitboard.bbKing[pos] xor generate64BPositionFromCoordinates(line,row)}
                        "queen" -> {bitboard.bbQueens[pos] = bitboard.bbQueens[pos] xor generate64BPositionFromCoordinates(line,row)}
                        "rook" -> {bitboard.bbRooks[pos] = bitboard.bbRooks[pos] xor generate64BPositionFromCoordinates(line,row)}
                        "bishop" -> {bitboard.bbBishops[pos] = bitboard.bbBishops[pos] xor generate64BPositionFromCoordinates(line,row)}
                        "knight" -> {bitboard.bbKnights[pos] = bitboard.bbKnights[pos] xor generate64BPositionFromCoordinates(line,row)}
                        "pawn" -> {bitboard.bbPawns[pos] = bitboard.bbPawns[pos] xor generate64BPositionFromCoordinates(line,row)}
                    }
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
                    ChessPiece("", -1,-1, 0, "", "", 0)
                }
            }

            asf

            chessboard.pieces = pieces
            return chessboard
        }


        fun generate64BPositionFromCoordinates(line:Int, row:Int) : Long{
            var pos = 1L shl 8*line
            pos = pos shl row
            return pos
        }

        class Coordinate(val line:Int, val row: Int)
        fun generateCoordinatesFrom64BPosition(long: Long) : MutableList<Coordinate> {
            var pos = long
            var cnt = 0
            val coordinateList = mutableListOf<Coordinate>()
            for(i in 0..64){
                if(pos and 1L == 1L)coordinateList.add(Coordinate(cnt / 8, (cnt/8) % 8))
                pos = pos shr 1
                ++cnt
            }
            return coordinateList
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


        private fun parseChessFormation(mode:String) : Array<Array<String>> {
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
