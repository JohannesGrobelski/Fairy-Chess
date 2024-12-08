package emerald.apps.fairychess.view.fragments.main

import android.content.Context
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import emerald.apps.fairychess.R
import emerald.apps.fairychess.model.multiplayer.MultiplayerDB
import emerald.apps.fairychess.view.MainActivity


class ProfileViewModel : ViewModel() {
    private val _playerStats = MutableLiveData<MultiplayerDB.PlayerStats>()
    val playerStats: LiveData<MultiplayerDB.PlayerStats> = _playerStats

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    fun updateStats(stats: MultiplayerDB.PlayerStats, name: String) {
        _playerStats.value = stats
        _userName.value = name
    }
}

class ProfileFragment : Fragment() {
    private var mainActivity: MainActivity? = null
    private val viewModel: ProfileViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            mainActivity = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        view.findViewById<Button>(R.id.btn_info).setOnClickListener {
            mainActivity?.showInfoDialog()
        }

        viewModel.userName.observe(viewLifecycleOwner) { userName ->
            view.findViewById<TextView>(R.id.tv_playerstats).text = userName
        }


        return view
    }

    override fun onDetach() {
        super.onDetach()
        mainActivity = null
    }
}