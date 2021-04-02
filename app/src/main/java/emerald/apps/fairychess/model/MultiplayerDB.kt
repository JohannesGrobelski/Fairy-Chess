package emerald.apps.fairychess.model

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import emerald.apps.fairychess.view.ChessActivity


class MultiplayerDB{
    companion object {
        const val TAG = "MultiplayerDB"
    }

    private var opponentMover : OpponentMover
    private var db : FirebaseFirestore
    private var chessGame : Chessgame

    constructor(activity: ChessActivity, chessgame: Chessgame, opponentMover: OpponentMover) {
        this.opponentMover = opponentMover
        this.chessGame = chessgame
        // Access a Cloud Firestore instance from your Activity
        db = Firebase.firestore
    }

    fun writePlayerMovement(movement: ChessPiece.Movement){
        val movementString = movement.toString()
    }

    fun searchGame(): Task<QuerySnapshot> {
        db.collection("test_games")
            .whereEqualTo("player2ID", "")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    fun createGame(game: Chessgame) : String {
        // Create a new game hashmap
        val gameHash = hashMapOf(
            "mode" to game.gameName,
            "finished" to game.player1Name,
            "player1ID" to game.player1Name,
            "player2ID" to game.player2Name,
            "movements" to game.moves
        )

        // Add a new document with a generated ID
        db.collection("games")
            .add(gameHash)
            .addOnSuccessListener { documentReference ->
                run {
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    return documentReference.id
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
                return@MultiplayerDB ""
            }
    }

    fun updateGame(game: Chessgame){
        // Create a new game hashmap
        val gameHash = hashMapOf(
            "mode" to game.gameName,
            "finished" to game.player1Name,
            "player1ID" to game.player1Name,
            "player2ID" to game.player2Name,
            "movements" to game.moves
        )

        // Add a new document with a generated ID
        db.collection("test_games").document("frank")
            .update(mapOf(
                "age" to 13,
                "favorites.color" to "Red"
            ))
    }


    fun moveOpponent(movementString : String){
        opponentMover.onOpponentMove(ChessPiece.Movement.fromStringToMovement(movementString))
    }




}


public interface OpponentMover{
    public fun onOpponentMove(movement: ChessPiece.Movement)
}