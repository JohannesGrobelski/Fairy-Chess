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

    var movingPatterns = MovementNotation.parseMovementString(movingPatternString)

    /** parlett notation: <conditions> <move type> <distance> <direction> <other>
     *
     */
    fun generateMovements() : List<Movement>{
        val targetCoordinates = mutableListOf<Movement>()
        for(movingPattern in movingPatterns){
            if(movingPattern.movetype == "~" || movingPattern.movetype == "^" || movingPattern.movetype == "g"){
                targetCoordinates.addAll(generateLeaperMovements(movingPattern))
            } else {
                targetCoordinates.addAll(generateRiderMovements(movingPattern))
            }
        }
        return targetCoordinates
    }

    fun generateLeaperMovements(movingPattern: MovementNotation) : List<Movement> {
        val targetSquares = mutableListOf<Movement>()
        if(movingPattern.grouping == "/" && movingPattern.distances.size == 2){
            //leaper-movements always have 8 sub-moves:
            //(2: increase/decrease)*(2: value1/value2)*(2: on File / on Rank) = 8 permutations
            val m1 = movingPattern.distances[0]
            val m2 = movingPattern.distances[1]
            if(m1.matches("[0-9]".toRegex()) && m2.matches("[0-9]".toRegex())){
                generate8LeaperMovements(movingPattern,targetSquares,m1.toInt(), m2.toInt())
            } else {
                if(m1 == "x" && m2 == "x"){//only in pairs (x,x): any distance in the given direction equal to its twin or zero
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

    fun generateLeaperMovement(movingPattern: MovementNotation, targetSquares : MutableList<Movement>, m1: Int, m2: Int) {
        if(positionFile+m1 in 0..7 && positionRank+m2 in 0..7){
            targetSquares.add(
                Movement(movingPattern,
                    positionFile,
                    positionRank,
                    positionFile+m1,
                    positionRank+m2)
            )
        }
    }



    fun generateRiderMovements(movingPattern: MovementNotation) : List<Movement> {
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

    fun generateDiagonalSquares(movementNotation: MovementNotation) : List<Movement>{
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
    fun generateForewardRightSquares(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
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

    /** left,forward: decrease file, increase rank*/
    fun generateForewardLeftSquares(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
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

    /** right,backward: increase file, decrease rank*/
    fun generateBackwardsRightSquares(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
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

    /** left,backward: decrease file, decrease rank*/
    fun generateBackwardsLeftSquares(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
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

    fun generateOrthogonalSquares(movementNotation: MovementNotation) : List<Movement>{
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
    fun generateForewardSquares(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
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
    fun generateBackwardsSquares(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
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
    fun generateRightSquares(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
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
    fun generateLeftSquares(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
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

    class Movement(val movementNotation : MovementNotation = MovementNotation("", emptyList(),"",emptyList(),"")
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

    }

    class MovementNotation(val grouping: String, val conditions: List<String>, val movetype: String, val distances: List<String>, val direction: String){
        companion object {
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