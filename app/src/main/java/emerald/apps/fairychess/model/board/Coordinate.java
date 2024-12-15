package emerald.apps.fairychess.model.board;

public class Coordinate {
    private final int fileValue;
    private final int rankValue;

    public Coordinate(int fileValue, int rankValue) {
        this.fileValue = fileValue;
        this.rankValue = rankValue;
    }

    public int getFile() {
        return fileValue;
    }

    public int getRank() {
        return rankValue;
    }
}
