package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.Bitboard.Companion.castlingMoves
import emerald.apps.fairychess.model.Bitboard.Companion.enpassanteSquares
import emerald.apps.fairychess.model.Bitboard.Companion.generate64BPositionFromCoordinate
import emerald.apps.fairychess.model.Bitboard.Companion.getPosition
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
    var enpassanteSquareMap = mutableMapOf<Int,ULong>()

    private var initHash = 0uL
    private var random : Random
    private var lastHash = 0uL

    //DEBUG
    //var zobristHashMap = mutableMapOf<ULong,Bitboard>()

    companion object {
        const val DEBUG = true
    }

    init {
        random = generateRandom(figureNameList)
        initHash = getPseudoRandomNumber()
        for(color in arrayOf("white","black")) {
            for(name in figureNameList){
                for(rank in 0..7){
                    for(file in 0..7){
                        pieceMap[generateMapKey(color,name,rank,file)] =
                            getPseudoRandomNumber()
                    }
                }
            }
        }
        sideToMoveIsBlack = getPseudoRandomNumber()
        for(castlingMove in castlingMoves){
            castlingRightMap[castlingMove] = getPseudoRandomNumber()
        }
        for(enpassanteSquare in enpassanteSquares){
            enpassanteSquareMap[enpassanteSquare.hashCode()] = getPseudoRandomNumber()
        }
    }

    private fun generateMapKey(figureColor: String, figureName:String, rank:Int, file:Int) : String{
        return figureColor+figureName+rank.toString()+file.toString()
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
        var hashKey = initHash
        //xor all pieces
        for(rank in 0..7) {
            for (file in 0..7) {
                val coordinate = Bitboard.Companion.Coordinate(rank,file)
                val pieceName = bitboard.getPieceName(coordinate)
                if(pieceName.isEmpty())continue
                val pieceColor = bitboard.getPieceColor(rank, file)
                if(!pieceMap.containsKey(generateMapKey(pieceColor,pieceName,rank,file))){
                    generateMapKey(pieceColor,pieceName,rank,file)
                }
                hashKey = hashKey xor pieceMap[generateMapKey(pieceColor,pieceName,rank,file)]!!
            }
        }
        //xor castling moves
        val castlingMoves = bitboard.getCastlingRights(bitboard.moveColor)
        for(castlingMove in castlingMoves){
            hashKey = hashKey xor castlingRightMap[castlingMove]!!
        }
        //xor enpassante
        if(bitboard.enpassanteSquare != null && enpassanteSquareMap.containsKey(bitboard.enpassanteSquare.hashCode())){
            hashKey = hashKey xor enpassanteSquareMap[bitboard.enpassanteSquare.hashCode()]!!
        }
        //xor movecolor
        if(bitboard.moveColor == "black")hashKey = hashKey xor sideToMoveIsBlack
        lastHash = hashKey
        return lastHash
    }

    fun updateHash(movement: Movement) : ULong {
        return lastHash
    }
}
