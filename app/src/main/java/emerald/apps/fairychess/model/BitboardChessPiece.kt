package emerald.apps.fairychess.model

class BitboardChessPiece(var positionFile:Int, var positionRank:Int) {


    val movingStringToMovementPatternsMap =
        mutableMapOf<String, List<ChessPiece.MovementNotation>>()

    /** */
    fun generateNonRelativeMovements(
        name: String,
        movingPatternString: String,
        bitboard: ULong
    ): ULong {
        var targetCoordinates = 0uL
        val name = ""
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
                targetCoordinates = targetCoordinates or generateLeaperMovements(movingPattern)
            } else { //rider
                targetCoordinates = targetCoordinates or generateRiderMovements(movingPattern)
            }
        }
        return targetCoordinates
    }


    /** generate a list of movement matching the movingPattern (Leaper) */
    fun generateLeaperMovements(movingPattern: ChessPiece.MovementNotation): ULong {
        var targetSquares = 0uL
        if (movingPattern.grouping == "/" && movingPattern.distances.size == 2) { //for now leaper movement consist of 2 subsequent movements
            //leaper-movements always have 8 sub-moves:
            //(2: increase/decrease)*(2: value1/value2)*(2: on File / on Rank) = 8 permutations
            val movement1 = movingPattern.distances[0]
            val movement2 = movingPattern.distances[1]
            if (movement1.matches("[0-9]".toRegex()) && movement2.matches("[0-9]".toRegex())) {
                generate8LeaperMovements(
                    movingPattern,
                    targetSquares,
                    movement1.toInt(),
                    movement2.toInt()
                )
            } else {
                if (movement1 == "x" && movement2 == "x") {//only in pairs (x,x): any distance in the given direction equal to its twin or zero
                    for (a in -7..7) {
                        //orthogonal
                        generateLeaperMovement(movingPattern, targetSquares, 0, a)
                        generateLeaperMovement(movingPattern, targetSquares, a, 0)
                        //diagonal
                        generateLeaperMovement(movingPattern, targetSquares, a, a)
                        generateLeaperMovement(movingPattern, targetSquares, -a, a)
                        generateLeaperMovement(movingPattern, targetSquares, a, -a)
                        generateLeaperMovement(movingPattern, targetSquares, -a, -a)
                    }
                }
            }
        }
        return targetSquares
    }

    /** generate all (8) leaper movements matching movingPattern (Leaper) */
    fun generate8LeaperMovements(
        movingPattern: ChessPiece.MovementNotation,
        targetSquares: ULong,
        m1: Int,
        m2: Int
    ) {
        generateLeaperMovement(movingPattern, targetSquares, m1, m2)
        generateLeaperMovement(movingPattern, targetSquares, -m1, m2)
        generateLeaperMovement(movingPattern, targetSquares, m1, -m2)
        generateLeaperMovement(movingPattern, targetSquares, -m1, -m2)
        generateLeaperMovement(movingPattern, targetSquares, m2, m1)
        generateLeaperMovement(movingPattern, targetSquares, -m2, m1)
        generateLeaperMovement(movingPattern, targetSquares, m2, -m1)
        generateLeaperMovement(movingPattern, targetSquares, -m2, -m1)
    }

    /** add a leaper movement to targetSquares defined by an delta (fileDif,rankDif) */
    fun generateLeaperMovement(
        movingPattern: ChessPiece.MovementNotation,
        targetSquares: ULong,
        fileDif: Int,
        rankDif: Int
    ) {
        if (positionFile + fileDif in 0..7 && positionRank + rankDif in 0..7) {
            targetSquares.add(
                ChessPiece.Movement(
                    movingPattern,
                    positionFile,
                    positionRank,
                    positionFile + fileDif,
                    positionRank + rankDif
                )
            )
        }
    }

    /** generate a list of rider-movements matching the movingPattern (rider) */
    fun generateRiderMovements(movingPattern: ChessPiece.MovementNotation): ULong {
        val targetSquares = ULong
        if (movingPattern.distances.isNotEmpty()) {
            when (movingPattern.direction) {
                ">" -> {
                    targetSquares.addAll(generateOrthogonalMovement(movingPattern))
                }
                "<" -> {
                    targetSquares.addAll(generateOrthogonalMovement(movingPattern))
                }
                "<>" -> {
                    targetSquares.addAll(generateOrthogonalMovement(movingPattern))
                }
                "=" -> {
                    targetSquares.addAll(generateOrthogonalMovement(movingPattern))
                }
                "<=" -> {
                    targetSquares.addAll(generateOrthogonalMovement(movingPattern))
                }
                ">=" -> {
                    targetSquares.addAll(generateOrthogonalMovement(movingPattern))
                }
                "+" -> {
                    targetSquares.addAll(generateOrthogonalMovement(movingPattern))
                }
                "X" -> {
                    targetSquares.addAll(generateDiagonalRiderMovement(movingPattern))
                }
                "X>" -> {
                    targetSquares.addAll(generateDiagonalRiderMovement(movingPattern))
                }
                "X<" -> {
                    targetSquares.addAll(generateDiagonalRiderMovement(movingPattern))
                }
                "*" -> {
                    targetSquares.addAll(generateOrthogonalMovement(movingPattern))
                    targetSquares.addAll(generateDiagonalRiderMovement(movingPattern))
                }
            }
        }
        return targetSquares
    }



}