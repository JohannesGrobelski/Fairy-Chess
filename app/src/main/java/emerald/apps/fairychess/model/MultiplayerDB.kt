package emerald.apps.fairychess.model

import android.util.Log
import com.google.firebase.firestore.FieldValue
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

    private var multiplayerDBSearchInterface : MultiplayerDBSearchInterface? = null
    private var multiplayerDBGameInterface : MultiplayerDBGameInterface? = null
    private var db : FirebaseFirestore
    private lateinit var chessGame : Chessgame

    var gameLaunched = false

    constructor(multiplayerDBGameInterface: MultiplayerDBGameInterface, chessgame: Chessgame) {
        this.chessGame = chessgame
        // Access a Cloud Firestore instance from your Activity
        db = Firebase.firestore
        this.multiplayerDBGameInterface = multiplayerDBGameInterface
    }

    constructor(multiplayerDBSearchInterface: MultiplayerDBSearchInterface) {
        db = Firebase.firestore
        this.multiplayerDBSearchInterface = multiplayerDBSearchInterface
    }

    fun writePlayerMovement(gameId: String, movement: ChessPiece.Movement){
        var gameRef = db.collection(GAMECOLLECTIONPATH).document(gameId)
        // Atomically add a new region to the "regions" array field.
        gameRef.update("moves", FieldValue.arrayUnion(ChessPiece.Movement.fromMovementToString(movement)));
    }

    fun searchForOpenGames(gameName: String, timeMode: String) {
        val resultList = mutableListOf<String>()
        db.collection(GAMECOLLECTIONPATH)
            .whereEqualTo("finished",false)
            .whereEqualTo("name",gameName)
            .whereNotEqualTo("player1ID","")
            .whereEqualTo("player2ID","")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        Log.d(TAG, document.id + " => " + document.data)
                        resultList.add(document.id)
                    }
                    multiplayerDBSearchInterface?.onGameSearchComplete(resultList)
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
            "name" to gameName,
            "finished" to false,
            "player1ID" to player1Name,
            "player2ID" to "",
            "moves" to listOf<String>()
        )
        var document_id = ""

        // Add a new document with a generated ID
        db.collection(GAMECOLLECTIONPATH)
            .add(gameHash)
            .addOnSuccessListener { documentReference ->
                run {
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    document_id = documentReference.id
                    multiplayerDBSearchInterface?.onCreateGame(gameName,document_id, "white")
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
                    multiplayerDBSearchInterface?.onPlayerNameSearchComplete(userName,resultList.size)
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }

    }

    fun joinGame(gameID: String, userName: String) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameID)
            .update(
                mapOf(
                    "player2ID" to userName
                )
            )
            .addOnSuccessListener {
                getGameDataAndJoinGame(gameID,userName)
            }
    }


    class GameData(val gameId: String, val playerID: String, val opponentID: String)
    fun getGameDataAndJoinGame(gameId: String,userName: String){
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .get()
            .addOnSuccessListener { docRef ->
                run {
                    val player1ID = docRef.getString("player1ID")!!
                    val player2ID = docRef.getString("player2ID")!!
                    var opponentID = player1ID
                    if(opponentID == userName)opponentID = player2ID
                    multiplayerDBSearchInterface?.onJoinGame(GameData(gameId,userName,opponentID))
                }
            }
            .addOnFailureListener { e ->
                run {
                    Log.w(TAG, "Error adding document", e)
                }
            }
    }


    fun createPlayer(playerID: String) {
        val playerHash = hashMapOf(
            "name" to playerID,
            "wins" to 0,
            "losses" to 0,
            "played_games" to 0
        )
        // Add a new document with a generated ID
        db.collection(PLAYERCOLLECTIONPATH)
            .add(playerHash)
            .addOnSuccessListener { documentReference ->
                run {
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    multiplayerDBSearchInterface?.onCreatePlayer(playerID)
                }
            }
            .addOnFailureListener { e ->
                run {
                    Log.w(TAG, "Error adding document", e)
                }
            }
    }

    class GameState(val gameFinished : Boolean, val moves : List<String>)

    fun listenToGameSearch(gameId: String) {
        val docRef = db.collection(GAMECOLLECTIONPATH).document(gameId)
        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "Current data: ${snapshot.data}")
                if(!gameLaunched){
                    multiplayerDBSearchInterface?.onGameChanged(snapshot.id)
                }
            } else {
                Log.d(TAG, "Current data: null")
            }
        }
    }

    fun listenToGameIngame(gameId: String) {
        val docRef = db.collection(GAMECOLLECTIONPATH).document(gameId)
        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "Current data: ${snapshot.data}")
                readGameState(gameId)
            } else {
                Log.d(TAG, "Current data: null")
            }
        }
    }

    fun hasSecondPlayerJoined(gameId: String) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if(task.result!!.exists()){
                        val document = task.result!!
                        if(document.exists()){
                            val player1ID : String = document.getString("player1ID")!!
                            val player2ID : String = document.getString("player2ID")!!
                            if(player1ID.isNotEmpty() && player2ID.isNotEmpty()){
                                getGameDataAndJoinGame(gameId,player1ID)
                            }
                        }
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }
    }

    fun finishGame(gameId: String, cause: String) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .update(
                mapOf(
                    "finished" to true
                )
            )
            .addOnSuccessListener { multiplayerDBGameInterface?.onFinishGame(gameId,cause)}
    }

    fun readGameState(gameId: String) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if(task.result!!.exists()){
                        val document = task.result!!
                        val finished : Boolean = document.getBoolean("finished")!!
                        val moves : List<String> = document.get("moves") as List<String>
                        val gameState = GameState(finished,moves)
                        multiplayerDBGameInterface?.onGameChanged(gameId,gameState)
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }
    }
}

public interface MultiplayerDBSearchInterface {
    fun onCreatePlayer(playerID: String)
    fun onPlayerNameSearchComplete(playerIDr: String, occurences: Int)
    fun onGameSearchComplete(gameIDList: List<String>)
    fun onJoinGame(gameData: MultiplayerDB.GameData)
    fun onCreateGame(gameName: String, gameID: String, player1Color : String)
    fun onGameChanged(gameId : String)
}

public interface MultiplayerDBGameInterface {
    fun onFinishGame(gameId: String, cause : String)
    fun onGameChanged(gameId: String, gameState: MultiplayerDB.GameState)
}

