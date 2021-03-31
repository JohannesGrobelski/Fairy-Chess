package emerald.apps.fairychess.model.pieces

import java.text.ParsePosition

open class ChessPiece (
    var name : String,
    var position : Array<Int>,
    var value : Int,
    var color : String,
    var movingPattern : String) {

    open fun move(rank : Int, file : Int) : Boolean {return true}
    open fun getTargetSquares() : List<Array<Int>> {return listOf()}
}