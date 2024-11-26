package emerald.apps.fairychess.model

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
        this.opponentColor = oppositeColor(gameParameters.playerColor)
        this.gameFinished = false
        this.chessActivity = chessActivity
    }

    /** execute movement and check if color allows movement */
    fun movePlayer(movement: Movement?, color: String): String {
        return if(movement != null){
            val returnValue = chessboard.checkMoveAndMove(color, movement)
            gameFinished = chessboard.checkForWinner() != ""
            returnValue
        } else {
            "no move made"
        }
    }

    fun getTargetMovements(sourceRank: Int, sourceFile: Int): List<Movement> {
        println(chessboard.toString())
        return chessboard.getTargetMovementsAsMovementList(Bitboard.Companion.Coordinate(sourceRank, sourceFile))
    }

    fun getPieceName(coordinate: Bitboard.Companion.Coordinate) : String{
        return chessboard.getPieceName(coordinate)
    }

    fun getPieceColor(rank: Int, file: Int) : String{
        return chessboard.getPieceColor(rank,file)
    }


    fun getChessboard() : Chessboard {
        return chessboard
    }

    fun setChessboard(chessboard: Chessboard) {
        this.chessboard = chessboard
    }

    fun makeMove(moveString: String) {
        when(moveString.count{ "_".contains(it) }){
            3 -> {makeMove(Movement.fromStringToMovement(moveString))} //normal movement
            4 -> {makeMove(PromotionMovement.fromStringToMovement(moveString))} //movement + promotion
        }
    }

    fun makeMove(movement: Movement){
        chessboard.checkMoveAndMove(chessboard.getMovecolor(), movement)
        gameFinished = chessboard.checkForWinner() != ""
    }

    val colors = arrayOf("white","black")
    fun oppositeColor(color: String) : String {
        return if(color.isEmpty()) color
        else {
            if(color == colors[0]) colors[1]
            else colors[0]
        }
    }

    fun checkForWinner() : String {
        return chessboard.checkForWinner()
    }
}

