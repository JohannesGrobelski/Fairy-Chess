package emerald.apps.fairychess.model.board

import emerald.apps.fairychess.utility.FigureParser

class Chessboard(
    private val chessFormationArray: Array<Array<String>>?,
    val figureMap: Map<String, FigureParser.Figure>
) {
    init {
        System.loadLibrary("stockfish")
    }

    private var movecolor: Color = Color.WHITE
    private var board: Array<Array<Pair<String, String>>> = Array(8) { Array(8) { Pair("", "") } }
    private var promotionCoordinate: Coordinate? = null
    private var fen: String = "" // store the current FEN string

    // Native method declarations
    private external fun initializeEngine()
    private external fun setPosition(fen: String)
    private external fun getLegalMoves(square: String): Array<String>
    private external fun makeMove(move: String): Boolean
    private external fun getCurrentFen(): String // get the current FEN
    private external fun getGameResult(): Int // 1 for white, -1 for black, 0 for draw, 2 for ongoing
    private external fun getPiece(squareStr : String): Pair<String,String> // 1 for white, -1 for black, 0 for draw, 2 for ongoing

    init {
        initializeEngine()
        setupInitialPosition()
    }

    private fun setupInitialPosition() {
        if (chessFormationArray != null) {
            // Convert your formation array to FEN and set it
            val fen = convertFormationToFen(chessFormationArray)
            setPosition(fen)
            updateBoardState()
        } else {
            // Start with standard chess position
            setPosition("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
            updateBoardState()
        }
        // Store the initial FEN
        fen = getCurrentFen()
    }

    private fun updateBoardState() {
        // Update internal board representation from Stockfish
        for (rank in 0..7) {
            for (file in 0..7) {
                val square = fileRankToSquare(file, rank)
                val (piece, color) = getPiece(square)
                board[rank][file] = Pair(piece, color)
            }
        }

        // Update promotion coordinate if any
        checkForPromotion(getCurrentFen())
    }

    fun checkMoveAndMove(color: Color, movement: Movement): String {
        val fromSquare = fileRankToSquare(movement.sourceFile, movement.sourceRank)
        val toSquare = fileRankToSquare(movement.targetFile, movement.targetRank)
        val moveStr = "$fromSquare$toSquare"

        return if (makeMove(moveStr)) {
            updateBoardState()
            movecolor = if (movecolor == Color.WHITE) Color.BLACK else Color.WHITE
            fen = getCurrentFen() // update the FEN after the move
            ""
        } else {
            "illegal move"
        }
    }

    fun getTargetMovementsAsMovementList(rank: Int, file: Int): List<Movement> {
        val square = fileRankToSquare(file, rank)
        val legalMoves = getLegalMoves(square)

        return legalMoves.map { moveStr ->
            val targetFile = moveStr[2] - 'a'
            val targetRank = moveStr[3] - '1'
            Movement(rank, file, targetRank, targetFile)
        }
    }

    fun getMovecolor(): Color = movecolor

    fun getPieceColor(rank: Int, file: Int): String {
        return board[rank][file].second
    }

    fun getPieceName(rank: Int, file: Int): String {
        return board[rank][file].first
    }

    fun getPromotionCoordinate(): Coordinate? = promotionCoordinate

    fun promotePiece(coordinate: Coordinate, promotion: String) {
        if (coordinate == promotionCoordinate) {
            val square = fileRankToSquare(coordinate.file, coordinate.rank)
            val moveStr = "${square}${square}${promotion.lowercase()[0]}"
            if (makeMove(moveStr)) {
                updateBoardState()
                fen = getCurrentFen() // update the FEN after the promotion
                promotionCoordinate = null
            }
        }
    }

    fun checkForWinner(): Color? {
        return when (getGameResult()) {
            1 -> Color.WHITE
            -1 -> Color.BLACK
            else -> null
        }
    }

    fun checkForPlayerWithDrawOpportunity(): Color? {
        // Check for draw opportunities (stalemate, threefold repetition, etc.)
        return if (getGameResult() == 0) movecolor else null
    }

    fun getCurrentFEN(): String {
        return fen
    }

    private fun fileRankToSquare(file: Int, rank: Int): String {
        return "${'a' + file}${rank + 1}"
    }


    private fun convertFormationToFen(formation: Array<Array<String>>): String {
        val sb = StringBuilder()

        for (rank in 7 downTo 0) {
            var emptyCount = 0
            for (file in 0..7) {
                val piece = formation[rank][file]
                if (piece.isEmpty()) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    // Convert piece name to FEN character
                    val fenChar = when (piece.lowercase()) {
                        "pawn" -> 'p'
                        "knight" -> 'n'
                        "bishop" -> 'b'
                        "rook" -> 'r'
                        "queen" -> 'q'
                        "king" -> 'k'
                        else -> 'p' // Default for fairy pieces
                    }
                    // Determine piece color based on file (as per your original logic)
                    val isWhite = file <= 4
                    sb.append(if (isWhite) fenChar.uppercase() else fenChar)
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (rank > 0) sb.append('/')
        }

        // Add other FEN components
        sb.append(" w KQkq - 0 1")
        return sb.toString()
    }

    private fun checkForPromotion(fen: String) {
        // Check if there's a pawn on the last rank
        val ranks = fen.split(" ")[0].split("/")
        for (file in 0..7) {
            // Check first rank for black pawns
            if (ranks[0][file] == 'p') {
                promotionCoordinate = Coordinate(file, 0)
                return
            }
            // Check last rank for white pawns
            if (ranks[7][file] == 'P') {
                promotionCoordinate = Coordinate(file, 7)
                return
            }
        }
        promotionCoordinate = null
    }
}