package emerald.apps.fairychess.model.bitboard

/**
 * Utility class for working with bitboards.
 */
object BitboardUtils {
    private const val BOARD_SIZE = 64
    private const val RANK_SIZE = 8
    private const val FILE_SIZE = 8

    // Piece point values
    const val PAWN_POINTS = 1
    const val KNIGHT_POINTS = 3
    const val BISHOP_POINTS = 3
    const val ROOK_POINTS = 5
    const val QUEEN_POINTS = 9
    const val KING_POINTS = 0 // King is not counted towards the point total

    /**
     * Retrieves the name of the piece at the specified position in the bitboard.
     *
     * @param bitboard The Bitboard instance.
     * @param pos The position (index) in the bitboard.
     * @param bbFigure The bitboard representing the piece figures.
     * @return The name of the piece at the specified position, or null if the position is empty.
     */
    @JvmStatic
    fun getPieceName(bitboard: Bitboard, pos: Int, bbFigure: Long): String? {
        val mask = 1L shl pos
        for ((piece, index) in bitboard.figureMap) {
            if (bbFigure and mask != 0L) {
                return piece
            }
        }
        return null
    }

    /**
     * Retrieves the name of the piece at the specified coordinate on the game board.
     *
     * @param bitboard The Bitboard instance.
     * @param coordinate The coordinate of the square on the game board.
     * @return The name of the piece at the specified coordinate, or null if the square is empty.
     */
    @JvmStatic
    fun getPieceName(bitboard: Bitboard, coordinate: Coordinate): String? {
        val pos = bitboardPosition(coordinate.file, coordinate.rank)
        val bbWhite = bitboard.bbWhite
        val bbBlack = bitboard.bbBlack

        return when {
            bbWhite and (1L shl pos) != 0L -> bitboard.figureMap.keys.firstOrNull { bitboard.figureMap[it] == pos }
            bbBlack and (1L shl pos) != 0L -> bitboard.figureMap.keys.firstOrNull { bitboard.figureMap[it] == pos }?.lowercase()
            else -> null
        }
    }

    /**
     * Calculates the total points for the black player.
     *
     * @return The total points for the black player.
     */
    @JvmStatic
    fun pointsBlack(bitboard: Bitboard): Int {
        var points = 0
        for ((piece, index) in bitboard.pieceIndexMap) {
            if (bitboard.bbBlack and (1L shl index) != 0L) {
                points += when (piece) {
                    "p" -> PAWN_POINTS
                    "n" -> KNIGHT_POINTS
                    "b" -> BISHOP_POINTS
                    "r" -> ROOK_POINTS
                    "q" -> QUEEN_POINTS
                    else -> 0 // King is not counted towards the point total
                }
            }
        }
        return points
    }

    /**
     * Calculates the total points for the white player.
     *
     * @return The total points for the white player.
     */
    @JvmStatic
    fun pointsWhite(bitboard: Bitboard): Int {
        var points = 0
        for ((piece, index) in bitboard.pieceIndexMap) {
            if (bitboard.bbWhite and (1L shl index) != 0L) {
                points += when (piece) {
                    "P" -> PAWN_POINTS
                    "N" -> KNIGHT_POINTS
                    "B" -> BISHOP_POINTS
                    "R" -> ROOK_POINTS
                    "Q" -> QUEEN_POINTS
                    else -> 0 // King is not counted towards the point total
                }
            }
        }
        return points
    }

    private fun bitboardPosition(file: Int, rank: Int): Int {
        return rank * RANK_SIZE + file
    }
}