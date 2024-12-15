package emerald.apps.fairychess.view;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import emerald.apps.fairychess.controller.ChessActivityListener;
import emerald.apps.fairychess.controller.MainActivityListener;
import emerald.apps.fairychess.databinding.ActivityChessBlackPerspectiveBinding;
import emerald.apps.fairychess.databinding.ActivityChessWhitePerspectiveBinding;

import java.util.ArrayList;
import java.util.List;

public class ChessActivity extends AppCompatActivity {
    private ChessActivityListener chessActivityListener;
    private String playerColor;

    // Safe accessor that casts to the correct type based on player color
    private ActivityChessWhitePerspectiveBinding whiteBinding;
    private ActivityChessBlackPerspectiveBinding blackBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get parameters from intent
        playerColor = getIntent().getStringExtra(MainActivityListener.playerColorExtra);
        String timeMode = getIntent().getStringExtra(MainActivityListener.gameTimeExtra);

        // Initialize the correct binding type
        if ("white".equals(playerColor)) {
            whiteBinding = ActivityChessWhitePerspectiveBinding.inflate(getLayoutInflater());
            setContentView(whiteBinding.getRoot());
            whiteBinding.tvPlayerTimeW.setText(timeMode);
            whiteBinding.tvOpponentTimeW.setText(timeMode);
        } else {
            blackBinding = ActivityChessBlackPerspectiveBinding.inflate(getLayoutInflater());
            setContentView(blackBinding.getRoot());
            blackBinding.tvPlayerTimeB.setText(timeMode);
            blackBinding.tvOpponentTimeB.setText(timeMode);
        }

        chessActivityListener = new ChessActivityListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        chessActivityListener.onDestroy();
    }

    /** Propagate user input (click on chessboard square) */
    public void onClickSquare(View v) {
        chessActivityListener.clickSquare(v);
    }

    /** Highlight the textfield displaying information regarding the active player
     * (active player = player that has to make a move)
     */
    public void highlightActivePlayer(String activePlayerColor) {
        boolean playerActive = playerColor.equals(activePlayerColor);
        if ("white".equals(playerColor)) {
            whiteBinding.tvPlayernameW.setTextColor(getTextFieldColor(playerActive));
            whiteBinding.tvPlayerELOW.setTextColor(getTextFieldColor(playerActive));
            whiteBinding.tvPlayerTimeW.setTextColor(getTextFieldColor(playerActive));
            whiteBinding.tvOpponentnameW.setTextColor(getTextFieldColor(!playerActive));
            whiteBinding.tvOpponentTimeW.setTextColor(getTextFieldColor(!playerActive));
            whiteBinding.tvOpponentELOW.setTextColor(getTextFieldColor(!playerActive));
        } else {
            blackBinding.tvPlayernameB.setTextColor(getTextFieldColor(playerActive));
            blackBinding.tvPlayerELOB.setTextColor(getTextFieldColor(playerActive));
            blackBinding.tvPlayerTimeB.setTextColor(getTextFieldColor(playerActive));
            blackBinding.tvOpponentnameB.setTextColor(getTextFieldColor(!playerActive));
            blackBinding.tvOpponentTimeB.setTextColor(getTextFieldColor(!playerActive));
            blackBinding.tvOpponentELOB.setTextColor(getTextFieldColor(!playerActive));
        }
    }

    /** Get color of textfield, depending on whether the player is active */
    public int getTextFieldColor(boolean active) {
        return active ? Color.RED : Color.WHITE;
    }

    public static class CapturedPiece {
        public final String color;
        public final String name;

        public CapturedPiece(String color, String name) {
            this.color = color;
            this.name = name;
        }
    }

    /** Draw all pieces captured by player as a layer-drawable by placing
     * the pictures of the captured pieces on top of each other
     */
    public void drawCapturedPiecesDrawable(String color, @NonNull List<CapturedPiece> capturedPieces) {
        List<Drawable> layerList = new ArrayList<>();
        int inset = 0;

        // Create an insetDrawable for each captured piece with an inset of 100 and add it to layerList
        for (CapturedPiece capturedPiece : capturedPieces) {
            Drawable drawable = getResources().getDrawable(
                chessActivityListener.getDrawableFromName(capturedPiece.name, capturedPiece.color)
            );
            InsetDrawable insetDrawable = new InsetDrawable(drawable, inset, 0, 0, 0);
            inset += 100;
            layerList.add(insetDrawable);
        }

        // Create a LayerDrawable from the layerList and display it
        LayerDrawable layerDrawable = new LayerDrawable(layerList.toArray(new Drawable[0]));

        if (color.equals(playerColor)) {
            if ("white".equals(playerColor)) {
                whiteBinding.ivCaptPiecesPlayerLine1W.setImageDrawable(layerDrawable);
            } else if ("black".equals(playerColor)) {
                blackBinding.ivCaptPiecesPlayerLine1.setImageDrawable(layerDrawable);
            }
        } else {
            if ("white".equals(playerColor)) {
                whiteBinding.ivCaptPiecesOpponentLine1W.setImageDrawable(layerDrawable);
            } else if ("black".equals(playerColor)) {
                blackBinding.ivCaptPiecesOpponentLine1.setImageDrawable(layerDrawable);
            }
        }
    }
}
