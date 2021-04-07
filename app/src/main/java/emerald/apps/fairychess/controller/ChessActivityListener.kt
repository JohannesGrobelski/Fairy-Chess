package emerald.apps.fairychess.controller

import android.app.AlertDialog
import android.graphics.Color
import android.view.View
import android.widget.*
import androidx.core.graphics.ColorUtils
import emerald.apps.fairychess.R
import emerald.apps.fairychess.model.ChessPiece
import emerald.apps.fairychess.model.Chessgame
import emerald.apps.fairychess.model.MultiplayerDB
import emerald.apps.fairychess.model.MultiplayerDBGameInterface
import emerald.apps.fairychess.view.ChessActivity
import kotlinx.android.synthetic.main.activity_chess_black_perspective.*
import kotlinx.android.synthetic.main.activity_chess_white_perspective.*
import kotlinx.android.synthetic.main.activity_chess_white_perspective.A1
import kotlinx.android.synthetic.main.activity_chess_white_perspective.A2
import kotlinx.android.synthetic.main.activity_chess_white_perspective.A3
import kotlinx.android.synthetic.main.activity_chess_white_perspective.A4
import kotlinx.android.synthetic.main.activity_chess_white_perspective.A5
import kotlinx.android.synthetic.main.activity_chess_white_perspective.A6
import kotlinx.android.synthetic.main.activity_chess_white_perspective.A7
import kotlinx.android.synthetic.main.activity_chess_white_perspective.A8
import kotlinx.android.synthetic.main.activity_chess_white_perspective.B1
import kotlinx.android.synthetic.main.activity_chess_white_perspective.B2
import kotlinx.android.synthetic.main.activity_chess_white_perspective.B3
import kotlinx.android.synthetic.main.activity_chess_white_perspective.B4
import kotlinx.android.synthetic.main.activity_chess_white_perspective.B5
import kotlinx.android.synthetic.main.activity_chess_white_perspective.B6
import kotlinx.android.synthetic.main.activity_chess_white_perspective.B7
import kotlinx.android.synthetic.main.activity_chess_white_perspective.B8
import kotlinx.android.synthetic.main.activity_chess_white_perspective.C1
import kotlinx.android.synthetic.main.activity_chess_white_perspective.C2
import kotlinx.android.synthetic.main.activity_chess_white_perspective.C3
import kotlinx.android.synthetic.main.activity_chess_white_perspective.C4
import kotlinx.android.synthetic.main.activity_chess_white_perspective.C5
import kotlinx.android.synthetic.main.activity_chess_white_perspective.C6
import kotlinx.android.synthetic.main.activity_chess_white_perspective.C7
import kotlinx.android.synthetic.main.activity_chess_white_perspective.C8
import kotlinx.android.synthetic.main.activity_chess_white_perspective.D1
import kotlinx.android.synthetic.main.activity_chess_white_perspective.D2
import kotlinx.android.synthetic.main.activity_chess_white_perspective.D3
import kotlinx.android.synthetic.main.activity_chess_white_perspective.D4
import kotlinx.android.synthetic.main.activity_chess_white_perspective.D5
import kotlinx.android.synthetic.main.activity_chess_white_perspective.D6
import kotlinx.android.synthetic.main.activity_chess_white_perspective.D7
import kotlinx.android.synthetic.main.activity_chess_white_perspective.D8
import kotlinx.android.synthetic.main.activity_chess_white_perspective.E1
import kotlinx.android.synthetic.main.activity_chess_white_perspective.E2
import kotlinx.android.synthetic.main.activity_chess_white_perspective.E3
import kotlinx.android.synthetic.main.activity_chess_white_perspective.E4
import kotlinx.android.synthetic.main.activity_chess_white_perspective.E5
import kotlinx.android.synthetic.main.activity_chess_white_perspective.E6
import kotlinx.android.synthetic.main.activity_chess_white_perspective.E7
import kotlinx.android.synthetic.main.activity_chess_white_perspective.E8
import kotlinx.android.synthetic.main.activity_chess_white_perspective.F1
import kotlinx.android.synthetic.main.activity_chess_white_perspective.F2
import kotlinx.android.synthetic.main.activity_chess_white_perspective.F3
import kotlinx.android.synthetic.main.activity_chess_white_perspective.F4
import kotlinx.android.synthetic.main.activity_chess_white_perspective.F5
import kotlinx.android.synthetic.main.activity_chess_white_perspective.F6
import kotlinx.android.synthetic.main.activity_chess_white_perspective.F7
import kotlinx.android.synthetic.main.activity_chess_white_perspective.F8
import kotlinx.android.synthetic.main.activity_chess_white_perspective.G1
import kotlinx.android.synthetic.main.activity_chess_white_perspective.G2
import kotlinx.android.synthetic.main.activity_chess_white_perspective.G3
import kotlinx.android.synthetic.main.activity_chess_white_perspective.G4
import kotlinx.android.synthetic.main.activity_chess_white_perspective.G5
import kotlinx.android.synthetic.main.activity_chess_white_perspective.G6
import kotlinx.android.synthetic.main.activity_chess_white_perspective.G7
import kotlinx.android.synthetic.main.activity_chess_white_perspective.G8
import kotlinx.android.synthetic.main.activity_chess_white_perspective.H1
import kotlinx.android.synthetic.main.activity_chess_white_perspective.H2
import kotlinx.android.synthetic.main.activity_chess_white_perspective.H3
import kotlinx.android.synthetic.main.activity_chess_white_perspective.H4
import kotlinx.android.synthetic.main.activity_chess_white_perspective.H5
import kotlinx.android.synthetic.main.activity_chess_white_perspective.H6
import kotlinx.android.synthetic.main.activity_chess_white_perspective.H7
import kotlinx.android.synthetic.main.activity_chess_white_perspective.H8


