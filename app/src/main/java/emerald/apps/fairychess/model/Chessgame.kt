package emerald.apps.fairychess.model

import emerald.apps.fairychess.controller.MainActivityListener
import emerald.apps.fairychess.model.Bitboard.Companion.chessboardToBitboard
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
    fun movePlayer(movement: ChessPiece.Movement?, color: String): String {
        if(movement != null){
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
        } else {
            return "no move made"
        }
    }

    fun getTargetMovements(sourceRank: Int, sourceFile: Int): List<ChessPiece.Movement> {
        //return chessboard.getTargetMovements(sourceFile, sourceRank,true)
        return chessboardToBitboard(chessboard).getTargetMovementsAsMovementList(gameParameters.playerColor,sourceRank, sourceFile)
    }

    fun getPieceName(rank: Int, file: Int) : String{
        return chessboard.pieces[rank][file].name
    }

    fun getPieceColor(rank: Int, file: Int) : String{
        return chessboard.pieces[rank][file].color
    }

    fun getChessboard() : Chessboard {
        return chessboard
    }

    fun setChessboard(chessboard: Chessboard) {
        this.chessboard = chessboard
    }


    fun makeMove(moveString: String) {
        when(moveString.count{ "_".contains(it) }){
            3 -> {makeMove(ChessPiece.Movement.fromStringToMovement(moveString))} //normal movement
            4 -> {makeMove(ChessPiece.PromotionMovement.fromStringToMovement(moveString))} //movement + promotion
        }
    }

    fun makeMove(movement: ChessPiece.Movement){
        chessboard.move(chessboard.moveColor, movement)
        gameFinished = chessboard.gameFinished
    }
}

