package emerald.apps.fairychess.controller

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface.OnShowListener
import android.content.Intent
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import emerald.apps.fairychess.R
import emerald.apps.fairychess.model.MultiplayerDB
import emerald.apps.fairychess.model.MultiplayerDBSearchInterface
import emerald.apps.fairychess.view.ChessActivity
import emerald.apps.fairychess.view.MainActivity


class MainActivityListener() : View.OnClickListener,MultiplayerDBSearchInterface {
    private lateinit var mainActivity : MainActivity
    private lateinit var multiplayerDB: MultiplayerDB

    lateinit var userNameDialog : AlertDialog
    var joinWaitDialog : AlertDialog? = null
    lateinit var gameSearchDialog: AlertDialog
    var foundGamesList : MutableList<Game> = mutableListOf()
    lateinit var gameListAdapter : GameListAdapter
    private var launchedGamesMap = mutableMapOf<String, Boolean>()

    private lateinit var userName:String
    class Game(
        val id: String,
        val gameName: String,
        val timeMode: String,
        val player2Color : String
    )
    class GameParameters(
        var name: String,
        var playMode: String,
        var time: String,
        var playerColor: String
    )
    private var gameParameters = GameParameters("", "", "", "")
    private var createdGameID = ""

    companion object {
        const val TAG = "MainActivityListener"
        const val userNameExtra = "userNameExtra"
        const val gameIdExtra = "gameId"
        const val gamePlayerNameExtra = "playerNameExtra"
        const val gameOpponentNameExtra = "opponentNameExtra"
        const val gameModeExtra = "gameMode"
        const val gameNameExtra = "name"
        const val gameTimeExtra = "gameTime"
        const val playerColorExtra = "playerColor"
    }


