package emerald.apps.fairychess.controller

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.*
import androidx.core.graphics.ColorUtils
import emerald.apps.fairychess.R
import emerald.apps.fairychess.model.*
import emerald.apps.fairychess.model.Movement.Companion.emptyMovement
import emerald.apps.fairychess.model.TimerUtils.Companion.transformLongToTimeString
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
import kotlinx.coroutines.*
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/** controller that propagates inputs from view to model and changes from model to view */
class ChessActivityListener() : MultiplayerDBGameInterface
    , ChessTimerPlayer.ChessTimerPlayerInterface
    , ChessTimerOpponent.ChessTimerOpponentInterface {

    private lateinit var chessActivity : ChessActivity
    private lateinit var chessgame: Chessgame
    private lateinit var multiplayerDB: MultiplayerDB
    private lateinit var chessAI: ChessAI

    lateinit var gameData: MultiplayerDB.GameData
    lateinit var gameParameters: MainActivityListener.GameParameters

    class PlayerSelectedSquare(var rank:Int, var file:Int)
    val playerSelectedSquare = PlayerSelectedSquare(-1,-1)

    //Views
    var elterLayout: LinearLayout? = null
    /* chess board layout consists of 64 imageviews represent which the chessboard model:
       At the core of the chessboard model is the pieces-array: representing 64 pieces with
       (rank,file)-coordinates
       (7,0) ... (7,7)      (A,8) ... (H,8)
       ...        ...    =   ...        ...
       (0,0) ... (0,7)      (A,1) ... (G,1)
     */
    private lateinit var imageViews: Array<Array<ImageView>> //imageViews[file][rank]
    private var playerTimer : ChessTimerPlayer? = null
    private var opponentTimer : ChessTimerOpponent? = null
    private lateinit var playerStats : MultiplayerDB.PlayerStats
    private lateinit var opponentStats : MultiplayerDB.PlayerStats
    private var playerStatsUpdated = false

    private var calcMoveJob : Job? = null

    constructor(chessActivity: ChessActivity) : this() {
        this.chessActivity = chessActivity
        //get game parameters from intent
        playerStats = chessActivity.intent.getParcelableExtra(MainActivityListener.gamePlayerStatsExtra)!!
        opponentStats = chessActivity.intent.getParcelableExtra(MainActivityListener.gameOpponentStatsExtra)!!
        gameData = MultiplayerDB.GameData(
            chessActivity.intent.getStringExtra(MainActivityListener.gameIdExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gamePlayerNameExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gameOpponentNameExtra)!!,
            playerStats.ELO,
            opponentStats.ELO
        )
        gameParameters = MainActivityListener.GameParameters(
            chessActivity.intent.getStringExtra(MainActivityListener.gameNameExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gameModeExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gameTimeExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.playerColorExtra)!!
        )
        //create ches game with parameters
        chessgame = Chessgame(chessActivity, gameData, gameParameters)

        //write information from intent into views and create/start timers
        if(gameParameters.playerColor=="white"){
            chessActivity.tv_playernameW.text = gameData.playerID
            chessActivity.tv_opponentnameW.text = gameData.opponentID
            chessActivity.tv_PlayerELOW.text = playerStats.ELO.toString()
            chessActivity.tv_OpponentELOW.text = opponentStats.ELO.toString()
        }
        if(gameParameters.playerColor=="black"){
            chessActivity.tv_playernameB.text = gameData.playerID
            chessActivity.tv_opponentnameB.text = gameData.opponentID
            chessActivity.tv_PlayerELOB.text = playerStats.ELO.toString()
            chessActivity.tv_OpponentELOB.text = opponentStats.ELO.toString()
        }
        playerTimer = ChessTimerPlayer.getPlTimerFromTimeMode(this, gameParameters.time)
        opponentTimer = ChessTimerOpponent.getOpTimerFromTimeMode(this, gameParameters.time)
        playerTimer?.create()
        opponentTimer?.create()

        //init chessboard-views
        create2DArrayImageViews()
        displayFigures()

        //init multiplayerDB
        if(gameParameters.playMode == "human"){
            multiplayerDB = MultiplayerDB(this, chessgame)
            multiplayerDB.listenToGameIngame(gameData.gameId)
        } else {
            chessAI = ChessAI("black")
        }
    }

    /** select, unselect and move figure */
    fun clickSquare(clickedView: View){
        //get file and rank of clicked view
        val clickedviewFullname: String = chessActivity.resources.getResourceName(clickedView.id)
        val clickedViewName: String = clickedviewFullname.substring(
            clickedviewFullname.lastIndexOf(
                "/"
            ) + 1
        )
        if(clickedViewName.matches("[A-Z][0-9]+".toRegex())){
            //calculate clickedFile and clickedRank
            val clickedFile = nameToFile(clickedViewName)
            val clickedRank = nameToRank(clickedViewName)
            //if a file and rank was selected => move from selected square to
            if(playerSelectedSquare.rank != -1 && playerSelectedSquare.file != -1
                && clickedFile != -1 && clickedRank != -1){
                val movement = Movement(
                        sourceRank = playerSelectedSquare.rank,
                        sourceFile = playerSelectedSquare.file,
                        targetRank = clickedRank,
                        targetFile = clickedFile
                )
                var moveResult = ""
                moveResult = chessgame.movePlayer(movement, chessgame.getBitboard().moveColor)
                if(chessgame.gameFinished){
                    if(chessgame.getBitboard().gameWinner == gameParameters.playerColor){
                        finishGame(chessgame.getBitboard().gameWinner + " won", true)
                    } else {
                        finishGame(chessgame.getBitboard().gameWinner + " won", false)
                    }
                } //check for winner
                if(chessgame.getBitboard().playerWithDrawOpportunity.isNotEmpty()){//check for draw
                    offerDraw(chessgame.getBitboard().playerWithDrawOpportunity)
                }
                moveResult += handlePromotion()
                if(gameParameters.playMode=="human" && moveResult==""){
                    multiplayerDB.writePlayerMovement(gameData.gameId, movement)
                }
                if(moveResult.isNotEmpty()){
                    Toast.makeText(chessActivity, moveResult, Toast.LENGTH_LONG).show()
                }
                if(gameParameters.playMode=="ai"){
                    //calculate ai move in coroutine to avoid blocking the ui thread
                    calcMoveJob = CoroutineScope(Dispatchers.Default).launch {
                        try{
                            var aiMovement :Movement
                            val calcTime = measureTimeMillis {
                                aiMovement = chessAI.calcMove(chessgame.getBitboard().clone())
                            }
                            println("move: "+aiMovement.asString("black"))
                            println("calcTime: $calcTime ms")
                            println("cnt_movements: "+chessAI.cnt_movements)
                            println("transpositionTableHits: "+chessAI.transpositionTableHits)
                            println("transpositionTableFails: "+chessAI.transpositionTableFails)
                            println("transpositionTableSize: "+chessAI.transpositionTable.size)
                            chessgame.movePlayer(aiMovement, chessAI.color)
                            withContext(Dispatchers.Main){
                                displayFigures()
                                Toast.makeText(chessActivity,"AI: "+aiMovement.asString("black"),Toast.LENGTH_LONG)
                            }
                        } catch (e: Exception) {
                            throw RuntimeException("To catch any exception thrown for yourTask", e)
                        }
                    }
                }
                displayFigures()
            }
            //mark the clicked view
            markFigure(clickedView)
            if(playerSelectedSquare.rank != -1 && playerSelectedSquare.file != -1){
                displayTargetMovements()
            }
        }
    }

    fun handlePromotion() : String{
        if(chessgame.getBitboard().promotionCoordinate != null) {
            pawnPromotion(chessgame.getBitboard().promotionCoordinate!!)
            return "promotion"
        }
        return ""
    }



    /** handle pawn promotion*/
    private fun pawnPromotion(pawnPromotionCandidate: Bitboard.Companion.Coordinate) {
        val pieceColor = chessgame.getBitboard().getPieceColor(pawnPromotionCandidate.rank,pawnPromotionCandidate.file)
            //handle pawn promotion of ai (exchange pawn with queen)
        if(pieceColor != gameParameters.playerColor && gameParameters.playMode=="ai"){ //always promote to queen
            chessgame.getBitboard().promotePawn(pawnPromotionCandidate,chessAI.getPromotion())
            displayFigures()
        }
        //handle user pawn promotion by creating and handling alert dialog
        else {
            if(pieceColor == gameParameters.playerColor){
                // create an alert builder
                val builder = AlertDialog.Builder(chessActivity)
                builder.setTitle("Pawn Promotion") // set the custom layout
                val customLayout: View =
                    chessActivity.layoutInflater.inflate(R.layout.promotion_layout, null)
                builder.setView(customLayout) // add a button
                val radioGroup = customLayout.findViewById<RadioGroup>(R.id.radiogroup)
                builder.setPositiveButton("OK") { _, _ ->
                    val radioButton =
                        customLayout.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
                    promotePawn(pieceColor, radioButton.text.toString().toLowerCase(), pawnPromotionCandidate)
                }
                // create and show the alert dialog
                val dialog = builder.create()
                dialog.show()
            }
        }
    }

    //propagate promotion information from the AlertDialog onto chessboard
    private fun promotePawn(color: String, promotion: String, pawnPromotionCandidate: Bitboard.Companion.Coordinate) {
        if (pawnPromotionCandidate.file < 0 || pawnPromotionCandidate.file > 7 || pawnPromotionCandidate.rank < 0 || pawnPromotionCandidate.rank > 7) return
        chessgame.getBitboard().promotePawn(pawnPromotionCandidate,promotion)
        displayFigures()

        val sourceRank = pawnPromotionCandidate.rank
        var sourceFile = 1
        if(color == "white")sourceFile = 6
        if(gameParameters.playMode == "human"){
            multiplayerDB.writePlayerMovement(
                gameData.gameId,
                PromotionMovement(
                    sourceRank = sourceRank,
                    sourceFile = sourceFile,
                    targetRank = pawnPromotionCandidate.rank,
                    targetFile = pawnPromotionCandidate.file,
                    promotion = promotion
                )
            )
        }

    }

    /** display figures from chess board in imageViews of chessActivity-layout */
    fun displayFigures() {
        for (rank in 0..7) {
            for (file in 0..7) {
                val x: Int = getDrawableFromName(
                    chessgame.getPieceName(Bitboard.Companion.Coordinate(rank, file)),
                    chessgame.getPieceColor(rank, file)
                )
                if (x != -1) imageViews[rank][file].setImageResource(x)
            }
        }
        //display captures
        chessActivity.drawCapturedPiecesDrawable(
            "white",
            chessgame.getBitboard().blackCapturedPieces
        )
        chessActivity.drawCapturedPiecesDrawable(
            "black",
            chessgame.getBitboard().whiteCapturedPieces
        )
        chessActivity.highlightActivePlayer(chessgame.getBitboard().moveColor)
        switchClocks(chessgame.getBitboard().moveColor)
    }

    /** switch clock from the player finishing the move to the other player */
    private fun switchClocks(activePlayerColor: String){
        if(gameParameters.playerColor == activePlayerColor){
            opponentTimer?.pause()
            playerTimer?.resume()
        } else {
            playerTimer?.pause()
            opponentTimer?.resume()
        }
    }

    /** create alert dialog for player that has a right for draw*/
    private fun offerDraw(color: String){
        if(gameParameters.playMode == "human" && color == gameParameters.playerColor){
            // create an alert builder
            val builder = AlertDialog.Builder(chessActivity)
            builder.setTitle("Do you want to draw?") // set the custom layout
            builder.setPositiveButton("yes") { _, _ ->
                run {
                    finishGame(gameData.playerID + " draw", null)
                }
            }
            builder.setNegativeButton("no",null)
            val dialog = builder.create()
            dialog.show()
        }
    }

    /** highlight the square from */
    private fun displayTargetMovements() {
        val targetMovements = chessgame.getTargetMovements(playerSelectedSquare.rank, playerSelectedSquare.file)
        for (targetMovement in targetMovements){
            markSquare(targetMovement.targetRank, targetMovement.targetFile)
        }
    }

    /** create 2D array of chesssquare-imageViews */
    private fun create2DArrayImageViews() {
        elterLayout = chessActivity.findViewById(R.id.elterLayout)
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

    /** get Drawable from figure name*/
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

    /** mark square for figure that was selected */
    fun markFigure(v: View) {
        val fullName: String = chessActivity.resources.getResourceName(v.id)
        val name: String = fullName.substring(fullName.lastIndexOf("/") + 1)
        val rank = nameToRank(name)
        val file = nameToFile(name)
        resetFieldColor()
        if(playerSelectedSquare.rank != -1 && playerSelectedSquare.file != -1){ //unselect
            playerSelectedSquare.rank = -1
            playerSelectedSquare.file = -1
        } else {
            imageViews[rank][file].setBackgroundColor(
                getMixedColor(rank, file, Color.RED)
            )
            playerSelectedSquare.rank = rank
            playerSelectedSquare.file = file
        }
    }

    fun markSquare(rank: Int, file: Int) {
        imageViews[rank][file].setBackgroundColor(
            getMixedColor(rank, file, Color.YELLOW)
        )
    }

    /** reset chessboard square color to "normal" after highlighting it */
    private fun resetFieldColor() {
        for(rank in 0..7){
            for(file in 0..7){
                if ((rank + file) % 2 != 0) imageViews[rank][file].setBackgroundColor(
                    chessActivity.resources.getColor(
                        R.color.colorWhite
                    )
                )
                if ((rank + file) % 2 == 0) imageViews[rank][file].setBackgroundColor(
                    chessActivity.resources.getColor(
                        R.color.colorBlack
                    )
                )
            }
        }
    }

    /** helper functions for highlighting square */
    private fun getMixedColor(rank: Int, file: Int, color: Int): Int {
        return if ((file + rank) % 2 == 0) ColorUtils.blendARGB(
            color,
            chessActivity.resources.getColor(R.color.colorWhite),
            0.8f
        ) else ColorUtils.blendARGB(
            color,
            chessActivity.resources.getColor(R.color.colorBlack),
            0.8f
        )
    }

    private fun nameToFile(name: String): Int {
        return Integer.valueOf(name.substring(1, 2)) - 1
    }

    private fun nameToRank(name: String): Int {
        return name.toLowerCase()[0] - 'a'
    }

    fun onDestroy() {
        finishGame(gameParameters.playerColor + " left the game", false)
    }

    /** finish a chess game by writing changes to multiplayerDB*/
    fun finishGame(cause: String, playerWon: Boolean?){
        if(chessgame.gameParameters.playMode=="human"){
            chessgame.gameFinished = true
            if(!playerStatsUpdated){
                ChessRatingSystem.updatePlayerStats(
                    playerStats,
                    opponentStats,
                    playerWon
                )
                multiplayerDB.setPlayerStats(gameData.playerID, playerStats, cause)
                playerStatsUpdated = true
            }
        } else {
            onFinishGame("", cause)
        }
    }

    override fun onSetPlayerstats(cause : String) {
        multiplayerDB.finishGame(chessgame.gameData.gameId, cause)
    }

    override fun onGameChanged(gameId: String, gameState: MultiplayerDB.GameState) {
        if(gameState.gameFinished){
            Toast.makeText(chessActivity, "opponent left game", Toast.LENGTH_LONG).show()
            finishGame("opponent left game", true)
        } else {
            if(gameState.moves.isNotEmpty()){
                if(gameParameters.playerColor == "white" && gameState.moves.size%2==0
                    || gameParameters.playerColor == "black" && gameState.moves.size%2==1){
                    chessgame.makeMove(gameState.moves[gameState.moves.lastIndex])
                    displayFigures()
                    if(chessgame.gameFinished){
                        if(chessgame.getBitboard().gameWinner == gameParameters.playerColor){
                            finishGame(chessgame.getBitboard().gameWinner + " won", true)
                        } else {
                            finishGame(chessgame.getBitboard().gameWinner + " won", false)
                        }
                    } //check for winner
                    if(chessgame.getBitboard().playerWithDrawOpportunity.isNotEmpty()){//check for draw
                        offerDraw(chessgame.getBitboard().playerWithDrawOpportunity)
                    }
                }
            }
        }
    }

    override fun onFinishGame(gameId: String, cause: String) {
        Toast.makeText(chessActivity, cause, Toast.LENGTH_LONG).show()
        val data = Intent()
            data.putExtra(MainActivityListener.gamePlayerStatsExtra, playerStats)
        chessActivity.setResult(RESULT_OK, data)
        chessActivity.finish()
    }

    override fun onTickOpponentTimer(millisUntilFinished: Long) {
        if(gameParameters.playerColor=="white"){
            chessActivity.tv_OpponentTimeW.text = transformLongToTimeString(millisUntilFinished)
        }
        if(gameParameters.playerColor=="black"){
            chessActivity.tv_OpponentTimeB.text = transformLongToTimeString(millisUntilFinished)
        }
    }

    override fun onFinishOpponentTimer() {
        chessgame.gameFinished = true
        finishGame("timeout. you won.", true)
    }

    override fun onTickPlayerTimer(millisUntilFinished: Long) {
        if(gameParameters.playerColor=="white"){
            chessActivity.tv_PlayerTimeW.text = transformLongToTimeString(millisUntilFinished)
        }
        if(gameParameters.playerColor=="black"){
            chessActivity.tv_PlayerTimeB.text = transformLongToTimeString(millisUntilFinished)
        }
    }

    override fun onFinishPlayerTimer() {
        finishGame("timeout. you lost.", false)
    }


}
