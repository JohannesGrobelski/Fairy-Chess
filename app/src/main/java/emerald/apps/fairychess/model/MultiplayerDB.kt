package emerald.apps.fairychess.model

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MultiplayerDB {
    companion object {
        const val TAG = "MultiplayerDB"
        const val GAMECOLLECTIONPATH = "test_games"
        const val PLAYERCOLLECTIONPATH = "test_players"
    }

    private lateinit var multiplayerDBInterface : MultiplayerDBSearchInterface
    private lateinit var opponentMover : OpponentMover
    private var db : FirebaseFirestore
    private lateinit var chessGame : Chessgame

    constructor(chessgame: Chessgame, opponentMover: OpponentMover) {
        this.opponentMover = opponentMover
        this.chessGame = chessgame
        // Access a Cloud Firestore instance from your Activity
        db = Firebase.firestore
        this.multiplayerDBInterface = multiplayerDBInterface
    }

    constructor(multiplayerDBInterface: MultiplayerDBSearchInterface) {
        db = Firebase.firestore
        this.multiplayerDBInterface = multiplayerDBInterface
    }

    fun writePlayerMovement(movement: ChessPiece.Movement){
        val movementString = movement.toString()
    }

    fun searchGames(gameName: String,timeMode: String) {
        val resultList = mutableListOf<String>()
        db.collection(GAMECOLLECTIONPATH)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        Log.d(TAG, document.id + " => " + document.data)
                        resultList.add(document.id)
                    }
                    multiplayerDBInterface.onGameSearchComplete(gameName,timeMode,resultList)
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }
    }

    fun checkGameForSecondPlayer(gameID: String): Boolean{
        var isNotEmpty = false

        db.collection(GAMECOLLECTIONPATH)
            .document("$GAMECOLLECTIONPATH/$gameID")
            .get()
            .addOnSuccessListener { documents ->
                run {
                    isNotEmpty = true
                }
            }
            .addOnFailureListener{
                Log.w(TAG, "Error adding document", it.cause)
                println("Error adding document" + it.cause)
            }
        return isNotEmpty
    }

    /**
     * create game and
     * @return document_id of the game
     */
    fun createGame(gameName: String, player1Name: String) : String {
        // Create a new game hashmap
        val gameHash = hashMapOf(
            "gameName" to gameName,
            "finished" to false,
            "player1ID" to player1Name,
            "player2ID" to "",
            "movements" to listOf<String>()
        )
        var document_id = ""

        // Add a new document with a generated ID
        db.collection(GAMECOLLECTIONPATH)
            .add(gameHash)
            .addOnSuccessListener { documentReference ->
                run {
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    document_id = documentReference.id
                }
            }
            .addOnFailureListener { e ->
                run {
                    Log.w(TAG, "Error adding document", e)
                    document_id =  ""
                }

            }
        return document_id
    }

    fun updateGame(game: Chessgame){
        // Create a new game hashmap
        val gameHash = hashMapOf(
            "gameName" to game.gameName,
            "finished" to game.player1Name,
            "player1ID" to game.player1Name,
            "player2ID" to game.player2Name,
            "movements" to game.moves
        )
    }

    fun searchUsers(userName: String) {
        val resultList = mutableListOf<String>()
        db.collection(PLAYERCOLLECTIONPATH)
            .whereEqualTo("name",userName)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        Log.d(TAG, document.id + " => " + document.data)
                        resultList.add(document.id)
                    }
                    multiplayerDBInterface.onPlayerNameSearchComplete(userName,resultList.size)
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }

    }


    fun moveOpponent(movementString: String){
        opponentMover.onOpponentMove(ChessPiece.Movement.fromStringToMovement(movementString))
    }

    fun joinGame(gameID: String, timeMode: String, gameName: String, userName: String) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameID)
            .update(
                mapOf(
                    "player2ID" to userName
                )
            )
            .addOnSuccessListener { multiplayerDBInterface.onJoinGame(gameID,timeMode,gameName)}
    }

    fun createPlayer(playerName: String) {
        // Create a new game hashmap
        val playerHash = hashMapOf(
            "name" to playerName
        )
        var document_id = ""

        // Add a new document with a generated ID
        db.collection(PLAYERCOLLECTIONPATH)
            .add(playerHash)
            .addOnSuccessListener { documentReference ->
                run {
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    document_id = documentReference.id
                    multiplayerDBInterface.onCreatePlayer(playerName)
                }
            }
            .addOnFailureListener { e ->
                run {
                    Log.w(TAG, "Error adding document", e)
                    document_id =  ""
                }
            }
    }


}

public interface MultiplayerDBSearchInterface {
    fun onCreatePlayer(playerName: String)
    fun onPlayerNameSearchComplete(playerNamer: String, occurences: Int)
    fun onGameSearchComplete(gameName: String, timeMode: String, gameIDList: List<String>)
    fun onJoinGame(gameID: String, timeMode: String, gameName: String)
    fun onCreateGame(gameName: String, gameID: String)
}


public interface OpponentMover{
    public fun onOpponentMove(movement: ChessPiece.Movement)
}