class ChessActivityListener() : MultiplayerDBGameInterface {
    //TODO: Castling: requires history of involved rook and king. Can be accomplished via a hasMovedBefore flag.
    //TODO: en Passant: Knowledge of the last move taken. Can be accommodated by retaining a lastMove data structure, or retaining the previous board state.
    //TODO: Fifty move rule: requires history of when the last capture or pawn move. Can be accomplished via a lastPawnMoveOrCapture counter
    //TODO: Threefold repetition: requires all previous board states since the last castle, pawn move or capture. A list of hashes of previous states may be an option. (Thanks dfeuer)

    private lateinit var chessActivity : ChessActivity
    private lateinit var chessgame: Chessgame
    private lateinit var multiplayerDB: MultiplayerDB

    lateinit var gameData: MultiplayerDB.GameData
    lateinit var gameParameters: MainActivityListener.GameParameters

    var selectionFile = -1
    var selectionRank = -1

    //Views
    var elterLayout: LinearLayout? = null
    /*
       pieces-array: (file,rank)-coordinates
       (7,0) ... (7,7)      (A,8) ... (H,8)
       ...        ...    =   ...        ...
       (0,0) ... (0,7)      (A,1) ... (G,1)
     */
    private lateinit var imageViews: Array<Array<ImageView>> //imageViews[file][rank]

    constructor(chessActivity: ChessActivity) : this() {
        this.chessActivity = chessActivity
        gameData = MultiplayerDB.GameData(
            chessActivity.intent.getStringExtra(MainActivityListener.gameIdExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gamePlayerNameExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gameOpponentNameExtra)!!
        )
        gameParameters = MainActivityListener.GameParameters(
            chessActivity.intent.getStringExtra(MainActivityListener.gameNameExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gameModeExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gameTimeExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.playerColorExtra)!!
        )

        chessgame = Chessgame(chessActivity,gameData, gameParameters)
        if(gameParameters.playerColor=="white"){
            chessActivity.tv_playernameW.text = gameData.playerID
            chessActivity.tv_opponentnameW.text = gameData.opponentID
        }
        if(gameParameters.playerColor=="black"){
            chessActivity.tv_playernameB.text = gameData.playerID
            chessActivity.tv_opponentnameB.text = gameData.opponentID
        }
        initViews()
        displayFigures()

        if(gameParameters.playMode == "human"){
            multiplayerDB = MultiplayerDB(this,chessgame)
            multiplayerDB.listenToGameIngame(gameData.gameId)
        }
    }

