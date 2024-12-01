package emerald.apps.fairychess

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import emerald.apps.fairychess.controller.MainActivityListener
import emerald.apps.fairychess.model.board.Movement
import emerald.apps.fairychess.model.multiplayer.MultiplayerDB
import emerald.apps.fairychess.model.multiplayer.MultiplayerDBGameInterface
import emerald.apps.fairychess.model.multiplayer.MultiplayerDBSearchInterface
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MultiplayerDBUnittest : MultiplayerDBSearchInterface, MultiplayerDBGameInterface {

    companion object {
        const val firestoreConnectionTimeoutSeconds = 20L
    }

    private lateinit var db : FirebaseFirestore
    private lateinit var multiplayerDB : MultiplayerDB
    private lateinit var signal : CountDownLatch

    private var playerCreated = false
    private var playerNameSearchOccurences = 0
    private lateinit var gameSearchResultList: List<MainActivityListener.GameSearchResult>
    private lateinit var createdGameID : String
    private lateinit var joinedGameParameters: MainActivityListener.GameParameters
    private lateinit var joinedGameData: MultiplayerDB.GameData
    private lateinit var playerStats : MultiplayerDB.PlayerStats
    private var gameChanged = false
    private var gameFinished = false
    private var secondPlayerJoined = false
    private lateinit var gameWritePlayerMovementChangeGameState : MultiplayerDB.GameState
    private var dynamicLinkCreated = ""

    @Before
    fun before(){
        db = Firebase.firestore
        multiplayerDB = MultiplayerDB(this,this)
    }


    @Test
    fun testCreateGame(){
        gameSearchResultList = listOf()
        val randomGameName = "test_chess_game"+((Math.random()*1000000).toInt())
        //create game
        signal = CountDownLatch(1);
        multiplayerDB.createGame(randomGameName, "Bullet (2 minutes)", "test_player1", 0.0)
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        //verify by searching for it
        signal = CountDownLatch(1);
        multiplayerDB.searchForOpenGames(randomGameName, "Bullet (2 minutes)", "test_player2")
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        var testGameFound = false
        for(game in gameSearchResultList){
            if(game.id == createdGameID)testGameFound = true
        }
        assertTrue(testGameFound)
    }

    @Test
    fun testCreateAndJoinGame(){
        gameChanged = false
        secondPlayerJoined = false
        gameSearchResultList = listOf()
        val randomGameName = "test_chess_game"+((Math.random()*1000000).toInt())
        //create game
        signal = CountDownLatch(1);
        multiplayerDB.createGame(randomGameName, "Bullet (2 minutes)", "test_player1", 0.0)
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        //join game with test_player2
        signal = CountDownLatch(1)
        val changeMap = mutableMapOf(
            MultiplayerDB.GAMEFIELD_PLAYER2ID to "test_player2",
            MultiplayerDB.GAMEFIELD_PLAYER2ELO to 0
        )
        multiplayerDB.joinGame(createdGameID, changeMap)
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        if(signal.count == 0L){
            assertTrue(joinedGameParameters.name == randomGameName)
            assertTrue(joinedGameParameters.time == "Bullet (2 minutes)")
            assertTrue(joinedGameData.playerID == "test_player2")
            assertTrue(joinedGameData.opponentID == "test_player1")
        } else {
            assertTrue("game not joined", false)
        }
        signal = CountDownLatch(1);
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        if(signal.count == 0L) {
            assertTrue(gameChanged)
        }
        if(gameChanged){
            signal = CountDownLatch(1);
            signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
            multiplayerDB.hasSecondPlayerJoined(createdGameID)
        }
    }

    @Test
    fun testCreateAndFinishGame() {
        gameChanged = false
        gameFinished = false
        secondPlayerJoined = false
        gameSearchResultList = listOf()
        val randomGameName = "test_chess_game" + ((Math.random() * 1000000).toInt())
        //create game
        signal = CountDownLatch(1);
        multiplayerDB.createGame(randomGameName, "Bullet (2 minutes)", "test_player1", 0.0)
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        //verify by searching for it
        signal = CountDownLatch(1);
        multiplayerDB.searchForOpenGames(randomGameName, "Bullet (2 minutes)", "test_player2")
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        var testGameFound = false
        for(game in gameSearchResultList){
            if(game.id == createdGameID)testGameFound = true
        }
        assertTrue(testGameFound)
        //finish game
        signal = CountDownLatch(1);
        multiplayerDB.finishGame(createdGameID,"test_cause")
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        assertTrue(gameFinished)
    }

    @Test
    fun testCreateGameAndCreateDynamicLink(){
        gameChanged = false
        dynamicLinkCreated = ""
        secondPlayerJoined = false
        gameSearchResultList = listOf()
        val randomGameName = "test_chess_game" + ((Math.random() * 1000000).toInt())
        //create game
        signal = CountDownLatch(1);
        multiplayerDB.createGame(randomGameName, "Bullet (2 minutes)", "test_player1", 0.0)
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        //verify by searching for it
        signal = CountDownLatch(1);
        multiplayerDB.searchForOpenGames(randomGameName, "Bullet (2 minutes)", "test_player2")
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        var testGameFound = false
        for(game in gameSearchResultList){
            if(game.id == createdGameID)testGameFound = true
        }
        assertTrue(testGameFound)
        //listen for game changes
        signal = CountDownLatch(1);
        multiplayerDB.listenToGameIngame(createdGameID)
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        assertTrue(gameChanged)

        //create dynamic link
        signal = CountDownLatch(1);
        multiplayerDB.shareGameId(createdGameID)
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        assertFalse(dynamicLinkCreated.isNotEmpty())
    }

    @Test
    fun testCreatePlayerAndGetStats(){
        playerCreated = false
        playerNameSearchOccurences = 0
        val randomPlayerName = "test_player_name"+((Math.random()*1000000).toInt())
        //create player
        signal = CountDownLatch(1);
        multiplayerDB.createPlayer(randomPlayerName)
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        assertTrue(playerCreated)
        //verify by searching for it
        signal = CountDownLatch(1);
        multiplayerDB.searchUsers(randomPlayerName)
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        assertTrue(playerNameSearchOccurences > 0)
        //get stats
        signal = CountDownLatch(1);
        multiplayerDB.getPlayerStats(randomPlayerName)
        signal.await(firestoreConnectionTimeoutSeconds, TimeUnit.SECONDS)
        if(signal.count == 0L){
            assertTrue(playerStats.ELO == 400.0)
            assertTrue(playerStats.games_lost == 0L)
            assertTrue(playerStats.games_won == 0L)
            assertTrue(playerStats.games_played == 0L)
        }
    }

    override fun onCreatePlayer(playerID: String) {
        this.playerCreated = true
        signal.countDown();
    }

    override fun onPlayerNameSearchComplete(playerIDr: String, occurences: Int) {
        this.playerNameSearchOccurences = occurences
        signal.countDown();
    }

    override fun onGameSearchComplete(gameSearchResultList: List<MainActivityListener.GameSearchResult>) {
        this.gameSearchResultList = gameSearchResultList
        signal.countDown();
    }

    override fun onJoinGame(
        gameParameters: MainActivityListener.GameParameters,
        gameData: MultiplayerDB.GameData
    ) {
        this.joinedGameData = gameData
        this.joinedGameParameters = gameParameters
        signal.countDown();
    }

    override fun onCreateGame(gameName: String, gameID: String, player1Color: String) {
        this.createdGameID = gameID
        signal.countDown();
    }

    override fun onGameChanged(gameId: String) {
        this.gameChanged = true
        signal.countDown();
    }

    override fun onGetPlayerstats(playerStats: MultiplayerDB.PlayerStats) {
        this.playerStats = playerStats
        signal.countDown();
    }

    override fun processGameInvite(gameId: String) {
        TODO("Not yet implemented")
    }

    override fun onFinishGame(gameId: String, cause: String) {
        this.gameFinished = true
        signal.countDown()
    }

    override fun onGameChanged(gameId: String, gameState: MultiplayerDB.GameState) {
        this.gameWritePlayerMovementChangeGameState = gameState
        this.gameChanged = true
        signal.countDown()
    }

    override fun onSetPlayerstats(exitCause: String) {
        signal.countDown()
    }
}