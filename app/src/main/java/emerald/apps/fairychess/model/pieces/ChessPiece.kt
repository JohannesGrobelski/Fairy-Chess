package emerald.apps.fairychess.model.pieces

class ChessPiece(
    var name: String,
    var position: Array<Int>,
    var value: Int,
    var color: String,
    var movingPatternString: String,
    val moveCounter: Int) {

    open fun move(rank : Int, file : Int) : Boolean {return true}
    open fun getTargetSquares() : List<Array<Int>> {return listOf()}
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
                "+" -> {
                    targetSquares.addAll(generateOrthogonalSquares(movingPattern))
                }
                ">" -> {
                    targetSquares.addAll(generateOrthogonalSquares(movingPattern))
                }
                "*" -> {
                    targetSquares.addAll(generateOrthogonalSquares(movingPattern))
                    targetSquares.addAll(generateDiagonalSquares(movingPattern))
                }
                "X" -> {
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
        //right,forward
        var difX = 1; var difY = 1
        while(position[0]+difX <= 7 && position[1]+difY <= 7) {
            if(Math.abs(difX) <= quantityInt && Math.abs(difY) <= quantityInt){
                targetSquares.add(Movement(movementNotation,position[0],position[1],position[0]+difX,position[1]+difY))
                ++difX
                ++difY
            } else break
        }
        //left,forward
        difX = -1; difY = 1
        while(position[0]+difX >= 0 && position[1]+difY <= 7) {
            if(Math.abs(difX) <= quantityInt && Math.abs(difY) <= quantityInt){
                targetSquares.add(Movement(movementNotation,position[0],position[1],position[0] + difX, position[1] + difY))
                --difX
                ++difY
            } else break
        }
        //right,backwards
        difX = 1; difY = -1
        while(position[0]+difX <= 7 && position[1]+difY >= 0) {
            if(Math.abs(difX) <= quantityInt && Math.abs(difY) <= quantityInt){
                targetSquares.add(Movement(movementNotation,position[0],position[1],position[0]+difX,position[1]+difY))
                ++difX
                --difY
            } else break
        }
        //left,backwards
        difX = -1; difY = -1
        while(position[0]+difX >= 0 && position[1]+difY >= 0) {
            if(Math.abs(difX) <= quantityInt && Math.abs(difY) <= quantityInt){
                targetSquares.add(Movement(movementNotation,position[0],position[1],position[0]+difX,position[1]+difY))
                --difX
                --difY
            } else break
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
                for(i in position[0] downTo 0){
                    if(Math.abs(position[0]-i) <= quantityInt)targetSquares.add(Movement(movementNotation,position[0],position[1],i,position[1]))
                    else break
                }
            } else {
                for(i in position[0]..7){
                    if(Math.abs(position[0]-i) <= quantityInt)targetSquares.add(Movement(movementNotation,position[0],position[1],i,position[1]))
                    else break
                }
            }
        }
        //backward
        if(mode == "*" || mode == "+" || mode == "<>" || mode == "<=" || mode == "<") {
            if(color == "black" && mode == "<"){
                for(i in position[0]..7){
                    if(Math.abs(position[0]-i) <= quantityInt)targetSquares.add(Movement(movementNotation,position[0],position[1],i,position[1]))
                    else break
                }
            } else {
                for(i in position[0] downTo 0){
                    if(Math.abs(position[0]-i) <= quantityInt)targetSquares.add(Movement(movementNotation,position[0],position[1],i,position[1]))
                    else break
                }
            }
        }
        //right
        if(mode == "*" || mode == "+" || mode == ">=" || mode == "=") {
            for(i in position[1]..7){
                if(Math.abs(position[1]-i) <= quantityInt)targetSquares.add(Movement(movementNotation,position[0],position[1],position[0],i))
                else break
            }
        }
        //left
        if(mode == "*" || mode == "+" || mode == "<=" || mode == "=") {
            for(i in position[1] downTo 0){
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