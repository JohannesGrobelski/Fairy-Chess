package emerald.apps.fairychess.model.Caching

import emerald.apps.fairychess.model.Movement

data class FigureState(
    val bbFigure: ULong,                // Position of the specific figure
    val bbFriendlyPieces: ULong,        // Position of all friendly pieces
    val bbEnemyPieces: ULong            // Position of all enemy pieces
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FigureState

        if (bbFigure != other.bbFigure) return false
        if (bbFriendlyPieces != other.bbFriendlyPieces) return false
        if (bbEnemyPieces != other.bbEnemyPieces) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bbFigure.hashCode()
        result = 31 * result + bbFriendlyPieces.hashCode()
        result = 31 * result + bbEnemyPieces.hashCode()
        return result
    }
}

class PossibleMovementsCache {
    private val possibleMovementsCache = mutableMapOf<FigureState, List<Movement>>()
    private var cacheHits: Long = 0
    private var cacheAccesses: Long = 0

    fun getPossibleMovements(bbFigure: ULong, bbColorComposite: Array<ULong>, isWhite: Boolean): List<Movement>? {
        cacheAccesses++
        val state = FigureState(
            bbFigure = bbFigure,
            bbFriendlyPieces = bbColorComposite[if (isWhite) 0 else 1],
            bbEnemyPieces = bbColorComposite[if (isWhite) 1 else 0]
        )
        val result = possibleMovementsCache[state]
        if (result != null) {
            cacheHits++
        }
        return result
    }

    fun putPossibleMovements(bbFigure: ULong, bbColorComposite: Array<ULong>, isWhite: Boolean, movements: List<Movement>) {
        val state = FigureState(
            bbFigure = bbFigure,
            bbFriendlyPieces = bbColorComposite[if (isWhite) 0 else 1],
            bbEnemyPieces = bbColorComposite[if (isWhite) 1 else 0]
        )
        possibleMovementsCache[state] = movements
    }

    fun clear() {
        possibleMovementsCache.clear()
        cacheHits = 0
        cacheAccesses = 0
    }

    fun getKeysize(): Int {
        return possibleMovementsCache.keys.size
    }

    fun getCacheHits(): Long = cacheHits

    fun getCacheAccesses(): Long = cacheAccesses

    fun getCacheHitRate(): Double = if (cacheAccesses > 0) {
        cacheHits.toDouble() / cacheAccesses.toDouble() * 100.0
    } else {
        0.0
    }
}