package emerald.apps.fairychess.controller

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.DialogInterface.OnShowListener
import android.content.Intent
import android.net.Uri
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import emerald.apps.fairychess.R
import emerald.apps.fairychess.model.rating.ChessRatingSystem
import emerald.apps.fairychess.model.multiplayer.MultiplayerDB
import emerald.apps.fairychess.model.multiplayer.MultiplayerDB.Companion.matchmakingWinningChanceOffset
import emerald.apps.fairychess.model.multiplayer.MultiplayerDBSearchInterface
import emerald.apps.fairychess.view.ChessActivity
import emerald.apps.fairychess.view.MainActivity
import kotlinx.coroutines.*
import java.util.*


class MainActivityListener() : View.OnClickListener, MultiplayerDBSearchInterface {
    private lateinit var mainActivity : MainActivity
    //private lateinit var multiplayerDB: MultiplayerDB

    lateinit var userNameDialog : AlertDialog
    var joinWaitDialog : AlertDialog? = null
    private var launchedGamesMap = mutableMapOf<String, Boolean>()

    private lateinit var userName:String
    private lateinit var playerStats: MultiplayerDB.PlayerStats
    private lateinit var opponentStats: MultiplayerDB.PlayerStats
    class GameSearchResult(
        val id: String,
        val gameName: String,
        val timeMode: String,
        val player1ELO: Double,
        val player2Color : String
    )
    class GameParameters(
        var name: String,
        var playMode: String,
        var time: String,
        var playerColor: String,
        var difficulty: Int
    )
    private var gameParameters = GameParameters("", "", "", "", 0)
    private var createdGameID = ""

