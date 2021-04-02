package emerald.apps.fairychess.model

import android.content.Context
import emerald.apps.fairychess.model.pieces.Chessboard

class Chessgame() : OpponentMover{
    private lateinit var chessboard: Chessboard
    private lateinit var context : Context
    private lateinit var game : String
    private lateinit var mode : String
    private lateinit var time : String

    //Variables für KI
    var game_mode_ai = true
    var hints = false

    lateinit var playerColor: String
    lateinit var opponentColor: String

    constructor(context: Context, game: String, mode: String, time: String, playerColor: String) : this() {
        chessboard = Chessboard(context, game)
        this.mode = mode
        this.time = time
        this.playerColor = playerColor
        this.opponentColor = chessboard.oppositeColor(playerColor)
    }

    fun movePlayer(movement: ChessPiece.Movement): String {
        val returnValue =  chessboard.move(playerColor, movement)
        if(returnValue == ""){
            when(mode){
                "ai" -> {
                    val ai = StubChessAI(opponentColor,this)
                    ai.moveFigure(this)
                }
                "human" -> {

                }
            }
        }
        return returnValue
    }

    override fun onOpponentMove(movement: ChessPiece.Movement) {
        chessboard.move(opponentColor,movement)
    }


    fun getTargetMovements(sourceFile: Int, sourceRank: Int): List<ChessPiece.Movement> {
        return chessboard.getTargetMovements(sourceFile, sourceRank)
    }

    fun getPieceName(file:Int,rank:Int) : String{
        return chessboard.pieces[file][rank].name
    }

    fun getPieceColor(file:Int,rank:Int) : String{
        return chessboard.pieces[file][rank].color
    }

    fun getChessboard() : Chessboard{
        return chessboard
    }


}

