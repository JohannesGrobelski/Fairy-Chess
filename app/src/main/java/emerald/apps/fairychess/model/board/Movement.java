package emerald.apps.fairychess.model.board;

import java.util.ArrayList;
import java.util.List;

public class Movement {
    private MovementNotation movementNotation;
    private int sourceFile;
    private int sourceRank;
    private int targetFile;
    private int targetRank;

    public Movement(MovementNotation movementNotation, int sourceFile, int sourceRank, int targetFile, int targetRank) {
        this.movementNotation = movementNotation;
        this.sourceFile = sourceFile;
        this.sourceRank = sourceRank;
        this.targetFile = targetFile;
        this.targetRank = targetRank;
    }

    public Movement(int sourceFile, int sourceRank, int targetFile, int targetRank) {
        this(new MovementNotation("", new ArrayList<>(), "", new ArrayList<>(), ""), sourceFile, sourceRank, targetFile, targetRank);
    }

    public Movement(MovementNotation movementNotation, Coordinate source, int targetFile, int targetRank) {
        this(movementNotation, source.getFile(), source.getRank(), targetFile, targetRank);
    }

    public Movement(Coordinate source, int targetFile, int targetRank) {
        this(source.getFile(), source.getRank(), targetFile, targetRank);
    }

    public Movement(Coordinate source, Coordinate target) {
        this(source.getFile(), source.getRank(), target.getFile(), target.getRank());
    }

    public Coordinate getSourceCoordinate() {
        return new Coordinate(sourceFile, sourceRank);
    }

    public Coordinate getTargetCoordinate() {
        return new Coordinate(targetFile, targetRank);
    }

    public int getRankDif() {
        return Math.abs(targetRank - sourceRank);
    }

    public int getFileDif() {
        return Math.abs(targetFile - sourceFile);
    }

    public int getSignRank() {
        return (int) Math.signum(targetRank - sourceRank);
    }

    public int getTargetFile(){
        return this.targetFile;
    }

    public int getTargetRank(){
        return this.targetRank;
    }

    public int getSourceFile(){
        return this.sourceFile;
    }

    public int getSourceRank(){
        return this.sourceRank;
    }


    @Override
    public boolean equals(Object other) {
        if (other instanceof Movement) {
            Movement movement = (Movement) other;
            return sourceFile == movement.sourceFile &&
                    sourceRank == movement.sourceRank &&
                    targetFile == movement.targetFile &&
                    targetRank == movement.targetRank &&
                    movementNotation.equals(movement.movementNotation);
        }
        return super.equals(other);
    }

    public static Movement emptyMovement() {
        return new Movement(0, 0, 0, 0);
    }

    public static String fromMovementToString(Movement movement) {
        return movement.sourceFile + "_" + movement.sourceRank + "_" + movement.targetFile + "_" + movement.targetRank;
    }

    public static Movement fromStringToMovement(String string) {
        String[] coordinates = string.split("_");
        if (coordinates.length == 4) {
            int sourceFile = Integer.parseInt(coordinates[0]);
            int sourceRank = Integer.parseInt(coordinates[1]);
            int targetFile = Integer.parseInt(coordinates[2]);
            int targetRank = Integer.parseInt(coordinates[3]);
            return new Movement(sourceFile, sourceRank, targetFile, targetRank);
        }
        return new Movement(-1, -1, -1, -1);
    }

    public static String fromMovementListToString(List<Movement> movements) {
        StringBuilder returnString = new StringBuilder();
        for (Movement movement : movements) {
            returnString.append(movement.sourceFile).append("_").append(movement.sourceRank)
                    .append("_").append(movement.targetFile).append("_").append(movement.targetRank);
            if (movement != movements.get(movements.size() - 1)) returnString.append(";");
        }
        return returnString.toString();
    }

    public static List<Movement> fromStringToMovementList(String string) {
        List<Movement> movementList = new ArrayList<>();
        for (String substring : string.split(";")) {
            if (substring.split("_").length == 4) {
                int sourceFile = Integer.parseInt(substring.split("_")[0]);
                int sourceRank = Integer.parseInt(substring.split("_")[1]);
                int targetFile = Integer.parseInt(substring.split("_")[2]);
                int targetRank = Integer.parseInt(substring.split("_")[3]);
                movementList.add(new Movement(sourceFile, sourceRank, targetFile, targetRank));
            }
        }
        return movementList;
    }

    public String asString(String playerColor) {
        return playerColor + ": " + sourceFile + "_" + sourceRank + "_" + targetFile + "_" + targetRank;
    }

    public String asString2(String playerColor) {
        return playerColor + ": " + getLetterFromInt(sourceFile) + sourceRank + "_" + getLetterFromInt(targetFile) + targetRank;
    }

    public String getLetterFromInt(int intValue) {
        switch (intValue) {
            case 0: return "A";
            case 1: return "B";
            case 2: return "C";
            case 3: return "D";
            case 4: return "E";
            case 5: return "F";
            case 6: return "G";
            case 7: return "H";
            default: return "";
        }
    }
}
