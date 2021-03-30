package emerald.apps.fairychess.model.pieces

/**
 *  leaper is a piece that moves directly to a square a fixed distance away.
 *  A leaper captures by occupying the square on which an enemy piece sits.
 *  The leaper's move cannot be blocked – it "leaps" over any intervening pieces
 *  – so the check of a leaper cannot be parried by interposing.
 *  Leapers are not able to create pins, but are effective forking pieces.
 *  source : wikipedia
 */
class Leaper(name: String, position: Array<Int>, value: Int, color: String,
             movingPattern: String
) : ChessPiece(name, position,
    value,
    color, movingPattern
) {

    private lateinit var difs : List<Array<Int>>

    override fun move(rank: Int, file: Int): Boolean {
        TODO("Not yet implemented")
    }

    fun getDifs():List<Array<Int>>{
        return listOf()
    }

    override fun getDestinations(): List<Array<Int>> {
        val destinations = mutableListOf<Array<Int>>()
        for(dif in difs){
            destinations.add(arrayOf(position[0]+dif[0],position[1]+dif[1]))
        }
        return destinations
    }

}