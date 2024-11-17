package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.bitboard.Bitboard
import emerald.apps.fairychess.model.bitboard.Coordinate
import java.util.*
import kotlin.math.abs
import kotlin.math.sign

open class Movement(val movementNotation : MovementNotation = MovementNotation("", emptyList(),"",emptyList(),"")
                    , val sourceRank : Int
                    , val sourceFile : Int
                    , val targetRank : Int
                    , val targetFile : Int) {

    constructor(sourceRank : Int, sourceFile : Int, targetRank : Int, targetFile : Int)
            : this(MovementNotation("", emptyList(),"",emptyList(),""),sourceRank, sourceFile, targetRank, targetFile)


    constructor(movementNotation: MovementNotation, source : Coordinate, targetRank:Int, targetFile:Int) : this(movementNotation, source.rank,source.file,targetRank,targetFile)
    constructor(source : Coordinate, targetRank:Int, targetFile:Int) : this(source.rank,source.file,targetRank,targetFile)
    constructor(source : Coordinate, target : Coordinate) : this(source.rank,source.file,target.rank,target.file)

    fun getSourceCoordinate(): Coordinate {
        return Coordinate(sourceRank, sourceFile)
    }
    fun getTargetCoordinate(): Coordinate {
        return Coordinate(targetRank, targetFile)
    }
    fun getRankDif() : Int{
        return abs(targetRank - sourceRank)
    }
    fun getSignRank() : Int {
        return sign(targetRank.toDouble() - sourceRank.toDouble()).toInt()
    }

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

    constructor(sourceRank : Int, sourceFile : Int, targetRank : Int, targetFile : Int,promotion: String)
            : this(MovementNotation("", emptyList(),"",emptyList(),""),sourceRank, sourceFile, targetRank, targetFile,promotion)

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
        val KING = MovementNotation("", listOf(),"", listOf("1"),"*")
        val PAWN_ENPASSANTE = MovementNotation("", listOf(),"", listOf("1"),"EN_PASSANTE")
        val CASTLING_SHORT_WHITE = MovementNotation("", listOf(),"CASTLING_SHORT_WHITE", listOf(),"")
        val CASTLING_LONG_WHITE = MovementNotation("", listOf(),"CASTLING_LONG_WHITE", listOf(),"")
        val CASTLING_SHORT_BLACK = MovementNotation("", listOf(),"CASTLING_SHORT_BLACK", listOf(),"")
        val CASTLING_LONG_BLACK = MovementNotation("", listOf(),"CASTLING_LONG_BLACK", listOf(),"")

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