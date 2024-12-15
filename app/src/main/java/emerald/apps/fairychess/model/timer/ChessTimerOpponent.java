package emerald.apps.fairychess.model.timer;

public class ChessTimerOpponent extends CountDownTimerWithPause {
    private final ChessTimerOpponentInterface chessTimerInterface;

    public ChessTimerOpponent(
            ChessTimerOpponentInterface chessTimerInterface, 
            long finished, 
            long interval
    ) {
        super(finished, interval, false);
        this.chessTimerInterface = chessTimerInterface;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        chessTimerInterface.onTickOpponentTimer(millisUntilFinished);
    }

    @Override
    public void onFinish() {
        chessTimerInterface.onFinishOpponentTimer();
    }

    public static ChessTimerOpponent getOpTimerFromTimeMode(
            ChessTimerOpponentInterface chessTimerOpponentInterface, 
            String timeMode
    ) {
        return switch (timeMode.toLowerCase()) {
            case "bullet (2 minutes)" -> new ChessTimerOpponent(chessTimerOpponentInterface, 120000, 1000);
            case "blitz (5 minutes)" -> new ChessTimerOpponent(chessTimerOpponentInterface, 300000, 1000);
            case "rapid (10 minutes)" -> new ChessTimerOpponent(chessTimerOpponentInterface, 600000, 1000);
            default -> null;
        };
    }

    public interface ChessTimerOpponentInterface {
        void onTickOpponentTimer(long millisUntilFinished);
        void onFinishOpponentTimer();
    }
}


