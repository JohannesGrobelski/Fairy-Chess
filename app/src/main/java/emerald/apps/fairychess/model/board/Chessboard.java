package emerald.apps.fairychess.model.board;

import android.util.Log;
import emerald.apps.fairychess.controller.MainActivityListener;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import emerald.apps.fairychess.model.board.GameStats;

/** Important for all coordinate notations we use standard notation (file,rank) e.g. A1.
 *  "The columns of a chessboard are known as files, the rows are known as ranks ...
 *   files are labeled a through h from left to right, and ranks are labeled 1 through 8 from bottom to top" - Wikipedia
 *
 */
public class Chessboard {
    private MainActivityListener.GameParameters gameParameters;

    public Chessboard(MainActivityListener.GameParameters gameParameters) {
        this.gameParameters = gameParameters;
        int[] gameBoardSizeMap = getGameboardSize(gameParameters.name);
        int[][] board = new int[gameBoardSizeMap[0]][gameBoardSizeMap[1]]; //[file][rank]
        System.loadLibrary("stockfish");
        initEngine();
        setupInitialPosition();
    }

    private static final Map<String, int[]> gameBoardSizeMap = new HashMap<String, int[]>() {{
        put("clobber", new int[]{5, 6}); //files,ranks
    }};

    public static int[] getGameboardSize(String variant) {
        return gameBoardSizeMap.getOrDefault(variant, new int[]{8, 8});
    }

    private int piecesCaptured = 0;
    private int movesMade = 0;
    private long gameDuration = 0;
    private final long gameStartTime = System.currentTimeMillis();  // Add game start timestamp

    private Color movecolor = Color.WHITE;

    private String[][][] board; //file,rank,(piecename and piececolor)
    private Coordinate promotionCoordinate = null;
    private String fen;

    // Constants
    public int VALUE_MATE = 0;
    public int VALUE_DRAW = 0;
    public int NOTATION_DEFAULT = 0;
    public int NOTATION_SAN = 0;
    public int NOTATION_LAN = 0;
    public int NOTATION_SHOGI_HOSKING = 0;
    public int NOTATION_SHOGI_HODGES = 0;
    public int NOTATION_SHOGI_HODGES_NUMBER = 0;
    public int NOTATION_JANGGI = 0;
    public int NOTATION_XIANGQI_WXF = 0;
    public int NOTATION_THAI_SAN = 0;
    public int NOTATION_THAI_LAN = 0;
    public int FEN_OK = 0;

    // Native method declarations
    public native int initEngine();
    public native int[] version();
    public native String info();
    public native String[] variants();
    public native void setOption(String name, String value);
    public native void loadVariantConfig(String config);
    public native String startFen(String variant);
    public native boolean twoBoards(String variant);
    public native boolean capturesToHand(String variant);
    public native String[] legalMoves(String variant, String fen, boolean chess960);
    public native boolean givesCheck(String variant, String fen, String[] moves, boolean chess960);
    public native boolean isCapture(String variant, String fen, String move, boolean chess960);
    public native int[] isImmediateGameEnd(String variant, String fen, String[] moves, boolean chess960);
    public native boolean[] hasInsufficientMaterial(String variant, String fen, String[] moves, boolean chess960);
    public native String calcBestMove(String variant, String fen, int depth, int movetime, boolean chess960);
    public native void setPosition(String fen, boolean chess960);
    public native boolean isLegalMove(String variant, String fen, String move, boolean chess960);
    public native String getGameResult(String variant, String fen, String[] move, boolean chess960);
    public native String getFEN(String variant, String fen, String[] moves, boolean chess960, boolean sfen, boolean showPromoted, int countStarted);

    private void setupInitialPosition() {
        fen = startFen(this.gameParameters.name);
        updateBoardState();
        setPosition(fen, this.gameParameters.name.equals("fischerandom"));
        // Store the initial FEN
    }

