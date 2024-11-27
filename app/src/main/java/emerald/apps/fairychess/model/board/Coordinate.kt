package emerald.apps.fairychess.model.board

class Coordinate(private val rankValue: Int, private val fileValue: Int) {
    val file: Int
        get() = fileValue

    val rank: Int
        get() = rankValue
}