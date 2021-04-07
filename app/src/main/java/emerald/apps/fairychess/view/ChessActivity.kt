package emerald.apps.fairychess.view

import android.graphics.Color.RED
import android.graphics.Color.WHITE
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import emerald.apps.fairychess.R
import emerald.apps.fairychess.controller.ChessActivityListener
import emerald.apps.fairychess.controller.MainActivityListener
import emerald.apps.fairychess.model.ChessPiece
import kotlinx.android.synthetic.main.activity_chess_black_perspective.*
import kotlinx.android.synthetic.main.activity_chess_white_perspective.*


class ChessActivity : AppCompatActivity() {
    private lateinit var chessActivityListener: ChessActivityListener
    private lateinit var playerColor : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playerColor = this.intent.getStringExtra(MainActivityListener.playerColorExtra)!!
        if(playerColor == "white"){
            setContentView(R.layout.activity_chess_white_perspective)
        } else {
            setContentView(R.layout.activity_chess_black_perspective)
        }
        
        chessActivityListener = ChessActivityListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        chessActivityListener.onDestroy()
    }


    fun onClickSquare(v: View){
        chessActivityListener.clickSquare(v)
    }


    fun highlightActivePlayer(activePlayerColor: String){
        if(playerColor == "white"){
            if(playerColor == activePlayerColor){
                tv_playernameW.setTextColor(RED)
                tv_opponentnameW.setTextColor(WHITE)
            }else {
                tv_opponentnameW.setTextColor(RED)
                tv_playernameW.setTextColor(WHITE)
            }
        } else {
            if(playerColor == activePlayerColor){
                tv_playernameB.setTextColor(RED)
                tv_opponentnameB.setTextColor(WHITE)
            }else {
                tv_opponentnameB.setTextColor(RED)
                tv_playernameB.setTextColor(WHITE)
            }
        }
    }

    fun drawCapturedPiecesDrawable(color: String, capturedPieces: List<ChessPiece>) {
        val layers = mutableListOf<Drawable>()
        var inset = 0
        for(capturedPiece in capturedPieces){
            val drawable = this.resources.getDrawable(
                chessActivityListener.getDrawableFromName(capturedPiece.name,capturedPiece.color)
            )
            val insetDrawable = InsetDrawable(drawable,inset,0,0,0)
            inset += 100
            layers.add(insetDrawable)
        }
        val layerDrawable = LayerDrawable(layers.toTypedArray())
        if(color == playerColor){
            if(playerColor == "white"){
                iv_captPiecesPlayerLine1W.setImageDrawable(layerDrawable)
            }else if(playerColor == "black"){
                iv_captPiecesPlayerLine1.setImageDrawable(layerDrawable)
            }
        }
        else {
            if(playerColor == "white"){
                iv_captPiecesOpponentLine1W.setImageDrawable(layerDrawable)
            }else if(playerColor == "black"){
                iv_captPiecesOpponentLine1.setImageDrawable(layerDrawable)
            }
        }
    }

}