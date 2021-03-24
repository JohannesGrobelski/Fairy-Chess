package emerald.apps.fairychess.view

import android.graphics.Color
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import emerald.apps.fairychess.R
import emerald.apps.fairychess.controller.ChessActivityListener

class ChessActivity : AppCompatActivity() {
    lateinit var chessActivityListener : ChessActivityListener




    //Views
    var elterLayout: LinearLayout? = null
    private lateinit var imageViews: Array<Array<ImageView>>

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_chess)
        chessActivityListener = ChessActivityListener(this)
    }

    fun player_action(v: View){

    }




}