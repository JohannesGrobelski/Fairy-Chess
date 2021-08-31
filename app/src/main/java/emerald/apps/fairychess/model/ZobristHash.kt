package emerald.apps.fairychess.model

import emerald.apps.fairychess.utility.FigureParser

class ZobristHash(figureMap : Map<String, FigureParser.Figure>) {

    init {

    }

    companion object {
        fun zobristHash(bitboard: Bitboard) : Int {
            return 0
        }
    }

}
