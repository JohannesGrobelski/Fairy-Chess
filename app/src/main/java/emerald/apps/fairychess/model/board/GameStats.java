package emerald.apps.fairychess.model.board;

public class GameStats {
    private long piecesCaptured;
    private long movesMade;
    private long gameDuration;

    public GameStats(long piecesCaptured, long movesMade, long gameDuration) {
        this.piecesCaptured = piecesCaptured;
        this.movesMade = movesMade;
        this.gameDuration = gameDuration;
    }

    public long getPiecesCaptured() {
        return piecesCaptured;
    }

    public long getMovesMade() {
        return movesMade;
    }

    public long getGameDuration() {
        return gameDuration;
    }

    @Override
    public String toString() {
        return "GameStats{" +
               "piecesCaptured=" + piecesCaptured +
               ", movesMade=" + movesMade +
               ", gameDuration=" + gameDuration +
               '}';
    }
}
