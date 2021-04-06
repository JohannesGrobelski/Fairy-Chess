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
import kotlinx.coroutines.*


class MainActivityListener() : View.OnClickListener,MultiplayerDBSearchInterface {
    lateinit var userNameDialog : AlertDialog
    var userName = ""
    var opponentName = ""
    private lateinit var mainActivity : MainActivity
    private lateinit var multiplayerDB: MultiplayerDB

    private var launchedGamesMap = mutableMapOf<String,Boolean>()

    constructor(mainActivity: MainActivity) : this(){
        this.mainActivity = mainActivity
        loadOrCreateUserName()
        multiplayerDB = MultiplayerDB(this)
    }

    companion object {
        const val TAG = "MainActivityListener"
        const val userNamePref = "userNamePref"
        const val gameIdExtra = "gameId"
        const val gamePlayerNameExtra = "playerNameExtra"
        const val gameOpponentNameExtra = "opponentNameExtra"
        const val gameModeExtra = "gameMode"
        const val gameNameExtra = "gameName"
        const val gameTimeExtra = "gameTime"
        const val playerColorExtra = "playerColor"
    }

    private lateinit var gameSearchParameterGameName : String
    private lateinit var gameSearchParameterTime : String
    private var gameSearchParameterPlayerColor = ""


    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btn_human -> {
                display_alertDialogGameParameters("human")
            }
            R.id.btn_ai -> {
                display_alertDialogGameParameters("ai")
            }
        }
    }

    fun display_alertDialogGameParameters(mode: String){
        val gameModes = mainActivity.resources.getStringArray(R.array.gamemodes)
        val timeModes = mainActivity.resources.getStringArray(R.array.timemodes)
        val inflater =
            mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val rootView: View = inflater.inflate(
            R.layout.alertdialog_online_game_parameters,
            null,
            false
        )
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
        val btn_start_game_search = rootView.findViewById<Button>(R.id.btn_start_game_search)

        val builder = AlertDialog.Builder(mainActivity)
        builder.setView(rootView)
        val dialog = builder.create()

        btn_start_game_search.setOnClickListener{
            if(mode == "human"){
                searchForGames(
                    spinner_gameName.selectedItem.toString(),
                    spinner_timemode.selectedItem.toString()
                )
            } else {
                gameSearchParameterGameName = spinner_gameName.selectedItem.toString()
                gameSearchParameterTime = spinner_timemode.selectedItem.toString()
                gameSearchParameterPlayerColor = "white"
                val gameData = MultiplayerDB.GameData("",userName,"ai")
                start_gameWithParameters(
                    FairyChessGame(
                        gameData,
                        gameSearchParameterGameName,
                        "ai",
                        gameSearchParameterTime,
                        gameSearchParameterPlayerColor
                    )
                )
            }
            dialog.dismiss()
        }
        dialog.show()
    }

    fun searchForGames(gameName: String, timeMode: String){
        multiplayerDB.searchForOpenGames(gameName, timeMode)
        this.gameSearchParameterGameName = gameName
        this.gameSearchParameterTime = timeMode
    }

    fun start_gameWithParameters(fairyChessGame: FairyChessGame){
        val intent = Intent(mainActivity, ChessActivity::class.java)
        intent.putExtra(gameIdExtra, fairyChessGame.gameData.gameId)
        intent.putExtra(gamePlayerNameExtra, fairyChessGame.gameData.playerID)
        intent.putExtra(gameOpponentNameExtra, fairyChessGame.gameData.opponentID)
        intent.putExtra(gameNameExtra, fairyChessGame.gameName)
        intent.putExtra(gameModeExtra, fairyChessGame.gameMode)
        intent.putExtra(gameTimeExtra, fairyChessGame.time)
        intent.putExtra(playerColorExtra, fairyChessGame.playerColor)
        mainActivity.startActivityForResult(intent,7777)
       // mainActivity.finish()
    }


    data class FairyChessGame(
        val gameData: MultiplayerDB.GameData,
        val gameName: String,
        val gameMode: String,
        val time: String,
        val playerColor: String
    )

    fun loadOrCreateUserName(){
        val sharedPrefs = mainActivity.getSharedPreferences("FairyChess", Context.MODE_PRIVATE)!!
        userName = sharedPrefs.getString(userNamePref, "")!!
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
        sharedPrefsEditor.putString(userNamePref, userName)
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

    override fun onGameSearchComplete(gameIDList: List<String>) {
        if(gameIDList.isNotEmpty()){ //no matching games found
            Log.d(TAG,"gameMode found => join gameMode")
            println("gameMode found => join gameMode")
            //join first gameMode
            Toast.makeText(
                mainActivity,
                "found gameMode: " + gameIDList[0],
                Toast.LENGTH_SHORT
            ).show()
            gameSearchParameterPlayerColor = "black"
            multiplayerDB.joinGame(gameIDList[0], userName)
        } else {
            Log.d(TAG,"no gameMode found => create gameMode")
            println("no gameMode found => create gameMode")
            multiplayerDB.createGame(gameSearchParameterGameName,userName)
        }
    }



    override fun onCreateGame(gameName: String, gameID: String, playerColor : String) {
        Log.d(TAG,"gameMode created")
        println("gameMode created")
        Toast.makeText(
            mainActivity,
            "created gameMode: $gameName",
            Toast.LENGTH_SHORT
        ).show()
        multiplayerDB.listenToGameSearch(gameID)
        this.gameSearchParameterPlayerColor = playerColor
    }

    override fun onGameChanged(gameId: String) {
        if(!launchedGamesMap.containsKey(gameId)){
            multiplayerDB.hasSecondPlayerJoined(gameId)
        }
    }

    override fun onJoinGame(gameData: MultiplayerDB.GameData) {
        if(!multiplayerDB.gameLaunched && !launchedGamesMap.containsKey(gameData.gameId)){
            multiplayerDB.gameLaunched = true
            Log.d(TAG, "$userName joined gameMode: $gameSearchParameterGameName")
            println("$userName joined gameMode: $gameSearchParameterGameName")
            Toast.makeText(
                mainActivity,
                "$userName joined gameMode: $gameSearchParameterGameName",
                Toast.LENGTH_SHORT
            ).show()
            launchedGamesMap[gameData.gameId] = true
            start_gameWithParameters(
                FairyChessGame(
                    gameData,
                    gameSearchParameterGameName,
                    "human",
                    gameSearchParameterTime,
                    gameSearchParameterPlayerColor
                )
            )
        }
    }
}