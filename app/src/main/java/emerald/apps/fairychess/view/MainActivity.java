package emerald.apps.fairychess.view;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import emerald.apps.fairychess.R;
import emerald.apps.fairychess.controller.MainActivityListener;
import emerald.apps.fairychess.databinding.ActivityMainBinding;
import emerald.apps.fairychess.view.fragments.main.ChessCreatorFragment;
import emerald.apps.fairychess.view.fragments.main.MultiplayerFragment;
import emerald.apps.fairychess.view.fragments.main.ProfileFragment;
import emerald.apps.fairychess.view.fragments.main.SingleplayerFragment;

public class MainActivity extends AppCompatActivity {

    private MainActivityListener mainActivityListener;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainActivityListener = new MainActivityListener(this, getIntent());

        // Set up ViewPager2 with fragments
        binding.viewPager.setAdapter(new TabsPagerAdapter(this));

        // Set up BottomNavigationView
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_singleplayer) {
                binding.viewPager.setCurrentItem(0);
            } else if (item.getItemId() == R.id.navigation_multiplayer) {
                binding.viewPager.setCurrentItem(1);
            } else if (item.getItemId() == R.id.navigation_chesscreator) {
                binding.viewPager.setCurrentItem(2);
            } else if (item.getItemId() == R.id.navigation_profile) {
                binding.viewPager.setCurrentItem(3);
            }
            return true;
        });

        // Sync ViewPager2 with BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.bottomNavigation.getMenu().getItem(position).setChecked(true);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainActivityListener.onResume();
    }

    public void onClick(View v) {
        mainActivityListener.onClick(v);
    }

    public MainActivityListener getMainActivityListener(){
        return this.mainActivityListener;
    }

    public void showInfoDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_info, null);

        // Get app version and set it
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            ((TextView) dialogView.findViewById(R.id.tvVersion)).setText("Version " + versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        AlertDialog infoDialog = new AlertDialog.Builder(this, R.style.BlurDialogTheme)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .create();

        // Apply background and other window attributes
        if (infoDialog.getWindow() != null) {
            infoDialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
            infoDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            mainActivityListener.setBlurRadius(15);
        }

        infoDialog.show();
    }

    private static class TabsPagerAdapter extends FragmentStateAdapter {

        public TabsPagerAdapter(FragmentActivity fa) {
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
                    return new SingleplayerFragment();
                case 1:
                    return new MultiplayerFragment();
                case 2:
                    return new ChessCreatorFragment();
                case 3:
                    return new ProfileFragment();
                default:
                    throw new IllegalArgumentException("Invalid position " + position);
            }
        }
    }
}
