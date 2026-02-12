package io.github.romanvht.mtgandroid.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.romanvht.mtgandroid.R
import io.github.romanvht.mtgandroid.databinding.ActivitySettingsBinding
import io.github.romanvht.mtgandroid.ui.fragments.SettingsFragment

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()
        }
    }
}
