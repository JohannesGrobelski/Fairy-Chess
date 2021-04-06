package emerald.apps.fairychess.model

import android.content.Context
import emerald.apps.fairychess.model.pieces.Chessboard
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import emerald.apps.fairychess.view.ChessActivity

class Chessgame() : OpponentMover {

    companion object {
        const val testPlayerName = "testPlayerName"
    }

    private lateinit var chessboard: Chessboard
    lateinit var gameMode : String
    private lateinit var gameTime : String


    //Variables für KI
    var game_mode_ai = true
    var hints = false

    lateinit var playerColor: String
    lateinit var opponentColor: String

    //db variables
    lateinit var gameId : String
    lateinit var gameName : String
    var gameFinished : Boolean = false
    lateinit var player1Name : String
    lateinit var player2Name : String

    constructor(chessActivity : ChessActivity, gameId: String, gameName: String, player1Name:String, player2Name: String, mode: String, time: String, playerColor: String) : this() {
        val chessFormationArray: Array<Array<String>> = ChessFormationParser.parseChessFormation(chessActivity,gameName.replace(" ","_"))
        val figureMap : Map<String, FigureParser.Figure> = FigureParser.parseFigureMapFromFile(chessActivity)

        chessboard = Chessboard(chessFormationArray,figureMap)
        this.gameId = gameId
        this.gameName = gameName
        this.player1Name = player1Name
        this.player2Name = player2Name
        this.gameMode = mode
        this.gameTime = time
        this.playerColor = playerColor
        this.gameFinished = false
        this.opponentColor = chessboard.oppositeColor(playerColor)
    }

    fun movePlayer(movement: ChessPiece.Movement): String {
        val returnValue =  chessboard.move(playerColor, movement)
        if(returnValue == ""){
            when(gameMode){
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

    fun movePlayer(movement: ChessPiece.Movement, color:String): String {
        val returnValue =  chessboard.move(color, movement)
        gameFinished = chessboard.gameFinished
        if(returnValue == ""){
            when(gameMode){
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

    fun addMove(moveString:  String) {
        val movement = ChessPiece.Movement.fromStringToMovement(moveString)
        makeMove(movement)
    }

    fun makeMove(movement: ChessPiece.Movement){
        chessboard.move(chessboard.moveColor, movement)
        gameFinished = chessboard.gameFinished
    }
}

