package emerald.apps.fairychess.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import emerald.apps.fairychess.R
import emerald.apps.fairychess.controller.MainActivityListener
import emerald.apps.fairychess.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var mainActivityListener : MainActivityListener

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainActivityListener = MainActivityListener(this, intent)
        binding.btnCreateGame.setOnClickListener(mainActivityListener)
        binding.btnSearchGame.setOnClickListener(mainActivityListener)
        binding.btnQuickmatch.setOnClickListener(mainActivityListener)
        binding.btnAi.setOnClickListener(mainActivityListener)
    }

    override fun onResume() {
        super.onResume()
        mainActivityListener.onResume()
    }

    fun onClick(v: View){
        mainActivityListener.onClick(v)
    }

}