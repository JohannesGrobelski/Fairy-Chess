package emerald.apps.fairychess.model

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 *  usecase: create player
    - check for playername duplicate
    - create unique player

    usercase: search for gameMode
    - search for gameMode
    - if found -> join the gameMode
    - unless
    -> create gameMode
    -> search
    -> set listener: (if 2 players have joined) -> join gameMode

    usecase: play gameMode
    - both joined -> start the gameMode
    - listener for move
    - write in move
    - if one leaves -> finnished true
 */
class MultiplayerDB {
    companion object {
        const val TAG = "MultiplayerDB"
        const val GAMECOLLECTIONPATH = "test_games"
        const val PLAYERCOLLECTIONPATH = "test_players"
    }

    private lateinit var multiplayerDBSearchInterface : MultiplayerDBSearchInterface
    private lateinit var multiplayerDBGameInterface : MultiplayerDBGameInterface
    private lateinit var opponentMover : OpponentMover
    private var db : FirebaseFirestore
    private lateinit var chessGame : Chessgame

    constructor(multiplayerDBGameInterface: MultiplayerDBGameInterface, chessgame: Chessgame, opponentMover: OpponentMover) {
        this.opponentMover = opponentMover
        this.chessGame = chessgame
        // Access a Cloud Firestore instance from your Activity
        db = Firebase.firestore
        this.multiplayerDBGameInterface = multiplayerDBGameInterface
    }

    constructor(multiplayerDBSearchInterface: MultiplayerDBSearchInterface) {
        db = Firebase.firestore
        this.multiplayerDBSearchInterface = multiplayerDBSearchInterface
    }

    fun writePlayerMovement(movement: ChessPiece.Movement){
        val movementString = movement.toString()
    }

    fun searchGames(gameName: String,timeMode: String) {
        val resultList = mutableListOf<String>()
        db.collection(GAMECOLLECTIONPATH)
            .whereEqualTo("finished",false)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        Log.d(TAG, document.id + " => " + document.data)
                        resultList.add(document.id)
                    }
                    multiplayerDBSearchInterface.onGameSearchComplete(resultList)
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
     * create gameMode and
     * @return document_id of the gameMode
     */
    fun createGame(gameName: String, player1Name: String) : String {
        // Create a new gameMode hashmap
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
                    multiplayerDBSearchInterface.onCreateGame(gameName,document_id, "white")
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
                    multiplayerDBSearchInterface.onPlayerNameSearchComplete(userName,resultList.size)
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }

    }


    fun moveOpponent(movementString: String){
        opponentMover.onOpponentMove(ChessPiece.Movement.fromStringToMovement(movementString))
    }

    fun joinGame(gameID: String, userName: String) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameID)
            .update(
                mapOf(
                    "player2ID" to userName
                )
            )
            .addOnSuccessListener { multiplayerDBSearchInterface.onJoinGame(gameID)}
    }

    fun createPlayer(playerName: String) {
        // Create a new gameMode hashmap
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
                    multiplayerDBSearchInterface.onCreatePlayer(playerName)
                }
            }
            .addOnFailureListener { e ->
                run {
                    Log.w(TAG, "Error adding document", e)
                    document_id =  ""
                }
            }
    }

    fun listenToGame(gameId: String) {
        val docRef = db.collection(GAMECOLLECTIONPATH).document(gameId)
        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "Current data: ${snapshot.data}")
                multiplayerDBSearchInterface.onGameChanged(snapshot.id)
            } else {
                Log.d(TAG, "Current data: null")
            }
        }
    }

    fun hasSecondPlayerJoined(gameId: String) {
        // Add a new document with a generated ID
        db.collection(GAMECOLLECTIONPATH)
            .whereNotEqualTo("player2ID","")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if(!task.result!!.isEmpty){
                        multiplayerDBSearchInterface.onSecondPlayerJoined(gameId)
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }
    }


}

public interface MultiplayerDBSearchInterface {
    fun onCreatePlayer(playerName: String)
    fun onPlayerNameSearchComplete(playerNamer: String, occurences: Int)
    fun onGameSearchComplete(gameIDList: List<String>)
    fun onJoinGame(gameID: String)
    fun onCreateGame(gameName: String, gameID: String, player1Color : String)
    fun onGameChanged(gameId : String)
    fun onSecondPlayerJoined(gameId : String)
}

public interface MultiplayerDBGameInterface {

}

public interface OpponentMover{
    public fun onOpponentMove(movement: ChessPiece.Movement)
}