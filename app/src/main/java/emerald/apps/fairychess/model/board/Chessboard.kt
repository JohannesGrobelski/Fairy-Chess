package emerald.apps.fairychess.model.board

class Chessboard(
    var variant: String,
    var difficulty: Int
) {


    private var movecolor: Color = Color.WHITE
    private var board: Array<Array<Pair<String, String>>> = Array(8) { Array(8) { Pair("", "") } } //[rank][file]
    private var promotionCoordinate: Coordinate? = null
    private lateinit var fen: String

    // Constants
    var VALUE_MATE: Int = 0
    var VALUE_DRAW: Int = 0
    var NOTATION_DEFAULT: Int = 0
    var NOTATION_SAN: Int = 0
    var NOTATION_LAN: Int = 0
    var NOTATION_SHOGI_HOSKING: Int = 0
    var NOTATION_SHOGI_HODGES: Int = 0
    var NOTATION_SHOGI_HODGES_NUMBER: Int = 0
    var NOTATION_JANGGI: Int = 0
    var NOTATION_XIANGQI_WXF: Int = 0
    var NOTATION_THAI_SAN: Int = 0
    var NOTATION_THAI_LAN: Int = 0
    var FEN_OK: Int = 0

    // Native method declarations
    external fun initEngine(): Int
    external fun version(): IntArray
    external fun info(): String
    external fun variants(): Array<String>
    external fun setOption(name: String, value: String)
    external fun loadVariantConfig(config: String)
    external fun startFen(variant: String): String
    external fun twoBoards(variant: String): Boolean
    external fun capturesToHand(variant: String): Boolean
    external fun legalMoves(variant: String, fen: String, chess960: Boolean = false): Array<String>
    external fun givesCheck(variant: String, fen: String, moves: Array<String>, chess960: Boolean = false): Boolean
    external fun isCapture(variant: String, fen: String, moves: Array<String>, move: String, chess960: Boolean = false): Boolean
    external fun isImmediateGameEnd(variant: String, fen: String, moves: Array<String>, chess960: Boolean = false): IntArray
    external fun hasInsufficientMaterial(variant: String, fen: String, moves: Array<String>, chess960: Boolean = false): BooleanArray
    external fun calcBestMove(variant: String, fen: String, depth: Int, movetime: Int, chess960: Boolean = false): String
    external fun setPosition(fen: String, chess960: Boolean)
    external fun isLegalMove(variant: String, fen: String, move: String, chess960: Boolean = false): Boolean
    external fun getGameResult() : Int
    external fun getFEN(
        variant: String,
        fen: String,
        moves: Array<String>,
        chess960: Boolean = false,
        sfen: Boolean = false,
        showPromoted: Boolean = false,
        countStarted: Int = 0
    ): String

    init {
        System.loadLibrary("stockfish")
        initEngine()
        setupInitialPosition()
    }

    private fun setupInitialPosition() {
        fen = startFen(this.variant)
        updateBoardState()
        setPosition(fen, variant == "fischerandom")
        // Store the initial FEN
    }

    private fun updateBoardState() {
        // Update promotion coordinate if any
        extractPiecesFromFen(fen)
        //checkForPromotion(fen)
    }

    fun checkMove(movement: Movement) : Boolean {
        val fromSquare = fileRankToSquare(movement.sourceRank, movement.sourceFile)
        val toSquare = fileRankToSquare(movement.targetRank, movement.targetFile)
        val moveStr = "$fromSquare$toSquare"

        return isLegalMove(variant, fen, moveStr)
    }

    fun checkMoveAndMove(movement: Movement): String {
        val fromSquare = fileRankToSquare(movement.sourceRank, movement.sourceFile)
        val toSquare = fileRankToSquare(movement.targetRank, movement.targetFile)
        val moveStr = "$fromSquare$toSquare"

        return if (isLegalMove(variant, fen, moveStr)) {
            movecolor = if (movecolor == Color.WHITE) Color.BLACK else Color.WHITE
            fen = getFEN(this.variant, fen, arrayOf(moveStr)) // update the FEN after the move
            updateBoardState()
            ""
        } else {
            "illegal move"
        }
    }

    fun move(movement: Movement) {
        val fromSquare = fileRankToSquare(movement.sourceFile,movement.sourceRank)
        val toSquare = fileRankToSquare(movement.targetFile,movement.targetRank)
        val moveStr = "$fromSquare$toSquare"

        movecolor = if (movecolor == Color.WHITE) Color.BLACK else Color.WHITE
        fen = getFEN(this.variant, fen, arrayOf(moveStr)) // update the FEN after the move
        updateBoardState()
    }

    fun getTargetMovementsAsMovementList(rank: Int, file: Int): List<Movement> {
        val square = fileRankToSquare(rank, file)
        val legalMoves = legalMoves(this.variant, fen, variant == "fischerandom")
            .filter { moveStr ->  moveStr[0] - 'a' == rank && moveStr[1] - '1' == file}
        return legalMoves
            .map { moveStr ->
                val targetRank = moveStr[2] - 'a'
                val targetFile = moveStr[3] - '1'
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
            if (isLegalMove(variant, fen, moveStr)) {
                fen = getFEN(this.variant, fen, arrayOf(moveStr))
                updateBoardState()
                promotionCoordinate = null
            }
        }
    }

    fun checkForWinner(): Color? {
        // Split the FEN string into its components
        val parts = fen.split(" ")

        // FEN has six required fields. The optional result field may follow
        if (parts.size < 7) return null

        val result = parts[6] // Get the result field

        return when (result) {
            "1-0" -> Color.WHITE
            "0-1" -> Color.BLACK
            "1/2-1/2" -> null
            else -> null
        }
    }

    fun calcMove(fenString: String): Movement {
        val bestMove = calcBestMove(this.variant, fenString, difficulty, 30000)
        return transformStringToMovement(bestMove)
    }

    fun checkForPlayerWithDrawOpportunity(): Color? {
        // Check for draw opportunities (stalemate, threefold repetition, etc.)
        //return if (getGameResult() == 0) movecolor else null
        return null
    }

    fun getCurrentFEN(): String {
        return fen
    }

    private fun fileRankToSquare(file: Int, rank: Int): String {
        return "${'a' + file}${rank + 1}"
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

    fun extractPiecesFromFen(fen: String) {
        val fenParts = fen.split(" ")

        // Get piece placement section
        val files = fenParts[0].split("/")

        // Replace numbers with spaces to create fixedFiles
        val fixedFiles = files.map { rank ->
            rank.flatMap { char ->
                if (char.isDigit()) {
                    List(char.digitToInt()) { ' ' } // Replace number with equivalent spaces
                } else {
                    listOf(char)
                }
            }.joinToString("")
        }

        for (rank in 0..7) {
            var file = 0
            for (char in fixedFiles[rank]) {
                val piece = extractPieceFromChar(char)
                this.board[file][7-rank] = piece
                file++ // Move to next file
            }
        }
        System.out.println(board.toString())
    }

    private fun transformStringToMovement(moveStr: String): Movement {
        // UCI format is like "e2e4" or "e7e8q" for promotion
        if (moveStr.length < 4) return Movement.emptyMovement()

        val sourceFile = moveStr[0] - 'a'
        val sourceRank = moveStr[1] - '1'
        val targetFile = moveStr[2] - 'a'
        val targetRank = moveStr[3] - '1'

        return if (moveStr.length == 5) {
            PromotionMovement(sourceRank, sourceFile, targetRank, targetFile, moveStr[4].toString())
        } else {
            Movement(sourceRank, sourceFile, targetRank, targetFile)
        }
    }

    //TODO: get promotion from engine
    fun getPromotion() : String {
        return "queen"
    }


    fun extractPieceFromChar(char: Char): Pair<String, String> {
        val color = if (char.isUpperCase()) "white" else "black"
        val pieceName = when (char.lowercase()) {
            "p" -> "pawn"
            "g" -> "grasshopper"
            "c" -> "chancellor"
            "n" -> "knight"
            "b" -> "bishop"
            "r" -> "rook"
            "q" -> "queen"
            "k" -> "king"
            else -> ""
        }
        return Pair(pieceName, color)
    }
}