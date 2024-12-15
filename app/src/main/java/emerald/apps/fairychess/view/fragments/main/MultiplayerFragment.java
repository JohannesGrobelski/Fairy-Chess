package emerald.apps.fairychess.view.fragments.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import emerald.apps.fairychess.R;
import emerald.apps.fairychess.view.MainActivity;

public class MultiplayerFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_multiplayer, container, false);

        Button searchGameButton = view.findViewById(R.id.btn_searchGame);
        if (searchGameButton != null) {
            searchGameButton.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).getMainActivityListener().onClick(v);
                }
            });
        }

        Button createGameButton = view.findViewById(R.id.btn_createGame);
        if (createGameButton != null) {
            createGameButton.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).getMainActivityListener().onClick(v);
                }
            });
        }

        return view;
    }
}
