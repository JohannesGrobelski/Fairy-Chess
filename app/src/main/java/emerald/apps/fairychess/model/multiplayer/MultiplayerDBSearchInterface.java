package emerald.apps.fairychess.model.multiplayer;

import java.util.List;

import emerald.apps.fairychess.controller.MainActivityListener;

public interface MultiplayerDBSearchInterface {
    void onCreatePlayer(String playerID);
    void onPlayerNameSearchComplete(String playerIDr, int occurences);
    void onGameSearchComplete(List<MainActivityListener.GameSearchResult> gameSearchResultList);
    void onJoinGame(MainActivityListener.GameParameters gameParameters, MultiplayerDB.GameData gameData);
    void onCreateGame(String gameName, String gameID, String player1Color);
    void onGameChanged(String gameId);
    void onGetPlayerstats(MultiplayerDB.PlayerStats playerStats);
    void processGameInvite(String gameId);
}


