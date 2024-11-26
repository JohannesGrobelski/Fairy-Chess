# Chess Engine Caching System

## Overview
The caching system consists of three specialized caches that optimize different aspects of chess engine operations:
- Value Cache: Stores position evaluations
- Movement Cache: Stores best moves for positions
- Possible Movements Cache: Stores legal moves for individual pieces

## Cache Components

### Value Cache
Caches numerical evaluations of board positions.

- **Key**: Complete board state including
    - Figure positions (bbFigures)
    - Moved/captured piece tracking (bbMovedCaptured)
    - Overall board composition (bbComposite)
    - Color-specific piece positions (bbColorComposite)
    - Current move color
- **Value**: Integer representing position evaluation
- **Features**:
    - Hit tracking via `valueCacheHits`
    - Immutable state copying for thread safety

### Movement Cache
Stores best moves found for specific board positions.

- **Key**: Same complete board state as Value Cache
- **Value**: Single `Movement` object representing best move found
- **Features**:
    - Hit tracking via `movementCacheHits`
    - Used primarily during search to remember best moves

### Possible Movements Cache
Optimized cache for storing legal moves of individual pieces.

- **Key**: Simplified state (`FigureState`) containing:
    - Piece position (bbFigure)
    - Friendly pieces position (bbFriendlyPieces)
    - Enemy pieces position (bbEnemyPieces)
- **Value**: List of possible `Movement` objects
- **Features**:
    - Detailed performance tracking:
        - Cache hits
        - Cache accesses
        - Hit rate calculation
    - Color-aware caching (automatically handles white/black perspectives)

## Common Features
All caches provide:
- `clear()`: Reset cache state
- `getKeysize()`: Get number of cached entries
- Thread-safe state handling through immutable copies

## Performance Monitoring
- Value and Movement caches track hits with simple counters
- Possible Movements cache provides detailed statistics:
    - `getCacheHits()`: Number of successful retrievals
    - `getCacheAccesses()`: Total lookup attempts
    - `getCacheHitRate()`: Success rate as percentage

## Implementation Notes

### State Management
- Uses data classes for cache keys
- Custom `equals()` and `hashCode()` implementations for array handling
- Defensive copying of mutable state (`clone()`, `toMap()`)

### Memory Safety
- Immutable copies created for all mutable state
- Array content comparison rather than reference comparison
- Proper deep copying of nested structures

## Usage Example
```kotlin
// Value Cache
val valueCache = ValueCache()
val evaluation = valueCache.getFromCache(currentBoard)
if (evaluation == null) {
    val newEval = evaluatePosition(currentBoard)
    valueCache.putInCache(currentBoard, newEval)
}

// Possible Movements Cache
val movesCache = PossibleMovementsCache()
val moves = movesCache.getPossibleMovements(pieceBitboard, colorComposites, isWhite)
if (moves == null) {
    val legalMoves = calculateLegalMoves(piece)
    movesCache.putPossibleMovements(pieceBitboard, colorComposites, isWhite, legalMoves)
}
```

## Performance Considerations
- Value and Movement caches use complete board states, suitable for position evaluation
- Possible Movements cache uses minimal state for piece mobility
- Hit tracking helps identify cache effectiveness
- Consider clearing caches periodically to manage memory usage

## Future Improvements
Consider implementing:
- Size limits for caches
- Least Recently Used (LRU) eviction
- More granular performance metrics
- Piece-type specific optimizations for Possible Movements cache