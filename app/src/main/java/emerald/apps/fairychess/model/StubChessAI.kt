package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.pieces.Chessboard
import java.util.*
import kotlin.math.sign

class StubChessAI {
    //Settings
    private val algorithm = "alphabeta"
    private val recursionDepth = 2
    private var cntHashHits = 0
    private var cntHashFails = 0

    //Fields
    private var cnt_movements = 0
    private val chessBoardAI: Chessboard? = null

    private var opponentMover : OpponentMover
    private var color: String

    constructor(color: String, opponentMover: OpponentMover) {
        this.opponentMover = opponentMover
        this.color = color
    }

    //Fields for move ordering
    private val transpositionTable = Hashtable<String, MovementValue>()

    fun moveRandomFigure(chessgame: Chessgame) {
        var randomSourceFile : Int
        var randomSourceRank : Int
        var targetMovements : List<ChessPiece.Movement>
        do {
            do {
                randomSourceFile = (Math.random() * 8).toInt()
                randomSourceRank = (Math.random() * 8).toInt()
            } while(
                chessgame.getPieceColor(randomSourceFile, randomSourceRank) != this.color
            )
            targetMovements = chessgame.getTargetMovements(randomSourceFile, randomSourceRank)
        } while (targetMovements.isEmpty())

        opponentMover.onOpponentMove(targetMovements[(Math.random() * targetMovements.size).toInt()])
    }

    fun moveFigure(chessgame: Chessgame){
        var movementValue : MovementValue? = null
        when (algorithm) {
            "alphabeta" -> movementValue = alpha_beta(
                recursionDepth, color,
                chessgame.getChessboard(), Int.MIN_VALUE, Int.MAX_VALUE
            )
            "alphabetatrans" -> movementValue = alpha_beta_trans(
                recursionDepth, color,
                chessgame.getChessboard(), Int.MIN_VALUE, Int.MAX_VALUE
            )
        }
        if(movementValue?.movement != null){
            opponentMover.onOpponentMove(movementValue.movement!!)
        }
    }


    class MovementValue(val movement: ChessPiece.Movement?, val value: Int)

    private fun alpha_beta_trans(
        tiefe: Int,
        color: String,
        chessboard: Chessboard,
        alpha: Int,
        beta: Int
    ): MovementValue? {
        var _chessboard = chessboard
        var alpha = alpha
        var beta = beta
        ++cnt_movements
        var bestValue = 0
        var bestMove : MovementValue? = null
        var currentValue: Int
        if (!transpositionTable.containsKey(color + _chessboard.hashCode())) {
            ++cntHashFails
            val possible_moves: List<ChessPiece.Movement> = _chessboard.getAllPossibleMoves(color)
            if (tiefe == 0 || possible_moves.isEmpty()) {
                return MovementValue(null, _chessboard.points_black() - _chessboard.points_white())
            }
            if (color == this.color) {
                bestValue = Int.MIN_VALUE
                for (zug in possible_moves) {
                    val geschlagen: ChessPiece? = _chessboard.moveAndReturnCapture(color, zug)
                    val chessboardCopy = _chessboard.copy()
                    val currentBestMove = alpha_beta_trans(
                        tiefe - 1,
                        _chessboard.oppositeColor(color),
                        _chessboard,
                        alpha,
                        beta
                    )
                    currentValue = currentBestMove?.value ?: 0
                    _chessboard = chessboardCopy.copy()
                    if (currentValue > bestValue) {
                        bestValue = currentValue
                        bestMove = MovementValue(zug, bestValue)
                    }
                    alpha = Math.max(alpha, currentValue)
                    if (alpha >= beta) break
                }
            } else {
                bestValue = Int.MAX_VALUE
                for (zug in possible_moves) {
                    val geschlagen: ChessPiece? = _chessboard.moveAndReturnCapture(color, zug)
                    val chessboardCopy = _chessboard.copy()
                    val currentBestMove = alpha_beta_trans(
                        tiefe - 1,
                        _chessboard.oppositeColor(color),
                        _chessboard,
                        alpha,
                        beta
                    )
                    currentValue = currentBestMove?.value ?: 0
                    _chessboard = chessboardCopy.copy()
                    if (currentValue < bestValue) {
                        bestValue = currentValue
                        bestMove = MovementValue(zug, bestValue)
                    }
                    beta = Math.min(beta, currentValue)
                    if (alpha >= beta) break
                }
            }
            transpositionTable.put(color + _chessboard.hashCode(), bestMove)
        } else {
            ++cntHashHits
            bestMove = transpositionTable[color + _chessboard.hashCode()]!!
        }
        return bestMove
    }

