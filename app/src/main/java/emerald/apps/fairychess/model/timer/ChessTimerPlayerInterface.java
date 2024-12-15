package emerald.apps.fairychess.model.timer;

public interface ChessTimerPlayerInterface {
    void onTickPlayerTimer(long millisUntilFinished);
    void onFinishPlayerTimer();
}

