package emerald.apps.fairychess.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import emerald.apps.fairychess.R
import emerald.apps.fairychess.controller.MainActivityListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var mainActivityListener : MainActivityListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivityListener = MainActivityListener(this)
        btn_human.setOnClickListener(mainActivityListener)
        btn_ai.setOnClickListener(mainActivityListener)
    }

    override fun onResume() {
        super.onResume()
        mainActivityListener.loadPlayerStats()
    }


}