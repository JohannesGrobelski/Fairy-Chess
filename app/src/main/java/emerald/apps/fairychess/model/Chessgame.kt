package emerald.apps.fairychess.model

import emerald.apps.fairychess.controller.MainActivityListener
import emerald.apps.fairychess.utility.ChessFormationParser.Companion.CHESS960
import emerald.apps.fairychess.utility.ChessFormationParser.Companion.parseChessFormation
import emerald.apps.fairychess.utility.FigureParser
import emerald.apps.fairychess.view.ChessActivity

class Chessgame() {

    private lateinit var bitboard: Bitboard

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
        val chessFormationArray: Array<Array<String>> = parseChessFormation(
            chessActivity, gameParameters.name.replace(
                " ",
                "_"
            ), gameParameters.name==CHESS960
        )

        figureMap = FigureParser.parseFigureMapFromFile(chessActivity)

        bitboard = Bitboard(chessFormationArray, figureMap)

        this.gameData = gameData
        this.gameParameters = gameParameters
        this.opponentColor = oppositeColor(gameParameters.playerColor)
        this.gameFinished = false
        this.chessActivity = chessActivity
    }

    /** execute movement and check if color allows movement */
    fun movePlayer(movement: Movement?, color: String): String {
        return if(movement != null){
            val returnValue = bitboard.checkMoveAndMove(color, movement)
            gameFinished = bitboard.gameFinished
            returnValue
        } else {
            "no move made"
        }
    }

    fun getTargetMovements(sourceRank: Int, sourceFile: Int): List<Movement> {
        println(bitboard.toString())
        return bitboard.getTargetMovementsAsMovementList(bitboard.moveColor,Bitboard.Companion.Coordinate(sourceRank, sourceFile))
    }

    fun getPieceName(coordinate: Bitboard.Companion.Coordinate) : String{
        return bitboard.getPieceName(coordinate)
    }

    fun getPieceColor(rank: Int, file: Int) : String{
        return bitboard.getPieceColor(rank,file)
    }

    fun getBitboard() : Bitboard {
        return bitboard
    }

    fun setChessboard(chessboard: Bitboard) {
        this.bitboard = chessboard
    }

    fun makeMove(moveString: String) {
        when(moveString.count{ "_".contains(it) }){
            3 -> {makeMove(Movement.fromStringToMovement(moveString))} //normal movement
            4 -> {makeMove(PromotionMovement.fromStringToMovement(moveString))} //movement + promotion
        }
    }

    fun makeMove(movement: Movement){
        bitboard.checkMoveAndMove(bitboard.moveColor, movement)
        gameFinished = bitboard.gameFinished
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

