package emerald.apps.fairychess.model

import emerald.apps.fairychess.controller.MainActivityListener
import emerald.apps.fairychess.model.board.Chessboard
import emerald.apps.fairychess.model.board.Color
import emerald.apps.fairychess.model.board.Movement
import emerald.apps.fairychess.model.board.PromotionMovement
import emerald.apps.fairychess.model.multiplayer.MultiplayerDB
import emerald.apps.fairychess.view.ChessActivity

class Chessgame() {


    private lateinit var chessboard: Chessboard

    lateinit var gameData: MultiplayerDB.GameData
    lateinit var gameParameters: MainActivityListener.GameParameters
    lateinit var opponentColor: String

    var gameFinished : Boolean = false
    private lateinit var chessActivity : ChessActivity

    constructor(
        chessActivity: ChessActivity,
        gameData: MultiplayerDB.GameData,
        gameParameters: MainActivityListener.GameParameters
    ) : this() {
        chessboard = Chessboard(gameParameters)

        this.gameData = gameData
        this.gameParameters = gameParameters
        this.opponentColor = oppositeColor(gameParameters.playerColor)
        this.gameFinished = false
        this.chessActivity = chessActivity
    }

    /** execute movement and check if color allows movement */
    fun movePlayerWithCheck(movement: Movement?, color: Color): String {
        return if(movement != null){
            val returnValue = chessboard.checkMoveAndMove(movement)
            //gameFinished = chessboard.checkForWinner() != null
            returnValue
        } else {
            "no move made"
        }
    }

    fun movePlayerWithoutCheck(movement: Movement?) {
        if(movement != null){
            chessboard.move(movement)
        }

    }

    fun getTargetMovements(sourceFile: Int, sourceRank: Int): List<Movement> {
        return chessboard.getTargetMovementsAsMovementList(sourceFile,sourceRank)
    }

    fun getPieceName(sourceFile: Int, sourceRank: Int) : String{
        return chessboard.getPieceName(sourceFile, sourceRank)
    }

    fun getPieceColor(file: Int, rank: Int) : String{
        return chessboard.getPieceColor(file,rank)
    }

    fun getChessboard() : Chessboard {
        return chessboard
    }

    fun makeMove(moveString: String) {
        when(moveString.count{ "_".contains(it) }){
            3 -> {makeMove(Movement.fromStringToMovement(moveString))} //normal movement
            4 -> {makeMove(PromotionMovement.fromStringToMovement(moveString))} //movement + promotion
        }
    }

    fun makeMove(movement: Movement){
        chessboard.checkMoveAndMove(movement)
        gameFinished = chessboard.checkForGameEnd() != ""
    }

    val colors = arrayOf("white","black")
    fun oppositeColor(color: String) : String {
        return if(color.isEmpty()) color
        else {
            if(color == colors[0]) colors[1]
            else colors[0]
        }
    }
}

