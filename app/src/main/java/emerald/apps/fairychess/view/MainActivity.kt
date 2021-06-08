package emerald.apps.fairychess.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import emerald.apps.fairychess.R
import emerald.apps.fairychess.controller.MainActivityListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.alertdialog_create_game.*

class MainActivity : AppCompatActivity() {
    lateinit var mainActivityListener : MainActivityListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivityListener = MainActivityListener(this)
        btn_createGame.setOnClickListener(mainActivityListener)
        btn_searchGame.setOnClickListener(mainActivityListener)
        btn_quickmatch.setOnClickListener(mainActivityListener)
    }

    override fun onResume() {
        super.onResume()
        mainActivityListener.onResume()
    }

    fun onClick(v: View){
        mainActivityListener.onClick(v)
    }

}