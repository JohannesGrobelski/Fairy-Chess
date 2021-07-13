package emerald.apps.fairychess.model

import kotlin.math.abs

class BitboardChessPiece(val color: String, val positionFile:Int, val positionRank:Int, val bbFigure: ULong, val movingPatternString: String) {


    val movingStringToMovementPatternsMap =
        mutableMapOf<String, List<ChessPiece.MovementNotation>>()


    /** generate a bitboard representing the target squares of the non relative movement for a piece */
    fun generateNonRelativeMovements(
    ): ULong {
        var targetCoordinates = 0uL
        val movementPatterns = mutableListOf<ChessPiece.MovementNotation>()
        if (movingStringToMovementPatternsMap.containsKey(movingPatternString)) {
            movementPatterns.addAll(movingStringToMovementPatternsMap[movingPatternString]!!)
        } else {
            movementPatterns.addAll(
                ChessPiece.MovementNotation.parseMovementString(
                    movingPatternString
                )
            )
            movingStringToMovementPatternsMap[movingPatternString] = movementPatterns
        }

        for (movingPattern in movementPatterns) {
            if (movingPattern.movetype == "~" || movingPattern.movetype == "^" || movingPattern.movetype == "g") { //leaper
                targetCoordinates = targetCoordinates or generateLeaperMovements(bbFigure,movingPattern)
            } else { //rider
                targetCoordinates = targetCoordinates or generateRiderMovements(bbFigure,movingPattern)
            }
        }
        return targetCoordinates
    }


    /** generate a list of movement matching the movingPattern (Leaper) */
    fun generateLeaperMovements(bbFigure: ULong, movingPattern: ChessPiece.MovementNotation): ULong {
        var newBBFigure = bbFigure
        if (movingPattern.grouping == "/" && movingPattern.distances.size == 2) { //for now leaper movement consist of 2 subsequent movements
            //leaper-movements always have 8 sub-moves:
            //(2: increase/decrease)*(2: value1/value2)*(2: on File / on Rank) = 8 permutations
            val distance1 = movingPattern.distances[0]
            val distance2 = movingPattern.distances[1]
            if (distance1.matches("[0-9]".toRegex()) && distance2.matches("[0-9]".toRegex())) {
                val d1 = distance1.toInt()
                val d2 = distance2.toInt()
                /** generate all (8) leaper movements matching movingPattern (Leaper) */
                newBBFigure = newBBFigure or generateLeaperMovement(bbFigure, d1, d2)
                newBBFigure = newBBFigure or generateLeaperMovement(bbFigure, -d1, d2)
                newBBFigure = newBBFigure or generateLeaperMovement(bbFigure, d1, -d2)
                newBBFigure = newBBFigure or generateLeaperMovement(bbFigure, -d1, -d2)
                newBBFigure = newBBFigure or generateLeaperMovement(bbFigure, d2, d1)
                newBBFigure = newBBFigure or generateLeaperMovement(bbFigure, -d2, d1)
                newBBFigure = newBBFigure or generateLeaperMovement(bbFigure, d2, -d1)
                newBBFigure = newBBFigure or generateLeaperMovement(bbFigure, -d2, -d1)
            } else {
                if (distance1 == "x" && distance2 == "x") {//only in pairs (x,x): any distance in the given direction equal to its twin or zero
                    for (distance in -7..7) {
                        //orthogonal
                        newBBFigure = newBBFigure or generateLeaperMovement(newBBFigure, 0, distance)
                        newBBFigure = newBBFigure or generateLeaperMovement(newBBFigure, distance, 0)
                        //diagonal
                        newBBFigure = newBBFigure or generateLeaperMovement(newBBFigure, distance, distance)
                        newBBFigure = newBBFigure or generateLeaperMovement(newBBFigure, -distance, distance)
                        newBBFigure = newBBFigure or generateLeaperMovement(newBBFigure, distance, -distance)
                        newBBFigure = newBBFigure or generateLeaperMovement(newBBFigure, -distance, -distance)
                    }
                }
            }
        }
        return newBBFigure
    }

    /** add a leaper movement to targetSquares defined by an delta (fileDif,rankDif) */
    fun generateLeaperMovement(
        bbFigure: ULong,
        fileDif: Int,
        rankDif: Int
    ) : ULong {
        if (positionFile + fileDif in 0..7 && positionRank + rankDif in 0..7) {
            return Bitboard.add64BPositionFromCoordinates(bbFigure,positionFile+fileDif,positionRank+rankDif)
        }
        return 0uL
    }

