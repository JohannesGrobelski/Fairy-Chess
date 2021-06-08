package emerald.apps.fairychess.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.ktx.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import emerald.apps.fairychess.controller.MainActivityListener
import emerald.apps.fairychess.model.Chessboard.Companion.oppositeColor
import emerald.apps.fairychess.model.Chessboard.Companion.randomColor
import java.io.Serializable

import com.google.firebase.dynamiclinks.ktx.component1
import com.google.firebase.dynamiclinks.ktx.component2

/** MultiplayerDB adds/changes FirebaseFirestore-Database like
 *
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
        const val matchmakingWinningChanceOffset = 0.3 //0.0 ... 0.5
        const val TAG = "MultiplayerDB"
        
        //collection paths
        const val GAMECOLLECTIONPATH = "test_games"
        const val PLAYERCOLLECTIONPATH = "test_players"

        //fields in player (in player collection)
        const val PLAYER_ID = "name"
        const val PLAYER_GAMES_PLAYED = "played_games"
        const val PLAYER_GAMES_WON = "wins"
        const val PLAYER_GAMES_LOST = "losses"
        const val PLAYER_ELO = "elo"

        //fields in game (in game collection)
        const val GAMEFIELD_FINISHED = "finished"
        const val GAMEFIELD_GAMENAME = "gameName"
        const val GAMEFIELD_TIMEMODE = "timeMode"
        const val GAMEFIELD_MOVES = "moves"
        const val GAMEFIELD_PLAYER1ID = "player1ID"
        const val GAMEFIELD_PLAYER1Color = "player1Color"
        const val GAMEFIELD_PLAYER1ELO = "player1ELO"
        const val GAMEFIELD_PLAYER2ID = "player2ID"
        const val GAMEFIELD_PLAYER2Color = "player2Color"
        const val GAMEFIELD_PLAYER2ELO = "player2ELO"
    }

    private var multiplayerDBSearchInterface : MultiplayerDBSearchInterface? = null //interface implemented by mainactivitylistener
    private var multiplayerDBGameInterface : MultiplayerDBGameInterface? = null //interface implemented by chessactivitylistener
    private var db : FirebaseFirestore
    private lateinit var chessGame : Chessgame

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

    constructor(multiplayerDBSearchInterface: MultiplayerDBSearchInterface,multiplayerDBGameInterface: MultiplayerDBGameInterface) {
        db = Firebase.firestore
        this.multiplayerDBSearchInterface = multiplayerDBSearchInterface
        this.multiplayerDBGameInterface = multiplayerDBGameInterface
    }

    fun createDynamicLink(gameId: String) {
        Firebase.dynamicLinks.shortLinkAsync {
            link = Uri.parse("https://www.example.com?game_id=$gameId")
            domainUriPrefix = "https://example.page.link"
            // Open links with this app on Android
            androidParameters {
                DynamicLink.AndroidParameters.Builder("com.example.android")
                    .setMinimumVersion(1)
                    .build()
            }
            socialMetaTagParameters {
                DynamicLink.SocialMetaTagParameters.Builder()
                    .setTitle("Example of a Dynamic Link")
                    .setDescription("This link works whether the app is installed or not!")
                    //.setImageUrl("https://www.example.com/icon.png") TODO create image url
                    .build()
            }
            googleAnalyticsParameters {
                DynamicLink.GoogleAnalyticsParameters.Builder()
                    .setSource("orkut")
                    .setMedium("social")
                    .setCampaign("example-promo")
                    .build()
            }
        }
        .addOnSuccessListener { (shortLink, flowchartLink) ->
            // Short link created
            Log.e("MultiplayerDB","dynamic link was created!")
            multiplayerDBSearchInterface?.processShortLink(shortLink, flowchartLink)
        }.addOnFailureListener {
            error:
                "com.google.android.gms.common.api.ApiException: 400: Your project does not own Dynamic Links domain"
            update google-services.json  (firebase)
            Log.e("MultiplayerDB","dynamic link could not be created: "+it.cause)
        }

    }

    fun writePlayerMovement(gameId: String, movement: ChessPiece.Movement){
        var gameRef = db.collection(GAMECOLLECTIONPATH).document(gameId)
        gameRef.update("moves", FieldValue.arrayUnion(ChessPiece.Movement.fromMovementToString(movement)));
    }

    fun writePlayerMovement(gameId: String, promotionMovement: ChessPiece.PromotionMovement){
        var gameRef = db.collection(GAMECOLLECTIONPATH).document(gameId)
        gameRef.update("moves", FieldValue.arrayUnion(ChessPiece.Movement.fromMovementToString(promotionMovement)));
    }

    /**
     * Search for matching open games = documents with matching parameters (gameName and timeMode),
     * field finished = false and player1ID not matching player2ID. When search was successful call callback function
     * (onGameSearchComplete in MainActivityListener)
     */
    fun searchForOpenGames(gameName: String = "", timeMode: String = "", player2ID: String) {
        val resultList = mutableListOf<MainActivityListener.GameSearchResult>()
        Log.d(TAG, "search for games $gameName, $timeMode, $player2ID")
        val collection = db.collection(GAMECOLLECTIONPATH)
            .whereEqualTo(GAMEFIELD_FINISHED,false)
            .whereEqualTo(GAMEFIELD_PLAYER2ID,"")
            .whereNotEqualTo(GAMEFIELD_PLAYER1ID,player2ID)

        if(gameName.isNotEmpty() && !gameName.startsWith("all"))
            collection.whereIn(GAMEFIELD_GAMENAME, listOf("all game modes",gameName))
        if(gameName.isNotEmpty() && !timeMode.startsWith("all"))
            collection.whereEqualTo(GAMEFIELD_TIMEMODE, timeMode)

        collection
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        Log.d(TAG, document.id + " => " + document.data)
                        resultList.add(
                            MainActivityListener.GameSearchResult(
                                document.id,
                                document.get(GAMEFIELD_GAMENAME) as String,
                                document.get(GAMEFIELD_TIMEMODE) as String,
                                document.get(GAMEFIELD_PLAYER1ELO) as Double,
                                document.get(GAMEFIELD_PLAYER2Color) as String
                            )
                        )
                    }
                    multiplayerDBSearchInterface?.onGameSearchComplete(resultList)
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }
    }

    /**
     * create open game = create document with matching gameName and timeMode and player1ID set.
     * when finished call the callback function (onCreateGame in MainActivityListener)
     * @return document_id of the gameMode
     */
    fun createGame(gameName: String, timeMode: String, player1ID: String, player1ELO: Double) {
        // Create a new gameMode hashmap
        val player1Color = randomColor()
        val player2Color = oppositeColor(player1Color)
        val gameHash = hashMapOf(
            GAMEFIELD_GAMENAME to gameName,
            GAMEFIELD_FINISHED to false,
            GAMEFIELD_TIMEMODE to timeMode,
            GAMEFIELD_MOVES to listOf<String>(),
            GAMEFIELD_PLAYER1ID to player1ID,
            GAMEFIELD_PLAYER1Color to player1Color,
            GAMEFIELD_PLAYER1ELO to player1ELO,
            GAMEFIELD_PLAYER2ID to "",
            GAMEFIELD_PLAYER2Color to player2Color
        )
        // Add a new document with a generated ID
        db.collection(GAMECOLLECTIONPATH)
            .add(gameHash)
            .addOnSuccessListener { documentReference ->
                run {
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    multiplayerDBSearchInterface?.onCreateGame(gameName,documentReference.id, player1Color)
                }
            }
            .addOnFailureListener { e ->
                run {
                    Log.w(TAG, "Error adding document", e)
                }
            }
    }

    /** Search for a user with userName and call onPlayerNameSearchComplete on success. */
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

    /** join the game with gameID by updating changeMap and if successful call getGameDataAndJoinGame */
    fun joinGame(gameID: String, changeMap : Map<String,Any>) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameID)
            .update(
                changeMap.toMap(),
            ).addOnSuccessListener {
                getGameDataAndJoinGame(gameID,changeMap[GAMEFIELD_PLAYER2ID]!!.toString())
            }
    }


    class GameData(val gameId: String, val playerID: String, val opponentID: String, var playerELO: Double, var opponentELO :Double)
    /** get GameData for gameID and if successful call onJoinGame */
    fun getGameDataAndJoinGame(gameId: String,userName: String){
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .get()
            .addOnSuccessListener { docRef ->
                run {
                    val player1ID = docRef.getString(GAMEFIELD_PLAYER1ID)!!
                    val player2ID = docRef.getString(GAMEFIELD_PLAYER2ID)!!
                    val player1ELO = docRef.getDouble(GAMEFIELD_PLAYER1ELO)!!
                    val player2ELO = docRef.getDouble(GAMEFIELD_PLAYER2ELO)!!
                    var opponentID = player1ID
                    var playerELO = player2ELO
                    var opponentELO = player1ELO
                    if(opponentID == userName){
                        opponentID = player2ID
                        opponentELO = player2ELO
                    }
                    val gameData = GameData(gameId,userName,opponentID,playerELO,opponentELO)

                    val name = docRef.getString(GAMEFIELD_GAMENAME)!!
                    val playMode = "human"
                    val time = docRef.getString(GAMEFIELD_TIMEMODE)!!
                    var playerColor = docRef.getString(GAMEFIELD_PLAYER1Color)!!
                    if(userName == player2ID)playerColor = docRef.getString(GAMEFIELD_PLAYER2Color)!!
                    val gameParameters = MainActivityListener.GameParameters(name,playMode,time,playerColor)

                    multiplayerDBSearchInterface?.onJoinGame(gameParameters,gameData)
                }
            }
            .addOnFailureListener { e ->
                run {
                    Log.w(TAG, "Error adding document", e)
                }
            }
    }

    /** create player with playerID and default player data, on success call onCreatePlayer */
    fun createPlayer(playerID: String) {
        val playerHash = hashMapOf(
            PLAYER_ELO to 400,
            PLAYER_ID to playerID,
            PLAYER_GAMES_WON to 0,
            PLAYER_GAMES_LOST to 0,
            PLAYER_GAMES_PLAYED to 0
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
    /** add snapshotlistener to listen to changes in a created game without second player
     *  if change occurs call onGameChanged */
    fun listenToGameSearch(gameId: String) {
        val docRef = db.collection(GAMECOLLECTIONPATH).document(gameId)
        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "Current data: ${snapshot.data}")
                multiplayerDBSearchInterface?.onGameChanged(snapshot.id)
            } else {
                Log.d(TAG, "Current data: null")
            }
        }
    }
    /** add snapshotlistener to listen to changes on game (moves and gamestatus)
     *  if change occurs call readGameState */
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

    /** check whether second player has joined in game with gameID
     *  if so call getGameDataAndJoinGame */
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

    /** finish game by updating variable finished to true, on success call onFinishGame */
    fun finishGame(gameId: String, cause: String) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .update(
                mapOf(
                    GAMEFIELD_FINISHED to true
                )
            )
            .addOnSuccessListener { multiplayerDBGameInterface?.onFinishGame(gameId,cause)}
    }

    /** read game state of game with gameID (document,finished,moves) and on sucess call onGameChanged,
     * (because this is only called if snapshot listener detects change in game document)*/
    fun readGameState(gameId: String) {
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if(task.result!!.exists()){
                        val document = task.result!!
                        val finished : Boolean = document.getBoolean(GAMEFIELD_FINISHED)!!
                        val moves : List<String> = document.get("moves") as List<String>
                        val gameState = GameState(finished,moves)
                        multiplayerDBGameInterface?.onGameChanged(gameId,gameState)
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }
    }

    /** parcelable class PlayerStats that save statistics of player*/
    class PlayerStats(var games_played : Long, var games_won : Long, var games_lost : Long, var ELO : Double) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readLong(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readDouble()
        ) {
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeLong(games_played)
            parcel.writeLong(games_won)
            parcel.writeLong(games_lost)
            parcel.writeDouble(ELO)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<PlayerStats> {
            override fun createFromParcel(parcel: Parcel): PlayerStats {
                return PlayerStats(parcel)
            }

            override fun newArray(size: Int): Array<PlayerStats?> {
                return arrayOfNulls(size)
            }
        }
    }

    /** get playerStats for player document with playerID (gamesPlayed,gamesWon,gamesLost and ELO) and on success call onGetPlayerstats*/
    fun getPlayerStats(playerID: String) {
        db.collection(PLAYERCOLLECTIONPATH)
            .whereEqualTo(PLAYER_ID,playerID)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for(document in task.result!!){
                        val gamesPlayed : Long = document.getLong(PLAYER_GAMES_PLAYED)!!
                        val gamesWon : Long = document.getLong(PLAYER_GAMES_WON)!!
                        val gamesLost : Long = document.getLong(PLAYER_GAMES_LOST)!!
                        val elo : Double = document.getDouble(PLAYER_ELO)!!
                        multiplayerDBSearchInterface?.onGetPlayerstats(PlayerStats(gamesPlayed,gamesWon,gamesLost,elo))
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }
    }

    /** set playerStats for player document with playerID (gamesPlayed,gamesWon,gamesLost and ELO) and on success call onSetPlayerstats*/
    fun setPlayerStats(playerID: String,playerStats: PlayerStats, cause: String) {
        //get doc id
        db.collection(PLAYERCOLLECTIONPATH)
            .whereEqualTo(PLAYER_ID,playerID)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for(document in task.result!!){
                        val docID = document.id
                        db.collection(PLAYERCOLLECTIONPATH)
                            .document(docID)
                            .update(
                                mapOf(
                                    PLAYER_GAMES_PLAYED to playerStats.games_played,
                                    PLAYER_GAMES_WON to playerStats.games_won,
                                    PLAYER_GAMES_LOST to playerStats.games_lost,
                                    PLAYER_ELO to playerStats.ELO
                                )
                            )
                            .addOnSuccessListener {
                                multiplayerDBGameInterface?.onSetPlayerstats(cause)
                            }
                            .addOnFailureListener() {
                                Log.e("MultiplayerDB", "could not set player stats: "+it.cause)
                            }
                    }
                } else {
                    Log.w(TAG, "Error getting documents.", task.exception)
                }
            }

    }

    /** update GAMEFIELD_FINISHED to true in game with gameID */
    fun cancelGame(gameId: String) {
        // Add a new document with a generated ID
        db.collection(GAMECOLLECTIONPATH)
            .document(gameId)
            .update(
                mapOf(
                    GAMEFIELD_FINISHED to true
                )
            )
    }


}

interface MultiplayerDBSearchInterface {
    fun onCreatePlayer(playerID: String)
    fun onPlayerNameSearchComplete(playerIDr: String, occurences: Int)
    fun onGameSearchComplete(gameSearchResultList: List<MainActivityListener.GameSearchResult>)
    fun onJoinGame(gameParameters: MainActivityListener.GameParameters,gameData: MultiplayerDB.GameData)
    fun onCreateGame(gameName: String, gameID: String, player1Color : String)
    fun onGameChanged(gameId : String)

    fun onGetPlayerstats(playerStats: MultiplayerDB.PlayerStats)
    fun processShortLink(shortLink: Uri?, flowchartLink: Uri?)
}

interface MultiplayerDBGameInterface {
    fun onFinishGame(gameId: String, cause : String)
    fun onGameChanged(gameId: String, gameState: MultiplayerDB.GameState)
    fun onSetPlayerstats(exitCause : String)
}

