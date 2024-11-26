package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.Bitboard.Companion.Coordinate
import emerald.apps.fairychess.utility.FigureParser
import emerald.apps.fairychess.view.ChessActivity

class Chessboard(
    private val chessFormationArray: Array<Array<String>>?,
    val figureMap: Map<String, FigureParser.Figure>
) {
    private var bitboard: Bitboard
    var gameFinished = false
    var gameWinner = ""
    var playerWithDrawOpportunity = ""

    init {
        bitboard = Bitboard(chessFormationArray, figureMap)
    }

    fun checkMoveAndMove(color:String, movement: Movement) : String{
        return bitboard.checkMoveAndMove(color, movement)
    }

    fun getTargetMovementsAsMovementList(coordinate : Coordinate) : List<Movement> {
        return bitboard.getTargetMovementsAsMovementList(coordinate);
    }

    fun getMovecolor() : String{
        return bitboard.moveColor
    }

    fun getPieceColor(rank: Int, file: Int) : String{
        return bitboard.getPieceColor(rank, file)
    }

    fun getPieceName(coordinate: Coordinate) : String{
        return bitboard.getPieceName(coordinate)
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

    fun getPromotionCoordinate() : Coordinate? {
        return bitboard.promotionCoordinate
    }

    fun promotePawn(coordinate: Coordinate, name : String){
        return bitboard.promotePawn(coordinate, name)
    }

    fun cloneBitboard() : Bitboard{
        return bitboard.clone()
    }

}