    companion object {
        const val DEBUG = false
        const val TAG = "MainActivityListener"
        const val userNameExtra = "userNameExtra"
        const val gameIdExtra = "gameId"
        const val gamePlayerNameExtra = "playerNameExtra"
        const val gamePlayerStatsExtra = "playerStatsExtra"
        const val gameOpponentNameExtra = "opponentNameExtra"
        const val gameOpponentStatsExtra = "opponentStatsExtra"
        const val gameModeExtra = "gameMode"
        const val gameNameExtra = "name"
        const val gameTimeExtra = "gameTime"
        const val gameDifficultyExtra = "gameDifficultyExtra"
        const val playerColorExtra = "playerColor"

        var clipboardCopyJob : Job? = null

        /**
         * copy string to clipboard
         */
        fun copyTextToClipboard(activity: Activity, text: String) {
            if(text.isNotEmpty()){
                clipboardCopyJob = CoroutineScope(Dispatchers.Main).launch {
                    val clipboard =
                        activity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = ClipData.newPlainText("Copied Text", text)
                    clipboard.setPrimaryClip(clip)
                    withContext(Dispatchers.Main){
                        activity.runOnUiThread{ //bug solution?
                            Toast.makeText(activity,"text copied",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    constructor(mainActivity: MainActivity) : this(){
        this.mainActivity = mainActivity
        loadOrCreateUserName()
        //multiplayerDB = MultiplayerDB(this)
        loadPlayerStats()
    }

    fun loadPlayerStats(){
        playerStats = MultiplayerDB.PlayerStats.getDefault()
        //multiplayerDB.getPlayerStats(userName)
    }

    fun loadPlayerStats(playerStats: MultiplayerDB.PlayerStats){
        this.playerStats = playerStats
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btn_ai -> {
                gameParameters.playMode = "ai"
                displayAlertDialogAIMatch()
            }
            R.id.btn_quickmatch -> {
                gameParameters.playMode = "human"
                quickMatch()
            }
            R.id.btn_searchGame -> {
                gameParameters.playMode = "human"
                displayAlertDialogSearchForGames()
            }
            R.id.btn_createGame -> {
                gameParameters.playMode = "human"
                displayAlertDialogCreateGames()
            }
            R.id.tv_playerstats -> {
                if(this::playerStats.isInitialized) {
                    display_alertDialogPlayerStats()
                } else {
                    Toast.makeText(mainActivity,"loading player stats",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** alert dialog to display playerstats (name,elo,games played,won and lost)*/
    fun display_alertDialogPlayerStats(){
        val inflater = LayoutInflater.from(mainActivity)

        //inflate the layout (depending on mode)
        val playerStatsDialogView = inflater.inflate(
            R.layout.alertdialog_playerstats,
            null,
            false
        )

        playerStatsDialogView.findViewById<TextView>(R.id.aD_playerStats_playerName).text =
            "playername : $userName"
        playerStatsDialogView.findViewById<TextView>(R.id.aD_playerStats_ELO).text =
            "ELO : "+playerStats.ELO
        playerStatsDialogView.findViewById<TextView>(R.id.aD_playerStats_gamesPlayed).text =
            "games played : "+playerStats.games_played
        playerStatsDialogView.findViewById<TextView>(R.id.aD_playerStats_gamesWon).text =
            "games won : "+playerStats.games_won
        playerStatsDialogView.findViewById<TextView>(R.id.aD_playerStats_gamesLost).text =
            "games lost : "+playerStats.games_lost

        val playerStatsDialogBuilder = AlertDialog.Builder(mainActivity).setView(playerStatsDialogView)
        playerStatsDialogBuilder.setPositiveButton("OK",null)
        playerStatsDialogBuilder.show()
    }

    /** alert dialog to search for online games  */
    fun displayAlertDialogAIMatch(){
        val gameModes = mainActivity.resources.getStringArray(R.array.gamemodes)
        val gamemode_descriptions = mainActivity.resources.getStringArray(R.array.gamemode_descriptions)
        val timeModes = mainActivity.resources.getStringArray(R.array.timemodes)
        val difficultyModes = mainActivity.resources.getStringArray(R.array.difficultyModes)

        val inflater = LayoutInflater.from(mainActivity)

        //inflate the layout (depending on mode)
        val createDialogView = inflater.inflate(
            R.layout.alertdialog_create_game,
            null,
            false
        )

        //create dialog
        val spinner_gameName : Spinner = createDialogView.findViewById(R.id.spinner_createGame_gameName)
        spinner_gameName.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_list_item_1,
            gameModes
        )

        // Update TextView description when a game mode is selected
        val tv_gameMode_description : TextView = createDialogView.findViewById(R.id.tv_gameMode_description)
        spinner_gameName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                for(desc in gamemode_descriptions){
                    if(desc.split(":")[0].lowercase(Locale.ROOT) == spinner_gameName.selectedItem.toString().toLowerCase() ){
                        tv_gameMode_description.text = desc
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                tv_gameMode_description.text = "Select a game mode to see its description"
            }
        }

        val spinner_timemode : Spinner = createDialogView.findViewById(R.id.spinner_createGame_timemode)
        spinner_timemode.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_list_item_1,
            timeModes
        )
        val spinner_diff : Spinner = createDialogView.findViewById(R.id.spinner_createGame_difficulty)
        spinner_diff.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_list_item_1,
            difficultyModes
        )
        val btn_create_game = createDialogView.findViewById<Button>(R.id.btn_createGame_create_game)
        val createDialog = AlertDialog.Builder(mainActivity).setView(createDialogView).create()

        btn_create_game.setOnClickListener{
            gameParameters.name = spinner_gameName.selectedItem.toString().toLowerCase()
            gameParameters.time = spinner_timemode.selectedItem.toString()
            gameParameters.playerColor = "white"
            val diffAi = spinner_diff.selectedItem.toString().split(" ")[1].toDouble()
            gameParameters.difficulty = spinner_diff.selectedItem.toString().split(" ")[1].toInt();
            this.opponentStats = MultiplayerDB.PlayerStats(0L,0L,0L,diffAi)
            val gameData = MultiplayerDB.GameData("aigame",userName,"AI",playerStats.ELO,diffAi)
            start_gameWithParameters(gameData,gameParameters)
        }

        createDialog.show()
    }

    /** alert dialog to search for online games  */
    fun displayAlertDialogSearchForGames(){
        val gameModes = mainActivity.resources.getStringArray(R.array.gamemodes)
        val timeModes = mainActivity.resources.getStringArray(R.array.timemodes)
        val inflater = LayoutInflater.from(mainActivity)

        //inflate the layout (depending on mode)
        val searchDialogView = inflater.inflate(
            R.layout.alertdialog_search_game,
            null,
            false
        )

        //create dialog
        val spinner_gameName : Spinner = searchDialogView.findViewById(R.id.spinner_gameName)
        spinner_gameName.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_list_item_1,
            gameModes
        )
        val spinner_timemode : Spinner = searchDialogView.findViewById(R.id.spinner_timemode)
        spinner_timemode.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_list_item_1,
            timeModes
        )
        val btn_search_game = searchDialogView.findViewById<Button>(R.id.btn_search_game)
        val searchDialog = AlertDialog.Builder(mainActivity).setView(searchDialogView).create()

        btn_search_game.setOnClickListener{
            gameParameters.name = spinner_gameName.selectedItem.toString().toLowerCase()
            gameParameters.time = spinner_timemode.selectedItem.toString()
            searchForGames(gameParameters.name,gameParameters.time)
        }
        searchDialog.show()
    }

    /** alert dialog to create online game  */
    fun displayAlertDialogCreateGames(){
        val gameModes = mainActivity.resources.getStringArray(R.array.gamemodes)
        val timeModes = mainActivity.resources.getStringArray(R.array.timemodes)
        val difficultyModes = mainActivity.resources.getStringArray(R.array.difficultyModes)
        val inflater = LayoutInflater.from(mainActivity)

        //inflate the layout (depending on mode)
        val searchDialogView = inflater.inflate(
            R.layout.alertdialog_create_game,
            null,
            false
        )

        //create dialog
        val spinner_gameName : Spinner = searchDialogView.findViewById(R.id.spinner_createGame_gameName)
        spinner_gameName.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_list_item_1,
            gameModes
        )
        val spinner_timemode : Spinner = searchDialogView.findViewById(R.id.spinner_createGame_timemode)
        spinner_timemode.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_list_item_1,
            timeModes
        )
        val spinner_difficultymode : Spinner = searchDialogView.findViewById(R.id.spinner_createGame_difficulty)
        spinner_timemode.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_list_item_1,
            difficultyModes
        )
        val btn_create_game = searchDialogView.findViewById<Button>(R.id.btn_createGame_create_game)
        val searchDialog = AlertDialog.Builder(mainActivity).setView(searchDialogView).create()

        btn_create_game.setOnClickListener{
            gameParameters.name = spinner_gameName.selectedItem.toString().toLowerCase()
            gameParameters.time = spinner_timemode.selectedItem.toString()
            gameParameters.difficulty = spinner_difficultymode.selectedItem.toString().split(" ")[1].toInt()
            //multiplayerDB.createGame(gameParameters.name,gameParameters.time,userName,playerStats.ELO)
        }
        searchDialog.show()
    }


    /** display a simple alert dialog to input a username. */
    fun openCreateUserNameDialog(takenUserName: String){
        val builder = AlertDialog.Builder(mainActivity)
        builder.setTitle("Create User")

        // Set up the input
        val input = EditText(mainActivity)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Type in a unique user name"
        if(takenUserName.isNotEmpty()) input.hint = "$takenUserName is already taken"
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("Create User") { dialog, which -> }
        builder.setNegativeButton("Close") { dialog, which -> dialog.cancel()}
        userNameDialog = builder.create()
        userNameDialog.setOnShowListener(OnShowListener {
            val button = (userNameDialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                userName = input.text.toString()
                //check if username is unique
                //multiplayerDB.searchUsers(userName)
                //dont dismiss dialog ... yet
            }
        })
        userNameDialog.show()
    }

    fun quickMatch(){
        //multiplayerDB.searchForOpenGames(player2ID = userName)
    }

    fun searchForGames(gameName: String, timeMode: String){
        gameParameters.name = gameName
        gameParameters.time = timeMode
        //multiplayerDB.searchForOpenGames(gameName, timeMode, userName)
    }

    /** saves game parameters and player data into bundle and start chess activity*/
    fun start_gameWithParameters(gameData: MultiplayerDB.GameData, gameParameters: GameParameters){
        launchedGamesMap[gameData.gameId] = true
        joinWaitDialog?.dismiss()

        val intent = Intent(mainActivity, ChessActivity::class.java)
        intent.putExtra(gameIdExtra, gameData.gameId)
        intent.putExtra(gamePlayerNameExtra, gameData.playerID)
        intent.putExtra(gamePlayerStatsExtra, playerStats)
        intent.putExtra(gameOpponentNameExtra, gameData.opponentID)
        intent.putExtra(gameOpponentStatsExtra, opponentStats)
        intent.putExtra(gameNameExtra, gameParameters.name)
        intent.putExtra(gameModeExtra, gameParameters.playMode)
        intent.putExtra(gameDifficultyExtra, gameParameters.difficulty)
        intent.putExtra(gameTimeExtra, gameParameters.time)
        intent.putExtra(playerColorExtra, gameParameters.playerColor)
        mainActivity.startActivity(intent)
    }

    /** try to load username from shared prefs, if not exist create one in sp and multiplayer db */
    fun loadOrCreateUserName(){
        val sharedPrefs = mainActivity.getSharedPreferences("FairyChess", Context.MODE_PRIVATE)!!
        userName = sharedPrefs.getString(userNameExtra, "")!!
        if(userName.isEmpty()){
            openCreateUserNameDialog("")
        } else {
            Toast.makeText(mainActivity, "welcome $userName!", Toast.LENGTH_SHORT).show()
        }
    }

    /** call back method after creating player in multiplayer db
     *  save name in sp and notify of success via Toast*/
    override fun onCreatePlayer(playerID: String) {
        //save username
        val sharedPrefsEditor = mainActivity.getSharedPreferences(
            "FairyChess",
            Context.MODE_PRIVATE
        )!!.edit()
        sharedPrefsEditor.putString(userNameExtra, userName)
        sharedPrefsEditor.apply()
        userNameDialog.dismiss()
        Toast.makeText(
            mainActivity,
            "user created $playerID",
            Toast.LENGTH_SHORT
        ).show()
    }

    /** call back method after searching player name (in process of creating new player)
     *  create player if playername unique, else notify user via toast*/
    override fun onPlayerNameSearchComplete(playerID: String, occurences: Int) {
        if(occurences==0){
            //multiplayerDB.createPlayer(userName)
        } else {
            userNameDialog.setMessage("user name already taken")
            Toast.makeText(
                mainActivity,
                "user name already taken$playerID",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** call back method after searching for game
     *  go through games matching parameters and pick a game with chance of winning in the defined area (if possible)
     *  and then join the game; if no matching game is found notify user via Toast*/
    override fun onGameSearchComplete(gameSearchResultList: List<GameSearchResult>) {
        if(gameSearchResultList.isNotEmpty()){
            //pre filter for fair games (winning chance is >30%)
            val fairGames = mutableListOf<GameSearchResult>()
            for(game in gameSearchResultList){
                if(ChessRatingSystem.Probability(playerStats.ELO,game.player1ELO)
                    in (0.5-matchmakingWinningChanceOffset) .. (0.5+matchmakingWinningChanceOffset)){
                        fairGames.add(game)
                }
            }

            if(fairGames.isNotEmpty()){
                //chose random game
                val chosenGame = gameSearchResultList[(Math.random()*fairGames.size).toInt()]
                gameParameters.time = chosenGame.timeMode //assign because in case of quickmatch this is unknown
                gameParameters.name = chosenGame.gameName

                Log.d(TAG, "gameMode found => join gameMode")
                if(DEBUG)println("gameMode found => join gameMode")
                gameParameters.playerColor = chosenGame.player2Color

                //set
                val changeMap = mutableMapOf(
                    MultiplayerDB.GAMEFIELD_PLAYER2ID to userName,
                    MultiplayerDB.GAMEFIELD_PLAYER2ELO to playerStats.ELO
                )
                if(chosenGame.gameName.startsWith("all")){
                    if(!gameParameters.name.startsWith("all")){
                        changeMap[MultiplayerDB.GAMEFIELD_GAMENAME] = gameParameters.name
                    } else {
                        changeMap[MultiplayerDB.GAMEFIELD_GAMENAME] = "normal chess" //standard
                    }
                }
                if(chosenGame.timeMode.startsWith("all")){
                    if(!gameParameters.time.startsWith("all")){
                        changeMap[MultiplayerDB.GAMEFIELD_TIMEMODE] = gameParameters.time
                    } else {
                        changeMap[MultiplayerDB.GAMEFIELD_TIMEMODE] = "blitz (2 minutes)" //standard
                    }
                }

                //join game
                /*multiplayerDB.joinGame(
                    chosenGame.id,changeMap.toMap()
                )*/
            } else {
                AlertDialog.Builder(mainActivity)
                    .setTitle("no games found")
                    .setPositiveButton("create game"
                    ) { _, _ -> /*multiplayerDB.createGame(gameParameters.name,gameParameters.time,userName,playerStats.ELO)*/}
                    .setNegativeButton("close",null)
                    .show()
            }
        } else{
            AlertDialog.Builder(mainActivity)
                .setTitle("no games found")
                .setPositiveButton("create game"
                ) { _, _ -> /*multiplayerDB.createGame(gameParameters.name,gameParameters.time,userName,playerStats.ELO)*/}
                .setNegativeButton("close",null)
                .show()
        }

    }

    /** call back method after creating multiplayer game in db
     *  display "waiting for opponent" dialog */
    override fun onCreateGame(gameName: String, gameID: String, playerColor: String) {
        Log.d(TAG, "gameMode created")
        if(DEBUG)println("gameMode created")
        Toast.makeText(
            mainActivity,
            "created gameMode: $gameName",
            Toast.LENGTH_SHORT
        ).show()
        createdGameID = gameID
        //multiplayerDB.listenToGameSearch(gameID)
        gameParameters.playerColor = playerColor

        val builder = AlertDialog.Builder(mainActivity)
        builder.setCancelable(false)
        builder.setView(R.layout.waiting_for_join_dialog)
        joinWaitDialog = builder.create()
        joinWaitDialog!!.show()
        val btn_canceln = joinWaitDialog!!.findViewById<Button>(R.id.btn_cancelJoinWait)
        btn_canceln.setOnClickListener{_ ->
            run{
                if(createdGameID.isNotEmpty()){
                    joinWaitDialog!!.dismiss()
                    //multiplayerDB.cancelGame(createdGameID)
                    createdGameID = ""
                }
            }
        }
        val btn_createDynLink = joinWaitDialog!!.findViewById<Button>(R.id.btn_createDynamicLink)
        btn_createDynLink.setOnClickListener{_ ->
            run{
               //multiplayerDB.createDynamicLink(createdGameID)
            }
        }
    }

    /** call back method after after game was changed,
     *  check if second player has joined */
    override fun onGameChanged(gameId: String) {
        if(!launchedGamesMap.containsKey(gameId)){
            //multiplayerDB.hasSecondPlayerJoined(gameId)
        }
    }

    /** call back method after getting player stats */
    override fun onGetPlayerstats(playerStats: MultiplayerDB.PlayerStats) {
        this.playerStats = playerStats
        mainActivity.findViewById<TextView>(R.id.tv_playerstats).text = userName
    }

    override fun processShortLink(shortLink: Uri?, flowchartLink: Uri?) {
        Toast.makeText(mainActivity,"Link Copied!",Toast.LENGTH_LONG).show()
        copyTextToClipboard(mainActivity,shortLink.toString())
    }

    /** call back method after joining game
     *  save game parameters and start game */
    override fun onJoinGame(onlineGameParameters: GameParameters, gameData: MultiplayerDB.GameData) {
        if(!launchedGamesMap.containsKey(gameData.gameId)){
            gameParameters.name = onlineGameParameters.name
            gameParameters.time = onlineGameParameters.time
            gameParameters.playerColor = onlineGameParameters.playerColor

            opponentStats = MultiplayerDB.PlayerStats(0,0,0,gameData.opponentELO)

            Log.d(TAG, "$userName joined gameMode: ${gameParameters.playMode}")
            if(DEBUG)println("$userName joined gameMode: ${gameParameters.name}")
            Toast.makeText(
                mainActivity,
                "$userName joined gameMode: ${gameParameters.name}",
                Toast.LENGTH_SHORT
            ).show()
            start_gameWithParameters(
                gameData,
                gameParameters
            )
        }
    }

    fun onResume() {
        loadPlayerStats()
    }
}