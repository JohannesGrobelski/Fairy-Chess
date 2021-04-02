package emerald.apps.fairychess.model

import emerald.apps.fairychess.model.pieces.Chessboard


class MultiplayerDB{

    private var opponentMover : OpponentMover
    private var color: String

    constructor(color: String, opponentMover: OpponentMover) {
        this.opponentMover = opponentMover
        this.color = color
    }

    fun movePlayer(movement: ChessPiece.Movement){
        //TODO
    }

    fun Chessboard.move(movementString : ChessPiece.Movement){

    }





}


public interface OpponentMover{
    public fun onOpponentMove(movement: ChessPiece.Movement)
}