    private void updateBoardState() {
        // Update promotion coordinate if any
        extractPiecesFromFen(fen);
        //checkForPromotion(fen)
    }

    public boolean checkMove(Movement movement) {
        String fromSquare = fileRankToSquare(movement.getSourceFile(), movement.getSourceRank());
        String toSquare = fileRankToSquare(movement.getTargetFile(), movement.getTargetRank());
        String moveStr = fromSquare + toSquare;

        return isLegalMove(this.gameParameters.name, fen, moveStr, false);
    }

    public String checkMoveAndMove(Movement movement) {
        String fromSquare = fileRankToSquare(movement.getSourceFile(), movement.getSourceRank());
        String toSquare = fileRankToSquare(movement.getTargetFile(), movement.getTargetRank());
        String moveStr = fromSquare + toSquare;

        if (isLegalMove(this.gameParameters.name, fen, moveStr, this.gameParameters.name.equals("fischerandom"))) {
            if (movecolor.getStringValue().equals(this.gameParameters.playerColor)) {
                boolean isCapture = isCapture(this.gameParameters.name, fen, moveStr, this.gameParameters.name.equals("fischerandom"));
                if (isCapture) {
                    piecesCaptured++;
                }
            }

            movecolor = (movecolor == Color.WHITE) ? Color.BLACK : Color.WHITE;
            fen = getFEN(this.gameParameters.name, fen, new String[]{moveStr}, this.gameParameters.name.equals("fischerandom"), false, false, 0); // update the FEN after the move
            movesMade++;  // Increment moves counter for statistic
            updateBoardState();
            return "";
        } else {
            return "illegal move";
        }
    }

    public void move(Movement movement) {
        String fromSquare = fileRankToSquare(movement.getSourceFile(), movement.getSourceRank());
        String toSquare = fileRankToSquare(movement.getTargetFile(), movement.getTargetRank());
        String moveStr = fromSquare + toSquare;

        // Check if move is a capture
        if (movecolor.getStringValue().equals(this.gameParameters.playerColor)) {
            boolean isCapture = isCapture(this.gameParameters.name, fen, moveStr, this.gameParameters.name.equals("fischerandom"));
            if (isCapture) {
                piecesCaptured++;
            }
        }

        movecolor = (movecolor == Color.WHITE) ? Color.BLACK : Color.WHITE;

        fen = getFEN(this.gameParameters.name, fen, new String[]{moveStr}, this.gameParameters.name.equals("fischerandom"), false, false, 0); // update the FEN after the move
        updateBoardState();
    }

    public List<Movement> getTargetMovementsAsMovementList(int file, int rank) {
        String square = fileRankToSquare(file, rank);
        String[] legalMoves = legalMoves(this.gameParameters.name, fen, this.gameParameters.name.equals("fischerandom"));
        List<Movement> movements = new ArrayList<>();
        for (String moveStr : legalMoves) {
            if (moveStr.charAt(0) - 'a' == file && moveStr.charAt(1) - '1' == rank) {
                int targetFile = moveStr.charAt(2) - 'a';
                int targetRank = moveStr.charAt(3) - '1';
                movements.add(new Movement(file, rank, targetFile, targetRank));
            }
        }
        return movements;
    }

    public Color getMovecolor() {
        return movecolor;
    }

    public String getPieceColor(int file, int rank) {
        return board[file][rank][1];
    }

    public String getPieceName(int file, int rank) {
        return board[file][rank][0];
    }

    public Coordinate getPromotionCoordinate() {
        return promotionCoordinate;
    }

    public void promotePiece(Coordinate coordinate, String promotion) {
        if (coordinate.equals(promotionCoordinate)) {
            String square = fileRankToSquare(coordinate.getRank(), coordinate.getFile());
            String moveStr = square + square + promotion.toLowerCase().charAt(0);
            if (isLegalMove(this.gameParameters.name, fen, moveStr, this.gameParameters.name.equals("fischerandom"))) {
                fen = getFEN(this.gameParameters.name, fen, new String[]{moveStr}, this.gameParameters.name.equals("fischerandom"), false, false, 0); // update the FEN after the move
                updateBoardState();
                promotionCoordinate = null;
            }
        }
    }

