package emerald.apps.fairychess.controller

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.transition.Visibility
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import emerald.apps.fairychess.R
import emerald.apps.fairychess.databinding.DialogGameEndBinding
import emerald.apps.fairychess.model.rating.ChessRatingSystem
import emerald.apps.fairychess.model.timer.ChessTimerOpponent
import emerald.apps.fairychess.model.timer.ChessTimerPlayer
import emerald.apps.fairychess.model.Chessgame
import emerald.apps.fairychess.model.board.Chessboard
import emerald.apps.fairychess.model.board.Coordinate
import emerald.apps.fairychess.model.board.Movement
import emerald.apps.fairychess.model.multiplayer.MultiplayerDB
import emerald.apps.fairychess.model.multiplayer.MultiplayerDBGameInterface
import emerald.apps.fairychess.model.board.PromotionMovement
import emerald.apps.fairychess.model.timer.TimerUtils.Companion.transformLongToTimeString
import emerald.apps.fairychess.view.ChessActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit


/** controller that propagates inputs from view to model and changes from model to view */
class ChessActivityListener() : MultiplayerDBGameInterface
    , ChessTimerPlayer.ChessTimerPlayerInterface
    , ChessTimerOpponent.ChessTimerOpponentInterface {

    private lateinit var chessActivity: ChessActivity
    private lateinit var chessgame: Chessgame
    private lateinit var multiplayerDB: MultiplayerDB

    // View references
    private lateinit var tvPlayerName: TextView
    private lateinit var tvOpponentName: TextView
    private lateinit var tvPlayerELO: TextView
    private lateinit var tvOpponentELO: TextView
    private lateinit var tvPlayerTime: TextView
    private lateinit var tvOpponentTime: TextView
    private lateinit var tvCalcStatsInfo : TextView
    private lateinit var tvCalcStatsHash : TextView

    lateinit var gameData: MultiplayerDB.GameData
    lateinit var gameParameters: MainActivityListener.GameParameters
    private lateinit var gameboardSize : Pair<Int,Int>

    class PlayerSelectedSquare(var file: Int, var rank: Int)
    val playerSelectedSquare = PlayerSelectedSquare(-1, -1)

    private lateinit var imageViews: Array<Array<ImageView>>
    private var playerTimer: ChessTimerPlayer? = null
    private var opponentTimer: ChessTimerOpponent? = null
    private lateinit var playerStats: MultiplayerDB.PlayerStats
    private lateinit var opponentStats: MultiplayerDB.PlayerStats
    private var playerStatsUpdated = false

    private var calcMoveJob: Job? = null

    constructor(chessActivity: ChessActivity) : this() {
        this.chessActivity = chessActivity
        getIntentData()
        gameboardSize = Chessboard.getGameboardSize(gameParameters.name)
        initializeViews()
        setupGame()
    }

    private fun initializeViews() {
        // Initialize views based on player color
        if (gameParameters.playerColor == "white") {
            tvPlayerName = chessActivity.findViewById(R.id.tv_playernameW)
            tvOpponentName = chessActivity.findViewById(R.id.tv_opponentnameW)
            tvPlayerELO = chessActivity.findViewById(R.id.tv_PlayerELOW)
            tvOpponentELO = chessActivity.findViewById(R.id.tv_OpponentELOW)
            tvPlayerTime = chessActivity.findViewById(R.id.tv_PlayerTimeW)
            tvOpponentTime = chessActivity.findViewById(R.id.tv_OpponentTimeW)
        } else {
            tvPlayerName = chessActivity.findViewById(R.id.tv_playernameB)
            tvOpponentName = chessActivity.findViewById(R.id.tv_opponentnameB)
            tvPlayerELO = chessActivity.findViewById(R.id.tv_PlayerELOB)
            tvOpponentELO = chessActivity.findViewById(R.id.tv_OpponentELOB)
            tvPlayerTime = chessActivity.findViewById(R.id.tv_PlayerTimeB)
            tvOpponentTime = chessActivity.findViewById(R.id.tv_OpponentTimeB)
        }
    }

    private fun getIntentData(){
        playerStats = chessActivity.intent.getParcelableExtra(MainActivityListener.gamePlayerStatsExtra)!!
        opponentStats = chessActivity.intent.getParcelableExtra(MainActivityListener.gameOpponentStatsExtra)!!

        gameParameters = MainActivityListener.GameParameters(
            chessActivity.intent.getStringExtra(MainActivityListener.gameNameExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gameModeExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gameTimeExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.playerColorExtra)!!,
            chessActivity.intent.getIntExtra(MainActivityListener.gameDifficultyExtra,0)
        )

        gameData = MultiplayerDB.GameData(
            chessActivity.intent.getStringExtra(MainActivityListener.gameIdExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gamePlayerNameExtra)!!,
            chessActivity.intent.getStringExtra(MainActivityListener.gameOpponentNameExtra)!!,
            playerStats.ELO,
            opponentStats.ELO
        )
    }

    private fun setupGame() {
        // Update UI with game data
        tvPlayerName.text = gameData.playerID
        tvOpponentName.text = gameData.opponentID
        tvPlayerELO.text = playerStats.ELO.toString()
        tvOpponentELO.text = opponentStats.ELO.toString()

        //create ches game with parameters
        chessgame = Chessgame(chessActivity, gameData, gameParameters)

        // Initialize timers and board
        initAdaptBoard()
        initializeTimers()
        displayFigures()

        //init multiplayerDB
        if(gameParameters.playMode == "human"){
            multiplayerDB = MultiplayerDB(this, chessgame)
            multiplayerDB.listenToGameIngame(gameData.gameId)
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
            if(playerSelectedSquare.file == -1 && playerSelectedSquare.rank == -1){
                //mark the clicked view
                markFigure(clickedView)
                displayTargetMovements()
                return
            } else {
                //if a file and rank was selected => move from selected square to
                //calculate clickedFile and clickedRank
                val clickedFile = nameToFile(clickedViewName)
                val clickedRank = nameToRank(clickedViewName)
                if(playerSelectedSquare.file == clickedFile && playerSelectedSquare.rank == clickedRank){
                    resetFieldColor()
                    return
                }
                val movement = Movement(
                    sourceFile = playerSelectedSquare.file,
                    sourceRank = playerSelectedSquare.rank,
                    targetFile = clickedFile,
                    targetRank = clickedRank
                )
                //check if movement is legal
                if(!chessgame.getChessboard().checkMove(movement)){
                    //Toast.makeText(chessActivity, "Illegal move!", Toast.LENGTH_SHORT).show()
                    resetFieldColor()
                    return
                }
                var moveResult = chessgame.movePlayerWithCheck(movement, chessgame.getChessboard().getMovecolor())
                //check for winner or draw
                handleGameEnd()
                /*
                if(chessgame.getChessboard().checkForPlayerWithDrawOpportunity() != null){//check for draw
                    offerDraw(chessgame.getChessboard().checkForPlayerWithDrawOpportunity()!!.stringValue)
                }
                moveResult += handlePromotion()
                 */
                if(gameParameters.playMode=="human" && moveResult==""){
                    multiplayerDB.writePlayerMovement(gameData.gameId, movement)
                } else if(gameParameters.playMode=="ai"){
                    //calculate ai move in coroutine to avoid blocking the ui thread
                    calcMoveJob = CoroutineScope(Dispatchers.Default).launch {
                        try{
                            val aiMovement = chessgame.getChessboard().calcMove(chessgame.getChessboard().getCurrentFEN())
                            chessgame.movePlayerWithoutCheck(aiMovement)
                            withContext(Dispatchers.Main) {
                                displayFigures()
                            }
                            handleGameEnd()
                        } catch (e: Exception) {
                            throw RuntimeException("To catch any exception thrown for yourTask", e)
                        }
                    }
                }
                displayFigures()
                resetFieldColor()
                //reset saved position after move
                playerSelectedSquare.file = -1
                playerSelectedSquare.rank = -1
            }
        } else {
            resetFieldColor()
        }
    }

    fun handlePromotion() : String{
        if(chessgame.getChessboard().getPromotionCoordinate() != null) {
            pawnPromotion(chessgame.getChessboard().getPromotionCoordinate()!!)
            return "promotion"
        }
        return ""
    }

    /** handle pawn promotion*/
    private fun pawnPromotion(pawnPromotionCandidate: Coordinate) {
        val pieceColor = chessgame.getChessboard().getPieceColor(pawnPromotionCandidate.file,pawnPromotionCandidate.rank)
        //handle pawn promotion of ai (exchange pawn with queen)
        if(pieceColor != gameParameters.playerColor && gameParameters.playMode=="ai"){ //always promote to queen
            chessgame.getChessboard().promotePiece(pawnPromotionCandidate,chessgame.getChessboard().getPromotion())
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
    private fun promotePawn(color: String, promotion: String, pawnPromotionCandidate: Coordinate) {

        if (pawnPromotionCandidate.file < 0 || pawnPromotionCandidate.file >= gameboardSize.first || pawnPromotionCandidate.rank < 0 || pawnPromotionCandidate.rank > gameboardSize.second) return
        chessgame.getChessboard().promotePiece(pawnPromotionCandidate,promotion)
        displayFigures()

        var sourceFile = 1
        val sourceRank = pawnPromotionCandidate.rank
        if(color == "white")sourceFile = gameboardSize.second - 2
        if(gameParameters.playMode == "human"){
            multiplayerDB.writePlayerMovement(
                gameData.gameId,
                PromotionMovement(
                    sourceFile = sourceFile,
                    sourceRank = sourceRank,
                    targetFile = pawnPromotionCandidate.file,
                    targetRank = pawnPromotionCandidate.rank,
                    promotion = promotion
                )
            )
        }

    }

    /** display figures from chess board in imageViews of chessActivity-layout */
    fun displayFigures() {
        val gameBoardSizeMap = Chessboard.getGameboardSize(gameParameters.name)

        for (file in 0..< gameBoardSizeMap.first) {
            for (rank in 0..< gameBoardSizeMap.second) {
                val x: Int = getDrawableFromName(
                    chessgame.getPieceName(file, rank),
                    chessgame.getPieceColor(file, rank)
                )
                if (x != -1) imageViews[file][rank].setImageResource(x)
            }
        }
        //display captures
        /*
        chessActivity.drawCapturedPiecesDrawable(
            "white",
            chessgame.getBitboard().blackCapturedPieces
        )
        chessActivity.drawCapturedPiecesDrawable(
            "black",
            chessgame.getBitboard().whiteCapturedPieces
        )
         */
        chessActivity.highlightActivePlayer(chessgame.getChessboard().getMovecolor().stringValue)
        switchClocks(chessgame.getChessboard().getMovecolor().stringValue)
    }

    private fun initAdaptBoard() {
        val gameboardSize : Pair<Int,Int> = Chessboard.getGameboardSize(gameParameters.name)

        val squares = Array(10) { file->
            Array(10) { rank ->
                val resId = chessActivity.resources.getIdentifier(
                    "${('A' + file).toChar()}${rank + 1}",
                    "id",
                    chessActivity.packageName
                )
                val imageView = chessActivity.findViewById<ImageView>(resId)

                // Hide squares that are outside the board dimensions
                if (file >= gameboardSize.first || rank >= gameboardSize.second) {
                    imageView.visibility = View.GONE
                    // Also hide the parent LinearLayout if all squares in the column are hidden
                    if (rank == 0) {  // Only need to do this once per column
                        imageView.parent?.let { parent ->
                            if (parent is LinearLayout && file >= gameboardSize.first) {
                                parent.visibility = View.GONE
                            }
                        }
                    }
                }
                imageView
            }
        }
        imageViews = squares
    }

    private fun initializeTimers() {
        playerTimer = ChessTimerPlayer.getPlTimerFromTimeMode(this, gameParameters.time)
        opponentTimer = ChessTimerOpponent.getOpTimerFromTimeMode(this, gameParameters.time)
        playerTimer?.create()
        opponentTimer?.create()
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
                    this.finishGame(gameData.playerID + " draw", null)
                }
            }
            builder.setNegativeButton("no",null)
            val dialog = builder.create()
            dialog.show()
        }
    }

    /** highlight the square from */
    private fun displayTargetMovements() {
        val targetMovements = chessgame.getTargetMovements(playerSelectedSquare.file,playerSelectedSquare.rank)
        for (targetMovement in targetMovements){
            markSquare(targetMovement.targetFile,targetMovement.targetRank)
        }
    }

    /** Get Drawable from figure name
     *
     */
    fun getDrawableFromName(type: String, color: String): Int {
        if (color == "white" && type == "king") {
            return R.drawable.white_king
        } else if (color == "white" && (type == "queen" || type == "met")) {
            return R.drawable.white_queen
        } else if (color == "white" && type == "pawn") {
            return R.drawable.white_pawn
        } else if (color == "white" && (type == "bishop" || type == "sa")) {
            return R.drawable.white_bishop
        } else if (color == "white" && type == "knight") {
            return R.drawable.white_knight
        } else if (color == "white" && type == "rook") {
            return R.drawable.white_rook
        } else if (color == "white" && type == "berolina") {
            return R.drawable.white_berolina
        } else if (color == "white" && type == "grasshopper") {
            return R.drawable.white_grasshopper
        } else if (color == "white" && type == "chancellor") {
            return R.drawable.white_chancellor
        } else if (color == "black" && type == "king") {
            return R.drawable.black_king
        } else if (color == "black" && (type == "queen" || type == "met")) {
            return R.drawable.black_queen
        } else if (color == "black" && type == "pawn") {
            return R.drawable.black_pawn
        } else if (color == "black" && (type == "bishop" || type == "sa")) {
            return R.drawable.black_bishop
        } else if (color == "black" && type == "knight") {
            return R.drawable.black_knight
        } else if (color == "black" && type == "rook") {
            return R.drawable.black_rook
        } else if (color == "black" && type == "berolina") {
            return R.drawable.black_berolina
        } else if (color == "black" && type == "grasshopper") {
            return R.drawable.black_grasshopper
        } else if (color == "black" && type == "chancellor") {
            return R.drawable.black_chancellor
        } else {
            return android.R.color.transparent
        }
    }

    /** mark square for figure that was selected */
    fun markFigure(v: View) {
        val fullName: String = chessActivity.resources.getResourceName(v.id)
        val name: String = fullName.substring(fullName.lastIndexOf("/") + 1)
        val file = nameToFile(name)
        val rank = nameToRank(name)
        resetFieldColor()
        if(playerSelectedSquare.file != -1 && playerSelectedSquare.rank != -1){ //unselect
            playerSelectedSquare.file = -1
            playerSelectedSquare.rank = -1
        } else {
            imageViews[file][rank].setBackgroundColor(
                getMixedColor(file, rank, Color.RED)
            )
            playerSelectedSquare.file = file
            playerSelectedSquare.rank = rank
        }
    }

    fun markSquare(file: Int, rank: Int) {
        imageViews[file][rank].setBackgroundColor(
            getMixedColor(file, rank, Color.YELLOW)
        )
    }

    /** reset chessboard square color to "normal" after highlighting it */
    private fun resetFieldColor() {
        playerSelectedSquare.file = -1
        playerSelectedSquare.rank = -1
        for(file in 0..<gameboardSize.first){
            for(rank in 0..<gameboardSize.second){
                if ((file + rank) % 2 != 0){
                    imageViews[file][rank].setBackgroundColor(
                        chessActivity.resources.getColor(
                            R.color.colorWhite
                        )
                    )
                } else {
                    imageViews[file][rank].setBackgroundColor(
                        chessActivity.resources.getColor(
                            R.color.colorBlack
                        )
                    )
                }
            }
        }
    }

    private fun handleGameEnd(){
        val gameEndResult = chessgame.getChessboard().checkForGameEnd()
        if(gameEndResult != ""){
            //end timers
            playerTimer?.cancel()
            opponentTimer?.cancel()
            if(gameEndResult.contains(gameParameters.playerColor)){
                finishGame(gameEndResult, true)
            } else {
                finishGame(gameEndResult, false)
            }
        }
    }

    /** helper functions for highlighting square */
    private fun getMixedColor(file: Int, rank: Int, color: Int): Int {
        return if ((file + rank) % 2 == 0) ColorUtils.blendARGB(
            color,
            chessActivity.resources.getColor(R.color.colorWhite),
            0.8f
        ) else ColorUtils.blendARGB( color,
            chessActivity.resources.getColor(R.color.colorBlack),
            0.8f
        )
    }

    private fun nameToFile(name: String): Int {
        return name.toLowerCase()[0] - 'a'
    }

    private fun nameToRank(name: String): Int {
        return Integer.valueOf(name.substring(1, 2)) - 1
    }

    fun onDestroy() {
        //finishGame(gameParameters.playerColor + " left the game", false)
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
                    handleGameEnd()
                }
            }
        }
    }

    override fun onFinishGame(gameId: String, cause: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                showGameEndDialog(cause)
            } catch (e: Exception) {
                // Handle any exceptions
                val data = Intent()
                data.putExtra(MainActivityListener.gamePlayerStatsExtra, playerStats)
                chessActivity.setResult(RESULT_OK, data)
                chessActivity.finish()
            }
        }
    }

    override fun onTickOpponentTimer(millisUntilFinished: Long) {
            tvOpponentName.text = transformLongToTimeString(millisUntilFinished)
    }

    override fun onFinishOpponentTimer() {
        chessgame.gameFinished = true
        finishGame("timeout. you won.", true)
    }

    override fun onTickPlayerTimer(millisUntilFinished: Long) {
        tvPlayerTime.text = transformLongToTimeString(millisUntilFinished)
    }

    override fun onFinishPlayerTimer() {
        finishGame("timeout. you lost.", false)
    }

    private fun showGameEndDialog(cause: String) {
        // Create custom layout for dialog
        val binding = DialogGameEndBinding.inflate(chessActivity.layoutInflater)

        with(binding) {
            // Set winner text
            tvWinner.text = cause

            val gameStats = chessgame.getChessboard().getGameStats()

            // Set statistics
            tvStatistics.text = buildString {
                append("Game Duration: ${formatDuration(gameStats.third)}\n")
                append("Moves Made: ${gameStats.second}\n")
                append("Pieces Captured: ${gameStats.first}\n")
                // Add more statistics as needed
            }
        }

        MaterialAlertDialogBuilder(chessActivity, R.style.MaterialAlertDialog_Rounded)
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                if (!chessActivity.isFinishing) {
                    val data = Intent()
                    data.putExtra(MainActivityListener.gamePlayerStatsExtra, playerStats)
                    chessActivity.setResult(RESULT_OK, data)
                    chessActivity.finish()
                }
            }
            .show()
    }

    // Utility function to format duration
    private fun formatDuration(durationMillis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
