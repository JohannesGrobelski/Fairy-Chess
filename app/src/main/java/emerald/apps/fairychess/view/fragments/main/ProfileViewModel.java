package emerald.apps.fairychess.view.fragments.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import emerald.apps.fairychess.model.multiplayer.MultiplayerDB;

public class ProfileViewModel extends ViewModel {
    private final MutableLiveData<MultiplayerDB.PlayerStats> _playerStats = new MutableLiveData<>();
    private final MutableLiveData<String> _userName = new MutableLiveData<>();

    public LiveData<MultiplayerDB.PlayerStats> getPlayerStats() {
        return _playerStats;
    }

    public LiveData<String> getUserName() {
        return _userName;
    }

    public void updateStats(MultiplayerDB.PlayerStats stats, String name) {
        _playerStats.setValue(stats);
        _userName.setValue(name);
    }
}