    public String checkForGameEnd() {
        String result = getGameResult(this.gameParameters.name, fen, new String[]{}, this.gameParameters.name.equals("fischerchess"));
        Log.i("Gameend?", result);
        return result;
    }

    public Movement calcMove(String fenString) {
        String bestMove = calcBestMove(this.gameParameters.name, fenString, this.gameParameters.difficulty, 30000, false);
        return transformStringToMovement(bestMove);
    }

    public Color checkForPlayerWithDrawOpportunity() {
        // Check for draw opportunities (stalemate, threefold repetition, etc.)
        //return (getGameResult() == 0) ? movecolor : null;
        return null;
    }

    public String getCurrentFEN() {
        return fen;
    }

    private String fileRankToSquare(int file, int rank) {
        return "" + (char) ('a' + file) + (rank + 1);
    }

    public void extractPiecesFromFen(String fen) {
        System.out.println("extractPiecesFromFen FEN: "+fen);
        int[] gameboardSize = getGameboardSize(gameParameters.name);
        String[] fenParts = fen.split(" ");

        // Get piece placement section
        String[] ranks = fenParts[0].split("/");

        // Replace numbers with spaces to create fixedFiles
        String[] fixedRanks = new String[ranks.length];
        for (int i = 0; i < ranks.length; i++) {
            StringBuilder sb = new StringBuilder();
            for (char charAt : ranks[i].toCharArray()) {
                if (Character.isDigit(charAt)) {
                    int count = Character.getNumericValue(charAt);
                    for (int j = 0; j < count; j++) {
                        sb.append(' '); // Replace number with equivalent spaces
                    }
                } else {
                    sb.append(charAt);
                }
            }
            fixedRanks[i] = sb.toString();
        }

        for (int rank = 0; rank < gameboardSize[1]; rank++) {
            int file = 0;
            for (char charAt : fixedRanks[rank].toCharArray()) {
                String[] piece = extractPieceFromChar(charAt);
                this.board[file][gameboardSize[1] - 1 - rank] = piece;
                file++; // Move to next file
            }
        }
        System.out.println(board.toString());
    }

    private Movement transformStringToMovement(String moveStr) {
        // UCI format is like "e2e4" or "e7e8q" for promotion
        if (moveStr.length() < 4) return Movement.emptyMovement();

        int sourceFile = moveStr.charAt(0) - 'a';
        int sourceRank = moveStr.charAt(1) - '1';
        int targetFile = moveStr.charAt(2) - 'a';
        int targetRank = moveStr.charAt(3) - '1';

        if (moveStr.length() == 5) {
            return new PromotionMovement(sourceFile, sourceRank, targetFile, targetRank, String.valueOf(moveStr.charAt(4)));
        } else {
            return new Movement(sourceFile, sourceRank, targetFile, targetRank);
        }
    }

    //TODO: get promotion from engine
    public String getPromotion() {
        return "queen";
    }

    // Add method to get current game duration
    public long getCurrentGameDuration() {
        return System.currentTimeMillis() - gameStartTime;
    }

    public String[] extractPieceFromChar(char charAt) {
        String color = Character.isUpperCase(charAt) ? "white" : "black";
        String pieceName;
        switch (Character.toLowerCase(charAt)) {
            case 'p':
                pieceName = "pawn";
                break;
            case 'g':
                pieceName = "grasshopper";
                break;
            case 'c':
                pieceName = "chancellor";
                break;
            case 'n':
                pieceName = "knight";
                break;
            case 'b':
                pieceName = "bishop";
                break;
            case 's':
                pieceName = "sa"; //Sa - Bishop in cambodian
                break;
            case 'r':
                pieceName = "rook";
                break;
            case 'q':
                pieceName = "queen";
                break;
            case 'm':
                pieceName = "met"; //Met - restricted queen in cambodian
                break;
            case 'k':
                pieceName = "king";
                break;
            default:
                pieceName = "";
        }
        return new String[]{pieceName, color};
    }

