package emerald.apps.fairychess.view.fragments.main

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import emerald.apps.fairychess.R
import emerald.apps.fairychess.view.MainActivity

class SingleplayerFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_singleplayer, container, false)

        view.findViewById<Button>(R.id.btn_ai)?.setOnClickListener {
            (activity as? MainActivity)?.mainActivityListener?.onClick(it)
        }

        return view
    }
}