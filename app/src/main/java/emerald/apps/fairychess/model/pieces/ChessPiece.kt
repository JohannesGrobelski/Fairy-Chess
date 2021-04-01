package emerald.apps.fairychess.model.pieces

import androidx.core.text.isDigitsOnly

class ChessPiece (
    var name : String,
    var position : Array<Int>,
    var value : Int,
    var color : String,
    var movingPatternString : String) {

    open fun move(rank : Int, file : Int) : Boolean {return true}
    open fun getTargetSquares() : List<Array<Int>> {return listOf()}
    var movingPatterns = Chessboard.Movement.parseMovementString(movingPatternString)

    /** parlett notation: <conditions> <move type> <distance> <direction> <other>
     *
     */
    fun generateTargetSquares() : List<Array<Int>>{
        val targetCoordinates = mutableListOf<Array<Int>>()
        for(movingPattern in movingPatterns){
            if(movingPattern.movetype == "~"){
                targetCoordinates.addAll(generateLeaperTargetSquares(movingPattern))
            } else {
                targetCoordinates.addAll(generateRiderTargetSquares(movingPattern))
            }
        }
        return targetCoordinates
    }

    fun generateLeaperTargetSquares(movingPattern: Chessboard.Movement) : List<Array<Int>> {
        val targetSquares = mutableListOf<Array<Int>>()
        if(movingPattern.grouping == "/" && movingPattern.distances.size == 2){
            //leaper-movements always have 8 sub-moves
            val m1 = movingPattern.distances[0].toInt()
            val m2 = movingPattern.distances[1].toInt()
            if(position[0]+m1 in 0..7 && position[1]+m2 in 0..7)targetSquares.add(arrayOf(position[0]+m1,position[1]+m2))
            if(position[0]-m1 in 0..7 && position[1]+m2 in 0..7)targetSquares.add(arrayOf(position[0]-m1,position[1]+m2))
            if(position[0]+m1 in 0..7 && position[1]-m2 in 0..7)targetSquares.add(arrayOf(position[0]+m1,position[1]-m2))
            if(position[0]-m1 in 0..7 && position[1]-m2 in 0..7)targetSquares.add(arrayOf(position[0]-m1,position[1]-m2))
            if(position[0]+m2 in 0..7 && position[1]+m1 in 0..7)targetSquares.add(arrayOf(position[0]+m2,position[1]+m1))
            if(position[0]-m2 in 0..7 && position[1]+m1 in 0..7)targetSquares.add(arrayOf(position[0]-m2,position[1]+m1))
            if(position[0]+m2 in 0..7 && position[1]-m1 in 0..7)targetSquares.add(arrayOf(position[0]+m2,position[1]-m1))
            if(position[0]-m2 in 0..7 && position[1]-m1 in 0..7)targetSquares.add(arrayOf(position[0]-m2,position[1]-m1))
        }

        return targetSquares
    }

    fun generateRiderTargetSquares(movingPattern: Chessboard.Movement) : List<Array<Int>> {
        val targetSquares = mutableListOf<Array<Int>>()
        if(movingPattern.distances.isNotEmpty()){
            when(movingPattern.direction){
                "+" -> {
                    targetSquares.addAll(generateOrthogonalSquares("+", movingPattern.distances[0]))
                }
                "*" -> {
                    targetSquares.addAll(generateOrthogonalSquares("+", movingPattern.distances[0]))
                    targetSquares.addAll(generateDiagonalSquares("X", movingPattern.distances[0]))
                }
                "X" -> {
                    targetSquares.addAll(generateDiagonalSquares("X", movingPattern.distances[0]))
                }
            }
        }
        return targetSquares
    }

    fun generateDiagonalSquares(mode: String, quantity: String) : List<Array<Int>>{
        val targetSquares = mutableListOf<Array<Int>>()
        var quantityInt = 7
        if(quantity.matches("[1-9]+".toRegex())){
            quantityInt = quantity.toInt()
        }

        //right,forward
        var difX = 1; var difY = 1
        while(position[0]+difX <= 7 && position[1]+difY <= 7) {
            if(Math.abs(difX) <= quantityInt && Math.abs(difY) <= quantityInt){
                targetSquares.add(arrayOf(position[0]+difX,position[1]+difY))
                ++difX
                ++difY
            } else break
        }

        //left,forward
        difX = -1; difY = 1
        while(position[0]+difX >= 0 && position[1]+difY <= 7) {
            if(Math.abs(difX) <= quantityInt && Math.abs(difY) <= quantityInt){
                targetSquares.add(arrayOf(position[0] + difX, position[1] + difY))
                --difX
                ++difY
            } else break
        }

        //right,backwards
        difX = 1; difY = -1
        while(position[0]+difX <= 7 && position[1]+difY >= 0) {
            if(Math.abs(difX) <= quantityInt && Math.abs(difY) <= quantityInt){
                targetSquares.add(arrayOf(position[0]+difX,position[1]+difY))
                ++difX
                --difY
            } else break
        }

        //left,backwards
        difX = -1; difY = -1
        while(position[0]+difX >= 0 && position[1]+difY >= 0) {
            if(Math.abs(difX) <= quantityInt && Math.abs(difY) <= quantityInt){
                targetSquares.add(arrayOf(position[0]+difX,position[1]+difY))
                --difX
                --difY
            } else break
        }
        return targetSquares
    }

    fun generateOrthogonalSquares(mode: String, quantity: String) : List<Array<Int>>{
        val targetSquares = mutableListOf<Array<Int>>()
        var quantityInt = 7
        if(quantity.matches("[1-9]+".toRegex()))quantityInt = quantity.toInt()
        //forward
        for(i in position[0]..7){
            if(Math.abs(position[0]-i) <= quantityInt)targetSquares.add(arrayOf(i,position[1]))
            else break
        }
        //backward
        for(i in position[0] downTo 0){
            if(Math.abs(position[0]-i) <= quantityInt)targetSquares.add(arrayOf(i,position[1]))
            else break
        }
        //right
        for(i in position[1]..7){
            if(Math.abs(position[1]-i) <= quantityInt)targetSquares.add(arrayOf(position[0],i))
            else break
        }
        //left
        for(i in position[1] downTo 0){
            if(Math.abs(position[1]-i) <= quantityInt)targetSquares.add(arrayOf(position[0],i))
            else break
        }
        return targetSquares
    }
}