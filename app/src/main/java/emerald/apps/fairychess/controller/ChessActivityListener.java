package emerald.apps.fairychess.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import emerald.apps.fairychess.R;
import emerald.apps.fairychess.databinding.DialogGameEndBinding;
import emerald.apps.fairychess.model.rating.ChessRatingSystem;
import emerald.apps.fairychess.model.timer.ChessTimerOpponent;
import emerald.apps.fairychess.model.timer.ChessTimerPlayer;
import emerald.apps.fairychess.model.Chessgame;
import emerald.apps.fairychess.model.board.Chessboard;
import emerald.apps.fairychess.model.board.Coordinate;
import emerald.apps.fairychess.model.board.GameStats;
import emerald.apps.fairychess.model.board.Movement;
import emerald.apps.fairychess.model.multiplayer.MultiplayerDB;
import emerald.apps.fairychess.model.multiplayer.MultiplayerDBGameInterface;
import emerald.apps.fairychess.model.timer.ChessTimerUtils;
import emerald.apps.fairychess.model.timer.ChessTimerPlayerInterface;
import emerald.apps.fairychess.view.ChessActivity;

import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import java.util.concurrent.TimeUnit;
import android.os.Handler;
import android.os.Looper;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** controller that propagates inputs from view to model and changes from model to view */
public class ChessActivityListener implements MultiplayerDBGameInterface, ChessTimerPlayerInterface, ChessTimerOpponent.ChessTimerOpponentInterface {

    private ChessActivity chessActivity;
    private Chessgame chessgame;
    private MultiplayerDB multiplayerDB;

    // View references
    private TextView tvPlayerName;
    private TextView tvOpponentName;
    private TextView tvPlayerELO;
    private TextView tvOpponentELO;
    private TextView tvPlayerTime;
    private TextView tvOpponentTime;
    private TextView tvCalcStatsInfo;
    private TextView tvCalcStatsHash;

    private MultiplayerDB.GameData gameData;
    private MainActivityListener.GameParameters gameParameters;
    private int[] gameboardSize;

    private PlayerSelectedSquare playerSelectedSquare = new PlayerSelectedSquare(-1, -1);

    private ImageView[][] imageViews;
    private ChessTimerPlayer playerTimer;
    private ChessTimerOpponent opponentTimer;
    private MultiplayerDB.PlayerStats playerStats;
    private MultiplayerDB.PlayerStats opponentStats;
    private boolean playerStatsUpdated = false;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ChessActivityListener(ChessActivity chessActivity) {
        this.chessActivity = chessActivity;
        getIntentData();
        gameboardSize = Chessboard.getGameboardSize(gameParameters.name);
        initializeViews();
        setupGame();
        if (gameParameters.playMode.equals("human")) {
            setupOnlineGame();
        }
        if (gameParameters.playMode.equals("ai") && gameParameters.playerColor.equals("black")) {
            makeAIMove();
        }
    }

    private void initializeViews() {
        // Initialize views based on player color
        if (gameParameters.playerColor.equals("white")) {
            tvPlayerName = chessActivity.findViewById(R.id.tv_playernameW);
            tvOpponentName = chessActivity.findViewById(R.id.tv_opponentnameW);
            tvPlayerELO = chessActivity.findViewById(R.id.tv_PlayerELOW);
            tvOpponentELO = chessActivity.findViewById(R.id.tv_OpponentELOW);
            tvPlayerTime = chessActivity.findViewById(R.id.tv_PlayerTimeW);
            tvOpponentTime = chessActivity.findViewById(R.id.tv_OpponentTimeW);
        } else {
            tvPlayerName = chessActivity.findViewById(R.id.tv_playernameB);
            tvOpponentName = chessActivity.findViewById(R.id.tv_opponentnameB);
            tvPlayerELO = chessActivity.findViewById(R.id.tv_PlayerELOB);
            tvOpponentELO = chessActivity.findViewById(R.id.tv_OpponentELOB);
            tvPlayerTime = chessActivity.findViewById(R.id.tv_PlayerTimeB);
            tvOpponentTime = chessActivity.findViewById(R.id.tv_OpponentTimeB);
        }
    }

