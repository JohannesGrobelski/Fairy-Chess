package emerald.apps.fairychess.model

import android.app.AlertDialog
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import emerald.apps.fairychess.R
import emerald.apps.fairychess.controller.MainActivityListener
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import emerald.apps.fairychess.view.ChessActivity

class Chessgame() {

    private lateinit var chessboard: Chessboard

    lateinit var gameData: MultiplayerDB.GameData
    lateinit var gameParameters: MainActivityListener.GameParameters
    lateinit var opponentColor: String

    var gameFinished : Boolean = false
    private lateinit var chessActivity : ChessActivity

    lateinit var figureMap : Map<String, FigureParser.Figure>

    constructor(
        chessActivity: ChessActivity,
        gameData: MultiplayerDB.GameData,
        gameParameters: MainActivityListener.GameParameters
    ) : this() {
        val chessFormationArray: Array<Array<String>> = ChessFormationParser.parseChessFormation(
            chessActivity, gameParameters.name.replace(
                " ",
                "_"
            )
        )
        figureMap = FigureParser.parseFigureMapFromFile(chessActivity)

        chessboard = Chessboard(chessFormationArray, figureMap)
        this.gameData = gameData
        this.gameParameters = gameParameters
        this.opponentColor = Chessboard.oppositeColor(gameParameters.playerColor)
        this.gameFinished = false
        this.chessActivity = chessActivity
    }

    /** execute movement and check if color allows movement */
    fun movePlayer(movement: ChessPiece.Movement, color: String): String {
        var returnValue = chessboard.move(color, movement)
        gameFinished = chessboard.gameFinished
        if(returnValue == ""){
            when(gameParameters.playMode){
                "ai" -> {
                    /*val ai = StubChessAI(opponentColor,this)
                    ai.moveFigure(this)*/
                }
                "human" -> {

                }
            }
        }
        return returnValue
    }

    fun getTargetMovements(sourceFile: Int, sourceRank: Int): List<ChessPiece.Movement> {
        return chessboard.getTargetMovements(sourceFile, sourceRank,true)
    }

    fun getPieceName(file: Int, rank: Int) : String{
        return chessboard.pieces[file][rank].name
    }

    fun getPieceColor(file: Int, rank: Int) : String{
        return chessboard.pieces[file][rank].color
    }

    fun getChessboard() : Chessboard {
        return chessboard
    }

    fun makeMove(moveString: String) {
        val count = moveString.count{ "_".contains(it) }
    if(count == 3)makeMove(ChessPiece.Movement.fromStringToMovement(moveString))
    if(count == 4)makeMove(ChessPiece.PromotionMovement.fromStringToMovement(moveString))
}

    fun makeMove(movement: ChessPiece.Movement){
        chessboard.move(chessboard.moveColor, movement)
        gameFinished = chessboard.gameFinished
    }
}

