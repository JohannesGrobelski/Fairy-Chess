package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.bitboard.Bitboard
import emerald.apps.fairychess.view.ChessActivity

data class Coordinate(val file: Int, val rank: Int)

class ChessBoard(private val figureMap: Map<String, Int>) {
    //state
    var bitboard = Bitboard(figureMap)
    var moveColor = "white"
    var gameFinished = false
    var bbMovedCaptured: Long = 0L
    var gameWinner = ""
    var playerWithDrawOpportunity = ""
    var promotionCoordinate: Coordinate? = null

    //history
    val boardStateHistory = mutableListOf<Map<String, LongArray>>() // move history, for each move map (figureName -> Bitboard)
    val movedCapturedHistory = mutableListOf<Long>() // history of bbMovedCaptured for each move
    val blackCapturedPieces = mutableListOf<ChessActivity.CapturedPiece>()
    val whiteCapturedPieces = mutableListOf<ChessActivity.CapturedPiece>()

    fun setPiece(piece: String, color: String, square: String) {
        bitboard.setPiece(piece, color, square)
    }

    fun getPiece(square: String): String? {
        return bitboard.getPiece(square)
    }

    fun getColor(square: String): String? {
        return bitboard.getColor(square)
    }

    fun isOccupied(square: String): Boolean {
        return bitboard.isOccupied(square)
    }

    fun move(color: String, move: Movement) {
        bitboard.move(color, move)
    }

    fun generateBoard(): Array<CharArray> {
        return bitboard.generateBoard()
    }

    fun resetBoard() {
        bitboard.resetBoard()
        gameFinished = false
        bbMovedCaptured = 0L
        gameWinner = ""
        playerWithDrawOpportunity = ""
        promotionCoordinate = null
        moveColor = "white"
        boardStateHistory.clear()
        movedCapturedHistory.clear()
        blackCapturedPieces.clear()
        whiteCapturedPieces.clear()
    }
}