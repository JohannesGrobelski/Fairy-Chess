package emerald.apps.fairychess.controller

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface.OnShowListener
import android.content.Intent
import android.text.InputType
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
    var searchGameJob : Job? = null
    var userName = ""
    private lateinit var mainActivity : MainActivity
    private lateinit var multiplayerDB: MultiplayerDB

    constructor(mainActivity: MainActivity) : this(){
        this.mainActivity = mainActivity
        loadOrCreateUserName()
        multiplayerDB = MultiplayerDB(this)
    }

    companion object {
        const val userNamePref = "userNamePref"
        const val gameIdExtra = "gameId"
        const val gameModeExtra = "gameMode"
        const val gameNameExtra = "gameName"
        const val gameTimeExtra = "gameTime"
        const val playerColorExtra = "playerColor"
    }

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

            }
            dialog.dismiss()
        }
        dialog.show()
    }

    fun searchForGames(gameName: String, timeMode: String){
        multiplayerDB.searchGames(gameName, timeMode)
    }

    fun start_gameWithParameters(fairyChessGame: FairyChessGame){
        val intent = Intent(mainActivity, ChessActivity::class.java)
        intent.putExtra(gameIdExtra, fairyChessGame.gameId)
        intent.putExtra(gameNameExtra, fairyChessGame.gameName)
        intent.putExtra(gameModeExtra, fairyChessGame.game)
        intent.putExtra(gameTimeExtra, fairyChessGame.time)
        intent.putExtra(playerColorExtra, fairyChessGame.playerColor)
        mainActivity.startActivity(intent)
        mainActivity.finish()
    }


    data class FairyChessGame(
        val gameId: String,
        val gameName: String,
        val game: String,
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

    override fun onCreatePlayer(playerName: String) {
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
            "user created $playerName",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onPlayerNameSearchComplete(playerName: String, occurences: Int) {
        if(occurences==0){
            multiplayerDB.createPlayer(userName)
        } else {
            userNameDialog.setMessage("user name already taken")
            Toast.makeText(
                mainActivity,
                "user name already taken$playerName",
                Toast.LENGTH_SHORT
            ).show()
            //openCreateUserNameDialog(playerName)
        }
    }

    override fun onGameSearchComplete(gameName: String, timeMode: String, gameIDList: List<String>) {
        if(gameIDList.isNotEmpty()){ //no matching games found
            //join first game
            run {
                Toast.makeText(
                    mainActivity,
                    "found game: " + gameIDList[0],
                    Toast.LENGTH_SHORT
                ).show()
                multiplayerDB.joinGame(gameIDList[0], timeMode, gameName, userName)
            }
        } else {
            Toast.makeText(
                mainActivity,
                "created game: $gameName",
                Toast.LENGTH_SHORT
            ).show()
            /*val gameId = multiplayerDB.createGame(gameName,userName)
            searchGameJob = CoroutineScope(Dispatchers.Default).launch {
                while(true){//search all ten seconds for a
                    if(MultiplayerDB().checkGameForSecondPlayer(gameId)){
                        start_gameWithParameters(FairyChessGame(gameId,gameName,"human",timeMode,"white"))
                    }
                    delay(10000)
                }
            }*/
        }
    }

    override fun onJoinGame(gameID: String, timeMode: String, gameName: String) {
        start_gameWithParameters(
            FairyChessGame(
                gameID,
                gameName,
                "human",
                timeMode,
                "black"
            )
        )
    }

    override fun onCreateGame(gameName: String, gameID: String) {
        TODO("Not yet implemented")
    }
}