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
import kotlinx.android.synthetic.main.activity_chess_black_perspective.*
import kotlinx.android.synthetic.main.activity_chess_white_perspective.*


class ChessActivity : AppCompatActivity() {
    private lateinit var chessActivityListener: ChessActivityListener
    private lateinit var playerColor : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //get parameters from intent
        playerColor = this.intent.getStringExtra(MainActivityListener.playerColorExtra)!!
        val timeMode = this.intent.getStringExtra(MainActivityListener.gameTimeExtra)!!
        if(playerColor == "white"){
            setContentView(R.layout.activity_chess_white_perspective)
            tv_PlayerTimeW.text = timeMode
            tv_OpponentTimeW.text = timeMode
        } else {
            setContentView(R.layout.activity_chess_black_perspective)
            tv_PlayerTimeB.text = timeMode
            tv_OpponentTimeB.text = timeMode
        }
        chessActivityListener = ChessActivityListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        chessActivityListener.onDestroy()
    }

    /** propagate user input (click on chessboard square) */
    fun onClickSquare(v: View){
        chessActivityListener.clickSquare(v)
    }

    /** highlight the textfield displaying information regarding the active player
     *  (active player = player that has to make a move)*/
    fun highlightActivePlayer(activePlayerColor: String){
        if(playerColor == "white"){
            val playerActive = playerColor == activePlayerColor
            tv_playernameW.setTextColor(getTextFieldColor(playerActive))
            tv_PlayerELOW.setTextColor(getTextFieldColor(playerActive))
            tv_PlayerTimeW.setTextColor(getTextFieldColor(playerActive))
            tv_opponentnameW.setTextColor(getTextFieldColor(!playerActive))
            tv_OpponentTimeW.setTextColor(getTextFieldColor(!playerActive))
            tv_OpponentELOW.setTextColor(getTextFieldColor(!playerActive))
        } else {
            val playerActive = playerColor == activePlayerColor
            tv_playernameB.setTextColor(getTextFieldColor(playerActive))
            tv_PlayerELOB.setTextColor(getTextFieldColor(playerActive))
            tv_PlayerTimeB.setTextColor(getTextFieldColor(playerActive))
            tv_opponentnameB.setTextColor(getTextFieldColor(!playerActive))
            tv_OpponentTimeB.setTextColor(getTextFieldColor(!playerActive))
            tv_OpponentELOB.setTextColor(getTextFieldColor(!playerActive))
        }
    }

    /** get color of textfield, depending on whether the player is active */
    fun getTextFieldColor(active:Boolean) : Int{
        return if(active) RED
        else WHITE
    }

    class CapturedPiece(val color: String, val name : String)
    /** draw all pieces captured by player as layer-drawable by placing
     * the pictures of the captured pieces on top of each other  */
    fun drawCapturedPiecesDrawable(color: String, capturedPieces: List<CapturedPiece>) {
        val layerList = mutableListOf<Drawable>()
        var inset = 0
        //create a insetDrawable for each captured piece with inset of 100 and add it to layerlist
        for(capturedPiece in capturedPieces){
            val drawable = this.resources.getDrawable(
                chessActivityListener.getDrawableFromName(capturedPiece.name,capturedPiece.color)
            )
            val insetDrawable = InsetDrawable(drawable,inset,0,0,0)
            inset += 100
            layerList.add(insetDrawable)
        }
        //create a layerDrawable from Layerlist and display it
        val layerDrawable = LayerDrawable(layerList.toTypedArray())
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