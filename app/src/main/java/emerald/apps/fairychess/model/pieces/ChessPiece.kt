package emerald.apps.fairychess.model.pieces

class ChessPiece(
    var name: String,
    var position: Array<Int>, //(rank,file)
    var value: Int,
    var color: String,
    var movingPatternString: String,
    val moveCounter: Int) {

    var movingPatterns = Chessboard.MovementNotation.parseMovementString(movingPatternString)

    /** parlett notation: <conditions> <move type> <distance> <direction> <other>
     *
     */
    fun generateMovements() : List<Movement>{
        val targetCoordinates = mutableListOf<Movement>()
        for(movingPattern in movingPatterns){
            if(movingPattern.movetype == "~"){
                targetCoordinates.addAll(generateLeaperMovements(movingPattern))
            } else {
                targetCoordinates.addAll(generateRiderMovements(movingPattern))
            }
        }
        return targetCoordinates
    }

    fun generateLeaperMovements(movingPattern: Chessboard.MovementNotation) : List<Movement> {
        val targetSquares = mutableListOf<Movement>()
        if(movingPattern.grouping == "/" && movingPattern.distances.size == 2){
            //leaper-movements always have 8 sub-moves
            val m1 = movingPattern.distances[0].toInt()
            val m2 = movingPattern.distances[1].toInt()
            if(position[0]+m1 in 0..7 && position[1]+m2 in 0..7){
                targetSquares.add(Movement(movingPattern,position[0],position[1],position[0]+m1,position[1]+m2))
            }
            if(position[0]-m1 in 0..7 && position[1]+m2 in 0..7){
                targetSquares.add(Movement(movingPattern,position[0],position[1],position[0]-m1,position[1]+m2))
            }
            if(position[0]+m1 in 0..7 && position[1]-m2 in 0..7){
                targetSquares.add(Movement(movingPattern,position[0],position[1],position[0]+m1,position[1]-m2))
            }
            if(position[0]-m1 in 0..7 && position[1]-m2 in 0..7){
                targetSquares.add(Movement(movingPattern,position[0],position[1],position[0]-m1,position[1]-m2))
            }
            if(position[0]+m2 in 0..7 && position[1]+m1 in 0..7){
                targetSquares.add(Movement(movingPattern,position[0],position[1],position[0]+m2,position[1]+m1))
            }
            if(position[0]-m2 in 0..7 && position[1]+m1 in 0..7){
                targetSquares.add(Movement(movingPattern,position[0],position[1],position[0]-m2,position[1]+m1))
            }
            if(position[0]+m2 in 0..7 && position[1]-m1 in 0..7){
                targetSquares.add(Movement(movingPattern,position[0],position[1],position[0]+m2,position[1]-m1))
            }
            if(position[0]-m2 in 0..7 && position[1]-m1 in 0..7){
                targetSquares.add(Movement(movingPattern,position[0],position[1],position[0]-m2,position[1]-m1))
            }
        }
        return targetSquares
    }

    fun generateRiderMovements(movingPattern: Chessboard.MovementNotation) : List<Movement> {
        val targetSquares = mutableListOf<Movement>()
        if(movingPattern.distances.isNotEmpty()){
            when(movingPattern.direction){
                ">" -> {targetSquares.addAll(generateOrthogonalSquares(movingPattern))}
                "<" -> {targetSquares.addAll(generateOrthogonalSquares(movingPattern))}
                "<>" -> {targetSquares.addAll(generateOrthogonalSquares(movingPattern))}
                "=" -> {targetSquares.addAll(generateOrthogonalSquares(movingPattern))}
                "<=" -> {targetSquares.addAll(generateOrthogonalSquares(movingPattern))}
                ">=" -> {targetSquares.addAll(generateOrthogonalSquares(movingPattern))}
                "+" -> {targetSquares.addAll(generateOrthogonalSquares(movingPattern))}
                "X" -> {targetSquares.addAll(generateDiagonalSquares(movingPattern))}
                "X>" -> {targetSquares.addAll(generateDiagonalSquares(movingPattern))}
                "X<" -> {targetSquares.addAll(generateDiagonalSquares(movingPattern))}
                "*" -> {
                    targetSquares.addAll(generateOrthogonalSquares(movingPattern))
                    targetSquares.addAll(generateDiagonalSquares(movingPattern))
                }
            }
        }
        return targetSquares
    }

    fun generateDiagonalSquares(movementNotation: Chessboard.MovementNotation) : List<Movement>{
        val targetSquares = mutableListOf<Movement>()
        var quantityInt = 7
        if(movementNotation.distances[0].matches("[1-9]+".toRegex())){
            quantityInt = movementNotation.distances[0].toInt()
        }
        var difRank=0; var difFile=0;
        if(movementNotation.direction == "X" || movementNotation.direction == "X>"){
            //right,forward
            difRank = 1; difFile = 1
            while(position[0]+difRank <= 7 && position[1]+difFile <= 7) {
                if(Math.abs(difRank) <= quantityInt && Math.abs(difFile) <= quantityInt){
                    targetSquares.add(Movement(movementNotation,position[0],position[1],position[0]+difRank,position[1]+difFile))
                    ++difRank
                    ++difFile
                } else break
            }
            //left,forward
            difRank = -1; difFile = 1
            while(position[0]+difRank >= 0 && position[1]+difFile <= 7) {
                if(Math.abs(difRank) <= quantityInt && Math.abs(difFile) <= quantityInt){
                    targetSquares.add(Movement(movementNotation,position[0],position[1],position[0] + difRank, position[1] + difFile))
                    --difRank
                    ++difFile
                } else break
            }
        }
        if(movementNotation.direction == "X" || movementNotation.direction == "X<") {
            //right,backwards
            difRank = 1; difFile = -1
            while(position[0]+difRank <= 7 && position[1]+difFile >= 0) {
                if(Math.abs(difRank) <= quantityInt && Math.abs(difFile) <= quantityInt){
                    targetSquares.add(Movement(movementNotation,position[0],position[1],position[0]+difRank,position[1]+difFile))
                    ++difRank
                    --difFile
                } else break
            }
            //left,backwards
            difRank = -1; difFile = -1
            while(position[0]+difRank >= 0 && position[1]+difFile >= 0) {
                if(Math.abs(difRank) <= quantityInt && Math.abs(difFile) <= quantityInt){
                    targetSquares.add(Movement(movementNotation,position[0],position[1],position[0]+difRank,position[1]+difFile))
                    --difRank
                    --difFile
                } else break
            }
        }
        return targetSquares
    }

    fun generateOrthogonalSquares(movementNotation: Chessboard.MovementNotation) : List<Movement>{
        val targetSquares = mutableListOf<Movement>()
        var quantityInt = 7
        if(movementNotation.distances[0].matches("[1-9]+".toRegex()))quantityInt = movementNotation.distances[0].toInt()
        val mode = movementNotation.direction
        //forward
        if(mode == "*" || mode == "+" || mode == "<>" || mode == ">=" || mode == ">"){
            if(color == "black" && mode == ">"){
                for(i in position[0]-1 downTo 0){
                    if(Math.abs(position[0]-i) <= quantityInt)targetSquares.add(Movement(movementNotation,position[0],position[1],i,position[1]))
                    else break
                }
            } else {
                for(i in position[0]+1..7){
                    if(Math.abs(position[0]-i) <= quantityInt)targetSquares.add(Movement(movementNotation,position[0],position[1],i,position[1]))
                    else break
                }
            }
        }
        //backward
        if(mode == "*" || mode == "+" || mode == "<>" || mode == "<=" || mode == "<") {
            if(color == "black" && mode == "<"){
                for(i in position[0]+1..7){
                    if(Math.abs(position[0]-i) <= quantityInt)targetSquares.add(Movement(movementNotation,position[0],position[1],i,position[1]))
                    else break
                }
            } else {
                for(i in position[0]-1 downTo 0){
                    if(Math.abs(position[0]-i) <= quantityInt)targetSquares.add(Movement(movementNotation,position[0],position[1],i,position[1]))
                    else break
                }
            }
        }
        //right
        if(mode == "*" || mode == "+" || mode == ">=" || mode == "=") {
            for(i in position[1]+1..7){
                if(Math.abs(position[1]-i) <= quantityInt)targetSquares.add(Movement(movementNotation,position[0],position[1],position[0],i))
                else break
            }
        }
        //left
        if(mode == "*" || mode == "+" || mode == "<=" || mode == "=") {
            for(i in position[1]-1 downTo 0){
                if(Math.abs(position[1]-i) <= quantityInt)targetSquares.add(Movement(movementNotation,position[0],position[1],position[0],i))
                else break
            }
        }
        return targetSquares
    }

    class Movement(val movementNotation : Chessboard.MovementNotation
                 , val sourceRank : Int
                 , val sourceFile : Int
                 , val targetRank : Int
                 , val targetFile : Int)
}