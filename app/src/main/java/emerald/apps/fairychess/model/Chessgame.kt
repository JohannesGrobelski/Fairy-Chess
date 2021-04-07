package emerald.apps.fairychess.model

import emerald.apps.fairychess.controller.MainActivityListener
import emerald.apps.fairychess.model.pieces.Chessboard
import emerald.apps.fairychess.utility.ChessFormationParser
import emerald.apps.fairychess.utility.FigureParser
import emerald.apps.fairychess.view.ChessActivity

class Chessgame() {

    companion object {
        const val testPlayerName = "testPlayerName"
    }

    private lateinit var chessboard: Chessboard


    lateinit var gameData: MultiplayerDB.GameData
    lateinit var gameParameters: MainActivityListener.GameParameters
    lateinit var opponentColor: String

    var gameFinished : Boolean = false


    constructor(chessActivity : ChessActivity, gameData: MultiplayerDB.GameData, gameParameters: MainActivityListener.GameParameters) : this() {
        val chessFormationArray: Array<Array<String>> = ChessFormationParser.parseChessFormation(chessActivity,gameParameters.name.replace(" ","_"))
        val figureMap : Map<String, FigureParser.Figure> = FigureParser.parseFigureMapFromFile(chessActivity)

        chessboard = Chessboard(chessFormationArray,figureMap)
        this.gameData = gameData
        this.gameParameters = gameParameters
        this.opponentColor = chessboard.oppositeColor(gameParameters.playerColor)
        this.gameFinished = false
    }

    fun movePlayer(movement: ChessPiece.Movement): String {
        val returnValue =  chessboard.move(gameParameters.playerColor, movement)
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

    fun movePlayer(movement: ChessPiece.Movement, color:String): String {
        val returnValue =  chessboard.move(color, movement)
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

