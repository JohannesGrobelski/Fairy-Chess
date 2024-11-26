import emerald.apps.fairychess.model.Bitboard

class ValueHash {
    data class BoardState(
        val bbFigures: Map<String, Array<ULong>>,  // Using Map instead of MutableMap for data class
        val bbMovedCaptured: ULong,
        val bbComposite: ULong,
        val bbColorComposite: Array<ULong>,
        val moveColor: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as BoardState

            if (bbFigures.size != other.bbFigures.size) return false
            for (key in bbFigures.keys) {
                if (!other.bbFigures.containsKey(key)) return false
                if (!bbFigures[key]!!.contentEquals(other.bbFigures[key]!!)) return false
            }
            if (bbMovedCaptured != other.bbMovedCaptured) return false
            if (bbComposite != other.bbComposite) return false
            if (!bbColorComposite.contentEquals(other.bbColorComposite)) return false
            if (moveColor != other.moveColor) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bbFigures.entries.sumOf { (key, value) ->
                key.hashCode() * 31 + value[0].hashCode() * 31 + value[1].hashCode()
            }
            result = 31 * result + bbMovedCaptured.hashCode()
            result = 31 * result + bbComposite.hashCode()
            result = 31 * result + bbColorComposite.contentHashCode()
            result = 31 * result + moveColor.hashCode()
            return result
        }
    }

    private val cache = mutableMapOf<BoardState, Int>()  // Or whatever your result type is

    fun getFromCache(bitboard: Bitboard): Int? {
        val state = BoardState(
            bbFigures = bitboard.bbFigures.toMap(),  // Create immutable copy
            bbMovedCaptured = bitboard.bbMovedCaptured,
            bbComposite = bitboard.bbComposite,
            bbColorComposite = bitboard.bbColorComposite.clone(),
            moveColor = bitboard.moveColor
        )
        return cache[state]
    }

    fun putInCache(bitboard: Bitboard, result: Int) {
        val state = BoardState(
            bbFigures = bitboard.bbFigures.toMap(),  // Create immutable copy
            bbMovedCaptured = bitboard.bbMovedCaptured,
            bbComposite = bitboard.bbComposite,
            bbColorComposite = bitboard.bbColorComposite.clone(),
            moveColor = bitboard.moveColor
        )
        cache[state] = result
    }

    fun getKeysize() : Int {
        return cache.keys.size
    }
}