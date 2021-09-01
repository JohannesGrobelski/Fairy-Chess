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

    var pieceMap = mutableMapOf<String,ULong>() //key: "PiecenameRankFile"
    var sideToMoveIsBlack = 0uL
    var castlingRightMap = mutableMapOf<MovementNotation,ULong>()
    var enpassanteSquareMap = mutableMapOf<Bitboard.Companion.Coordinate,ULong>()

    private var random : Random

    init {
        random = generateRandom(figureNameList)
        for(name in figureNameList){
            for(rank in 0..8){
                for(file in 0..8){
                    pieceMap[generatePieceMapKey(name,rank,file)] =
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

    private fun generatePieceMapKey(figureName:String, rank:Int, file:Int) : String{
        return figureName+rank.toString()+file.toString()
    }

    fun generateRandom(figureNameList: List<String>) : Random {
        var randomSeed = 0
        for(name in figureNameList){
            randomSeed = randomSeed xor name.hashCode()
        }
        return Random(randomSeed)
    }

    fun getPseudoRandomNumber() : ULong {
        return random.nextULong()
    }

    /** transform a board position of arbitrary size
     * into a number of a set length */
    fun generateHash(bitboard: Bitboard) : ULong {
        var hashKey = 0uL
        for(rank in 0..7){
            for(file in 0..7){
                val coordinate = Bitboard.Companion.Coordinate(rank,file)
                val pieceName = bitboard.getPieceName(coordinate)
                if(pieceName.isEmpty())continue
                if(pieceMap.containsKey(generatePieceMapKey(pieceName,rank,file))){
                    hashKey = hashKey xor pieceMap[generatePieceMapKey(pieceName,rank,file)]!!
                }
            }
        }
        if(bitboard.moveColor == "black")hashKey = hashKey xor sideToMoveIsBlack
        for(castlingCoordinate in bitboard.getCastlingRights(bitboard.moveColor)){
            hashKey = hashKey xor castlingRightMap[castlingCoordinate]!!
        }
        for(enpassanteSquare in bitboard.getEnpassanteSquares()){
            hashKey = hashKey xor enpassanteSquareMap[enpassanteSquare]!!
        }
        return hashKey
    }



}
