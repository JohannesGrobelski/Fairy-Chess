package emerald.apps.fairychess.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import emerald.apps.fairychess.BoardCreatorFragment
import emerald.apps.fairychess.CreationValidationFragment
import emerald.apps.fairychess.PieceCreatorFragment
import emerald.apps.fairychess.R
import emerald.apps.fairychess.RuleCreatorFragment
import emerald.apps.fairychess.databinding.ActivityChessDesignerBinding
import java.io.File
import java.io.IOException

class ChessDesignerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChessDesignerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChessDesignerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = CDTabsPagerAdapter(this)

        // Set up BottomNavigationView
        binding.bottomNavigationCD.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_cd_board -> binding.viewPager.currentItem = 0
                R.id.navigation_cd_pieces -> binding.viewPager.currentItem = 1
                R.id.navigation_cd_rules -> binding.viewPager.currentItem = 2
                R.id.navigation_cd_validation -> binding.viewPager.currentItem = 3
            }
            true
        }

        // Sync ViewPager2 with BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNavigationCD.menu.getItem(position).isChecked = true
            }
        })

        binding.fabSave.setOnClickListener {
            saveConfiguration()
        }
    }

    private fun saveConfiguration() {
        val configDir = File(filesDir, "variants")
        try {
            configDir.mkdirs()
            generateAndSaveConfigs(configDir)
            showSuccessMessage()
        } catch (e: IOException) {
            showErrorMessage(e)
        }
    }

    private fun generateAndSaveConfigs(configDir: File) {
        TODO("Not yet implemented")
    }


    private fun showSuccessMessage() {
        Snackbar.make(binding.root, "Configuration saved successfully", Snackbar.LENGTH_SHORT).show()
    }

    private fun showErrorMessage(e: IOException) {
        Snackbar.make(binding.root, "Failed to save configuration: ${e.message}",
            Snackbar.LENGTH_LONG).show()
    }
}

private class CDTabsPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BoardCreatorFragment()
            1 -> PieceCreatorFragment()
            2 -> RuleCreatorFragment()
            3 -> CreationValidationFragment()
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }
}