    /** select, unselect and move figure */
    fun clickSquare(clickedView: View){
        //get file and rank of clicked view
        val clickedviewFullname: String = chessActivity.resources.getResourceName(clickedView.id)
        val clickedViewName: String = clickedviewFullname.substring(clickedviewFullname.lastIndexOf("/") + 1)
        if(clickedViewName.matches("[A-Z][0-9]+".toRegex())){
            //calculate clickedFile and clickedRank
            val clickedFile = nameToFile(clickedViewName)
            val clickedRank = nameToRank(clickedViewName)
            //if a file and rank was selected => move from selected square to
            if(selectionRank != -1 && selectionFile != -1
                && clickedFile != -1 && clickedRank != -1){
                val movement = ChessPiece.Movement(sourceFile = selectionFile,sourceRank = selectionRank,targetFile = clickedFile,targetRank = clickedRank)
                var moveResult:String
                if(gameParameters.playMode=="ai"){
                    moveResult = chessgame.movePlayer(movement,chessgame.getChessboard().moveColor)
                    if(chessgame.gameFinished)finishGame(chessgame.getChessboard().gameWinner+" won")
                } else {
                    moveResult = chessgame.movePlayer(movement,gameParameters.playerColor)
                }
                moveResult += handlePromotion()
                if(gameParameters.playMode=="human" && moveResult==""){
                    multiplayerDB.writePlayerMovement(gameData.gameId,movement)
                }
                displayFigures()
                if(moveResult.isNotEmpty()){
                    Toast.makeText(chessActivity,moveResult,Toast.LENGTH_LONG).show()
                }
            }
            //mark the clicked view
            markFigure(clickedView)
            if(selectionRank != -1 && selectionFile != -1){
                displayTargetMovements()
            }
        }
    }

    fun handlePromotion() : String{
        if(chessgame.getChessboard().promotion != null){
            pawnPromotion(chessgame.getChessboard().promotion!!)
            chessgame.getChessboard().promotion = null
            return "promotion"
        }
        return ""
    }