    constructor(mainActivity: MainActivity) : this(){
        this.mainActivity = mainActivity
        loadOrCreateUserName()
        multiplayerDB = MultiplayerDB(this)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btn_human -> {
                gameParameters.playMode = "human"
                display_alertDialogGameParameters("human")
            }
            R.id.btn_ai -> {
                gameParameters.playMode = "human"
                display_alertDialogGameParameters("ai")
            }
        }
    }

    fun display_alertDialogGameParameters(mode: String){
        val gameModes = mainActivity.resources.getStringArray(R.array.gamemodes)
        val timeModes = mainActivity.resources.getStringArray(R.array.timemodes)
        val inflater = LayoutInflater.from(mainActivity)
        val rootView: View

        //inflate the layout (depending on mode)
        rootView = if(mode == "ai"){
            inflater.inflate(
                R.layout.alertdialog_offline_game_parameters,
                null,
                false
            )
        } else {
            inflater.inflate(
                R.layout.alertdialog_online_game_parameters,
                null,
                false
            )
        }

        //create dialog
        val spinner_gameName : Spinner = rootView.findViewById(R.id.spinner_gameName)
        spinner_gameName.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_list_item_1,
            gameModes
        )
        val spinner_timemode : Spinner = rootView.findViewById(R.id.spinner_timemode)
        spinner_timemode.adapter = ArrayAdapter(
            mainActivity,
            android.R.layout.simple_list_item_1,
            timeModes
        )
        val btn_create_game = rootView.findViewById<Button>(R.id.btn_create_game)
        val builder = AlertDialog.Builder(mainActivity)
        builder.setView(rootView)
        gameSearchDialog = builder.create()

        if(mode == "human"){
            searchForGames(
                spinner_gameName.selectedItem.toString(),
                spinner_timemode.selectedItem.toString()
            )
            gameListAdapter = GameListAdapter(mainActivity, foundGamesList)
            val LV_matchingGames : ListView = rootView.findViewById(R.id.LV_matchingGames)
            LV_matchingGames.adapter = gameListAdapter

            spinner_gameName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    searchForGames(
                        spinner_gameName.selectedItem.toString(),
                        spinner_timemode.selectedItem.toString()
                    )
                }
            }
            spinner_timemode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    searchForGames(
                        spinner_gameName.selectedItem.toString(),
                        spinner_timemode.selectedItem.toString()
                    )
                }
            }
            LV_matchingGames.setOnItemClickListener{ parent, view, position, id ->
                run {
                    gameParameters.name = foundGamesList[position].gameName
                    gameParameters.time = foundGamesList[position].timeMode
                    gameParameters.playerColor = foundGamesList[position].player2Color
                    multiplayerDB.joinGame(foundGamesList[position].id, userName)
                }
            }
        }

        btn_create_game.setOnClickListener{
            gameParameters.name = spinner_gameName.selectedItem.toString()
            gameParameters.time = spinner_timemode.selectedItem.toString()
            if(mode == "human"){
                val chosenGameName = spinner_gameName.selectedItem.toString()
                val chosenTimeMode = spinner_timemode.selectedItem.toString()
                if(chosenGameName.startsWith("all")){
                    Toast.makeText(mainActivity,"please chose a game type",Toast.LENGTH_LONG).show()
                }
                else if(chosenTimeMode.startsWith("all")){
                    Toast.makeText(mainActivity,"please chose a time mode",Toast.LENGTH_LONG).show()
                }
                if(!chosenGameName.startsWith("all") && !chosenTimeMode.startsWith("all")){
                    multiplayerDB.createGame(
                        spinner_gameName.selectedItem.toString(),
                        spinner_timemode.selectedItem.toString(),
                        userName
                    )
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
                                multiplayerDB.cancelGame(createdGameID)
                                createdGameID = ""
                            }
                        }
                    }
                    gameSearchDialog.dismiss()
                }
            } else {
                gameParameters = GameParameters(
                    spinner_gameName.selectedItem.toString(),
                    "ai",
                    spinner_timemode.selectedItem.toString(),
                    "white"
                )
                start_gameWithParameters(
                    MultiplayerDB.GameData("", userName, "ai"),
                    gameParameters
                )
                gameSearchDialog.dismiss()
            }
        }
        gameSearchDialog.show()
    }

    fun searchForGames(gameName: String, timeMode: String){
        gameParameters.name = gameName
        gameParameters.time = timeMode
        multiplayerDB.searchForOpenGames(gameName, timeMode, userName)
    }

    fun start_gameWithParameters(gameData: MultiplayerDB.GameData, gameParameters: GameParameters){
        gameSearchDialog.dismiss()
        launchedGamesMap[gameData.gameId] = true
        joinWaitDialog?.dismiss()

        val intent = Intent(mainActivity, ChessActivity::class.java)
        intent.putExtra(gameIdExtra, gameData.gameId)
        intent.putExtra(gamePlayerNameExtra, gameData.playerID)
        intent.putExtra(gameOpponentNameExtra, gameData.opponentID)
        intent.putExtra(gameNameExtra, gameParameters.name)
        intent.putExtra(gameModeExtra, gameParameters.playMode)
        intent.putExtra(gameTimeExtra, gameParameters.time)
        intent.putExtra(playerColorExtra, gameParameters.playerColor)
        mainActivity.startActivityForResult(intent, 7777)
    }

    fun loadOrCreateUserName(){
        val sharedPrefs = mainActivity.getSharedPreferences("FairyChess", Context.MODE_PRIVATE)!!
        userName = sharedPrefs.getString(userNameExtra, "")!!
        if(userName.isEmpty()){
            openCreateUserNameDialog("")
        } else {
            Toast.makeText(mainActivity, "welcome $userName!", Toast.LENGTH_SHORT).show()
        }
    }

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
        builder.setPositiveButton("OK") { dialog, which -> }
        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel()}
        userNameDialog = builder.create()
        userNameDialog.setOnShowListener(OnShowListener {
            val button = (userNameDialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                userName = input.text.toString()
                //check if username is unique
                multiplayerDB.searchUsers(userName)
                //dont dismiss dialog ... yet
            }
        })
        userNameDialog.show()
    }

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

    override fun onPlayerNameSearchComplete(playerID: String, occurences: Int) {
        if(occurences==0){
            multiplayerDB.createPlayer(userName)
        } else {
            userNameDialog.setMessage("user name already taken")
            Toast.makeText(
                mainActivity,
                "user name already taken$playerID",
                Toast.LENGTH_SHORT
            ).show()
            //openCreateUserNameDialog(playerID)
        }
    }

    override fun onGameSearchComplete(gameIDList: List<Game>) {
        Log.d(TAG, "gameMode found => join gameMode")
        println("gameMode found => join gameMode")
        gameParameters.playerColor = "black"
        foundGamesList.clear()
        foundGamesList.addAll(gameIDList)
        gameListAdapter.notifyDataSetChanged()
    }

    override fun onCreateGame(gameName: String, gameID: String, playerColor: String) {
        Log.d(TAG, "gameMode created")
        println("gameMode created")
        Toast.makeText(
            mainActivity,
            "created gameMode: $gameName",
            Toast.LENGTH_SHORT
        ).show()
        createdGameID = gameID
        joinWaitDialog?.findViewById<Button>(R.id.btn_cancelJoinWait)?.visibility = View.VISIBLE
        multiplayerDB.listenToGameSearch(gameID)
        gameParameters.playerColor = playerColor
    }

    override fun onGameChanged(gameId: String) {
        if(!launchedGamesMap.containsKey(gameId)){
            multiplayerDB.hasSecondPlayerJoined(gameId)
        }
    }


    override fun onJoinGame(gameData: MultiplayerDB.GameData) {
        if(!launchedGamesMap.containsKey(gameData.gameId)){

            Log.d(TAG, "$userName joined gameMode: ${gameParameters.playMode}")
            println("$userName joined gameMode: ${gameParameters.name}")
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
}