    private void getIntentData() {
        playerStats = chessActivity.getIntent().getParcelableExtra(MainActivityListener.gamePlayerStatsExtra);
        opponentStats = chessActivity.getIntent().getParcelableExtra(MainActivityListener.gameOpponentStatsExtra);

        gameParameters = new MainActivityListener.GameParameters(
            chessActivity.getIntent().getStringExtra(MainActivityListener.gameNameExtra),
            chessActivity.getIntent().getStringExtra(MainActivityListener.gameModeExtra),
            chessActivity.getIntent().getStringExtra(MainActivityListener.gameTimeExtra),
            chessActivity.getIntent().getStringExtra(MainActivityListener.playerColorExtra),
            chessActivity.getIntent().getIntExtra(MainActivityListener.gameDifficultyExtra, 0)
        );

        gameData = new MultiplayerDB.GameData(
            chessActivity.getIntent().getStringExtra(MainActivityListener.gameIdExtra),
            chessActivity.getIntent().getStringExtra(MainActivityListener.gamePlayerNameExtra),
            chessActivity.getIntent().getStringExtra(MainActivityListener.gameOpponentNameExtra),
            playerStats.ELO,
            opponentStats.ELO
        );
    }

    private void setupGame() {
        // Update UI with game data
        tvPlayerName.setText(gameData.playerID);
        tvOpponentName.setText(gameData.opponentID);
        tvPlayerELO.setText(String.valueOf(playerStats.ELO));
        tvOpponentELO.setText(String.valueOf(opponentStats.ELO));

        //create ches game with parameters
        chessgame = new Chessgame(chessActivity, gameData, gameParameters);

        // Initialize timers and board
        initAdaptBoard();
        initializeTimers();
        displayFigures();

        //init multiplayerDB
        if (gameParameters.playMode.equals("human")) {
            multiplayerDB = new MultiplayerDB(this, chessgame);
            multiplayerDB.listenToGameIngame(gameData.gameId);
        }
    }

    private void setupOnlineGame() {
        //init multiplayerDB and set initial FEN if needed
        multiplayerDB = new MultiplayerDB(this, chessgame);
        // If we're player1 (creator), set the initial FEN
        if (!gameData.playerID.equals(gameData.opponentID)) {  // This check suggests we're player1
            String initialFen = chessgame.getChessboard().getCurrentFEN();
            multiplayerDB.writeFenAfterMovement(gameData.gameId, initialFen);
        }
        multiplayerDB.listenToGameIngame(gameData.gameId);
    }

    /** select, unselect and move figure */
    public void clickSquare(View clickedView) {
        //get file and rank of clicked view
        String clickedviewFullname = chessActivity.getResources().getResourceName(clickedView.getId());
        String clickedViewName = clickedviewFullname.substring(clickedviewFullname.lastIndexOf("/") + 1);
        if (clickedViewName.matches("[A-Z][0-9]+")) {
            if (playerSelectedSquare.getFile() == -1 && playerSelectedSquare.getRank() == -1) {
                //mark the clicked view
                markFigure(clickedView);
                displayTargetMovements();
                return;
            } else {
                //if a file and rank was selected => move from selected square to
                //calculate clickedFile and clickedRank
                int clickedFile = nameToFile(clickedViewName);
                int clickedRank = nameToRank(clickedViewName);
                if (playerSelectedSquare.getFile() == clickedFile && playerSelectedSquare.getRank() == clickedRank) {
                    resetFieldColor();
                    return;
                }
                Movement movement = new Movement(
                    playerSelectedSquare.getFile(),
                    playerSelectedSquare.getRank(),
                    clickedFile,
                    clickedRank
                );
                //check if movement is legal
                if (!chessgame.getChessboard().checkMove(movement)) {
                    //Toast.makeText(chessActivity, "Illegal move!", Toast.LENGTH_SHORT).show();
                    resetFieldColor();
                    return;
                }
                String moveResult = chessgame.movePlayerWithCheck(movement, chessgame.getChessboard().getMovecolor());
                //check for winner or draw
                handleGameEnd();
                /*
                if(chessgame.getChessboard().checkForPlayerWithDrawOpportunity() != null){//check for draw
                    offerDraw(chessgame.getChessboard().checkForPlayerWithDrawOpportunity().stringValue);
                }
                moveResult += handlePromotion();
                 */
                if (gameParameters.playMode.equals("human") && moveResult.equals("")) {
                    //persist fen after movement in firestore
                    String fenAfterMove = chessgame.getChessboard().getCurrentFEN();
                    multiplayerDB.writeFenAfterMovement(gameData.gameId, fenAfterMove);
                } else if (gameParameters.playMode.equals("ai")) {
                    makeAIMove();
                }
                displayFigures();
                resetFieldColor();
                //reset saved position after move
                playerSelectedSquare.setFile(-1);
                playerSelectedSquare.setRank(-1);
            }
        } else {
            resetFieldColor();
        }
    }

