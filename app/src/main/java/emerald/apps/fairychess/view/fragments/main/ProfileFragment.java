package emerald.apps.fairychess.view.fragments.main;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import emerald.apps.fairychess.R;
import emerald.apps.fairychess.model.multiplayer.MultiplayerDB;
import emerald.apps.fairychess.view.MainActivity;

public class ProfileFragment extends Fragment {
    private MainActivity mainActivity;
    private ProfileViewModel viewModel;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) {
            mainActivity = (MainActivity) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Set up the button click listener
        Button infoButton = view.findViewById(R.id.btn_info);
        if (infoButton != null) {
            infoButton.setOnClickListener(v -> {
                if (mainActivity != null) {
                    mainActivity.showInfoDialog();
                }
            });
        }

        // Observe the userName LiveData and update the TextView
        TextView playerStatsTextView = view.findViewById(R.id.tv_playerstats);
        viewModel.getUserName().observe(getViewLifecycleOwner(), userName -> {
            if (playerStatsTextView != null) {
                playerStatsTextView.setText(userName);
            }
        });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mainActivity = null;
    }
}
