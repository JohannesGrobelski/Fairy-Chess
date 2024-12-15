package emerald.apps.fairychess.view;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.snackbar.Snackbar;
import emerald.apps.fairychess.view.fragments.chessdesigner.*;
import emerald.apps.fairychess.PieceCreatorFragment;
import emerald.apps.fairychess.R;
import emerald.apps.fairychess.databinding.ActivityChessDesignerBinding;
import java.io.File;
import java.io.IOException;

public class ChessDesignerActivity extends AppCompatActivity {

    private ActivityChessDesignerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChessDesignerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.viewPager.setAdapter(new CDTabsPagerAdapter(this));

        // Set up BottomNavigationView
        binding.bottomNavigationCD.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_cd_board) {
                binding.viewPager.setCurrentItem(0);
            } else if (itemId == R.id.navigation_cd_pieces) {
                binding.viewPager.setCurrentItem(1);
            } else if (itemId == R.id.navigation_cd_rules) {
                binding.viewPager.setCurrentItem(2);
            } else if (itemId == R.id.navigation_cd_validation) {
                binding.viewPager.setCurrentItem(3);
            }

            return true;
        });

        // Sync ViewPager2 with BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.bottomNavigationCD.getMenu().getItem(position).setChecked(true);
            }
        });

        binding.fabSave.setOnClickListener(v -> saveConfiguration());
    }

    private void saveConfiguration() {
        File configDir = new File(getFilesDir(), "variants");
        try {
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            generateAndSaveConfigs(configDir);
            showSuccessMessage();
        } catch (IOException e) {
            showErrorMessage(e);
        }
    }

    private void generateAndSaveConfigs(File configDir) throws IOException {
        // Placeholder method for generating and saving configurations
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void showSuccessMessage() {
        Snackbar.make(binding.getRoot(), "Configuration saved successfully", Snackbar.LENGTH_SHORT).show();
    }

    private void showErrorMessage(IOException e) {
        Snackbar.make(binding.getRoot(), "Failed to save configuration: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
    }

    private static class CDTabsPagerAdapter extends FragmentStateAdapter {

        public CDTabsPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public int getItemCount() {
            return 4;
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new BoardCreatorFragment();
                case 1:
                    return new PieceCreatorFragment();
                case 2:
                    return new RuleCreatorFragment();
                case 3:
                    return new CreationValidationFragment();
                default:
                    throw new IllegalArgumentException("Invalid position " + position);
            }
        }
    }
}
