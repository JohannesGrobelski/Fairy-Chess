package emerald.apps.fairychess.model;

import emerald.apps.fairychess.controller.MainActivityListener;
import emerald.apps.fairychess.model.board.Chessboard;
import emerald.apps.fairychess.model.board.Color;
import emerald.apps.fairychess.model.board.Movement;
import emerald.apps.fairychess.model.board.PromotionMovement;
import emerald.apps.fairychess.model.multiplayer.MultiplayerDB;
import emerald.apps.fairychess.view.ChessActivity;
import java.util.List;
import java.util.Arrays;

public class Chessgame {
    private Chessboard chessboard;
    public MultiplayerDB.GameData gameData;
    public MainActivityListener.GameParameters gameParameters;
    public String opponentColor;
    public boolean gameFinished = false;
    private ChessActivity chessActivity;
    private static final String[] colors = {"white", "black"};

    public Chessgame() {
    }

    public Chessgame(ChessActivity chessActivity, MultiplayerDB.GameData gameData, MainActivityListener.GameParameters gameParameters) {
        this.chessboard = new Chessboard(gameParameters);
        this.gameData = gameData;
        this.gameParameters = gameParameters;
        this.opponentColor = oppositeColor(gameParameters.playerColor);
        this.gameFinished = false;
        this.chessActivity = chessActivity;
    }

    /** execute movement and check if color allows movement */
    public String movePlayerWithCheck(Movement movement, Color color) {
        if (movement != null) {
            String returnValue = chessboard.checkMoveAndMove(movement);
            //gameFinished = chessboard.checkForWinner() != null;
            return returnValue;
        } else {
            return "no move made";
        }
    }

    public void movePlayerWithoutCheck(Movement movement) {
        if (movement != null) {
            chessboard.move(movement);
        }
    }

    public List<Movement> getTargetMovements(int sourceFile, int sourceRank) {
        return chessboard.getTargetMovementsAsMovementList(sourceFile, sourceRank);
    }

    public String getPieceName(int sourceFile, int sourceRank) {
        return chessboard.getPieceName(sourceFile, sourceRank);
    }

    public String getPieceColor(int file, int rank) {
        return chessboard.getPieceColor(file, rank);
    }

    public Chessboard getChessboard() {
        return chessboard;
    }

    public void makeMove(String moveString) {
        long underscoreCount = moveString.chars().filter(ch -> ch == '_').count();
        if (underscoreCount == 3) {
            makeMove(Movement.fromStringToMovement(moveString)); // normal movement
        } else if (underscoreCount == 4) {
            makeMove(PromotionMovement.fromStringToMovement(moveString)); // movement + promotion
        }
    }

    public void makeMove(Movement movement) {
        chessboard.checkMoveAndMove(movement);
        gameFinished = !chessboard.checkForGameEnd().isEmpty();
    }

    public String oppositeColor(String color) {
        if (color.isEmpty()) return color;
        return color.equals(colors[0]) ? colors[1] : colors[0];
    }
}
