package emerald.apps.fairychess.model

import java.lang.StringBuilder
import java.util.*

class ChessPiece(
    var name: String,
    var positionFile: Int,
    var positionRank: Int,
    var value: Int,
    var color: String,
    var movingPatternString: String,
    val moveCounter: Int
) {

    companion object {
        val emptyChessPiece = ChessPiece("",-1,-1,0,"","",0)
    }

    /** parlett notation syntax: <conditions> <move type> <distance> <direction> <other> */
    var movingPatterns = MovementNotation.parseMovementString(movingPatternString)

    /** generate a list of Movements possible for chesspiece (according to parlett notation) */
    fun generateMovements() : List<Movement>{
        val targetCoordinates = mutableListOf<Movement>()
        for(movingPattern in movingPatterns){
            if(movingPattern.movetype == "~" || movingPattern.movetype == "^" || movingPattern.movetype == "g"){ //leaper
                targetCoordinates.addAll(generateLeaperMovements(movingPattern))
            } else { //rider
                targetCoordinates.addAll(generateRiderMovements(movingPattern))
            }
        }
        return targetCoordinates
    }

    /** generate a list of movement matching the movingPattern (Leaper) */
    fun generateLeaperMovements(movingPattern: MovementNotation) : List<Movement> {
        val targetSquares = mutableListOf<Movement>()
        if(movingPattern.grouping == "/" && movingPattern.distances.size == 2){ //for now leaper movement consist of 2 subsequent movements
            //leaper-movements always have 8 sub-moves:
            //(2: increase/decrease)*(2: value1/value2)*(2: on File / on Rank) = 8 permutations
            val movement1 = movingPattern.distances[0]
            val movement2 = movingPattern.distances[1]
            if(movement1.matches("[0-9]".toRegex()) && movement2.matches("[0-9]".toRegex())){
                generate8LeaperMovements(movingPattern,targetSquares,movement1.toInt(), movement2.toInt())
            } else {
                if(movement1 == "x" && movement2 == "x"){//only in pairs (x,x): any distance in the given direction equal to its twin or zero
                    for(a in -7..7){
                        //orthogonal
                        generateLeaperMovement(movingPattern,targetSquares,0, a)
                        generateLeaperMovement(movingPattern,targetSquares,a, 0)
                        //diagonal
                        generateLeaperMovement(movingPattern,targetSquares,a, a)
                        generateLeaperMovement(movingPattern,targetSquares,-a, a)
                        generateLeaperMovement(movingPattern,targetSquares,a, -a)
                        generateLeaperMovement(movingPattern,targetSquares,-a, -a)
                    }
                }
            }
        }
        return targetSquares
    }

    /** generate all (8) leaper movements matching movingPattern (Leaper) */
    fun generate8LeaperMovements(movingPattern: MovementNotation, targetSquares : MutableList<Movement>, m1: Int, m2: Int) {
        generateLeaperMovement(movingPattern,targetSquares,m1,m2)
        generateLeaperMovement(movingPattern,targetSquares,-m1,m2)
        generateLeaperMovement(movingPattern,targetSquares,m1,-m2)
        generateLeaperMovement(movingPattern,targetSquares,-m1,-m2)
        generateLeaperMovement(movingPattern,targetSquares,m2,m1)
        generateLeaperMovement(movingPattern,targetSquares,-m2,m1)
        generateLeaperMovement(movingPattern,targetSquares,m2,-m1)
        generateLeaperMovement(movingPattern,targetSquares,-m2,-m1)
    }

    /** add a leaper movement to targetSquares defined by an delta (fileDif,rankDif) */
    fun generateLeaperMovement(movingPattern: MovementNotation, targetSquares : MutableList<Movement>, fileDif: Int, rankDif: Int) {
        if(positionFile+fileDif in 0..7 && positionRank+rankDif in 0..7){
            targetSquares.add(
                Movement(movingPattern,
                    positionFile,
                    positionRank,
                    positionFile+fileDif,
                    positionRank+rankDif)
            )
        }
    }

    /** generate a list of rider-movements matching the movingPattern (rider) */
    fun generateRiderMovements(movingPattern: MovementNotation) : List<Movement> {
        val targetSquares = mutableListOf<Movement>()
        if(movingPattern.distances.isNotEmpty()){
            when(movingPattern.direction){
                ">" -> {targetSquares.addAll(generateOrthogonalMovement(movingPattern))}
                "<" -> {targetSquares.addAll(generateOrthogonalMovement(movingPattern))}
                "<>" -> {targetSquares.addAll(generateOrthogonalMovement(movingPattern))}
                "=" -> {targetSquares.addAll(generateOrthogonalMovement(movingPattern))}
                "<=" -> {targetSquares.addAll(generateOrthogonalMovement(movingPattern))}
                ">=" -> {targetSquares.addAll(generateOrthogonalMovement(movingPattern))}
                "+" -> {targetSquares.addAll(generateOrthogonalMovement(movingPattern))}
                "X" -> {targetSquares.addAll(generateDiagonalRiderMovement(movingPattern))}
                "X>" -> {targetSquares.addAll(generateDiagonalRiderMovement(movingPattern))}
                "X<" -> {targetSquares.addAll(generateDiagonalRiderMovement(movingPattern))}
                "*" -> {
                    targetSquares.addAll(generateOrthogonalMovement(movingPattern))
                    targetSquares.addAll(generateDiagonalRiderMovement(movingPattern))
                }
            }
        }
        return targetSquares
    }

    /** generate all diagonal rider movements */
    fun generateDiagonalRiderMovement(movementNotation: MovementNotation) : List<Movement>{
        val targetSquares = mutableListOf<Movement>()
        var distance = 7
        if(movementNotation.distances[0].matches("[0-9]".toRegex())){
            distance = movementNotation.distances[0].toInt()
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X>"){
            if (color == "black" && movementNotation.direction == "X>"){
                generateSouthEastDiagonalMovement(targetSquares,movementNotation,distance)
                generateSouthWestDiagonalMovement(targetSquares,movementNotation,distance)
            } else {
                generateNorthEastDiagonalMovement(targetSquares,movementNotation,distance)
                generateNorthWestDiagonalMovement(targetSquares,movementNotation,distance)
            }
        }
        if(movementNotation.direction == "*" || movementNotation.direction == "X" || movementNotation.direction == "X<") {
            if (color == "black" && movementNotation.direction == "X>"){
                generateNorthEastDiagonalMovement(targetSquares,movementNotation,distance)
                generateNorthWestDiagonalMovement(targetSquares,movementNotation,distance)
            } else {
                generateSouthEastDiagonalMovement(targetSquares,movementNotation,distance)
                generateSouthWestDiagonalMovement(targetSquares,movementNotation,distance)
            }
        }
        return targetSquares
    }

    /** NorthEastDiagonalMovement: right,forward: increase file, increase rank*/
    fun generateNorthEastDiagonalMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        var difFile = 1; var difRank = 1;
        while(positionFile+difFile <= 7 && positionRank+difRank <= 7) {
            if(Math.abs(difRank) <= distance && Math.abs(difFile) <= distance){
                inputSquares.add(
                    Movement(movementNotation,
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

    /** NorthWestDiagonalMovement: left,forward: decrease file, increase rank*/
    fun generateNorthWestDiagonalMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        var difFile = -1; var difRank = 1;
        while(positionFile+difFile >= 0 && positionRank+difRank <= 7) {
            if(Math.abs(difFile) <= distance && Math.abs(difRank) <= distance){
                inputSquares.add(
                    Movement(movementNotation,
                    positionFile,
                    positionRank,
                    positionFile+difFile,
                    positionRank+difRank)
                )
                --difFile
                ++difRank
            } else break
        }
        return inputSquares
    }

    /** SouthEastDiagonalMovement: right,backward: increase file, decrease rank*/
    fun generateSouthEastDiagonalMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        var difFile = 1; var difRank = -1
        while(positionFile+difFile >= 0 && positionRank+difRank <= 7) {
            if(Math.abs(difRank) <= distance && Math.abs(difFile) <= distance){
                inputSquares.add(
                    Movement(movementNotation,
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

    /** SouthWestDiagonalMovement: left,backward: decrease file, decrease rank*/
    fun generateSouthWestDiagonalMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        var difRank = -1; var difFile = -1
        while(positionFile+difFile >= 0 && positionRank+difRank >= 0) {
            if(Math.abs(difRank) <= distance && Math.abs(difFile) <= distance){
                inputSquares.add(
                    Movement(movementNotation,
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

    /** generate all orthogonal movements horizontal (WEST,EAST movements) and vertical (NORTH,SOUTH) movements*/
    fun generateOrthogonalMovement(movementNotation: MovementNotation) : List<Movement>{
        val targetSquares = mutableListOf<Movement>()
        var distance = 7
        if(movementNotation.distances[0].matches("[1-9]+".toRegex()))distance = movementNotation.distances[0].toInt()
        //forward(>) and backwards(<) are color-dependent because they are depending on direction of the figures
        //color-independent movements
        if(movementNotation.direction.contains("=") || movementNotation.direction == "+" || movementNotation.direction == "*") {
            generateWestMovement(targetSquares,movementNotation,distance)
            generateEastMovement(targetSquares,movementNotation,distance)
        }
        if(movementNotation.direction == "+" || movementNotation.direction == "*" || movementNotation.direction == "<>"
            || movementNotation.direction.contains(">") || movementNotation.direction.contains("<")){
                //color-dependent movements
                if(movementNotation.direction.contains(">") && !movementNotation.direction.contains("<")){
                    //forwards but not backwards
                    if(color == "black"){
                        generateSouthMovement(targetSquares,movementNotation,distance)
                    } else {
                        generateNorthMovement(targetSquares,movementNotation,distance)
                    }
                } else if(movementNotation.direction.contains("<") && !movementNotation.direction.contains(">")){
                    //backwards but not forwards
                    if(color == "black"){
                        generateNorthMovement(targetSquares,movementNotation,distance)
                    } else {
                        generateSouthMovement(targetSquares,movementNotation,distance)
                    }
                } else { //color-independent movements
                    generateNorthMovement(targetSquares,movementNotation,distance)
                    generateSouthMovement(targetSquares,movementNotation,distance)
                }

        }
        return targetSquares
    }

    /** forward: increase rank */
    fun generateNorthMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        if(movementNotation.direction == "*" || movementNotation.direction == "+" || movementNotation.direction == "<>" || movementNotation.direction == ">=" || movementNotation.direction == ">"){
            for(i in positionRank+1..7){
                if(Math.abs(positionRank-i) <= distance)inputSquares.add(
                    Movement(movementNotation,
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
    fun generateSouthMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        for(i in positionRank-1 downTo 0){
            if(Math.abs(positionRank-i) <= distance)inputSquares.add(
                Movement(movementNotation,
                positionFile,
                positionRank,
                positionFile,
                i)
            )
        }
        return inputSquares
    }

    /** right: increase file */
    fun generateEastMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        for(i in positionFile+1..7){
            if(Math.abs(positionFile-i) <= distance)inputSquares.add(
                Movement(movementNotation,
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
    fun generateWestMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        for(i in positionFile-1 downTo 0){
            if(Math.abs(positionFile-i) <= distance)inputSquares.add(
                Movement(movementNotation,
                positionFile,
                positionRank,
                i,
                positionRank)
            )
            else break
        }
        return inputSquares
    }

    open class Movement(val movementNotation : MovementNotation = MovementNotation("", emptyList(),"",emptyList(),"")
                        , val sourceFile : Int
                        , val sourceRank : Int
                        , val targetFile : Int
                        , val targetRank : Int) {
        companion object{
            fun fromMovementToString(movement: Movement) : String {
                return movement.sourceFile.toString()+"_"+movement.sourceRank+"_"+movement.targetFile.toString()+"_"+movement.targetRank
            }

            fun fromStringToMovement(string: String) : Movement {
                val coordinates = string.split("_")
                if(coordinates.size == 4){
                    val sourceFile = coordinates[0].toInt()
                    val sourceRank = coordinates[1].toInt()
                    val targetFile = coordinates[2].toInt()
                    val targetRank = coordinates[3].toInt()
                    return Movement(sourceFile = sourceFile,sourceRank = sourceRank,targetFile = targetFile,targetRank = targetRank)
                }
                return Movement(sourceFile = -1,sourceRank = -1,targetFile = -1,targetRank = -1)
            }

            fun fromMovementListToString(movements: List<Movement>) : String {
                val returnString = StringBuilder("")
                for(movement in movements){
                    returnString.append(
                        movement.sourceFile.toString()+"_"+movement.sourceRank
                                +"_"+movement.targetFile.toString()+"_"+movement.targetRank)
                    if(movement != movements.last())returnString.append(";")
                }
                return returnString.toString()
            }

            fun fromStringToMovementList(string: String) : List<Movement> {
                val movementList = mutableListOf<Movement>()
                for(substring in string.split(";")){
                    val sourceFile = string.split("_")[0].toInt()
                    val sourceRank = string.split("_")[0].toInt()
                    val targetFile = string.split("_")[0].toInt()
                    val targetRank = string.split("_")[0].toInt()
                    movementList.add(Movement(sourceFile = sourceFile,sourceRank = sourceRank,targetFile = targetFile,targetRank = targetRank))
                }
                return movementList
            }
        }

        fun asString(playerColor : String): String {
            return playerColor+": "+sourceFile.toString()+"_"+sourceRank+"_"+targetFile+"_"+targetRank
        }
    }

    class PromotionMovement(movementNotation : MovementNotation = MovementNotation("", emptyList(),"",emptyList(),"")
                            , sourceFile : Int
                            , sourceRank : Int
                            , targetFile : Int
                            , targetRank : Int
                            , var promotion : String)  : Movement(movementNotation,sourceFile,sourceRank,targetFile,targetRank) {

        companion object {
            fun fromMovementToString(promotionMovement: PromotionMovement): String {
                return promotionMovement.sourceFile.toString() + "_" + promotionMovement.sourceRank + "_" +
                       promotionMovement.targetFile.toString() + "_" + promotionMovement.targetRank+"_"+promotionMovement.promotion
            }

            fun fromStringToMovement(string: String): Movement {
                val coordinates = string.split("_")
                if (coordinates.size == 5) {
                    val sourceFile = coordinates[0].toInt()
                    val sourceRank = coordinates[1].toInt()
                    val targetFile = coordinates[2].toInt()
                    val targetRank = coordinates[3].toInt()
                    val promotion = coordinates[4]
                    return PromotionMovement(
                        sourceFile = sourceFile,
                        sourceRank = sourceRank,
                        targetFile = targetFile,
                        targetRank = targetRank,
                        promotion = promotion
                    )
                }
                return Movement(sourceFile = -1, sourceRank = -1, targetFile = -1, targetRank = -1)
            }
        }

    }

    class MovementNotation(val grouping: String, val conditions: List<String>, val movetype: String, val distances: List<String>, val direction: String){


        companion object {
            val CASTLING_MOVEMENT = MovementNotation("", listOf(),"", listOf(),"")

            fun parseMovementString(movementString : String) : List<MovementNotation> {
                if(movementString.isEmpty())return emptyList()
                val movementList = mutableListOf<MovementNotation>()
                val movementArray = movementString.split(",")
                for(submovement in movementArray){
                    var submovementString = submovement
                    var grouping = ""
                    var conditions = mutableListOf<String>()
                    var movetype = ""
                    var distances = mutableListOf<String>()
                    var direction = ""
                    //move type
                    if(submovementString.contains("~")){movetype = "~";submovementString = submovementString.replace("~","")}
                    if(submovementString.contains("^")){movetype = "^";submovementString = submovementString.replace("^","")}
                    if(submovementString.contains("g")){movetype = "g";submovementString = submovementString.replace("g","")}
                    //grouping
                    if(submovementString.contains("/")){grouping = "/";submovementString = submovementString.replace("/","")}
                    if(submovementString.contains("&")){grouping = "&";submovementString = submovementString.replace("&","")}
                    if(submovementString.contains(".")){grouping = ".";submovementString = submovementString.replace(".","")}
                    //move conditions
                    if(submovementString.contains("i")){conditions.add("i");submovementString = submovementString.replace("i","")}
                    if(submovementString.contains("c")){conditions.add("c");submovementString = submovementString.replace("c","")}
                    if(submovementString.contains("o")){conditions.add("o");submovementString = submovementString.replace("o","")}
                    //direction
                    if(submovementString.contains(">=")){direction = ">=";submovementString = submovementString.replace(">=","")}
                    if(submovementString.contains("<=")){direction = "<=";submovementString = submovementString.replace("<=","")}
                    if(submovementString.contains("<>")){direction = "<>";submovementString = submovementString.replace("<>","")}
                    if(submovementString.contains("=")){direction = "=";submovementString = submovementString.replace("=","")}
                    if(submovementString.contains("X>")){direction = "X>";submovementString = submovementString.replace("X>","")}
                    if(submovementString.contains("X<")){direction = "X<";submovementString = submovementString.replace("X<","")}
                    if(submovementString.contains("X")){direction = "X";submovementString = submovementString.replace("X","")}
                    if(submovementString.contains(">")){direction = ">";submovementString = submovementString.replace(">","")}
                    if(submovementString.contains("<")){direction = "<";submovementString = submovementString.replace("<","")}
                    if(submovementString.contains("+")){direction = "+";submovementString = submovementString.replace("+","")}
                    if(submovementString.contains("*")){direction = "*";submovementString = submovementString.replace("*","")}
                    //distance
                    if(grouping == ""){
                        if(submovementString.contains("n"))distances.add("n")
                        if(submovementString.contains("[0-9]".toRegex()))distances.add(submovementString.replace("\\D+".toString(),""))
                    } else {
                        distances = submovementString.split("").toMutableList()
                        distances.removeAll(Collections.singleton(""))
                    }
                    movementList.add(
                        MovementNotation(
                            grouping,
                            conditions,
                            movetype,
                            distances.toList(),
                            direction
                        )
                    )
                }
                return movementList
            }
        }
    }
}