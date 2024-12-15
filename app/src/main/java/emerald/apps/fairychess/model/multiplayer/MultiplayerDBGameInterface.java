package emerald.apps.fairychess.model.multiplayer;

public interface MultiplayerDBGameInterface {
    void onFinishGame(String gameId, String cause);
    void onGameChanged(String gameId, MultiplayerDB.GameState gameState);
    void onSetPlayerstats(String exitCause);
}