    public String handlePromotion() {
        if (chessgame.getChessboard().getPromotionCoordinate() != null) {
            pawnPromotion(chessgame.getChessboard().getPromotionCoordinate());
            return "promotion";
        }
        return "";
    }

    /** handle pawn promotion*/
    private void pawnPromotion(Coordinate pawnPromotionCandidate) {
        String pieceColor = chessgame.getChessboard().getPieceColor(pawnPromotionCandidate.getFile(), pawnPromotionCandidate.getRank());
        //handle pawn promotion of ai (exchange pawn with queen)
        if (!pieceColor.equals(gameParameters.playerColor) && gameParameters.playMode.equals("ai")) { //always promote to queen
            chessgame.getChessboard().promotePiece(pawnPromotionCandidate, chessgame.getChessboard().getPromotion());
            displayFigures();
        }
        //handle user pawn promotion by creating and handling alert dialog
        else {
            if (pieceColor.equals(gameParameters.playerColor)) {
                // create an alert builder
                AlertDialog.Builder builder = new AlertDialog.Builder(chessActivity);
                builder.setTitle("Pawn Promotion"); // set the custom layout
                View customLayout = chessActivity.getLayoutInflater().inflate(R.layout.promotion_layout, null);
                builder.setView(customLayout); // add a button
                RadioGroup radioGroup = customLayout.findViewById(R.id.radiogroup);
                builder.setPositiveButton("OK", (dialog, which) -> {
                    RadioButton radioButton = customLayout.findViewById(radioGroup.getCheckedRadioButtonId());
                    promotePawn(pieceColor, radioButton.getText().toString().toLowerCase(), pawnPromotionCandidate);
                });
                // create and show the alert dialog
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }
    }

    private void makeAIMove() {
        //calculate ai move in coroutine to avoid blocking the ui thread
        executorService.submit(() -> {
            try {
                Movement aiMovement = chessgame.getChessboard().calcMove(chessgame.getChessboard().getCurrentFEN());
                chessgame.movePlayerWithoutCheck(aiMovement);
                chessActivity.runOnUiThread(() -> {
                    displayFigures();
                });
                handleGameEnd();
            } catch (Exception e) {
                throw new RuntimeException("To catch any exception thrown for yourTask", e);
            }
        });
    }

    //propagate promotion information from the AlertDialog onto chessboard
    private void promotePawn(String color, String promotion, Coordinate pawnPromotionCandidate) {
        if (pawnPromotionCandidate.getFile() < 0 || pawnPromotionCandidate.getFile() >= gameboardSize[0]|| pawnPromotionCandidate.getRank() < 0 || pawnPromotionCandidate.getRank() > gameboardSize[1]) return;
        chessgame.getChessboard().promotePiece(pawnPromotionCandidate, promotion);
        displayFigures();

        if (gameParameters.playMode.equals("human")) {
            String fenAfterMove = chessgame.getChessboard().getCurrentFEN();
            multiplayerDB.writeFenAfterMovement(gameData.gameId, fenAfterMove);
        }
    }

    /** display figures from chess board in imageViews of chessActivity-layout */
    public void displayFigures() {
        int[] gameBoardSizeMap = Chessboard.getGameboardSize(gameParameters.name);

        for (int file = 0; file < gameBoardSizeMap[0]; file++) {
            for (int rank = 0; rank < gameBoardSizeMap[1]; rank++) {
                int x = getDrawableFromName(
                    chessgame.getPieceName(file, rank),
                    chessgame.getPieceColor(file, rank)
                );
                if (x != -1) imageViews[file][rank].setImageResource(x);
            }
        }
        //display captures
        /*
        chessActivity.drawCapturedPiecesDrawable(
            "white",
            chessgame.getBitboard().blackCapturedPieces
        );
        chessActivity.drawCapturedPiecesDrawable(
            "black",
            chessgame.getBitboard().whiteCapturedPieces
        );
         */
        chessActivity.highlightActivePlayer(chessgame.getChessboard().getMovecolor().getStringValue());
        switchClocks(chessgame.getChessboard().getMovecolor().getStringValue());
    }

    private void initAdaptBoard() {
        int[] gameboardSize = Chessboard.getGameboardSize(gameParameters.name);

        ImageView[][] squares = new ImageView[10][10];
        for (int file = 0; file < 10; file++) {
            for (int rank = 0; rank < 10; rank++) {
                int resId = chessActivity.getResources().getIdentifier(
                    (char) ('A' + file) + "" + (rank + 1),
                    "id",
                    chessActivity.getPackageName()
                );
                ImageView imageView = chessActivity.findViewById(resId);

                // Hide squares that are outside the board dimensions
                if (file >= gameboardSize[0]|| rank >= gameboardSize[1]) {
                    imageView.setVisibility(View.GONE);
                    // Also hide the parent LinearLayout if all squares in the column are hidden
                    if (rank == 0) {  // Only need to do this once per column
                        View parent = (View) imageView.getParent();
                        if (parent instanceof LinearLayout && file >= gameboardSize[0]) {
                            parent.setVisibility(View.GONE);
                        }
                    }
                }
                squares[file][rank] = imageView;
            }
        }
        imageViews = squares;
    }

    private void initializeTimers() {
        playerTimer = ChessTimerPlayer.getPlTimerFromTimeMode(this, gameParameters.time);
        opponentTimer = ChessTimerOpponent.getOpTimerFromTimeMode(this, gameParameters.time);
        playerTimer.create();
        opponentTimer.create();
    }

    /** switch clock from the player finishing the move to the other player */
    private void switchClocks(String activePlayerColor) {
        if (gameParameters.playerColor.equals(activePlayerColor)) {
            opponentTimer.pause();
            playerTimer.resume();
        } else {
            playerTimer.pause();
            opponentTimer.resume();
        }
    }

    /** create alert dialog for player that has a right for draw*/
    private void offerDraw(String color) {
        if (gameParameters.playMode.equals("human") && color.equals(gameParameters.playerColor)) {
            // create an alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(chessActivity);
            builder.setTitle("Do you want to draw?"); // set the custom layout
            builder.setPositiveButton("yes", (dialog, which) -> {
                finishGame(gameData.playerID + " draw", null);
            });
            builder.setNegativeButton("no", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /** highlight the square from */
    private void displayTargetMovements() {
        List<Movement> targetMovements = chessgame.getTargetMovements(playerSelectedSquare.getFile(), playerSelectedSquare.getRank());
        for (Movement targetMovement : targetMovements) {
            markSquare(targetMovement.getTargetFile(), targetMovement.getTargetRank());
        }
    }

    /** Get Drawable from figure name
     *
     */
    public int getDrawableFromName(String type, String color) {
        if (color.equals("white") && type.equals("king")) {
            return R.drawable.white_king;
        } else if (color.equals("white") && (type.equals("queen") || type.equals("met"))) {
            return R.drawable.white_queen;
        } else if (color.equals("white") && type.equals("pawn")) {
            return R.drawable.white_pawn;
        } else if (color.equals("white") && (type.equals("bishop") || type.equals("sa"))) {
            return R.drawable.white_bishop;
        } else if (color.equals("white") && type.equals("knight")) {
            return R.drawable.white_knight;
        } else if (color.equals("white") && type.equals("rook")) {
            return R.drawable.white_rook;
        } else if (color.equals("white") && type.equals("berolina")) {
            return R.drawable.white_berolina;
        } else if (color.equals("white") && type.equals("grasshopper")) {
            return R.drawable.white_grasshopper;
        } else if (color.equals("white") && type.equals("chancellor")) {
            return R.drawable.white_chancellor;
        } else if (color.equals("black") && type.equals("king")) {
            return R.drawable.black_king;
        } else if (color.equals("black") && (type.equals("queen") || type.equals("met"))) {
            return R.drawable.black_queen;
        } else if (color.equals("black") && type.equals("pawn")) {
            return R.drawable.black_pawn;
        } else if (color.equals("black") && (type.equals("bishop") || type.equals("sa"))) {
            return R.drawable.black_bishop;
        } else if (color.equals("black") && type.equals("knight")) {
            return R.drawable.black_knight;
        } else if (color.equals("black") && type.equals("rook")) {
            return R.drawable.black_rook;
        } else if (color.equals("black") && type.equals("berolina")) {
            return R.drawable.black_berolina;
        } else if (color.equals("black") && type.equals("grasshopper")) {
            return R.drawable.black_grasshopper;
        } else if (color.equals("black") && type.equals("chancellor")) {
            return R.drawable.black_chancellor;
        } else {
            return android.R.color.transparent;
        }
    }

    private static class PlayerSelectedSquare {
        private int file;
        private int rank;

        public PlayerSelectedSquare(int file, int rank) {
            this.file = file;
            this.rank = rank;
        }

        public int getFile(){
            return file;
        } 

        public int getRank(){
            return rank;
        } 

        public void setFile(int file){
            this.file = file;
        } 

        public void setRank(int rank){
            this.rank = rank;
        } 
    }

     /** mark square for figure that was selected */
    public void markFigure(View v) {
        String fullName = chessActivity.getResources().getResourceName(v.getId());
        String name = fullName.substring(fullName.lastIndexOf("/") + 1);
        int file = nameToFile(name);
        int rank = nameToRank(name);
        resetFieldColor();
        if (playerSelectedSquare.getFile() != -1 && playerSelectedSquare.getRank() != -1) { //unselect
            playerSelectedSquare.setFile(-1);
            playerSelectedSquare.setRank(-1);
        } else {
            imageViews[file][rank].setBackgroundColor(
                getMixedColor(file, rank, Color.RED)
            );
            playerSelectedSquare.setFile(file);
            playerSelectedSquare.setRank(rank);
        }
    }

    public void markSquare(int file, int rank) {
        imageViews[file][rank].setBackgroundColor(
            getMixedColor(file, rank, Color.YELLOW)
        );
    }

    /** reset chessboard square color to "normal" after highlighting it */
    private void resetFieldColor() {
        playerSelectedSquare.setFile(-1);
        playerSelectedSquare.setRank(-1);
        for (int file = 0; file < gameboardSize[0]; file++) {
            for (int rank = 0; rank < gameboardSize[1]; rank++) {
                if ((file + rank) % 2 != 0) {
                    imageViews[file][rank].setBackgroundColor(
                        chessActivity.getResources().getColor(R.color.colorWhite)
                    );
                } else {
                    imageViews[file][rank].setBackgroundColor(
                        chessActivity.getResources().getColor(R.color.colorBlack)
                    );
                }
            }
        }
    }

    private void handleGameEnd() {
        String gameEndResult = chessgame.getChessboard().checkForGameEnd();
        if (!gameEndResult.isEmpty()) {
            //end timers
            if (playerTimer != null) playerTimer.cancel();
            if (opponentTimer != null) opponentTimer.cancel();
            if (gameEndResult.contains(gameParameters.playerColor)) {
                finishGame(gameEndResult, true);
            } else {
                finishGame(gameEndResult, false);
            }
        }
    }

    /** helper functions for highlighting square */
    private int getMixedColor(int file, int rank, int color) {
        return (file + rank) % 2 == 0 ? 
            ColorUtils.blendARGB(color, chessActivity.getResources().getColor(R.color.colorWhite), 0.8f) :
            ColorUtils.blendARGB(color, chessActivity.getResources().getColor(R.color.colorBlack), 0.8f);
    }

    private int nameToFile(String name) {
        return Character.toLowerCase(name.charAt(0)) - 'a';
    }

    private int nameToRank(String name) {
        return Integer.parseInt(name.substring(1, 2)) - 1;
    }

    public void onDestroy() {
        finishGame(gameParameters.playerColor + " left the game", false);
    }

    /** finish a chess game by writing changes to multiplayerDB*/
    public void finishGame(String cause, Boolean playerWon) {
        if (chessgame.gameParameters.playMode.equals("human")) {
            chessgame.gameFinished = true;
            if (!playerStatsUpdated) {
                ChessRatingSystem.updatePlayerStats(
                    playerStats,
                    opponentStats,
                    playerWon
                );
                multiplayerDB.setPlayerStats(gameData.playerID, playerStats, cause);
                multiplayerDB.cancelGame(gameData.gameId);
                playerStatsUpdated = true;
            }
        } else {
            onFinishGame("", cause);
        }
    }

    @Override
    public void onSetPlayerstats(String cause) {
        multiplayerDB.finishGame(chessgame.gameData.gameId, cause);
    }

    @Override
    public void onGameChanged(String gameId, MultiplayerDB.GameState gameState) {
        if (gameState.gameFinished) {
            Toast.makeText(chessActivity, "opponent left game", Toast.LENGTH_LONG).show();
            finishGame("opponent left game", true);
        } else {
            if (!gameState.currentFen.isEmpty()) {
                chessgame.getChessboard().updateFen(gameState.currentFen);
                displayFigures();
                handleGameEnd();
            }
        }
    }

    @Override
    public void onFinishGame(String gameId, String cause) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            try {
                showGameEndDialog(cause);
            } catch (Exception e) {
                // Handle exceptions
                Intent data = new Intent();
                data.putExtra(MainActivityListener.gamePlayerStatsExtra, playerStats);
                chessActivity.setResult(Activity.RESULT_OK, data);
                chessActivity.finish();
            }
        });
    }

    @Override
    public void onTickOpponentTimer(long millisUntilFinished) {
        tvOpponentName.setText(ChessTimerUtils.transformLongToTimeString(millisUntilFinished));
    }

    @Override
    public void onFinishOpponentTimer() {
        chessgame.gameFinished = true;
        finishGame("timeout. you won.", true);
    }

    @Override
    public void onTickPlayerTimer(long millisUntilFinished) {
        tvPlayerTime.setText(ChessTimerUtils.transformLongToTimeString(millisUntilFinished));
    }

    @Override
    public void onFinishPlayerTimer() {
        finishGame("timeout. you lost.", false);
    }

    private void showGameEndDialog(String cause) {
        // Create custom layout for dialog
        DialogGameEndBinding binding = DialogGameEndBinding.inflate(chessActivity.getLayoutInflater());

        binding.tvWinner.setText(cause);

        GameStats gameStats = chessgame.getChessboard().getGameStats();

        binding.tvStatistics.setText(
            "Game Duration: " + formatDuration(gameStats.getGameDuration()) + "\n" +
            "Moves Made: " + gameStats.getMovesMade()+ "\n" +
            "Pieces Captured: " + gameStats.getPiecesCaptured()+ "\n"
            // Add more statistics as needed
        );

        new AlertDialog.Builder(chessActivity, R.style.MaterialAlertDialog_Rounded)
            .setView(binding.getRoot())
            .setCancelable(false)
            .setPositiveButton("OK", (dialog, which) -> {
                dialog.dismiss();
                if (!chessActivity.isFinishing()) {
                    Intent data = new Intent();
                    data.putExtra(MainActivityListener.gamePlayerStatsExtra, playerStats);
                    chessActivity.setResult(Activity.RESULT_OK, data);
                    chessActivity.finish();
                }
            })
            .show();
    }

    // Utility function to format duration
    private String formatDuration(long durationMillis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}

