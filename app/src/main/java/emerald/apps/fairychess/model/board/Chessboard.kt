package emerald.apps.fairychess.model.board

import android.util.Log
import emerald.apps.fairychess.controller.MainActivityListener

/** Important for all coordinate notations we use standard notation (file,rank) e.g. A1.
 *  "The columns of a chessboard are known as files, the rows are known as ranks ...
 *   files are labeled a through h from left to right, and ranks are labeled 1 through 8 from bottom to top" - Wikipedia
 *
 */
class Chessboard(
    val gameParameters: MainActivityListener.GameParameters
) {

    companion object {
        val gameBoardSizeMap = mapOf("clobber" to Pair(5,6)) //files,ranks

        fun getGameboardSize(variant: String) : Pair<Int,Int> {
            return if(gameBoardSizeMap.containsKey(variant)){
                gameBoardSizeMap[variant]!!
            } else {
                Pair(8,8)
            }
        }
    }
    var piecesCaptured: Int = 0
    var movesMade: Int = 0
    var gameDuration: Long = 0
    private val gameStartTime: Long = System.currentTimeMillis()  // Add game start timestamp

    private var movecolor: Color = Color.WHITE
    private var board: Array<Array<Pair<String, String>>> //file,rank
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
    external fun isCapture(variant: String, fen: String, move: String, chess960: Boolean = false): Boolean
    external fun isImmediateGameEnd(variant: String, fen: String, moves: Array<String>, chess960: Boolean = false): IntArray
    external fun hasInsufficientMaterial(variant: String, fen: String, moves: Array<String>, chess960: Boolean = false): BooleanArray
    external fun calcBestMove(variant: String, fen: String, depth: Int, movetime: Int, chess960: Boolean = false): String
    external fun setPosition(fen: String, chess960: Boolean)
    external fun isLegalMove(variant: String, fen: String, move: String, chess960: Boolean = false): Boolean
    external fun getGameResult(variant: String, fen: String, move: Array<String>, chess960: Boolean = false) : String
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
        val gameBoardSizeMap = getGameboardSize(gameParameters.name)
        board = Array(gameBoardSizeMap.first) { Array(gameBoardSizeMap.second) { Pair("", "") } } //[file][rank]
        System.loadLibrary("stockfish")
        initEngine()
        setupInitialPosition()
    }

    private fun setupInitialPosition() {
        fen = startFen(this.gameParameters.name)
        updateBoardState()
        setPosition(fen, this.gameParameters.name == "fischerandom")
        // Store the initial FEN
    }

    private fun updateBoardState() {
        // Update promotion coordinate if any
        extractPiecesFromFen(fen)
        //checkForPromotion(fen)
    }

    fun checkMove(movement: Movement) : Boolean {
        val fromSquare = fileRankToSquare(movement.sourceFile,movement.sourceRank)
        val toSquare = fileRankToSquare(movement.targetFile, movement.targetRank)
        val moveStr = "$fromSquare$toSquare"

        return isLegalMove(this.gameParameters.name, fen, moveStr)
    }

    fun checkMoveAndMove(movement: Movement): String {
        val fromSquare = fileRankToSquare(movement.sourceFile,movement.sourceRank)
        val toSquare = fileRankToSquare(movement.targetFile, movement.targetRank)
        val moveStr = "$fromSquare$toSquare"

        return if (isLegalMove(this.gameParameters.name, fen, moveStr)) {
            if(movecolor.stringValue == this.gameParameters.playerColor){
                val isCapture = isCapture(this.gameParameters.name, fen, moveStr, this.gameParameters.name == "fischerandom")
                if (isCapture) {
                    piecesCaptured++
                }
            }

            movecolor = if (movecolor == Color.WHITE) Color.BLACK else Color.WHITE
            fen = getFEN(this.gameParameters.name, fen, arrayOf(moveStr)) // update the FEN after the move
            ++movesMade  // Increment moves counter for statistic
            updateBoardState()
            ""
        } else {
            "illegal move"
        }
    }

    fun move(movement: Movement) {
        val fromSquare = fileRankToSquare(movement.sourceFile,movement.sourceRank)
        val toSquare = fileRankToSquare(movement.targetFile, movement.targetRank)
        val moveStr = "$fromSquare$toSquare"

        // Check if move is a capture
        if(movecolor.stringValue == this.gameParameters.playerColor){
            val isCapture = isCapture(this.gameParameters.name, fen, moveStr, this.gameParameters.name == "fischerandom")
            if (isCapture) {
                piecesCaptured++
            }
        }

        movecolor = if (movecolor == Color.WHITE) Color.BLACK else Color.WHITE
        fen = getFEN(this.gameParameters.name, fen, arrayOf(moveStr)) // update the FEN after the move
        updateBoardState()
    }

    fun getTargetMovementsAsMovementList(file: Int,rank: Int): List<Movement> {
        val square = fileRankToSquare(file,rank)
        val legalMoves = legalMoves(this.gameParameters.name, fen, this.gameParameters.name == "fischerandom")
            .filter { moveStr ->  moveStr[0] - 'a' == file && moveStr[1] - '1' == rank}
        return legalMoves
            .map { moveStr ->
                val targetFile = moveStr[2] - 'a'
                val targetRank = moveStr[3] - '1'
                Movement(file, rank, targetFile, targetRank)
            }
    }

    fun getMovecolor(): Color = movecolor

    fun getPieceColor(file: Int, rank: Int): String {
        return board[file][rank].second
    }

    fun getPieceName(file: Int, rank: Int): String {
        return board[file][rank].first
    }

    fun getPromotionCoordinate(): Coordinate? = promotionCoordinate

    fun promotePiece(coordinate: Coordinate, promotion: String) {
        if (coordinate == promotionCoordinate) {
            val square = fileRankToSquare(coordinate.rank, coordinate.file)
            val moveStr = "${square}${square}${promotion.lowercase()[0]}"
            if (isLegalMove(this.gameParameters.name, fen, moveStr)) {
                fen = getFEN(this.gameParameters.name, fen, arrayOf(moveStr))
                updateBoardState()
                promotionCoordinate = null
            }
        }
    }

    fun checkForGameEnd(): String {
        val result = getGameResult(this.gameParameters.name, fen, arrayOf(), (this.gameParameters.name == "fischerchess"))
        Log.i("Gameend?",result)
        return result
    }

    fun calcMove(fenString: String): Movement {
        val bestMove = calcBestMove(this.gameParameters.name, fenString, this.gameParameters.difficulty, 30000)
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

    fun extractPiecesFromFen(fen: String) {
        val gameboardSize = getGameboardSize(gameParameters.name)
        val fenParts = fen.split(" ")

        // Get piece placement section
        val ranks = fenParts[0].split("/")

        // Replace numbers with spaces to create fixedFiles
        val fixedRanks = ranks.map { file ->
            file.flatMap { char ->
                if (char.isDigit()) {
                    List(char.digitToInt()) { ' ' } // Replace number with equivalent spaces
                } else {
                    listOf(char)
                }
            }.joinToString("")
        }

        for (rank in 0..<gameboardSize.second) {
            var file= 0
            for (char in fixedRanks[rank]) {
                val piece = extractPieceFromChar(char)
                this.board[file][gameboardSize.second-1-rank] = piece
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
            PromotionMovement(sourceFile, sourceRank, targetFile, targetRank, moveStr[4].toString())
        } else {
            Movement(sourceFile, sourceRank, targetFile, targetRank)
        }
    }

    //TODO: get promotion from engine
    fun getPromotion() : String {
        return "queen"
    }

    // Add method to get current game duration
    fun getCurrentGameDuration(): Long {
        return System.currentTimeMillis() - gameStartTime
    }

    fun extractPieceFromChar(char: Char): Pair<String, String> {
        val color = if (char.isUpperCase()) "white" else "black"
        val pieceName = when (char.lowercase()) {
            "p" -> "pawn"
            "g" -> "grasshopper"
            "c" -> "chancellor"
            "n" -> "knight"
            "b" -> "bishop"
            "s" -> "sa" //Sa - Bishop in cambodian
            "r" -> "rook"
            "q" -> "queen"
            "m" -> "met" //Met - restricted queen in cambodian
            "k" -> "king"
            else -> ""
        }
        return Pair(pieceName, color)
    }

    // Add method to get game statistics
    fun getGameStats(): Triple<Int, Int, Long> {
        return Triple(
            piecesCaptured,
            movesMade,
            getCurrentGameDuration()
        )
    }

    fun updateFen(currentFen: String) {
        fen = currentFen
        updateBoardState()
        setPosition(fen, this.gameParameters.name == "fischerandom")
    }

    override fun toString(): String {
        val gameBoardSize = getGameboardSize(gameParameters.name)
        val files = gameBoardSize.first
        val ranks = gameBoardSize.second
        val sb = StringBuilder()

        // Add file labels at the top
        sb.append("   ") // Space for left margin
        for (file in 0 until files) {
            sb.append(" ${('A' + file)} ")
        }
        sb.append("\n")

        // Add top border
        sb.append("  ╔")
        for (file in 0 until files) {
            sb.append("═══")
            if (file < files - 1) sb.append("╦")
        }
        sb.append("╗\n")

        // Add board content with rank labels
        for (rank in ranks - 1 downTo 0) {
            sb.append("${rank + 1} ║") // Rank number and left border

            // Add pieces
            for (file in 0 until files) {
                val piece = board[file][rank]
                val symbol = when {
                    piece.first.isEmpty() -> " "
                    piece.second == "white" -> when(piece.first) {
                        "pawn" -> "P"
                        "knight" -> "N"
                        "bishop" -> "B"
                        "rook" -> "R"
                        "queen" -> "Q"
                        "king" -> "K"
                        "grasshopper" -> "G"
                        "chancellor" -> "C"
                        "sa" -> "S"
                        "met" -> "M"
                        else -> "?"
                    }
                    else -> when(piece.first) {
                        "pawn" -> "p"
                        "knight" -> "n"
                        "bishop" -> "b"
                        "rook" -> "r"
                        "queen" -> "q"
                        "king" -> "k"
                        "grasshopper" -> "g"
                        "chancellor" -> "c"
                        "sa" -> "s"
                        "met" -> "m"
                        else -> "?"
                    }
                }
                sb.append(" $symbol ")

                // Add vertical border
                if (file < files - 1) sb.append("║")
            }
            sb.append("║\n") // Right border

            // Add rank separator, except for the last rank
            if (rank > 0) {
                sb.append("  ╠")
                for (file in 0 until files) {
                    sb.append("═══")
                    if (file < files - 1) sb.append("╬")
                }
                sb.append("╣\n")
            }
        }

        // Add bottom border
        sb.append("  ╚")
        for (file in 0 until files) {
            sb.append("═══")
            if (file < files - 1) sb.append("╩")
        }
        sb.append("╝\n")

        // Add current state information
        sb.append("\nMove: ${movecolor.stringValue}")
        sb.append("\nFEN: $fen")

        return sb.toString()
    }

}