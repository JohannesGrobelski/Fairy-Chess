package emerald.apps.fairychess.controller

import android.content.Intent
import android.view.View
import emerald.apps.fairychess.R
import emerald.apps.fairychess.view.ChessActivity
import emerald.apps.fairychess.view.MainActivity
import kotlinx.android.synthetic.main.activity_main.view.*

class MainActivityListener(var mainActivity: MainActivity) : View.OnClickListener {


    override fun onClick(v: View?) {
        when(v?.id){
            R.id.btn_human -> {
                val intent = Intent(mainActivity,ChessActivity::class.java)
                intent.putExtra("mode", "human")
                mainActivity.startActivity(intent)
                mainActivity.finish()
            }
            R.id.btn_ki -> {
                val intent = Intent(mainActivity,ChessActivity::class.java)
                intent.putExtra("mode", "ai")
                mainActivity.startActivity(intent)
                mainActivity.finish()
            }
        }
    }

}