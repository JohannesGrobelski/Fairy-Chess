package emerald.apps.fairychess.model.board;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

class MovementNotation {
    private String grouping;
    private List<String> conditions;
    private String movetype;
    private List<String> distances;
    private String direction;

    public MovementNotation(String grouping, List<String> conditions, String movetype, List<String> distances, String direction) {
        this.grouping = grouping;
        this.conditions = conditions;
        this.movetype = movetype;
        this.distances = distances;
        this.direction = direction;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MovementNotation) {
            MovementNotation movementNotation = (MovementNotation) other;
            return grouping.equals(movementNotation.grouping) &&
                    conditions.equals(movementNotation.conditions) &&
                    movetype.equals(movementNotation.movetype) &&
                    distances.equals(movementNotation.distances) &&
                    direction.equals(movementNotation.direction);
        }
        return super.equals(other);
    }

    @Override
    public String toString() {
        return grouping + conditions.toString() + movetype + distances + direction;
    }

    public static final MovementNotation KING = new MovementNotation("", new ArrayList<>(), "", Collections.singletonList("1"), "*");
    public static final MovementNotation PAWN_ENPASSANTE = new MovementNotation("", new ArrayList<>(), "", Collections.singletonList("1"), "EN_PASSANTE");
    public static final MovementNotation CASTLING_SHORT_WHITE = new MovementNotation("", new ArrayList<>(), "CASTLING_SHORT_WHITE", new ArrayList<>(), "");
    public static final MovementNotation CASTLING_LONG_WHITE = new MovementNotation("", new ArrayList<>(), "CASTLING_LONG_WHITE", new ArrayList<>(), "");
    public static final MovementNotation CASTLING_SHORT_BLACK = new MovementNotation("", new ArrayList<>(), "CASTLING_SHORT_BLACK", new ArrayList<>(), "");
    public static final MovementNotation CASTLING_LONG_BLACK = new MovementNotation("", new ArrayList<>(), "CASTLING_LONG_BLACK", new ArrayList<>(), "");

    public static List<MovementNotation> parseMovementString(String movementString) {
        if (movementString.isEmpty()) return new ArrayList<>();
        List<MovementNotation> movementList = new ArrayList<>();
        String[] movementArray = movementString.split(",");
        for (String submovement : movementArray) {
            String submovementString = submovement;
            String grouping = "";
            List<String> conditions = new ArrayList<>();
            String movetype = "";
            List<String> distances = new ArrayList<>();
            String direction = "";
            // move type
            if (submovementString.contains("~")) {
                movetype = "~";
                submovementString = submovementString.replace("~", "");
            }
            if (submovementString.contains("^")) {
                movetype = "^";
                submovementString = submovementString.replace("^", "");
            }
            if (submovementString.contains("g")) {
                movetype = "g";
                submovementString = submovementString.replace("g", "");
            }
            // grouping
            if (submovementString.contains("/")) {
                grouping = "/";
                submovementString = submovementString.replace("/", "");
            }
            if (submovementString.contains("&")) {
                grouping = "&";
                submovementString = submovementString.replace("&", "");
            }
            if (submovementString.contains(".")) {
                grouping = ".";
                submovementString = submovementString.replace(".", "");
            }
            // move conditions
            if (submovementString.contains("i")) {
                conditions.add("i");
                submovementString = submovementString.replace("i", "");
            }
            if (submovementString.contains("c")) {
                conditions.add("c");
                submovementString = submovementString.replace("c", "");
            }
            if (submovementString.contains("o")) {
                conditions.add("o");
                submovementString = submovementString.replace("o", "");
            }
            // direction
            if (submovementString.contains(">=")) {
                direction = ">=";
                submovementString = submovementString.replace(">=", "");
            }
            if (submovementString.contains("<=")) {
                direction = "<=";
                submovementString = submovementString.replace("<=", "");
            }
            if (submovementString.contains("<>")) {
                direction = "<>";
                submovementString = submovementString.replace("<>", "");
            }
            if (submovementString.contains("=")) {
                direction = "=";
                submovementString = submovementString.replace("=", "");
            }
            if (submovementString.contains("X>")) {
                direction = "X>";
                submovementString = submovementString.replace("X>", "");
            }
            if (submovementString.contains("X<")) {
                direction = "X<";
                submovementString = submovementString.replace("X<", "");
            }
            if (submovementString.contains("X")) {
                direction = "X";
                submovementString = submovementString.replace("X", "");
            }
            if (submovementString.contains(">")) {
                direction = ">";
                submovementString = submovementString.replace(">", "");
            }
            if (submovementString.contains("<")) {
                direction = "<";
                submovementString = submovementString.replace("<", "");
            }
            if (submovementString.contains("+")) {
                direction = "+";
                submovementString = submovementString.replace("+", "");
            }
            if (submovementString.contains("*")) {
                direction = "*";
                submovementString = submovementString.replace("*", "");
            }
            // distance
            if (grouping.isEmpty()) {
                if (submovementString.contains("n")) distances.add("n");
                if (submovementString.matches(".*[0-9].*")) distances.add(submovementString.replaceAll("\\D+", ""));
            } else {
                Collections.addAll(distances, submovementString.split(""));
                distances.removeAll(Collections.singleton(""));
            }
            movementList.add(new MovementNotation(grouping, conditions, movetype, distances, direction));
        }
        return movementList;
    }
}
