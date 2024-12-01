package emerald.apps.fairychess.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import emerald.apps.fairychess.R


class MultiplayerFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_multiplayer, container, false)

        view.findViewById<Button>(R.id.btn_searchGame)?.setOnClickListener {
            (activity as? MainActivity)?.mainActivityListener?.onClick(it)
        }

        view.findViewById<Button>(R.id.btn_createGame)?.setOnClickListener {
            (activity as? MainActivity)?.mainActivityListener?.onClick(it)
        }

        return view
    }
}