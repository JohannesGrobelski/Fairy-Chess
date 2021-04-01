package emerald.apps.fairychess.movementNotation.directionl.pieces

import emerald.apps.fairychess.model.pieces.Chessboard

class ChessPiece(
    var name: String,
    var positionFile: Int,
    var positionRank: Int,
    var value: Int,
    var color: String,
    var movingPatternString: String,
    val moveCounter: Int
) {

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
            //leaper-movements always have 8 sub-moves:
            //(2: increase/decrease)*(2: value1/value2)*(2: on File / on Rank) = 8 permutations
            val m1 = movingPattern.distances[0].toInt()
            val m2 = movingPattern.distances[1].toInt()
            if(positionFile+m1 in 0..7 && positionRank+m2 in 0..7){
                targetSquares.add(Movement(movingPattern,
                    positionFile,
                    positionRank,
                    positionFile+m1,
                    positionRank+m2))
            }
            if(positionFile-m1 in 0..7 && positionRank+m2 in 0..7){
                targetSquares.add(Movement(movingPattern,
                    positionFile,
                    positionRank,
                    positionFile-m1,
                    positionRank+m2))
            }
            if(positionFile+m1 in 0..7 && positionRank-m2 in 0..7){
                targetSquares.add(Movement(movingPattern,
                    positionFile,
                    positionRank,
                    positionFile+m1,
                    positionRank-m2))
            }
            if(positionFile-m1 in 0..7 && positionRank-m2 in 0..7){
                targetSquares.add(Movement(movingPattern,
                    positionFile,
                    positionRank,
                    positionFile-m1,
                    positionRank-m2))
            }
            if(positionFile+m2 in 0..7 && positionRank+m1 in 0..7){
                targetSquares.add(Movement(movingPattern,
                    positionFile,
                    positionRank,
                    positionFile+m2,
                    positionRank+m1))
            }
            if(positionFile-m2 in 0..7 && positionRank+m1 in 0..7){
                targetSquares.add(Movement(movingPattern,
                    positionFile,
                    positionRank,
                    positionFile-m2,
                    positionRank+m1))
            }
            if(positionFile+m2 in 0..7 && positionRank-m1 in 0..7){
                targetSquares.add(Movement(movingPattern,
                    positionFile,
                    positionRank,
                    positionFile+m2,
                    positionRank-m1))
            }
            if(positionFile-m2 in 0..7 && positionRank-m1 in 0..7){
                targetSquares.add(Movement(movingPattern,
                    positionFile,
                    positionRank,
                    positionFile-m2,
                    positionRank-m1))
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
        var difRank=0; var difFile=0;
        var distance = 7
        if(movementNotation.distances[0].matches("[0-9]".toRegex())){
            distance = movementNotation.distances[0].toInt()
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X>"){
            if (color == "black" && movementNotation.direction == "X>"){
                generateBackwardsRightSquares(targetSquares,movementNotation,distance)
                generateBackwardsLeftSquares(targetSquares,movementNotation,distance)
            } else {
                generateForewardRightSquares(targetSquares,movementNotation,distance)
                generateForewardLeftSquares(targetSquares,movementNotation,distance)
            }
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X<") {
            if (color == "black" && movementNotation.direction == "X>"){
                generateForewardRightSquares(targetSquares,movementNotation,distance)
                generateForewardLeftSquares(targetSquares,movementNotation,distance)
            } else {
                generateBackwardsRightSquares(targetSquares,movementNotation,distance)
                generateBackwardsLeftSquares(targetSquares,movementNotation,distance)
            }
        }
        return targetSquares
    }

    /** right,forward: increase file, increase rank*/
    fun generateForewardRightSquares(inputSquares : MutableList<Movement>, movementNotation: Chessboard.MovementNotation, distance : Int) : List<Movement> {
        var difFile = 1; var difRank = 1;
        while(positionFile+difFile <= 7 && positionRank+difRank <= 7) {
            if(Math.abs(difRank) <= distance && Math.abs(difFile) <= distance){
                inputSquares.add(Movement(movementNotation,
                    positionFile,
                    positionRank,
                    positionFile+difFile,
                    positionRank+difRank)
                )
                ++difFile
                ++difRank
            } else break
        }
        return inputSquares
    }

    /** left,forward: decrease file, increase rank*/
    fun generateForewardLeftSquares(inputSquares : MutableList<Movement>, movementNotation: Chessboard.MovementNotation, distance : Int) : List<Movement> {
        var difFile = -1; var difRank = 1;
        while(positionFile+difFile >= 0 && positionRank+difRank <= 7) {
            if(Math.abs(difFile) <= distance && Math.abs(difRank) <= distance){
                inputSquares.add(Movement(movementNotation,
                    positionFile,
                    positionRank,
                    positionFile+difFile,
                    positionRank+difRank))
                --difFile
                ++difRank
            } else break
        }
        return inputSquares
    }

    /** right,backward: increase file, decrease rank*/
    fun generateBackwardsRightSquares(inputSquares : MutableList<Movement>, movementNotation: Chessboard.MovementNotation, distance : Int) : List<Movement> {
        var difFile = 1; var difRank = -1
        while(positionFile+difFile >= 0 && positionRank+difRank <= 7) {
            if(Math.abs(difRank) <= distance && Math.abs(difFile) <= distance){
                inputSquares.add(Movement(movementNotation,
                    positionFile,
                    positionRank,
                    positionFile+difFile,
                    positionRank+difRank)
                )
                ++difFile
                --difRank
            } else break
        }
        return inputSquares
    }

    /** left,backward: decrease file, decrease rank*/
    fun generateBackwardsLeftSquares(inputSquares : MutableList<Movement>, movementNotation: Chessboard.MovementNotation, distance : Int) : List<Movement> {
        var difRank = -1; var difFile = -1
        while(positionFile+difFile >= 0 && positionRank+difRank >= 0) {
            if(Math.abs(difRank) <= distance && Math.abs(difFile) <= distance){
                inputSquares.add(Movement(movementNotation,
                    positionFile,
                    positionRank,
                    positionFile+difFile,
                    positionRank+difRank)
                )
                --difRank
                --difFile
            } else break
        }
        return inputSquares
    }

    fun generateOrthogonalSquares(movementNotation: Chessboard.MovementNotation) : List<Movement>{
        val targetSquares = mutableListOf<Movement>()
        var distance = 7
        if(movementNotation.distances[0].matches("[1-9]+".toRegex()))distance = movementNotation.distances[0].toInt()

        //color-independent movements
        if(movementNotation.direction.contains("=") || movementNotation.direction == "+" || movementNotation.direction == "*") {
            generateLeftSquares(targetSquares,movementNotation,distance)
            generateRightSquares(targetSquares,movementNotation,distance)
        }

        if(movementNotation.direction == "+" || movementNotation.direction == "*" || movementNotation.direction == "<>"
            || movementNotation.direction.contains(">") || movementNotation.direction.contains("<")){
                //color-dependent movements
                if(movementNotation.direction.contains(">") && !movementNotation.direction.contains("<")){
                    //forwards but not backwards
                    if(color == "black"){
                        generateBackwardsSquares(targetSquares,movementNotation,distance)
                    } else {
                        generateForewardSquares(targetSquares,movementNotation,distance)
                    }
                } else if(movementNotation.direction.contains("<") && !movementNotation.direction.contains(">")){
                    //backwards but not forwards
                    if(color == "black"){
                        generateForewardSquares(targetSquares,movementNotation,distance)
                    } else {
                        generateBackwardsSquares(targetSquares,movementNotation,distance)
                    }
                } else { //color-independent movements
                    generateForewardSquares(targetSquares,movementNotation,distance)
                    generateBackwardsSquares(targetSquares,movementNotation,distance)
                }

        }

        return targetSquares
    }

    /** forward: increase rank */
    fun generateForewardSquares(inputSquares : MutableList<Movement>, movementNotation: Chessboard.MovementNotation, distance : Int) : List<Movement> {
        if(movementNotation.direction == "*" || movementNotation.direction == "+" || movementNotation.direction == "<>" || movementNotation.direction == ">=" || movementNotation.direction == ">"){
            for(i in positionRank+1..7){
                if(Math.abs(positionRank-i) <= distance)inputSquares.add(Movement(movementNotation,
                    positionFile,
                    positionRank,
                    positionFile,
                    i)
                )
                else break
            }
        }
        return inputSquares
    }

    /** backward: decrease rank */
    fun generateBackwardsSquares(inputSquares : MutableList<Movement>, movementNotation: Chessboard.MovementNotation, distance : Int) : List<Movement> {
        for(i in positionRank-1 downTo 0){
            if(Math.abs(positionRank-i) <= distance)inputSquares.add(Movement(movementNotation,
                positionFile,
                positionRank,
                positionFile,
                i)
            )
        }
        return inputSquares
    }

    /** right: increase file */
    fun generateRightSquares(inputSquares : MutableList<Movement>, movementNotation: Chessboard.MovementNotation, distance : Int) : List<Movement> {
        for(i in positionFile+1..7){
            if(Math.abs(positionFile-i) <= distance)inputSquares.add(Movement(movementNotation,
                positionFile,
                positionRank,
                i,
                positionRank)
            )
            else break
        }
        return inputSquares
    }

    /** left: decrease file */
    fun generateLeftSquares(inputSquares : MutableList<Movement>, movementNotation: Chessboard.MovementNotation, distance : Int) : List<Movement> {
        for(i in positionFile-1 downTo 0){
            if(Math.abs(positionFile-i) <= distance)inputSquares.add(Movement(movementNotation,
                positionFile,
                positionRank,
                i,
                positionRank)
            )
            else break
        }
        return inputSquares
    }

    class Movement(val movementNotation : Chessboard.MovementNotation
             , val sourceFile : Int
             , val sourceRank : Int
             , val targetFile : Int
             , val targetRank : Int)
}