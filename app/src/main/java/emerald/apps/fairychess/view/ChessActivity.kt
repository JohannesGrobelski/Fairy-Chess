package emerald.apps.fairychess.view

import android.graphics.Color.RED
import android.graphics.Color.WHITE
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import emerald.apps.fairychess.R
import emerald.apps.fairychess.controller.ChessActivityListener
import emerald.apps.fairychess.controller.MainActivityListener
import emerald.apps.fairychess.databinding.ActivityChessBlackPerspectiveBinding
import emerald.apps.fairychess.databinding.ActivityChessWhitePerspectiveBinding


class ChessActivity : AppCompatActivity() {
    private lateinit var chessActivityListener: ChessActivityListener
    private lateinit var playerColor : String

    // Safe accessor that casts to the correct type based on player color
    private lateinit var whiteBinding : ActivityChessWhitePerspectiveBinding
    private lateinit var blackBinding : ActivityChessBlackPerspectiveBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //get parameters from intent
        playerColor = this.intent.getStringExtra(MainActivityListener.playerColorExtra)!!
        val timeMode = this.intent.getStringExtra(MainActivityListener.gameTimeExtra)!!

        // Initialize the correct binding type
        if(playerColor == "white"){
            whiteBinding = ActivityChessWhitePerspectiveBinding.inflate(layoutInflater)
            setContentView(whiteBinding.root)
            whiteBinding.tvPlayerTimeW.text = timeMode
            whiteBinding.tvOpponentTimeW.text = timeMode
        } else {
            blackBinding = ActivityChessBlackPerspectiveBinding.inflate(layoutInflater)
            setContentView(blackBinding.root)
            blackBinding.tvPlayerTimeB.text = timeMode
            blackBinding.tvOpponentTimeB.text = timeMode
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
            whiteBinding.tvPlayernameW.setTextColor(getTextFieldColor(playerActive))
            whiteBinding.tvPlayerELOW.setTextColor(getTextFieldColor(playerActive))
            whiteBinding.tvPlayerTimeW.setTextColor(getTextFieldColor(playerActive))
            whiteBinding.tvOpponentnameW.setTextColor(getTextFieldColor(!playerActive))
            whiteBinding.tvOpponentTimeW.setTextColor(getTextFieldColor(!playerActive))
            whiteBinding.tvOpponentELOW.setTextColor(getTextFieldColor(!playerActive))
        } else {
            val playerActive = playerColor == activePlayerColor
            blackBinding.tvPlayernameB.setTextColor(getTextFieldColor(playerActive))
            blackBinding.tvPlayerELOB.setTextColor(getTextFieldColor(playerActive))
            blackBinding.tvPlayerTimeB.setTextColor(getTextFieldColor(playerActive))
            blackBinding.tvOpponentnameB.setTextColor(getTextFieldColor(!playerActive))
            blackBinding.tvOpponentTimeB.setTextColor(getTextFieldColor(!playerActive))
            blackBinding.tvOpponentELOB.setTextColor(getTextFieldColor(!playerActive))
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
                whiteBinding.ivCaptPiecesPlayerLine1W.setImageDrawable(layerDrawable)
            }else if(playerColor == "black"){
                blackBinding.ivCaptPiecesPlayerLine1.setImageDrawable(layerDrawable)
            }
        }
        else {
            if(playerColor == "white"){
                whiteBinding.ivCaptPiecesOpponentLine1W.setImageDrawable(layerDrawable)
            }else if(playerColor == "black"){
                blackBinding.ivCaptPiecesOpponentLine1.setImageDrawable(layerDrawable)
            }
        }
    }
}