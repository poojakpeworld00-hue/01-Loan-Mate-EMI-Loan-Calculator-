package com.loanmate.loan.emicalculator.carloan.homelloan.loan.tabs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.homelloan.LoanTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnFragmentSettingsBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.LnItemSettingRowBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.LoanViewModelFactory
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.currency.LoanCurrencyActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.terms.LoanTermsActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.prefs.LanguageActivity

class LoanSettingsFragment : Fragment() {

    private var _binding: LnFragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoanSettingsViewModel by viewModels {
        LoanViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LnFragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.resourceProvider = { resId -> getString(resId) }

        // PREFERENCES
        bindRow(
            binding.rowCurrency,
            R.string.ln_settings_currency_title,
            R.string.ln_settings_currency_subtitle,
            R.drawable.ln_ic_set_currency,
            R.drawable.ln_bg_grid_green,
            R.color.ln_grid_green_fg
        ) { startActivity(Intent(requireContext(), LoanCurrencyActivity::class.java)) }

        bindRow(
            binding.rowLanguage,
            R.string.ln_settings_language_title,
            R.string.ln_settings_language_subtitle,
            R.drawable.ln_ic_set_language,
            R.drawable.ln_bg_grid_purple,
            R.color.ln_grid_purple_fg
        ) { startActivity(Intent(requireContext(), LanguageActivity::class.java)) }

        // SUPPORT
        bindRow(
            binding.rowRate,
            R.string.ln_settings_rate_title,
            R.string.ln_settings_rate_subtitle,
            R.drawable.ln_ic_set_rate,
            R.drawable.ln_bg_grid_yellow,
            R.color.ln_grid_yellow_fg
        ) { rate() }

        bindRow(
            binding.rowShare,
            R.string.ln_settings_share_title,
            R.string.ln_settings_share_subtitle,
            R.drawable.ln_ic_set_share,
            R.drawable.ln_bg_grid_pink,
            R.color.ln_grid_pink_fg
        ) { share() }

        // LEGAL
        bindRow(
            binding.rowDisclaimer,
            R.string.ln_settings_disclaimer_title,
            R.string.ln_settings_disclaimer_subtitle,
            R.drawable.ln_ic_set_disclaimer,
            R.drawable.ln_bg_grid_red,
            R.color.ln_grid_red_fg
        ) { startActivity(Intent(requireContext(), LoanTermsActivity::class.java)) }

        bindRow(
            binding.rowPrivacy,
            R.string.ln_settings_privacy_title,
            R.string.ln_settings_privacy_subtitle,
            R.drawable.ln_ic_set_privacy,
            R.drawable.ln_bg_grid_dark,
            R.color.ln_grid_dark_fg
        ) { open(base.privacyLink.ifBlank { getString(R.string.privacy_policy_url) }) }

        observe()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun observe() {
        viewModel.currencyLabel.observe(viewLifecycleOwner) {
            binding.rowCurrency.tvValue.text = it
        }
        viewModel.languageLabel.observe(viewLifecycleOwner) {
            binding.rowLanguage.tvValue.text = it
        }
    }

    private fun bindRow(
        b: LnItemSettingRowBinding,
        @StringRes titleRes: Int,
        @StringRes subtitleRes: Int,
        @DrawableRes iconRes: Int,
        @DrawableRes iconBgRes: Int,
        @ColorRes iconTintRes: Int,
        onClick: () -> Unit
    ) {
        b.tvTitle.setText(titleRes)
        b.tvSubtitle.setText(subtitleRes)
        b.ivIcon.setImageResource(iconRes)
        b.iconHolder.setBackgroundResource(iconBgRes)
        b.ivIcon.imageTintList =
            ContextCompat.getColorStateList(requireContext(), iconTintRes)
        b.tvValue.text = ""
        b.itemRoot.setOnClickListener { onClick() }
    }

    /* Share / rate / privacy behave as Loan Mate's own SettingsActivity. */

    private fun share() {
        val text = getString(R.string.share_message, getString(R.string.store_name), storeLink())
        Spots.kitMarkOwnLaunch() // no resume ad when the user comes back from the share sheet
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
    }

    private fun rate() {
        try {
            Spots.kitMarkOwnLaunch()
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${requireContext().packageName}")))
        } catch (_: ActivityNotFoundException) {
            open(storeLink())
        }
    }

    private fun open(url: String) {
        if (url.isBlank()) {
            Toast.makeText(requireContext(), R.string.link_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            Spots.kitMarkOwnLaunch()
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Spots.restoreOwnLaunch()
            Toast.makeText(requireContext(), R.string.link_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun storeLink() =
        base.storeLink.ifBlank { "https://play.google.com/store/apps/details?id=${requireContext().packageName}" }

    private val base get() = (requireActivity().application as LoanTwo).container.config.value.base

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
