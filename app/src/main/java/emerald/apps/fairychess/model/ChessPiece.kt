package emerald.apps.fairychess.model

import java.lang.StringBuilder
import java.util.*
import kotlin.math.abs

class ChessPiece(
    var name: String,
    var positionRank: Int,
    var positionFile: Int,
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

    /** generate a list of movement matching the movementNotation (Leaper) */
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
                    for(distance in -7..7){
                        //orthogonal
                        generateLeaperMovement(movingPattern,targetSquares,0, distance)
                        generateLeaperMovement(movingPattern,targetSquares,distance, 0)
                        //diagonal
                        generateLeaperMovement(movingPattern,targetSquares,distance, distance)
                        generateLeaperMovement(movingPattern,targetSquares,-distance, distance)
                        generateLeaperMovement(movingPattern,targetSquares,distance, -distance)
                        generateLeaperMovement(movingPattern,targetSquares,-distance, -distance)
                    }
                }
            }
        }
        return targetSquares
    }

    /** generate all movement-variations with the 2 distances (8 different leaper movements) matching movementNotation (Leaper)
     * number of distances * number of sequences * number of sign = 2*2*2 = 8 */
    fun generate8LeaperMovements(movingPattern: MovementNotation, targetSquares : MutableList<Movement>, dis1: Int, dis2: Int) {
        generateLeaperMovement(movingPattern,targetSquares,dis1,dis2)
        generateLeaperMovement(movingPattern,targetSquares,-dis1,dis2)
        generateLeaperMovement(movingPattern,targetSquares,dis1,-dis2)
        generateLeaperMovement(movingPattern,targetSquares,-dis1,-dis2)
        generateLeaperMovement(movingPattern,targetSquares,dis2,dis1)
        generateLeaperMovement(movingPattern,targetSquares,-dis2,dis1)
        generateLeaperMovement(movingPattern,targetSquares,dis2,-dis1)
        generateLeaperMovement(movingPattern,targetSquares,-dis2,-dis1)
    }

    /** add a leaper movement to targetSquares defined by an delta (fileDif,rankDif) */
    fun generateLeaperMovement(movingPattern: MovementNotation, targetSquares : MutableList<Movement>, rankDif: Int, fileDif: Int) {
        if(positionRank+rankDif in 0..7 && positionFile+fileDif in 0..7){
            targetSquares.add(
                Movement(movingPattern,
                    positionRank,
                    positionFile,
                    positionRank+rankDif,
                    positionFile+fileDif)
            )
        }
    }

    /** generate a list of rider-movements matching the movementNotation (rider) */
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
            if(abs(difRank) <= distance && abs(difFile) <= distance){
                inputSquares.add(
                    Movement(movementNotation,
                    positionRank,
                    positionFile,
                    positionRank+difRank,
                    positionFile+difFile)
                )
                ++difFile
                ++difRank
            } else break
        }
        return inputSquares
    }

    /** NorthWestDiagonalMovement: left,forward: increase file, decrease rank*/
    fun generateNorthWestDiagonalMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        var difFile = 1; var difRank = -1;
        while(positionFile+difFile <= 7 && positionRank+difRank >= 0) {
            if(abs(difFile) <= distance && abs(difRank) <= distance){
                inputSquares.add(
                    Movement(movementNotation,
                    positionRank,
                    positionFile,
                    positionRank+difRank,
                    positionFile+difFile)
                )
                ++difFile
                --difRank
            } else break
        }
        return inputSquares
    }

    /** SouthEastDiagonalMovement: right,backward: decrease file, increase rank*/
    fun generateSouthEastDiagonalMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        var difFile = -1; var difRank = 1
        while(positionFile+difFile >= 0 && positionRank+difRank <= 7) {
            if(abs(difRank) <= distance && abs(difFile) <= distance){
                inputSquares.add(
                    Movement(movementNotation,
                    positionRank,
                    positionFile,
                    positionRank+difRank,
                    positionFile+difFile)
                )
                --difFile
                ++difRank
            } else break
        }
        return inputSquares
    }

    /** SouthWestDiagonalMovement: left,backward: decrease file, decrease rank*/
    fun generateSouthWestDiagonalMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        var difRank = -1; var difFile = -1
        while(positionFile+difFile >= 0 && positionRank+difRank >= 0) {
            if(abs(difRank) <= distance && abs(difFile) <= distance){
                inputSquares.add(
                    Movement(movementNotation,
                    positionRank,
                    positionFile,
                    positionRank+difRank,
                    positionFile+difFile)
                )
                --difRank
                --difFile
            } else break
        }
        return inputSquares
    }

    /** generate all orthogonal movements: horizontal (WEST,EAST movements) and vertical (NORTH,SOUTH)*/
    fun generateOrthogonalMovement(movementNotation: MovementNotation) : List<Movement>{
        val targetSquares = mutableListOf<Movement>()
        var distance = 7
        if(movementNotation.distances[0].matches("[1-9]+".toRegex()))distance = movementNotation.distances[0].toInt()
        //forward(>) and backwards(<) are color-dependent because they are depending on direction of the figures
        //forward-backwards(=), orthogonal (+) and orthogonal-diagonal (*) are color-independent movements
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

    /** forward: increase file */
    fun generateNorthMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        if(movementNotation.direction == "*" || movementNotation.direction == "+" || movementNotation.direction == "<>" || movementNotation.direction == ">=" || movementNotation.direction == ">"){
            for(newFile in positionFile+1..7){
                if(abs(positionFile-newFile) <= distance)inputSquares.add(
                    Movement(movementNotation,
                    positionRank,
                    positionFile,
                    positionRank,
                    newFile)
                )
                else break
            }
        }
        return inputSquares
    }

    /** backward: decrease file */
    fun generateSouthMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        if(movementNotation.direction == "*" || movementNotation.direction == "+" || movementNotation.direction == "<>" || movementNotation.direction == ">=" || movementNotation.direction == ">"){
            for(newFile in positionFile-1 downTo 0){
                if(abs(positionFile-newFile) <= distance)inputSquares.add(
                    Movement(movementNotation,
                        positionRank,
                        positionFile,
                        positionRank,
                        newFile)
                )
                else break
            }
        }
        return inputSquares
    }

    /** right: increase rank */
    fun generateEastMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        for(newRank in positionRank+1..7){
            if(abs(positionRank-newRank) <= distance)inputSquares.add(
                Movement(movementNotation,
                positionRank,
                positionFile,
                newRank,
                positionFile)
            )
            else break
        }
        return inputSquares
    }

    /** left: decrease file */
    fun generateWestMovement(inputSquares : MutableList<Movement>, movementNotation: MovementNotation, distance : Int) : List<Movement> {
        for(newRank in positionRank-1 downTo 0){
            if(abs(positionRank-newRank) <= distance)inputSquares.add(
                Movement(movementNotation,
                    positionRank,
                    positionFile,
                    newRank,
                    positionFile)
            )
            else break
        }
        return inputSquares
    }

    open class Movement(val movementNotation : MovementNotation = MovementNotation("", emptyList(),"",emptyList(),"")
                        , val sourceRank : Int
                        , val sourceFile : Int
                        , val targetRank : Int
                        , val targetFile : Int) {

        override fun equals(other: Any?) : Boolean {
            return if(other is Movement){
                (sourceRank == other.sourceRank
                && sourceFile == other.sourceFile
                && targetRank == other.targetRank
                && targetFile == other.targetFile
                && movementNotation == other.movementNotation
                )
            } else super.equals(other)
        }



        companion object{
            fun emptyMovement() : Movement {
                return Movement(sourceFile = 0,sourceRank = 0,targetFile = 0,targetRank = 0)
            }

            fun fromMovementToString(movement: Movement) : String {
                return movement.sourceFile.toString()+"_"+movement.sourceRank+"_"+movement.targetFile.toString()+"_"+movement.targetRank
            }

            fun fromStringToMovement(string: String) : Movement {
                val coordinates = string.split("_")
                if(coordinates.size == 4){
                    val sourceRank = coordinates[0].toInt()
                    val sourceFile = coordinates[1].toInt()
                    val targetRank = coordinates[2].toInt()
                    val targetFile = coordinates[3].toInt()
                    return Movement(sourceRank = sourceRank,sourceFile = sourceFile,targetRank = targetRank,targetFile = targetFile)
                }
                return Movement(sourceRank = -1,sourceFile = -1,targetRank = -1,targetFile = -1)
            }

            fun fromMovementListToString(movements: List<Movement>) : String {
                val returnString = StringBuilder("")
                for(movement in movements){
                    returnString.append(
                        movement.sourceRank.toString()+"_"+movement.sourceFile
                                +"_"+movement.targetRank.toString()+"_"+movement.targetFile)
                    if(movement != movements.last())returnString.append(";")
                }
                return returnString.toString()
            }

            fun fromStringToMovementList(string: String) : List<Movement> {
                val movementList = mutableListOf<Movement>()
                for(substring in string.split(";")){
                    if(substring.split("_").size == 4){
                        val sourceRank = substring.split("_")[0].toInt()
                        val sourceFile = substring.split("_")[1].toInt()
                        val targetRank = substring.split("_")[2].toInt()
                        val targetFile = substring.split("_")[3].toInt()
                        movementList.add(Movement(sourceRank = sourceRank,sourceFile = sourceFile,targetRank = targetRank,targetFile = targetFile))
                    }
                }
                return movementList
            }
        }

        fun asString(playerColor : String): String {
            return playerColor+": "+sourceRank.toString()+"_"+sourceFile+"_"+targetRank+"_"+targetFile
        }

        fun asString2(playerColor : String): String {
            return playerColor+": "+getLetterFromInt(sourceRank)+sourceFile+"_"+getLetterFromInt(targetRank)+targetFile
        }

        fun getLetterFromInt(int: Int) : String{
            when(int){
                0 -> return "A"
                1 -> return "B"
                2 -> return "C"
                3 -> return "D"
                4 -> return "E"
                5 -> return "F"
                6 -> return "G"
                7 -> return "H"
            }
            return ""
        }
    }

    class PromotionMovement(movementNotation : MovementNotation = MovementNotation("", emptyList(),"",emptyList(),"")
                            , sourceRank: Int
                            , sourceFile: Int
                            , targetRank: Int
                            , targetFile: Int
                            , var promotion : String)  : Movement(movementNotation,sourceRank,sourceFile,targetRank,targetFile) {

        companion object {
            fun fromMovementToString(promotionMovement: PromotionMovement): String {
                return promotionMovement.sourceRank.toString() + "_" + promotionMovement.sourceFile + "_" +
                       promotionMovement.targetRank.toString() + "_" + promotionMovement.targetFile+"_"+promotionMovement.promotion
            }

            fun fromStringToMovement(string: String): Movement {
                val coordinates = string.split("_")
                if (coordinates.size == 5) {
                    val sourceRank = coordinates[0].toInt()
                    val sourceFile = coordinates[1].toInt()
                    val targetRank = coordinates[2].toInt()
                    val targetFile = coordinates[3].toInt()
                    val promotion = coordinates[4]
                    return PromotionMovement(
                        sourceRank = sourceRank,
                        sourceFile = sourceFile,
                        targetRank = targetRank,
                        targetFile = targetFile,
                        promotion = promotion
                    )
                }
                return Movement(sourceRank = -1, sourceFile = -1, targetRank = -1, targetFile = -1)
            }
        }

    }

    class MovementNotation(val grouping: String, val conditions: List<String>, val movetype: String, val distances: List<String>, val direction: String){

        override fun equals(other: Any?) : Boolean {
            return if(other is MovementNotation){
                (grouping == other.grouping
                        && conditions == other.conditions
                        && movetype == other.movetype
                        && distances == other.distances
                        && direction == other.direction
                        )
            } else super.equals(other)
        }

        override fun toString(): String {
            return grouping+conditions.toString()+movetype+distances+direction
        }

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