    private fun pawnPromotion(pawnPromotionCandidate: ChessPiece) {
        if(pawnPromotionCandidate.color != gameParameters.playerColor && gameParameters.playMode=="ai"){
            val value = chessgame.figureMap["queen"]!!.value
            val movingPattern = chessgame.figureMap["queen"]!!.movementParlett
            chessgame.getChessboard().pieces[pawnPromotionCandidate.positionFile][pawnPromotionCandidate.positionRank] =
                ChessPiece(
                    "queen",
                    pawnPromotionCandidate.positionFile,
                    pawnPromotionCandidate.positionRank,
                    value,
                    chessgame.getChessboard().oppositeColor(gameParameters.playerColor),
                    movingPattern,
                    0
                )
            displayFigures()
        }
        else {
            if(pawnPromotionCandidate.color == gameParameters.playerColor){
                // create an alert builder
                val builder = AlertDialog.Builder(chessActivity)
                builder.setTitle("Pawn Promotion") // set the custom layout
                val customLayout: View =
                    chessActivity.layoutInflater.inflate(R.layout.promotion_layout, null)
                builder.setView(customLayout) // add a button
                val radioGroup = customLayout.findViewById<RadioGroup>(R.id.radiogroup)
                builder.setPositiveButton("OK") { dialog, which ->
                    val radioButton =
                        customLayout.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
                    promote(radioButton.text.toString().toLowerCase(), pawnPromotionCandidate)
                }
                // create and show the alert dialog
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    // do something with the data coming from the AlertDialog
    private fun promote(promotion: String, chessPiece: ChessPiece) {
        if (chessPiece.positionFile < 0 || chessPiece.positionFile > 7 || chessPiece.positionRank < 0 || chessPiece.positionRank > 7) return
        val color: String = chessgame.getChessboard().pieces[chessPiece.positionFile][chessPiece.positionRank].color
        val value = chessgame.figureMap[promotion]!!.value
        val movingPattern = chessgame.figureMap[promotion]!!.movementParlett

        chessgame.getChessboard().pieces[chessPiece.positionFile][chessPiece.positionRank] =
            ChessPiece(promotion,chessPiece.positionFile,chessPiece.positionRank,value,color,movingPattern,0)
        displayFigures()

        var sourceFile = chessPiece.positionFile
        var sourceRank = 6
        if(chessPiece.color == "white") chessPiece.positionRank = 1
        multiplayerDB.writePlayerMovement(gameData.gameId,
            ChessPiece.PromotionMovement(
                sourceFile = sourceFile,
                sourceRank = sourceRank,
                targetFile = chessPiece.positionFile,
                targetRank = chessPiece.positionRank,
                promotion = promotion
            )
        )
    }


    fun displayFigures() {
        for (file in 0..7) {
            for (rank in 0..7) {
                val x: Int = getDrawableFromName(
                    chessgame.getPieceName(file,rank),
                    chessgame.getPieceColor(file,rank)
                )
                if (x != -1) imageViews[file][rank].setImageResource(x)
            }
        }
        //display captures
        chessActivity.drawCapturedPiecesDrawable("white",chessgame.getChessboard().blackCapturedPieces)
        chessActivity.drawCapturedPiecesDrawable("black",chessgame.getChessboard().whiteCapturedPieces)
        chessActivity.highlightActivePlayer(chessgame.getChessboard().moveColor)
    }

    private fun displayTargetMovements() {
        val targetMovements = chessgame.getTargetMovements(selectionFile,selectionRank)
        for (targetMovement in targetMovements){
            markSquare(targetMovement.targetFile,targetMovement.targetRank)
        }
    }

    private fun initViews() {
        elterLayout = chessActivity.findViewById<LinearLayout>(R.id.elterLayout)
        imageViews = arrayOf(
            arrayOf(
                chessActivity.A1, chessActivity.A2, chessActivity.A3, chessActivity.A4,
                chessActivity.A5, chessActivity.A6, chessActivity.A7, chessActivity.A8
            ),
            arrayOf(
                chessActivity.B1, chessActivity.B2, chessActivity.B3, chessActivity.B4,
                chessActivity.B5, chessActivity.B6, chessActivity.B7, chessActivity.B8
            ),
            arrayOf(
                chessActivity.C1, chessActivity.C2, chessActivity.C3, chessActivity.C4,
                chessActivity.C5, chessActivity.C6, chessActivity.C7, chessActivity.C8
            ),
            arrayOf(
                chessActivity.D1, chessActivity.D2, chessActivity.D3, chessActivity.D4,
                chessActivity.D5, chessActivity.D6, chessActivity.D7, chessActivity.D8
            ),
            arrayOf(
                chessActivity.E1, chessActivity.E2, chessActivity.E3, chessActivity.E4,
                chessActivity.E5, chessActivity.E6, chessActivity.E7, chessActivity.E8
            ),
            arrayOf(
                chessActivity.F1, chessActivity.F2, chessActivity.F3, chessActivity.F4,
                chessActivity.F5, chessActivity.F6, chessActivity.F7, chessActivity.F8
            ),
            arrayOf(
                chessActivity.G1, chessActivity.G2, chessActivity.G3, chessActivity.G4,
                chessActivity.G5, chessActivity.G6, chessActivity.G7, chessActivity.G8
            ),
            arrayOf(
                chessActivity.H1, chessActivity.H2, chessActivity.H3, chessActivity.H4,
                chessActivity.H5, chessActivity.H6, chessActivity.H7, chessActivity.H8
            ),
        )
    }

    fun getDrawableFromName(type: String, color: String): Int {
        if (color == "white" && type == "king") {
            return R.drawable.white_king
        } else if (color == "white" && type == "queen") {
            return R.drawable.white_queen
        } else if (color == "white" && type == "pawn") {
            return R.drawable.white_pawn
        } else if (color == "white" && type == "bishop") {
            return R.drawable.white_bishop
        } else if (color == "white" && type == "knight") {
            return R.drawable.white_knight
        } else if (color == "white" && type == "rook") {
            return R.drawable.white_rook
        } else if (color == "white" && type == "berolina") {
            return R.drawable.white_berolina
        } else if (color == "white" && type == "grasshopper") {
            return R.drawable.white_grasshopper
        } else if (color == "black" && type == "king") {
            return R.drawable.black_king
        } else if (color == "black" && type == "queen") {
            return R.drawable.black_queen
        } else if (color == "black" && type == "pawn") {
            return R.drawable.black_pawn
        } else if (color == "black" && type == "bishop") {
            return R.drawable.black_bishop
        } else if (color == "black" && type == "knight") {
            return R.drawable.black_knight
        } else if (color == "black" && type == "rook") {
            return R.drawable.black_rook
        } else if (color == "black" && type == "berolina") {
            return R.drawable.black_berolina
        } else if (color == "black" && type == "grasshopper") {
            return R.drawable.black_grasshopper
        } else {
            return android.R.color.transparent
        }
    }

    fun markFigure(v: View) {
        val fullName: String = chessActivity.getResources().getResourceName(v.getId())
        val name: String = fullName.substring(fullName.lastIndexOf("/") + 1)
        val file = nameToFile(name)
        val rank = nameToRank(name)
        resetFieldColor()
        if(selectionFile != -1 && selectionRank != -1){ //unselect
            selectionFile = -1
            selectionRank = -1
        } else {
            imageViews[file][rank].setBackgroundColor(
                getMixedColor(file, rank, Color.RED)
            )
            selectionFile = file
            selectionRank = rank
        }
    }

    fun markSquare(file : Int, rank : Int) {
        imageViews[file][rank].setBackgroundColor(
            getMixedColor(file, rank, Color.YELLOW)
        )
    }

    private fun resetFieldColor() {
        for(rank in 0..7){
            for(file in 0..7){
                if ((rank + file) % 2 != 0) imageViews[file][rank].setBackgroundColor(
                    chessActivity.resources.getColor(
                        R.color.colorWhite
                    )
                )
                if ((rank + file) % 2 == 0) imageViews[file][rank].setBackgroundColor(
                    chessActivity.resources.getColor(
                        R.color.colorBlack
                    )
                )
            }
        }
    }

    //Hilfsfunktionen
    private fun getMixedColor(x: Int, y: Int, color: Int): Int {
        return if ((x + y) % 2 == 0) ColorUtils.blendARGB(
            color,
            chessActivity.getResources().getColor(R.color.colorWhite),
            0.8f
        ) else ColorUtils.blendARGB(
            color,
            chessActivity.getResources().getColor(R.color.colorBlack),
            0.8f
        )
    }

    private fun nameToRank(name: String): Int {
        return Integer.valueOf(name.substring(1, 2)) - 1
    }

    private fun nameToFile(name: String): Int {
        return name.toLowerCase()[0] - 'a'
    }

    fun onDestroy() {
        finishGame(gameParameters.playerColor+" left the game")
    }

    fun finishGame(cause: String){
        if(chessgame.gameParameters.playMode=="human"){
            multiplayerDB.finishGame(chessgame.gameData.gameId, cause)
        } else {
            onFinishGame("",cause)
        }
    }

    override fun onGameChanged(gameId: String, gameState: MultiplayerDB.GameState) {
        if(gameState.gameFinished){
            Toast.makeText(chessActivity,"opponent left game",Toast.LENGTH_LONG).show()
            onFinishGame(gameData.gameId,"opponent left game")
        } else {
            if(gameState.moves.isNotEmpty()){
                if(gameParameters.playerColor == "white" && gameState.moves.size%2==0
                    || gameParameters.playerColor == "black" && gameState.moves.size%2==1){
                        chessgame.makeMove(gameState.moves[gameState.moves.lastIndex])
                        displayFigures()
                        if(chessgame.gameFinished){
                            finishGame("$gameParameters.playerColor won")
                        }
                }
            }
        }
    }


    override fun onFinishGame(gameId: String, cause : String) {
        Toast.makeText(chessActivity,cause,Toast.LENGTH_LONG).show()
        chessActivity.finish()
    }


}
