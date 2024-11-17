package emerald.apps.fairychess.model.bitboard

import emerald.apps.fairychess.model.Movement

/**
 * A class that represents the game board using bitboards.
 *
 * @param figureMap A map that maps piece names to their corresponding indices.
 */
class Bitboard(val figureMap: Map<String, Int>) {
    companion object {
        const val BOARD_SIZE = 64
        const val RANK_SIZE = 8
        const val FILE_SIZE = 8
    }

    val pieceIndexMap = figureMap.keys.mapIndexed { index, piece -> piece to index }.toMap()
    var bbWhite: Long = 0L
    var bbBlack: Long = 0L
    var bbAll: Long = 0L

    /**
     * Converts a square notation (e.g., "a1", "h8") to a corresponding index in the bitboard.
     *
     * @param square The square notation.
     * @return The index of the square in the bitboard.
     */
    private fun squareToIndex(square: String): Int {
        val file = square[0] - 'a'
        val rank = square[1] - '1'
        return rank * RANK_SIZE + file
    }

    /**
     * Converts a bitboard index to a square notation (e.g., "a1", "h8").
     *
     * @param index The index in the bitboard.
     * @return The square notation.
     */
    private fun indexToSquare(index: Int): String {
        val file = index % FILE_SIZE
        val rank = index / RANK_SIZE
        return ('a' + file).toString() + (rank + 1).toString()
    }

    /**
     * Moves a piece on the game board according to the specified movement.
     *
     * @param color The color of the piece to be moved.
     * @param move The movement to be applied.
     */
    fun move(color: String, move: Movement) {
        val sourceIndex = squareToIndex(indexToSquare(move.sourceRank * RANK_SIZE + move.sourceFile))
        val targetIndex = squareToIndex(indexToSquare(move.targetRank * RANK_SIZE + move.targetFile))

        val sourceMask = 1L shl sourceIndex
        val targetMask = 1L shl targetIndex

        when (color) {
            "white" -> {
                bbWhite = (bbWhite and sourceMask.inv()) or targetMask
                bbAll = (bbAll and sourceMask.inv()) or targetMask
            }
            "black" -> {
                bbBlack = (bbBlack and sourceMask.inv()) or targetMask
                bbAll = (bbAll and sourceMask.inv()) or targetMask
            }
            else -> throw IllegalArgumentException("Invalid color: $color")
        }
    }

    /**
     * Sets a piece of the given color on the specified square.
     *
     * @param piece The name of the piece.
     * @param color The color of the piece, either "white" or "black".
     * @param square The square notation where the piece should be placed.
     * @throws IllegalArgumentException if the piece or color is invalid.
     */
    fun setPiece(piece: String, color: String, square: String) {
        val pieceIndex = pieceIndexMap[piece] ?: throw IllegalArgumentException("Invalid piece: $piece")
        val squareIndex = squareToIndex(square)
        val mask = 1L shl squareIndex

        when (color) {
            "white" -> {
                bbWhite = bbWhite or mask
                bbAll = bbAll or mask
            }
            "black" -> {
                bbBlack = bbBlack or mask
                bbAll = bbAll or mask
            }
            else -> throw IllegalArgumentException("Invalid color: $color")
        }
    }

    /**
     * Retrieves the piece at the specified square.
     *
     * @param square The square notation.
     * @return The name of the piece on the square, or null if the square is empty.
     */
    fun getPiece(square: String): String? {
        val squareIndex = squareToIndex(square)
        val mask = 1L shl squareIndex

        for ((piece, index) in pieceIndexMap) {
            if (bbWhite and mask != 0L || bbBlack and mask != 0L) {
                return piece
            }
        }
        return null
    }

    /**
     * Retrieves the color of the piece at the specified square.
     *
     * @param square The square notation.
     * @return The color of the piece on the square, either "white" or "black", or null if the square is empty.
     */
    fun getColor(square: String): String? {
        val squareIndex = squareToIndex(square)
        val mask = 1L shl squareIndex

        return when {
            bbWhite and mask != 0L -> "white"
            bbBlack and mask != 0L -> "black"
            else -> null
        }
    }

    /**
     * Checks if the specified square is occupied.
     *
     * @param square The square notation.
     * @return `true` if the square is occupied, `false` otherwise.
     */
    fun isOccupied(square: String): Boolean {
        val squareIndex = squareToIndex(square)
        return bbAll and (1L shl squareIndex) != 0L
    }

    /**
     * Applies a movement to the game board.
     *
     * @param movement The movement to be applied.
     */
    fun applyMovement(movement: Movement) {
        val sourceSquare = indexToSquare(movement.sourceRank * RANK_SIZE + movement.sourceFile)
        val targetSquare = indexToSquare(movement.targetRank * RANK_SIZE + movement.targetFile)
        val piece = getPiece(sourceSquare)
        val color = getColor(sourceSquare)

        if (piece != null && color != null) {
            removePiece(sourceSquare)
            setPiece(piece, color, targetSquare)
        }
    }

    /**
     * Removes a piece from the specified square.
     *
     * @param square The square notation.
     */
    private fun removePiece(square: String) {
        val squareIndex = squareToIndex(square)
        val mask = 1L shl squareIndex

        bbWhite = bbWhite and mask.inv()
        bbBlack = bbBlack and mask.inv()
        bbAll = bbAll and mask.inv()
    }

    /**
     * Resets the game board to its initial state.
     */
    fun resetBoard() {
        bbWhite = 0L
        bbBlack = 0L
        bbAll = 0L
    }

    /**
     * Generates a representation of the game board as a 2D character array.
     *
     * @return A 2D character array representing the game board.
     */
    fun generateBoard(): Array<CharArray> {
        val board = Array(RANK_SIZE) { CharArray(FILE_SIZE) }

        for (rank in 0 until RANK_SIZE) {
            for (file in 0 until FILE_SIZE) {
                val square = indexToSquare(rank * RANK_SIZE + file)
                val piece = getPiece(square)
                val color = getColor(square)

                board[rank][file] = when {
                    piece != null && color == "white" -> piece.uppercase().first()
                    piece != null && color == "black" -> piece.lowercase().first()
                    else -> '.'
                }
            }
        }
        return board
    }
}