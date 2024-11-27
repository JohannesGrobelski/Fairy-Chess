package emerald.apps.fairychess.model.timer

import java.util.concurrent.TimeUnit


class ChessTimerPlayer(
    private val chessTimerPlayerInterface: ChessTimerPlayerInterface,
    finished: Long,
    interval: Long
)
    : CountDownTimerWithPause(finished, interval, false){

    override fun onTick(millisUntilFinished: Long) {
        chessTimerPlayerInterface.onTickPlayerTimer(millisUntilFinished)
    }

    override fun onFinish() {
        chessTimerPlayerInterface.onFinishPlayerTimer()
    }

    companion object {
        fun getPlTimerFromTimeMode(
            chessTimerPlayerInterface: ChessTimerPlayerInterface,
            timeMode: String
        ) : ChessTimerPlayer? {
            return when(timeMode){
                "bullet (2 minutes)" -> ChessTimerPlayer(chessTimerPlayerInterface, 120000, 1000)
                "blitz (5 minutes)" -> ChessTimerPlayer(chessTimerPlayerInterface, 300000, 1000)
                "rapid (10 minutes)" -> ChessTimerPlayer(chessTimerPlayerInterface, 600000, 1000)
                else -> null
            }
        }
    }

    interface ChessTimerPlayerInterface {
        fun onTickPlayerTimer(millisUntilFinished: Long)
        fun onFinishPlayerTimer()
    }
}

class ChessTimerOpponent(
    private val chessTimerInterface: ChessTimerOpponentInterface,
    finished: Long,
    interval: Long
)
    : CountDownTimerWithPause(finished, interval, false){

    override fun onTick(millisUntilFinished: Long) {
        chessTimerInterface.onTickOpponentTimer(millisUntilFinished)
    }

    override fun onFinish() {
        chessTimerInterface.onFinishOpponentTimer()
    }

    companion object {
        fun getOpTimerFromTimeMode(
            chessTimerOpponentInterface: ChessTimerOpponentInterface,
            timeMode: String
        ) : ChessTimerOpponent?{
            return when(timeMode){
                "bullet (2 minutes)" -> ChessTimerOpponent(chessTimerOpponentInterface,120000,1000)
                "blitz (5 minutes)" -> ChessTimerOpponent(chessTimerOpponentInterface, 300000, 1000)
                "rapid (10 minutes)" -> ChessTimerOpponent(chessTimerOpponentInterface,600000,1000)
                else -> null
            }
        }
    }

    interface ChessTimerOpponentInterface {
        fun onTickOpponentTimer(millisUntilFinished: Long)
        fun onFinishOpponentTimer()
    }
}

class TimerUtils {
    companion object {
        fun transformLongToTimeString(millis: Long) : String {
            return java.lang.String.format(
                "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
                    TimeUnit.MILLISECONDS.toHours(
                        millis
                    )
                ),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(
                        millis
                    )
                )
            )
        }
    }
}