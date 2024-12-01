package emerald.apps.fairychess.view

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import emerald.apps.fairychess.R
import emerald.apps.fairychess.controller.MainActivityListener
import emerald.apps.fairychess.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var mainActivityListener : MainActivityListener

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainActivityListener = MainActivityListener(this, intent)

        // Set up ViewPager2 with fragments
        binding.viewPager.adapter = TabsPagerAdapter(this)

        // Set up BottomNavigationView
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_singleplayer -> binding.viewPager.currentItem = 0
                R.id.navigation_multiplayer -> binding.viewPager.currentItem = 1
                R.id.navigation_chesscreator -> binding.viewPager.currentItem = 2
                R.id.navigation_profile -> binding.viewPager.currentItem = 3
            }
            true
        }

        // Sync ViewPager2 with BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNavigation.menu.getItem(position).isChecked = true
            }
        })
    }

    override fun onResume() {
        super.onResume()
        mainActivityListener.onResume()
    }

    fun onClick(v: View){
        mainActivityListener.onClick(v)
    }

    fun showInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_info, null)

        // Get app version and set it
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        dialogView.findViewById<TextView>(R.id.tvVersion).text = "Version ${packageInfo.versionName}"

        AlertDialog.Builder(this, R.style.MaterialAlertDialog_Rounded)
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .show()
    }
}

private class TabsPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SingleplayerFragment()
            1 -> MultiplayerFragment()
            2 -> ChessCreatorFragment()
            3 -> ProfileFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}