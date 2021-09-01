package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.Bitboard.Companion.castlingMoves
import emerald.apps.fairychess.model.Bitboard.Companion.enpassanteSquares
import kotlin.random.Random
import kotlin.random.nextULong

/** *
 * Zobrist Hashing, is a technique to transform a board position of arbitrary size
 * into a number of a set length, with an equal distribution over all possible numbers ...
 *
 *
 * source: chessprogramming.org
 */
@ExperimentalUnsignedTypes
class ZobristHash(figureNameList : List<String>) {
    class PieceCoordinate(val figureName : String, val coordinate: Bitboard.Companion.Coordinate)

    var pieceMap = mutableMapOf<PieceCoordinate,ULong>()
    var sideToMoveIsBlack = 0uL
    var castlingRightMap = mutableMapOf<MovementNotation,ULong>()
    var enpassanteSquareMap = mutableMapOf<Bitboard.Companion.Coordinate,ULong>()

    private var randomSeed = 0

    init {
        randomSeed = generateRandomSeed(figureNameList)
        for(name in figureNameList){
            for(rank in 0..8){
                for(file in 0..8){
                    pieceMap[PieceCoordinate(name,Bitboard.Companion.Coordinate(rank,file))] =
                        getPseudoRandomNumber()
                }
            }
        }
        sideToMoveIsBlack = getPseudoRandomNumber()
        for(castlingMove in castlingMoves){
            castlingRightMap[castlingMove] = getPseudoRandomNumber()
        }
        for(enpassanteSquare in enpassanteSquares){
            enpassanteSquareMap[enpassanteSquare] = getPseudoRandomNumber()
        }
    }

    fun generateRandomSeed(figureNameList: List<String>) : Int {
        var randomSeed = 0
        for(name in figureNameList){
            randomSeed = randomSeed xor name.hashCode()
        }
        return randomSeed
    }

    fun getPseudoRandomNumber() : ULong {
        return Random(randomSeed).nextULong()
    }

    /** transform a board position of arbitrary size
     * into a number of a set length */
    fun generateHash(bitboard: Bitboard) : ULong {
        var hashKey = 0uL
        for(rank in 0..7){
            for(file in 0..7){
                val coordinate = Bitboard.Companion.Coordinate(rank,file)
                val pieceName = bitboard.getPieceName(coordinate)
                val pieceCoordinate = pieceMap[PieceCoordinate(pieceName,coordinate)]!!
                hashKey = hashKey xor pieceCoordinate
            }
        }
        if(bitboard.moveColor == "black")hashKey = hashKey xor sideToMoveIsBlack
        for(castlingCoordinate in bitboard.getCastlingRights()){
            hashKey = hashKey xor castlingRightMap[castlingCoordinate]!!
        }
        for(enpassanteSquare in bitboard.getEnpassanteSquares()){
            hashKey = hashKey xor enpassanteSquareMap[enpassanteSquare]!!
        }
        return hashKey
    }



}
