package emerald.apps.fairychess.model.board

class Coordinate(private val fileValue: Int, private val rankValue: Int) {
    val file: Int
        get() = fileValue

    val rank: Int
        get() = rankValue
}