    /** generate a list of rider-movements matching the movingPattern (rider) */
    fun generateRiderMovements(bbFigure: ULong, movingPattern: ChessPiece.MovementNotation): ULong {
        var newBBFigure = bbFigure
        if (movingPattern.distances.isNotEmpty()) {
            when (movingPattern.direction) {
                ">" -> {
                    newBBFigure = newBBFigure or generateOrthogonalMovement(newBBFigure, movingPattern)
                }
                "<" -> {
                    newBBFigure = newBBFigure or generateOrthogonalMovement(newBBFigure, movingPattern)
                }
                "<>" -> {
                    newBBFigure = newBBFigure or generateOrthogonalMovement(newBBFigure, movingPattern)
                }
                "=" -> {
                    newBBFigure = newBBFigure or generateOrthogonalMovement(newBBFigure, movingPattern)
                }
                "<=" -> {
                    newBBFigure = newBBFigure or generateOrthogonalMovement(newBBFigure, movingPattern)
                }
                ">=" -> {
                    newBBFigure = newBBFigure or generateOrthogonalMovement(newBBFigure, movingPattern)
                }
                "+" -> {
                    newBBFigure = newBBFigure or generateOrthogonalMovement(newBBFigure, movingPattern)
                }
                "X" -> {
                    newBBFigure = newBBFigure or generateOrthogonalMovement(newBBFigure, movingPattern)
                }
                "X>" -> {
                    newBBFigure = newBBFigure or generateOrthogonalMovement(newBBFigure, movingPattern)
                }
                "X<" -> {
                    newBBFigure = newBBFigure or generateDiagonalMovement(newBBFigure, movingPattern)
                }
                "*" -> {
                    newBBFigure = newBBFigure or generateOrthogonalMovement(newBBFigure, movingPattern)
                    newBBFigure = newBBFigure or generateDiagonalMovement(newBBFigure, movingPattern)
                }
            }
        }
        return newBBFigure
    }

    /** generate all orthogonal movements: horizontal (WEST,EAST movements) and vertical (NORTH,SOUTH)*/
    fun generateOrthogonalMovement(bbFigure: ULong, movementNotation: ChessPiece.MovementNotation) : ULong {
        var newBBFigure = bbFigure
        var distance = 7
        if(movementNotation.distances[0].matches("[1-9]+".toRegex()))distance = movementNotation.distances[0].toInt()
        //forward(>) and backwards(<) are color-dependent because they are depending on direction of the figures
        //color-independent movements
        if(movementNotation.direction.contains("=") || movementNotation.direction == "+" || movementNotation.direction == "*") {
            newBBFigure = newBBFigure or generateWestMovement(newBBFigure,distance)
            newBBFigure = newBBFigure or generateEastMovement(newBBFigure,distance)
        }
        if(movementNotation.direction == "+" || movementNotation.direction == "*" || movementNotation.direction == "<>"
            || movementNotation.direction.contains(">") || movementNotation.direction.contains("<")){
            //color-dependent movements
            if(movementNotation.direction.contains(">") && !movementNotation.direction.contains("<")){
                //forwards but not backwards
                newBBFigure = if(color == "black"){
                    newBBFigure or generateSouthMovement(newBBFigure,distance)
                } else {
                    newBBFigure or generateNorthMovement(newBBFigure,distance)
                }
            } else if(movementNotation.direction.contains("<") && !movementNotation.direction.contains(">")){
                //backwards but not forwards
                newBBFigure = if(color == "black"){
                    newBBFigure or generateNorthMovement(newBBFigure,distance)
                } else {
                    newBBFigure or generateSouthMovement(newBBFigure,distance)
                }
            } else { //color-independent movements
                newBBFigure = newBBFigure or generateNorthMovement(newBBFigure,distance)
                newBBFigure = newBBFigure or generateSouthMovement(newBBFigure,distance)
            }

        }
        return newBBFigure
    }

    /** forward: increase rank */
    fun generateNorthMovement(bbFigure : ULong, distance : Int) : ULong{
        var newBBFigure = bbFigure
        for(i in positionRank+1..7){// ... inside board (between 0 and 7)
            if(abs(positionRank-i) <= distance) {// ... and difference smaller than allowed distance add Coordinate to bitboard
                newBBFigure = addCoordinateToBitboard(newBBFigure,positionFile,positionRank)
            }
            else break
        }
        return newBBFigure
    }

    /** backward: decrease rank */
    fun generateSouthMovement(bbFigure : ULong, distance : Int) : ULong{
        var newBBFigure = bbFigure
        for(i in positionRank-1 downTo 0){// ... inside board (between 0 and 7)
            if(abs(positionRank-i) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                newBBFigure = addCoordinateToBitboard(newBBFigure,positionFile,positionRank)
            }
            else break
        }
        return newBBFigure
    }

