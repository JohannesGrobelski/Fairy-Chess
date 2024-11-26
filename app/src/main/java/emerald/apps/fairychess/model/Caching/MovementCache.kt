package emerald.apps.fairychess.model.Caching

import emerald.apps.fairychess.model.Bitboard
import emerald.apps.fairychess.model.Movement

class MovementCache {
    var movementCacheHits = 0


    data class BoardState(
        val bbFigures: Map<String, Array<ULong>>,
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

    private val moveCache = mutableMapOf<BoardState, Movement>()

    fun getMove(bitboard: Bitboard): Movement? {
        val state = BoardState(
            bbFigures = bitboard.bbFigures.toMap(),
            bbMovedCaptured = bitboard.bbMovedCaptured,
            bbComposite = bitboard.bbComposite,
            bbColorComposite = bitboard.bbColorComposite.clone(),
            moveColor = bitboard.moveColor
        )
        if(moveCache[state] != null){
            ++movementCacheHits
        }
        return moveCache[state]
    }

    fun putMove(bitboard: Bitboard, move: Movement) {
        val state = BoardState(
            bbFigures = bitboard.bbFigures.toMap(),
            bbMovedCaptured = bitboard.bbMovedCaptured,
            bbComposite = bitboard.bbComposite,
            bbColorComposite = bitboard.bbColorComposite.clone(),
            moveColor = bitboard.moveColor
        )
        moveCache[state] = move
    }

    fun clear() {
        moveCache.clear()
    }

    fun getKeysize() : Int {
        return moveCache.keys.size
    }
}