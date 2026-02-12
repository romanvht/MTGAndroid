package io.github.romanvht.mtgandroid.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.romanvht.mtgandroid.R
import io.github.romanvht.mtgandroid.utils.FormatUtils
import io.github.romanvht.mtgandroid.utils.MtgWrapper
import io.github.romanvht.mtgandroid.utils.PreferencesUtils
import io.github.romanvht.mtgandroid.utils.ValidationUtils
import androidx.core.net.toUri
import io.github.romanvht.mtgandroid.BuildConfig

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        setupGenerateSecretButton()
        setupPreferenceSummaries()
        setupValidation()

        findPreference<Preference>("app_version")?.summary = BuildConfig.VERSION_NAME

        findPreference<Preference>("github_link")?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "https://github.com/romanvht/MTGAndroid".toUri()
            }
            startActivity(intent)
            true
        }
    }

    private fun setupGenerateSecretButton() {
        findPreference<Preference>("generate_secret")?.setOnPreferenceClickListener {
            val domain = PreferencesUtils.getDomain(requireContext())

            if (!ValidationUtils.isValidDomain(domain)) {
                Toast.makeText(
                    requireContext(),
                    R.string.error_empty_domain,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnPreferenceClickListener true
            }

            val cleanDomain = FormatUtils.cleanDomain(domain)
            val secret = MtgWrapper.generateSecret(requireContext(), cleanDomain)

            if (secret != null) {
                PreferencesUtils.setSecret(requireContext(), secret)

                findPreference<EditTextPreference>("secret")?.text = secret

                Toast.makeText(
                    requireContext(),
                    R.string.secret_generated,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    R.string.error_generate_secret,
                    Toast.LENGTH_SHORT
                ).show()
            }

            true
        }
    }

    private fun setupPreferenceSummaries() {
        findPreference<EditTextPreference>("domain")?.apply {
            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
            setOnPreferenceChangeListener { _, newValue ->
                ValidationUtils.isValidDomain(newValue as String).also { isValid ->
                    if (!isValid) {
                        Toast.makeText(
                            requireContext(),
                            R.string.error_domain_format,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        generateSecretForDomain(FormatUtils.cleanDomain(newValue))
                    }
                }
            }
        }

        findPreference<EditTextPreference>("ip_address")?.apply {
            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
            setOnPreferenceChangeListener { _, newValue ->
                ValidationUtils.isValidIpAddress(newValue as String).also { isValid ->
                    if (!isValid) {
                        Toast.makeText(
                            requireContext(),
                            R.string.error_invalid_ip,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        findPreference<EditTextPreference>("port")?.apply {
            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
            setOnBindEditTextListener { editText ->
                editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            setOnPreferenceChangeListener { _, newValue ->
                val normalizedPort = FormatUtils.normalizePort(newValue as String)

                when {
                    !ValidationUtils.isValidPort(normalizedPort) -> {
                        Toast.makeText(
                            requireContext(),
                            R.string.error_invalid_port,
                            Toast.LENGTH_SHORT
                        ).show()
                        false
                    }
                    !ValidationUtils.isNonPrivilegedPort(normalizedPort) -> {
                        Toast.makeText(
                            requireContext(),
                            R.string.warning_privileged_port,
                            Toast.LENGTH_LONG
                        ).show()
                        true
                    }
                    else -> true
                }
            }
        }

        findPreference<EditTextPreference>("secret")?.apply {
            setOnBindEditTextListener { editText ->
                editText.isSingleLine = false
                editText.setLines(3)
            }
            setSummaryProvider { preference ->
                val secret = (preference as EditTextPreference).text
                secret?.ifEmpty { getString(R.string.error_empty_secret) }
                    ?: getString(R.string.error_empty_secret)
            }
        }
    }

    private fun generateSecretForDomain(cleanDomain: String) {
        val secret = MtgWrapper.generateSecret(requireContext(), cleanDomain)

        if (secret != null) {
            PreferencesUtils.setSecret(requireContext(), secret)
            findPreference<EditTextPreference>("secret")?.text = secret

            Toast.makeText(
                requireContext(),
                R.string.secret_generated,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                requireContext(),
                R.string.error_generate_secret,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupValidation() {
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "domain" -> {
                    val domain = PreferencesUtils.getDomain(requireContext())
                    if (domain.isNotEmpty()) {
                        PreferencesUtils.setDomain(
                            requireContext(),
                            FormatUtils.cleanDomain(domain)
                        )
                    }
                }
                "ip_address" -> {
                    val ip = PreferencesUtils.getIpAddress(requireContext())
                    PreferencesUtils.setIpAddress(
                        requireContext(),
                        FormatUtils.normalizeIpAddress(ip)
                    )
                }
                "port" -> {
                    val port = PreferencesUtils.getPort(requireContext())
                    PreferencesUtils.setPort(
                        requireContext(),
                        FormatUtils.normalizePort(port)
                    )
                }
            }
        }
    }
}
