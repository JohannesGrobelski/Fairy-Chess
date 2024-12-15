package emerald.apps.fairychess.model.board;

import java.util.Random;

public enum Color {
    WHITE("white"),
    BLACK("black");

    private final String stringValue;

    Color(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public static Color randomColor() {
        Color[] colors = Color.values();
        return colors[new Random().nextInt(colors.length)];
    }

    public static Color oppositeColor(Color color) {
        return (color == WHITE) ? BLACK : WHITE;
    }
}
