package emerald.apps.fairychess.model.board;

import java.util.ArrayList;

public class PromotionMovement extends Movement {
    private String promotion;

    public PromotionMovement(MovementNotation movementNotation, int sourceFile, int sourceRank, int targetFile, int targetRank, String promotion) {
        super(movementNotation, sourceFile, sourceRank, targetFile, targetRank);
        this.promotion = promotion;
    }

    public PromotionMovement(int sourceFile, int sourceRank, int targetFile, int targetRank, String promotion) {
        this(new MovementNotation("", new ArrayList<>(), "", new ArrayList<>(), ""), sourceFile, sourceRank, targetFile, targetRank, promotion);
    }

    public static String fromMovementToString(PromotionMovement promotionMovement) {
        return promotionMovement.getSourceRank()+ "_" + promotionMovement.getSourceFile()+ "_" +
                promotionMovement.getTargetRank()+ "_" + promotionMovement.getTargetFile()+ "_" + promotionMovement.promotion;
    }

    public static Movement fromStringToMovement(String string) {
        String[] coordinates = string.split("_");
        if (coordinates.length == 5) {
            int sourceFile = Integer.parseInt(coordinates[0]);
            int sourceRank = Integer.parseInt(coordinates[1]);
            int targetFile = Integer.parseInt(coordinates[2]);
            int targetRank = Integer.parseInt(coordinates[3]);
            String promotion = coordinates[4];
            return new PromotionMovement(sourceFile, sourceRank, targetFile, targetRank, promotion);
        }
        return new Movement(-1, -1, -1, -1);
    }
}


