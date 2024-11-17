package emerald.apps.fairychess.model.bitboard

import kotlin.math.abs
import kotlin.math.sign

class Coordinate(val rank: Int, val file: Int){
    override fun equals(other: Any?): Boolean {
        if(other is Coordinate){
            return (file == other.file) && (rank == other.rank)
        }
        return super.equals(other)
    }
    fun inRange() : Boolean {
        return rank in 0..7 && file in 0..7
    }
    fun getSign() : Int{
        return sign((rank - file).toDouble()).toInt()
    }
    fun getDistance(): Int {
        return abs(rank - file).toInt()
    }
    fun newCoordinateFromFileOffset(fileOffset : Int): Coordinate {
        return Coordinate(rank, file + fileOffset)
    }
    fun newCoordinateFromRankOffset(rankOffset : Int): Coordinate {
        return Coordinate(rank + rankOffset, file)
    }
}