    private fun alpha_beta(
        tiefe: Int,
        color: String,
        chessboard: Chessboard,
        alpha: Int,
        beta: Int
    ): MovementValue? {
        var _chessboard = chessboard
        var alpha = alpha
        var beta = beta
        ++cnt_movements
        val possible_moves: List<ChessPiece.Movement> = _chessboard.getAllPossibleMoves(color)
        if (tiefe == 0 || possible_moves.isEmpty()) {
            return MovementValue(null, _chessboard.points_black() - _chessboard.points_white())
        }
        var bestValue = 0
        var bestMove : MovementValue? = null
        var currentValue: Int
        if (color == this.color) {
            bestValue = Int.MIN_VALUE
            for (zug in possible_moves) {
                val geschlagen: ChessPiece? = _chessboard.moveAndReturnCapture(color, zug)
                val chessboardCopy = _chessboard.copy()
                val currentBestMove = alpha_beta_trans(
                    tiefe - 1,
                    _chessboard.oppositeColor(color),
                    _chessboard,
                    alpha,
                    beta
                )
                currentValue = currentBestMove?.value ?: 0
                _chessboard = chessboardCopy.copy()
                if (currentValue > bestValue) {
                    bestValue = currentValue
                    bestMove = MovementValue(zug, bestValue)
                }
                alpha = Math.max(alpha, currentValue)
                if (alpha >= beta) break
            }
        } else {
            bestValue = Int.MAX_VALUE
            for (zug in possible_moves) {
                val geschlagen: ChessPiece? = _chessboard.moveAndReturnCapture(color, zug)
                val chessboardCopy = _chessboard.copy()
                val currentBestMove = alpha_beta_trans(
                    tiefe - 1,
                    _chessboard.oppositeColor(color),
                    _chessboard,
                    alpha,
                    beta
                )
                currentValue = currentBestMove?.value ?: 0
                _chessboard = chessboardCopy.copy()
                if (currentValue < bestValue) {
                    bestValue = currentValue
                    bestMove = MovementValue(zug, bestValue)
                }
                beta = Math.min(beta, currentValue)
                if (alpha >= beta) break
            }
        }
        return bestMove
    }

    fun Chessboard.moveAndReturnCapture(color: String, movement: ChessPiece.Movement) : ChessPiece?{
        var capturedChessPiece : ChessPiece?
        if(color != moveColor)return null
        //check movement
        var userMovement : ChessPiece.Movement? = null
        if(pieces[movement.sourceFile][movement.sourceRank].color == "")return null
        else if(pieces[movement.sourceFile][movement.sourceRank].color == pieces[movement.targetFile][movement.targetRank].color)return null
        else if(pieces[movement.sourceFile][movement.sourceRank].color != moveColor)return null
        else {
            val targetMovements = getTargetMovements(movement.sourceFile, movement.sourceRank)
            for(targetMovement in targetMovements){
                if(targetMovement.targetFile == movement.targetFile && targetMovement.targetRank == movement.targetRank){
                    userMovement = targetMovement
                }
            }
            if(userMovement == null)return null
        }

        //valid movement
        if (userMovement.movementNotation.movetype == "g") {
            //capture the piece hopped over
            val signFile = sign((userMovement.targetFile - userMovement.sourceFile).toDouble()).toInt()
            val signRank = sign((userMovement.targetRank - userMovement.sourceRank).toDouble()).toInt()
            val captureFile = userMovement.targetFile-signFile
            val captureRank = userMovement.targetRank-signRank
            if(pieces[captureFile][captureRank].color != moveColor){
                pieces[captureFile][captureRank] = ChessPiece(
                    "",
                    movement.sourceFile,
                    movement.sourceRank,
                    0,
                    "",
                    "",
                    0,
                )
            }
        }
        capturedChessPiece = pieces[movement.targetFile][movement.targetRank]
        pieces[movement.targetFile][movement.targetRank] = ChessPiece(
            pieces[movement.sourceFile][movement.sourceRank].name,
            movement.targetFile,
            movement.targetRank,
            pieces[movement.sourceFile][movement.sourceRank].value,
            pieces[movement.sourceFile][movement.sourceRank].color,
            pieces[movement.sourceFile][movement.sourceRank].movingPatternString,
            pieces[movement.sourceFile][movement.sourceRank].moveCounter + 1,
        )
        pieces[movement.sourceFile][movement.sourceRank] = ChessPiece(
            "",
            movement.sourceFile,
            movement.sourceRank,
            0,
            "",
            "",
            0,
        )

        ++moveCounter
        switchColors()
        return capturedChessPiece
    }
}