    /** right: increase file */
    fun generateEastMovement(bbFigure : ULong, distance : Int) : ULong{
        var newBBFigure = bbFigure
        for(i in positionFile+1..7){// ... inside board (between 0 and 7)
            if(abs(positionFile-i) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                newBBFigure = addCoordinateToBitboard(newBBFigure,positionFile,positionRank)

            }
            else break
        }
        return newBBFigure
    }

    /** left: decrease file */
    fun generateWestMovement(bbFigure : ULong, distance : Int) : ULong{
        var newBBFigure = bbFigure
        //if coordinate is ...
        for(i in positionFile-1 downTo 0){// ... inside board (between 0 and 7)
            if(abs(positionFile-i) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                newBBFigure = addCoordinateToBitboard(newBBFigure,positionFile,positionRank)
            }
            else break
        }
        return newBBFigure
    }

    /** generate all diagonal rider movements */
    fun generateDiagonalMovement(bbFigure : ULong, movementNotation: ChessPiece.MovementNotation) : ULong{
        val newBBFigure = bbFigure
        var distance = 7
        if(movementNotation.distances[0].matches("[0-9]".toRegex())){
            distance = movementNotation.distances[0].toInt()
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X>"){
            if (color == "black" && movementNotation.direction == "X>"){
                generateSouthEastDiagonalMovement(newBBFigure,distance)
                generateSouthWestDiagonalMovement(newBBFigure,distance)
            } else {
                generateNorthEastDiagonalMovement(newBBFigure,distance)
                generateNorthWestDiagonalMovement(newBBFigure,distance)
            }
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X<") {
            if (color == "black" && movementNotation.direction == "X>"){
                generateNorthEastDiagonalMovement(newBBFigure,distance)
                generateNorthWestDiagonalMovement(newBBFigure,distance)
            } else {
                generateSouthEastDiagonalMovement(newBBFigure,distance)
                generateSouthWestDiagonalMovement(newBBFigure,distance)
            }
        }
        return newBBFigure
    }

    /** NorthEastDiagonalMovement: right,forward: increase file, increase rank*/
    fun generateNorthEastDiagonalMovement(bbFigure : ULong,  distance : Int) : ULong {
        var newBBFigure = bbFigure
        var difFile = 1; var difRank = 1
        //if coordinate is ...
        while(positionFile+difFile <= 7 && positionRank+difRank <= 7) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                newBBFigure = addCoordinateToBitboard(newBBFigure,positionFile,positionRank,)
                ++difFile
                ++difRank
            } else break
        }
        return newBBFigure
    }

    /** NorthWestDiagonalMovement: left,forward: decrease file, increase rank*/
    fun generateNorthWestDiagonalMovement(bbFigure : ULong,  distance : Int) : ULong {
        var newBBFigure = bbFigure
        var difFile = -1; var difRank = 1
        //if coordinate is ...
        while(positionFile+difFile >= 0 && positionRank+difRank <= 7) {// ... inside board (between 0 and 7)
            if(abs(difFile) <= distance && abs(difRank) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                newBBFigure = addCoordinateToBitboard(newBBFigure,positionFile,positionRank,)
                --difFile
                ++difRank
            } else break
        }
        return newBBFigure
    }

    /** SouthEastDiagonalMovement: right,backward: increase file, decrease rank*/
    fun generateSouthEastDiagonalMovement(bbFigure : ULong,  distance : Int) : ULong {
        var newBBFigure = bbFigure
        var difFile = 1; var difRank = -1
        //if coordinate is ...
        while(positionFile+difFile <= 7 && positionRank+difRank >= 0) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){ // ... and difference smaller than allowed distance add Coordinate to bitboard
                newBBFigure = addCoordinateToBitboard(newBBFigure,positionFile,positionRank,)
                ++difFile
                --difRank
            } else break
        }
        return newBBFigure
    }

    /** SouthWestDiagonalMovement: left,backward: decrease file, decrease rank*/
    fun generateSouthWestDiagonalMovement(bbFigure : ULong,  distance : Int) : ULong {
        var newBBFigure = bbFigure
        var difRank = -1; var difFile = -1
        //if coordinate is ...
        while(positionFile+difFile >= 0 && positionRank+difRank >= 0) {// ... inside board (between 0 and 7)
            if(abs(difRank) <= distance && abs(difFile) <= distance){// ... and difference smaller than allowed distance add Coordinate to bitboard
                newBBFigure = addCoordinateToBitboard(newBBFigure,positionFile,positionRank,)
                --difRank
                --difFile
            } else break
        }
        return newBBFigure
    }

    fun addCoordinateToBitboard(bbFigure : ULong, positionFile: Int, positionRank: Int) : ULong{
        return bbFigure or Bitboard.generate64BPositionFromCoordinates(
            positionFile,
            positionRank,
        )
    }
    


}