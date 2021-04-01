package emerald.apps.fairychess.model.pieces

import kotlin.math.sign

class ChessPiece (
    var name : String,
    var position : Array<Int>,
    var value : Int,
    var color : String,
    var movingPatternString : String) {

    open fun move(rank : Int, file : Int) : Boolean {return true}
    open fun getTargetSquares() : List<Array<Int>> {return listOf()}


    /** parlett notation: <conditions> <move type> <distance> <direction> <other>
     *
     */
    fun generateTargetSquares() : List<Array<Int>>{
        val targetCoordinates = mutableListOf<Array<Int>>()
        val movingPatterns = movingPatternString.split(",")
        for(movingPattern in movingPatterns){
            if(movingPattern.startsWith("~")){
                targetCoordinates.addAll(generateLeaperTargetSquares(movingPattern.replaceFirst("~","")))
            } else {
                targetCoordinates.addAll(generateRiderTargetSquares(movingPattern))
            }
        }
        return targetCoordinates
    }

    fun generateLeaperTargetSquares(movingPattern: String) : List<Array<Int>> {
        val targetSquares = mutableListOf<Array<Int>>()
        val movements = movingPattern.split("/")
        //leaper-movements always have 8 sub-moves
        val m1 = movements[0].toInt()
        val m2 = movements[1].toInt()
        if(position[0]+m1 in 0..7 && position[1]+m2 in 0..7)targetSquares.add(arrayOf(position[0]+m1,position[1]+m2))
        if(position[0]-m1 in 0..7 && position[1]+m2 in 0..7)targetSquares.add(arrayOf(position[0]-m1,position[1]+m2))
        if(position[0]+m1 in 0..7 && position[1]-m2 in 0..7)targetSquares.add(arrayOf(position[0]+m1,position[1]-m2))
        if(position[0]-m1 in 0..7 && position[1]-m2 in 0..7)targetSquares.add(arrayOf(position[0]-m1,position[1]-m2))
        if(position[0]+m2 in 0..7 && position[1]+m1 in 0..7)targetSquares.add(arrayOf(position[0]+m2,position[1]+m1))
        if(position[0]-m2 in 0..7 && position[1]+m1 in 0..7)targetSquares.add(arrayOf(position[0]-m2,position[1]+m1))
        if(position[0]+m2 in 0..7 && position[1]-m1 in 0..7)targetSquares.add(arrayOf(position[0]+m2,position[1]-m1))
        if(position[0]-m2 in 0..7 && position[1]-m1 in 0..7)targetSquares.add(arrayOf(position[0]-m2,position[1]-m1))
        return targetSquares
    }

    fun generateRiderTargetSquares(movingPattern: String) : List<Array<Int>> {
        val targetSquares = mutableListOf<Array<Int>>()
        var quantity = ""
        var dir = ""
        if(movingPattern.length == 2){
            quantity = movingPattern.toCharArray()[0].toString()
            dir = movingPattern.toCharArray()[1].toString()
        }
        when(dir){
            "+" -> {
                targetSquares.addAll(generateOrthogonalSquares("+"))
            }
            "*" -> {
                targetSquares.addAll(generateOrthogonalSquares("+"))
                targetSquares.addAll(generateDiagonalSquares("X"))
            }
            "X" -> {
                targetSquares.addAll(generateDiagonalSquares("X"))
            }
        }
        return targetSquares
    }

    fun generateDiagonalSquares(mode:String) : List<Array<Int>>{
        val targetSquares = mutableListOf<Array<Int>>()

        var difX = 1; var difY = 1
        //right,forward
        while(position[0]+difX <= 7 && position[1]+difY <= 7) {
            targetSquares.add(arrayOf(position[0]+difX,position[1]+difY))
            ++difX
            ++difY
        }
        difX = 0; difY = 0

        //left,forward
        while(position[0]+difX >= 0 && position[1]+difY <= 7) {
            targetSquares.add(arrayOf(position[0]+difX,position[1]+difY))
            --difX
            ++difY
        }
        difX = 0; difY = 0

        //right,backwards
        while(position[0]+difX <= 7 && position[1]+difY >= 0) {
            targetSquares.add(arrayOf(position[0]+difX,position[1]+difY))
            ++difX
            --difY
        }
        difX = 0; difY = 0

        //left,backwards
        while(position[0]+difX >= 0 && position[1]+difY >= 0) {
            targetSquares.add(arrayOf(position[0]+difX,position[1]+difY))
            --difX
            --difY
        }
        return targetSquares
    }

    fun generateOrthogonalSquares(mode:String) : List<Array<Int>>{
        val targetSquares = mutableListOf<Array<Int>>()

        //forward
        for(i in position[0]..7){
            targetSquares.add(arrayOf(i,position[1]))
        }
        //backward
        for(i in position[0] downTo 0){
            targetSquares.add(arrayOf(i,position[1]))
        }
        //right
        for(i in position[1]..7){
            targetSquares.add(arrayOf(position[0],i))
        }
        //left
        for(i in position[1] downTo 0){
            targetSquares.add(arrayOf(position[0],i))
        }
        return targetSquares
    }
}