    // Add method to get game statistics
    public GameStats getGameStats() {
        return new GameStats(piecesCaptured, movesMade, getCurrentGameDuration());
    }

    public void updateFen(String currentFen) {
        fen = currentFen;
        updateBoardState();
        setPosition(fen, this.gameParameters.name.equals("fischerandom"));
    }

    @Override
    public String toString() {
        int[] gameBoardSize = getGameboardSize(gameParameters.name);
        int files = gameBoardSize[0];
        int ranks = gameBoardSize[1];
        StringBuilder sb = new StringBuilder();

        // Add file labels at the top
        sb.append("   "); // Space for left margin
        for (int file = 0; file < files; file++) {
            sb.append(" ").append((char) ('A' + file)).append(" ");
        }
        sb.append("\n");

        // Add top border
        sb.append("  ╔");
        for (int file = 0; file < files; file++) {
            sb.append("═══");
            if (file < files - 1) sb.append("╦");
        }
        sb.append("╗\n");

        // Add board content with rank labels
        for (int rank = ranks - 1; rank >= 0; rank--) {
            sb.append(rank + 1).append(" ║"); // Rank number and left border

            // Add pieces
            for (int file = 0; file < files; file++) {
                String[] piece = board[file][rank];
                String symbol;
                if (piece[0].isEmpty()) {
                    symbol = " ";
                } else if (piece[1].equals("white")) {
                    switch (piece[0]) {
                        case "pawn":
                            symbol = "P";
                            break;
                        case "knight":
                            symbol = "N";
                            break;
                        case "bishop":
                            symbol = "B";
                            break;
                        case "rook":
                            symbol = "R";
                            break;
                        case "queen":
                            symbol = "Q";
                            break;
                        case "king":
                            symbol = "K";
                            break;
                        case "grasshopper":
                            symbol = "G";
                            break;
                        case "chancellor":
                            symbol = "C";
                            break;
                        case "sa":
                            symbol = "S";
                            break;
                        case "met":
                            symbol = "M";
                            break;
                        default:
                            symbol = "?";
                    }
                } else {
                    switch (piece[0]) {
                        case "pawn":
                            symbol = "p";
                            break;
                        case "knight":
                            symbol = "n";
                            break;
                        case "bishop":
                            symbol = "b";
                            break;
                        case "rook":
                            symbol = "r";
                            break;
                        case "queen":
                            symbol = "q";
                            break;
                        case "king":
                            symbol = "k";
                            break;
                        case "grasshopper":
                            symbol = "g";
                            break;
                        case "chancellor":
                            symbol = "c";
                            break;
                        case "sa":
                            symbol = "s";
                            break;
                        case "met":
                            symbol = "m";
                            break;
                        default:
                            symbol = "?";
                    }
                }
                sb.append(" ").append(symbol).append(" ");

                // Add vertical border
                if (file < files - 1) sb.append("║");
            }
            sb.append("║\n"); // Right border

            // Add rank separator, except for the last rank
            if (rank > 0) {
                sb.append("  ╠");
                for (int file = 0; file < files; file++) {
                    sb.append("═══");
                    if (file < files - 1) sb.append("╬");
                }
                sb.append("╣\n");
            }
        }

        // Add bottom border
        sb.append("  ╚");
        for (int file = 0; file < files; file++) {
            sb.append("═══");
            if (file < files - 1) sb.append("╩");
        }
        sb.append("╝\n");

        // Add current state information
        sb.append("\nMove: ").append(movecolor.getStringValue());
        sb.append("\nFEN: ").append(fen);

        return sb.toString();
    }
}

