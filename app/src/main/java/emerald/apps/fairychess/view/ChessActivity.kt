package emerald.apps.fairychess.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import emerald.apps.fairychess.R
import emerald.apps.fairychess.controller.ChessActivityListener

class ChessActivity : AppCompatActivity() {
    private lateinit var chessActivityListener: ChessActivityListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chess)

        chessActivityListener = ChessActivityListener(this)
    }

    fun onClickSquare(v: View){
        chessActivityListener.clickSquare(v)
    }



}