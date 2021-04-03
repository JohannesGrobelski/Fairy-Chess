package emerald.apps.fairychess.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import emerald.apps.fairychess.R
import emerald.apps.fairychess.controller.ChessActivityListener
import emerald.apps.fairychess.controller.MainActivityListener

class ChessActivity : AppCompatActivity() {
    private lateinit var chessActivityListener: ChessActivityListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val playerColor = this.intent.getStringExtra(MainActivityListener.playerColorExtra)!!
        if(playerColor == "white"){
            setContentView(R.layout.activity_chess_white_perspective)
        } else {
            setContentView(R.layout.activity_chess_black_perspective)
        }


        chessActivityListener = ChessActivityListener(this)
    }

    fun onClickSquare(v: View){
        chessActivityListener.clickSquare(v)
    }

    override fun onDestroy() {
        super.onDestroy()
        chessActivityListener.onDestroy()
    }


}