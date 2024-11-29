package emerald.apps.fairychess.model.board

enum class Color(val stringValue: String) {
    WHITE("white"),
    BLACK("black");

    companion object {
        fun randomColor(): Color {
            return values().random()
        }

        fun oppositeColor(color: Color): Color {
            return if (color == WHITE) BLACK else WHITE
        }
    }
}