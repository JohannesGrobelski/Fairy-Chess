package emerald.apps.fairychess.controller

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import emerald.apps.fairychess.R
import emerald.apps.fairychess.view.ChessActivity
import emerald.apps.fairychess.view.MainActivity

class MainActivityListener(var mainActivity: MainActivity) : View.OnClickListener {

    companion object {
        const val gameModeExtra = "game_mode"
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

    fun display_alertDialogGameParameters(mode:String){
        val gameModes = mainActivity.resources.getStringArray(R.array.gamemodes)
        val timeModes = mainActivity.resources.getStringArray(R.array.timemodes)
        val inflater =
            mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val rootView: View = inflater.inflate(R.layout.alertdialog_online_game_parameters, null, false)
        val spinner_gamemode : Spinner = rootView.findViewById(R.id.spinner_gamemode)
        spinner_gamemode.adapter = ArrayAdapter(mainActivity,android.R.layout.simple_list_item_1,gameModes)
        val spinner_timemode : Spinner = rootView.findViewById(R.id.spinner_timemode)
        spinner_timemode.adapter = ArrayAdapter(mainActivity,android.R.layout.simple_list_item_1,timeModes)
        val btn_start_game_search = rootView.findViewById<Button>(R.id.btn_start_game_search)

        val builder = AlertDialog.Builder(mainActivity)
        builder.setView(rootView)
        val dialog = builder.create()

        btn_start_game_search.setOnClickListener{
            run {
                start_gameWithParameters(FairyChessGame(mode,
                    spinner_gamemode.selectedItem.toString(),
                    spinner_timemode.selectedItem.toString()))
                dialog.dismiss()
                Toast.makeText(mainActivity, "started game search: ${spinner_gamemode.selectedItem.toString()} ${spinner_timemode.selectedItem.toString()}",Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()

    }

    fun start_gameWithParameters(fairyChessGame: FairyChessGame){
        val intent = Intent(mainActivity,ChessActivity::class.java)
        intent.putExtra("mode", fairyChessGame.mode)
        intent.putExtra(gameModeExtra, fairyChessGame.game)
        intent.putExtra("time", fairyChessGame.time)
        mainActivity.startActivity(intent)
        mainActivity.finish()
    }


    data class FairyChessGame(val mode:String, val game:String, val time:String)
}