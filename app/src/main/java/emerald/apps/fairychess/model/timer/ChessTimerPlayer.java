package emerald.apps.fairychess.model.timer;

public class ChessTimerPlayer extends CountDownTimerWithPause {
    private final ChessTimerPlayerInterface chessTimerPlayerInterface;

    public ChessTimerPlayer(
            ChessTimerPlayerInterface chessTimerPlayerInterface, 
            long finished, 
            long interval
    ) {
        super(finished, interval, false);
        this.chessTimerPlayerInterface = chessTimerPlayerInterface;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        chessTimerPlayerInterface.onTickPlayerTimer(millisUntilFinished);
    }

    @Override
    public void onFinish() {
        chessTimerPlayerInterface.onFinishPlayerTimer();
    }

    public static ChessTimerPlayer getPlTimerFromTimeMode(
            ChessTimerPlayerInterface chessTimerPlayerInterface, 
            String timeMode
    ) {
        return switch (timeMode.toLowerCase()) {
            case "bullet (2 minutes)" -> new ChessTimerPlayer(chessTimerPlayerInterface, 120000, 1000);
            case "blitz (5 minutes)" -> new ChessTimerPlayer(chessTimerPlayerInterface, 300000, 1000);
            case "rapid (10 minutes)" -> new ChessTimerPlayer(chessTimerPlayerInterface, 600000, 1000);
            default -> null;
        };
    }

}


