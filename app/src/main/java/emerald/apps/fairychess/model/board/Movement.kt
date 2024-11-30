package emerald.apps.fairychess.model.board

import java.util.*
import kotlin.math.abs
import kotlin.math.sign

open class Movement(val movementNotation : MovementNotation = MovementNotation("", emptyList(),"",emptyList(),"")
                    , val sourceFile : Int
                    , val sourceRank : Int
                    , val targetFile : Int
                    , val targetRank : Int) {

    constructor(sourceFile : Int, sourceRank : Int, targetFile : Int, targetRank : Int)
            : this(MovementNotation("", emptyList(),"",emptyList(),""), sourceFile, sourceRank, targetFile, targetRank)

    constructor(movementNotation: MovementNotation, source: Coordinate, targetFile: Int, targetRank: Int)
            : this(movementNotation, source.file, source.rank, targetFile, targetRank)
    constructor(source: Coordinate, targetFile: Int, targetRank: Int)
            : this(source.file, source.rank, targetFile, targetRank)
    constructor(source: Coordinate, target: Coordinate)
            : this(source.file, source.rank, target.file, target.rank)

    fun getSourceCoordinate(): Coordinate {
        return Coordinate(sourceFile, sourceRank)
    }

    fun getTargetCoordinate(): Coordinate {
        return Coordinate(targetFile, targetRank)
    }

    fun getRankDif(): Int {
        return abs(targetRank - sourceRank)
    }

    fun getFileDif(): Int {
        return abs(targetFile - sourceFile)
    }

    fun getSignRank(): Int {
        return sign(targetRank.toDouble() - sourceRank.toDouble()).toInt()
    }

    override fun equals(other: Any?): Boolean {
        return if(other is Movement) {
            (sourceFile == other.sourceFile
                    && sourceRank == other.sourceRank
                    && targetFile == other.targetFile
                    && targetRank == other.targetRank
                    && movementNotation == other.movementNotation
                    )
        } else super.equals(other)
    }

    companion object {
        fun emptyMovement(): Movement {
            return Movement(sourceFile = 0, sourceRank = 0, targetFile = 0, targetRank = 0)
        }

        fun fromMovementToString(movement: Movement): String {
            return movement.sourceFile.toString() + "_" + movement.sourceRank + "_" + movement.targetFile.toString() + "_" + movement.targetRank
        }

        fun fromStringToMovement(string: String): Movement {
            val coordinates = string.split("_")
            if(coordinates.size == 4) {
                val sourceFile = coordinates[0].toInt()
                val sourceRank = coordinates[1].toInt()
                val targetFile = coordinates[2].toInt()
                val targetRank = coordinates[3].toInt()
                return Movement(sourceFile = sourceFile, sourceRank = sourceRank, targetFile = targetFile, targetRank = targetRank)
            }
            return Movement(sourceFile = -1, sourceRank = -1, targetFile = -1, targetRank = -1)
        }

        fun fromMovementListToString(movements: List<Movement>): String {
            val returnString = StringBuilder("")
            for(movement in movements) {
                returnString.append(
                    movement.sourceFile.toString() + "_" + movement.sourceRank
                            + "_" + movement.targetFile.toString() + "_" + movement.targetRank)
                if(movement != movements.last()) returnString.append(";")
            }
            return returnString.toString()
        }

        fun fromStringToMovementList(string: String): List<Movement> {
            val movementList = mutableListOf<Movement>()
            for(substring in string.split(";")) {
                if(substring.split("_").size == 4) {
                    val sourceFile = substring.split("_")[0].toInt()
                    val sourceRank = substring.split("_")[1].toInt()
                    val targetFile = substring.split("_")[2].toInt()
                    val targetRank = substring.split("_")[3].toInt()
                    movementList.add(Movement(sourceFile = sourceFile, sourceRank = sourceRank, targetFile = targetFile, targetRank = targetRank))
                }
            }
            return movementList
        }
    }

    fun asString(playerColor: String): String {
        return playerColor + ": " + sourceFile.toString() + "_" + sourceRank + "_" + targetFile + "_" + targetRank
    }

    fun asString2(playerColor: String): String {
        return playerColor + ": " + getLetterFromInt(sourceFile) + sourceRank + "_" + getLetterFromInt(targetFile) + targetRank
    }

    fun getLetterFromInt(int: Int): String {
        when(int) {
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

class PromotionMovement(movementNotation: MovementNotation = MovementNotation("", emptyList(),"",emptyList(),"")
                        , sourceFile: Int
                        , sourceRank: Int
                        , targetFile: Int
                        , targetRank: Int
                        , var promotion: String) : Movement(movementNotation, sourceFile, sourceRank, targetFile, targetRank) {

    constructor(sourceFile: Int, sourceRank: Int, targetFile: Int, targetRank: Int, promotion: String)
            : this(MovementNotation("", emptyList(),"",emptyList(),""), sourceFile, sourceRank, targetFile, targetRank, promotion)

    companion object {
        fun fromMovementToString(promotionMovement: PromotionMovement): String {
            return promotionMovement.sourceFile.toString() + "_" + promotionMovement.sourceRank + "_" +
                    promotionMovement.targetFile.toString() + "_" + promotionMovement.targetRank + "_" + promotionMovement.promotion
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

class MovementNotation(val grouping: String, val conditions: List<String>, val movetype: String, val distances: List<String>, val direction: String) {
    // MovementNotation class remains unchanged as it doesn't deal with coordinate ordering

    override fun equals(other: Any?): Boolean {
        return if(other is MovementNotation) {
            (grouping == other.grouping
                    && conditions == other.conditions
                    && movetype == other.movetype
                    && distances == other.distances
                    && direction == other.direction
                    )
        } else super.equals(other)
    }

    override fun toString(): String {
        return grouping + conditions.toString() + movetype + distances + direction
    }

    companion object {
        val KING = MovementNotation("", listOf(),"", listOf("1"),"*")
        val PAWN_ENPASSANTE = MovementNotation("", listOf(),"", listOf("1"),"EN_PASSANTE")
        val CASTLING_SHORT_WHITE = MovementNotation("", listOf(),"CASTLING_SHORT_WHITE", listOf(),"")
        val CASTLING_LONG_WHITE = MovementNotation("", listOf(),"CASTLING_LONG_WHITE", listOf(),"")
        val CASTLING_SHORT_BLACK = MovementNotation("", listOf(),"CASTLING_SHORT_BLACK", listOf(),"")
        val CASTLING_LONG_BLACK = MovementNotation("", listOf(),"CASTLING_LONG_BLACK", listOf(),"")

        // parseMovementString function remains unchanged as it doesn't deal with coordinate ordering
        fun parseMovementString(movementString: String): List<MovementNotation> {
            if(movementString.isEmpty()) return emptyList()
            val movementList = mutableListOf<MovementNotation>()
            val movementArray = movementString.split(",")
            for(submovement in movementArray) {
                var submovementString = submovement
                var grouping = ""
                var conditions = mutableListOf<String>()
                var movetype = ""
                var distances = mutableListOf<String>()
                var direction = ""
                //move type
                if(submovementString.contains("~")) {movetype = "~";submovementString = submovementString.replace("~","")}
                if(submovementString.contains("^")) {movetype = "^";submovementString = submovementString.replace("^","")}
                if(submovementString.contains("g")) {movetype = "g";submovementString = submovementString.replace("g","")}
                //grouping
                if(submovementString.contains("/")) {grouping = "/";submovementString = submovementString.replace("/","")}
                if(submovementString.contains("&")) {grouping = "&";submovementString = submovementString.replace("&","")}
                if(submovementString.contains(".")) {grouping = ".";submovementString = submovementString.replace(".","")}
                //move conditions
                if(submovementString.contains("i")) {conditions.add("i");submovementString = submovementString.replace("i","")}
                if(submovementString.contains("c")) {conditions.add("c");submovementString = submovementString.replace("c","")}
                if(submovementString.contains("o")) {conditions.add("o");submovementString = submovementString.replace("o","")}
                //direction
                if(submovementString.contains(">=")) {direction = ">=";submovementString = submovementString.replace(">=","")}
                if(submovementString.contains("<=")) {direction = "<=";submovementString = submovementString.replace("<=","")}
                if(submovementString.contains("<>")) {direction = "<>";submovementString = submovementString.replace("<>","")}
                if(submovementString.contains("=")) {direction = "=";submovementString = submovementString.replace("=","")}
                if(submovementString.contains("X>")) {direction = "X>";submovementString = submovementString.replace("X>","")}
                if(submovementString.contains("X<")) {direction = "X<";submovementString = submovementString.replace("X<","")}
                if(submovementString.contains("X")) {direction = "X";submovementString = submovementString.replace("X","")}
                if(submovementString.contains(">")) {direction = ">";submovementString = submovementString.replace(">","")}
                if(submovementString.contains("<")) {direction = "<";submovementString = submovementString.replace("<","")}
                if(submovementString.contains("+")) {direction = "+";submovementString = submovementString.replace("+","")}
                if(submovementString.contains("*")) {direction = "*";submovementString = submovementString.replace("*","")}
                //distance
                if(grouping == "") {
                    if(submovementString.contains("n")) distances.add("n")
                    if(submovementString.contains("[0-9]".toRegex())) distances.add(submovementString.replace("\\D+".toString(),""))
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