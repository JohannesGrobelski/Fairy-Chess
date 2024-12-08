package emerald.apps.fairychess.view.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import emerald.apps.fairychess.R
import emerald.apps.fairychess.view.MainActivity

class ChessCreatorFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chesscreator, container, false)

        view.findViewById<Button>(R.id.btn_chess_creator_new)?.setOnClickListener {
            (activity as? MainActivity)?.mainActivityListener?.onClick(it)
        }

        view.findViewById<Button>(R.id.btn_chess_creator_edit)?.setOnClickListener {
            (activity as? MainActivity)?.mainActivityListener?.onClick(it)
        }

        return view
    }
}